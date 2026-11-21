import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';
import 'formulario_de_orden.dart';

String _fecha(DateTime? d) => d == null
    ? '—'
    : '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

/// HU-174 · Una orden de compra: de cada línea lo pedido, lo recibido y lo que falta (C5), y lo que se
/// puede hacer según su estado — editar y confirmar el borrador, aprobarla, mandarle el PDF al
/// proveedor (C4) o recibir la mercancía.
class PantallaOrdenDeCompra extends ConsumerStatefulWidget {
  const PantallaOrdenDeCompra({super.key, required this.ordenId, this.onRecibir});

  final String ordenId;
  final ValueChanged<String>? onRecibir;

  @override
  ConsumerState<PantallaOrdenDeCompra> createState() => _PantallaOrdenDeCompraState();
}

class _PantallaOrdenDeCompraState extends ConsumerState<PantallaOrdenDeCompra> {
  OrdenDeCompraVista? _orden;
  String? _error;
  bool _ocupado = false;

  RepositorioDeOrdenes get _repo => ref.read(repositorioDeOrdenesProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() => _hacer(() => _repo.ver(widget.ordenId));

  /// Ejecuta una acción que devuelve la orden actualizada y muestra su rechazo si lo hay.
  Future<void> _hacer(Future<OrdenDeCompraVista> Function() accion, {String? aviso}) async {
    setState(() {
      _ocupado = true;
      _error = null;
    });
    try {
      final o = await accion();
      if (!mounted) return;
      setState(() {
        _orden = o;
        _ocupado = false;
      });
      if (aviso != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(aviso)));
      }
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _ocupado = false;
        _error = switch (fallo) {
          SinPermiso() => 'No tienes permiso para hacer esto con la orden.',
          ErrorDeRed() => 'No hay conexión. Intenta de nuevo.',
          _ => fallo.mensaje,
        };
      });
    }
  }

  Future<void> _editar() async {
    final guardada = await Navigator.of(context).push<OrdenDeCompraVista>(
      MaterialPageRoute(builder: (_) => FormularioDeOrden(orden: _orden)),
    );
    if (guardada != null && mounted) setState(() => _orden = guardada);
  }

  Future<void> _confirmar() => _hacer(() => _repo.confirmar(widget.ordenId)).then((_) {
        final o = _orden;
        if (o == null || !mounted || _error != null) return;
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(o.estado == 'APROBADA'
              ? 'Orden confirmada y aprobada.'
              : 'Orden confirmada: queda esperando aprobación.'),
        ));
      });

  /// HU-176 · Pide el motivo y anula la orden o la cierra con faltante.
  Future<void> _terminar({required bool cerrar}) async {
    final o = _orden!;
    final motivo = await showDialog<String>(
      context: context,
      builder: (_) => _DialogoDeMotivo(
        titulo: cerrar ? 'Cerrar ${o.numero} con faltante' : 'Anular ${o.numero}',
        explicacion: cerrar
            ? 'La orden no recibirá más mercancía. Lo que faltó queda a la vista en sus líneas.'
            : 'La orden queda anulada y no se le podrá recibir nada.',
        accion: cerrar ? 'Cerrar orden' : 'Anular orden',
      ),
    );
    if (motivo == null || !mounted) return;
    await _hacer(
      () => cerrar ? _repo.cerrar(o.id, motivo) : _repo.anular(o.id, motivo),
      aviso: cerrar ? 'La orden ${o.numero} quedó cerrada.' : 'La orden ${o.numero} quedó anulada.',
    );
  }

  /// HU-174 C4 · El PDF sale por «Compartir» (WhatsApp, correo…) y la orden pasa a ENVIADA.
  Future<void> _enviarAlProveedor() async {
    final o = _orden!;
    setState(() {
      _ocupado = true;
      _error = null;
    });
    try {
      final pdf = await _repo.pdf(o.id);
      await ref.read(compartidorDePdfProvider).compartir(pdf, 'orden-de-compra-${o.numero}.pdf');
      if (!mounted) return;
      if (o.estado == 'APROBADA') {
        await _hacer(() => _repo.enviar(o.id), aviso: 'La orden ${o.numero} quedó como enviada al proveedor.');
      } else {
        setState(() => _ocupado = false);
      }
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _ocupado = false;
        _error = fallo.mensaje;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final o = _orden;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(o == null ? 'Orden de compra' : 'Orden ${o.numero}',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: o == null
          ? Center(
              child: _error == null
                  ? const CircularProgressIndicator()
                  : Padding(padding: const EdgeInsets.all(24), child: AvisoDeError(_error!)),
            )
          : SingleChildScrollView(
              padding: const EdgeInsets.all(14),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 900),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                    _cabecera(o),
                    if (o.motivoAnulacion != null || o.motivoCierre != null) _fin(o),
                    const SizedBox(height: 12),
                    for (final l in o.lineas) _linea(l),
                    const SizedBox(height: 12),
                    _totales(o),
                    const SizedBox(height: 12),
                    if (_error != null) AvisoDeError(_error!, key: const Key('detalle-error')),
                    ..._acciones(o),
                  ]),
                ),
              ),
            ),
    );
  }

  Widget _cabecera(OrdenDeCompraVista o) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(
            child: Text(o.proveedorNombre ?? 'Proveedor',
                style: RegentaType.seccion.copyWith(fontSize: 18, color: RegentaColors.inkFuerte)),
          ),
          ChipDeEstado(
            key: const Key('detalle-estado'),
            texto: o.estadoVisible,
            fondo: o.esperaAprobacion ? RegentaColors.warnSoft : RegentaColors.sunken,
            tinta: o.esperaAprobacion ? RegentaColors.warn : RegentaColors.ink2,
          ),
        ]),
        const SizedBox(height: 4),
        Text('Emitida ${_fecha(o.fechaEmision)} · para el ${_fecha(o.fechaEsperada)}'
            '${o.aprobacionAutomatica ? ' · aprobada sola' : ''}',
            style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
      ]);

  Widget _fin(OrdenDeCompraVista o) {
    final anulada = o.motivoAnulacion != null;
    return Container(
      key: const Key('detalle-fin'),
      margin: const EdgeInsets.only(top: 10),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: RegentaColors.sunken,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Text(
        '${anulada ? 'Anulada' : 'Cerrada con faltante'} el ${_fecha(anulada ? o.anuladaEn : o.cerradaEn)}: '
        '${anulada ? o.motivoAnulacion : o.motivoCierre}',
        style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2),
      ),
    );
  }

  Widget _linea(LineaDeOrdenVista l) {
    final falta = l.faltante > 0;
    return Container(
      key: Key('detalle-linea-${l.productoId}'),
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(l.nombre, style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
            Text(
              [if (l.codigoProveedor != null) 'cód. ${l.codigoProveedor}', '${formatearPesos(l.costoUnitario)} c/u',
                    if (l.impuestoPct > 0) 'IVA ${l.impuestoPct}%']
                  .join(' · '),
              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted),
            ),
          ]),
        ),
        Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Text('pedido ${l.cantidadPedida} · recibido ${l.cantidadRecibida}',
              key: Key('detalle-avance-${l.productoId}'),
              style: RegentaType.codigo.copyWith(fontSize: 11.5, color: RegentaColors.ink)),
          Text(falta ? 'faltan ${l.faltante}' : 'completo',
              key: Key('detalle-falta-${l.productoId}'),
              style: RegentaType.codigo.copyWith(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w700,
                  color: falta ? RegentaColors.warn : RegentaColors.ok)),
        ]),
      ]),
    );
  }

  Widget _totales(OrdenDeCompraVista o) {
    Widget fila(String e, num v, {bool fuerte = false}) => Row(children: [
          Expanded(child: Text(e, style: RegentaType.cuerpo.copyWith(fontWeight: fuerte ? FontWeight.w800 : null))),
          Text(formatearPesos(v),
              style: RegentaType.codigo.copyWith(fontWeight: fuerte ? FontWeight.w800 : FontWeight.w600)),
        ]);
    return Column(children: [
      fila('Base', o.subtotal),
      fila('IVA', o.impuestoTotal),
      if (o.flete > 0) fila('Flete', o.flete),
      fila('Total', o.total, fuerte: true),
    ]);
  }

  List<Widget> _acciones(OrdenDeCompraVista o) {
    Widget boton(String clave, String texto, IconData icono, VoidCallback alTocar, {bool principal = false}) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: SizedBox(
            height: RegentaSpacing.hitTarget,
            child: principal
                ? FilledButton.icon(
                    key: Key(clave), onPressed: _ocupado ? null : alTocar, icon: Icon(icono, size: 18), label: Text(texto))
                : OutlinedButton.icon(
                    key: Key(clave), onPressed: _ocupado ? null : alTocar, icon: Icon(icono, size: 18), label: Text(texto)),
          ),
        );
    return [
      if (o.editable) boton('detalle-editar', 'Editar líneas', Icons.edit_outlined, () => unawaited(_editar())),
      if (o.editable && !o.esperaAprobacion)
        boton('detalle-confirmar', 'Confirmar orden', Icons.check_circle_outline, () => unawaited(_confirmar()),
            principal: true),
      if (o.esperaAprobacion)
        boton('detalle-aprobar', 'Aprobar', Icons.verified_outlined,
            () => unawaited(_hacer(() => _repo.aprobar(o.id), aviso: 'Orden aprobada.')),
            principal: true),
      if (o.enCurso)
        boton('detalle-pdf', o.estado == 'APROBADA' ? 'Enviar al proveedor (PDF)' : 'Compartir PDF otra vez',
            Icons.picture_as_pdf_outlined, () => unawaited(_enviarAlProveedor()),
            principal: o.estado == 'APROBADA'),
      if (o.enCurso && widget.onRecibir != null)
        boton('detalle-recibir', 'Recibir mercancía', Icons.inventory_outlined, () => widget.onRecibir!(o.id),
            principal: o.estado != 'APROBADA'),
      if (o.cerrable)
        boton('detalle-cerrar', 'Cerrar con faltante', Icons.lock_outline, () => unawaited(_terminar(cerrar: true))),
      if (o.anulable)
        boton('detalle-anular', 'Anular orden', Icons.block, () => unawaited(_terminar(cerrar: false))),
    ];
  }
}

/// HU-176 · El motivo es obligatorio para anular o cerrar.
class _DialogoDeMotivo extends StatefulWidget {
  const _DialogoDeMotivo({required this.titulo, required this.explicacion, required this.accion});

  final String titulo;
  final String explicacion;
  final String accion;

  @override
  State<_DialogoDeMotivo> createState() => _DialogoDeMotivoState();
}

class _DialogoDeMotivoState extends State<_DialogoDeMotivo> {
  final TextEditingController _motivo = TextEditingController();

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final listo = _motivo.text.trim().isNotEmpty;
    return AlertDialog(
      title: Text(widget.titulo, style: RegentaType.seccion),
      content: SizedBox(
        width: 420,
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(widget.explicacion, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
          const SizedBox(height: 12),
          TextField(
            key: const Key('motivo-fin'),
            controller: _motivo,
            autofocus: true,
            maxLength: 200,
            maxLines: 2,
            onChanged: (_) => setState(() {}),
            style: estiloDeCampo,
            decoration: decoracionDeCampo(hint: 'Motivo (obligatorio)'),
          ),
        ]),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Volver')),
        FilledButton(
          key: const Key('motivo-confirmar'),
          onPressed: listo ? () => Navigator.of(context).pop(_motivo.text.trim()) : null,
          child: Text(widget.accion),
        ),
      ],
    );
  }
}
