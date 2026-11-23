import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../devoluciones/pantalla_recepciones.dart';
import '../ordenes/datos.dart';
import '../ordenes/formulario_de_orden.dart';
import '../ordenes/pantalla_orden.dart';
import '../informes/datos.dart';
import '../informes/pantalla_informes_de_compras.dart';
import '../proveedores/ui/piezas.dart';
import '../recepcion/pantalla_recepciones_por_enviar.dart';
import '../recepcion/recepciones_por_enviar.dart';
import '../sugerencias/pantalla_sugerencias.dart';
import 'formato.dart';

/// Las órdenes de compra del negocio (HU-174 C1): número, proveedor, estado, fecha esperada y total,
/// con filtros por estado y por proveedor. Tocar una abre su detalle; «Nueva orden» la arma.
class PantallaCompras extends ConsumerStatefulWidget {
  const PantallaCompras({
    super.key,
    required this.onOrdenSeleccionada,
    this.onProveedores,
    this.onCompraDirecta,
    this.onCuentasPorPagar,
    this.onConfiguracion,
  });

  /// Recibir la mercancía de una orden (abre la recepción).
  final ValueChanged<String> onOrdenSeleccionada;

  /// HU-166 · Abre el listado de proveedores. Nulo: el botón no aparece.
  final VoidCallback? onProveedores;

  /// HU-167 · Registra una compra que llegó con su factura, sin orden. Nulo: el botón no aparece.
  final VoidCallback? onCompraDirecta;

  /// HU-170 · Lo que se le debe a los proveedores. Nulo: el atajo no aparece.
  final VoidCallback? onCuentasPorPagar;

  /// HU-173 · Aprobación de órdenes y tolerancia al recibir. Nulo: el atajo no aparece.
  final VoidCallback? onConfiguracion;

  @override
  ConsumerState<PantallaCompras> createState() => _PantallaComprasState();
}

/// Los filtros de estado del listado.
enum FiltroDeOrdenes {
  todas('Todas'),
  borradores('Borradores'),
  porAprobar('Por aprobar'),
  enCurso('En curso'),
  cerradas('Cerradas');

  const FiltroDeOrdenes(this.etiqueta);

  final String etiqueta;

  bool admite(OrdenDeCompraVista o) => switch (this) {
        FiltroDeOrdenes.todas => true,
        FiltroDeOrdenes.borradores => o.estado == 'BORRADOR' && !o.esperaAprobacion,
        FiltroDeOrdenes.porAprobar => o.esperaAprobacion,
        FiltroDeOrdenes.enCurso => o.enCurso,
        FiltroDeOrdenes.cerradas => o.estado == 'RECIBIDA' || o.estado == 'CERRADA' || o.estado == 'ANULADA',
      };
}

class _PantallaComprasState extends ConsumerState<PantallaCompras> {
  List<OrdenDeCompraVista>? _ordenes;
  String? _error;
  FiltroDeOrdenes _filtro = FiltroDeOrdenes.todas;
  String? _proveedor;

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() async {
    setState(() => _error = null);
    try {
      final ordenes = await ref.read(repositorioDeOrdenesProvider).listar();
      if (mounted) setState(() => _ordenes = ordenes);
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _ordenes ??= const [];
        _error = fallo is ErrorDeRed ? 'No hay conexión: no se pudieron cargar las órdenes.' : fallo.mensaje;
      });
    }
  }

  Future<void> _abrir(String ordenId) async {
    await Navigator.of(context).push<void>(MaterialPageRoute(
      builder: (_) => PantallaOrdenDeCompra(ordenId: ordenId, onRecibir: widget.onOrdenSeleccionada),
    ));
    if (mounted) unawaited(_cargar());
  }

  Future<void> _nueva() async {
    final creada = await Navigator.of(context).push<OrdenDeCompraVista>(
      MaterialPageRoute(builder: (_) => const FormularioDeOrden()),
    );
    if (!mounted) return;
    unawaited(_cargar());
    if (creada != null) unawaited(_abrir(creada.id));
  }

  /// HU-181 · Las sugerencias; la orden que se cree desde ahí se abre al volver.
  Future<void> _sugerencias() async {
    String? creada;
    await Navigator.of(context).push<void>(MaterialPageRoute(
      builder: (rutaContext) => PantallaSugerencias(onOrdenCreada: (id) {
        creada = id;
        Navigator.of(rutaContext).pop();
      }),
    ));
    if (!mounted) return;
    unawaited(_cargar());
    if (creada != null) unawaited(_abrir(creada!));
  }

  @override
  Widget build(BuildContext context) {
    final todas = _ordenes ?? const <OrdenDeCompraVista>[];
    final proveedores = <String, String>{
      for (final o in todas) o.proveedorId: o.proveedorNombre ?? 'Proveedor',
    };
    final visibles = todas
        .where((o) => _filtro.admite(o) && (_proveedor == null || o.proveedorId == _proveedor))
        .toList();

    // En web van con texto; en el celular, solo el ícono (con su nombre al dejarlo presionado),
    // para que quepan en la barra.
    List<Widget> acciones({required bool conTexto}) => <Widget>[
          if (widget.onCompraDirecta != null)
            _Atajo(
              clave: const Key('compras-compra-directa'),
              texto: 'Compra directa',
              icono: Icons.receipt_long_outlined,
              conTexto: conTexto,
              onPressed: widget.onCompraDirecta!,
            ),
          // HU-181 · Qué reponer, agrupado por proveedor, para volverlo orden.
          _Atajo(
            clave: const Key('compras-sugerencias'),
            texto: 'Sugerencias',
            icono: Icons.lightbulb_outline,
            conTexto: conTexto,
            onPressed: () => unawaited(_sugerencias()),
          ),
          // HU-178 · Lo recibido, para devolverle al proveedor o anular.
          _Atajo(
            clave: const Key('compras-recepciones'),
            texto: 'Recepciones',
            icono: Icons.assignment_return_outlined,
            conTexto: conTexto,
            onPressed: () => unawaited(Navigator.of(context)
                .push<void>(MaterialPageRoute(builder: (_) => const PantallaRecepciones()))),
          ),
          // HU-183 · Cuánto se compra, a quién y cómo cambian los costos.
          if (ref.watch(puedeVerInformesDeComprasProvider))
            _Atajo(
              clave: const Key('compras-informes'),
              texto: 'Informes',
              icono: Icons.insights_outlined,
              conTexto: conTexto,
              onPressed: () => unawaited(Navigator.of(context)
                  .push<void>(MaterialPageRoute(builder: (_) => const PantallaInformesDeCompras()))),
            ),
          if (widget.onCuentasPorPagar != null)
            _Atajo(
              clave: const Key('compras-cuentas-por-pagar'),
              texto: 'Cuentas por pagar',
              icono: Icons.account_balance_wallet_outlined,
              conTexto: conTexto,
              onPressed: widget.onCuentasPorPagar!,
            ),
          if (widget.onProveedores != null)
            _Atajo(
              clave: const Key('compras-proveedores'),
              texto: 'Proveedores',
              icono: Icons.store_mall_directory_outlined,
              conTexto: conTexto,
              onPressed: widget.onProveedores!,
            ),
          if (widget.onConfiguracion != null)
            IconButton(
              key: const Key('compras-configuracion'),
              tooltip: 'Configuración de compras',
              icon: const Icon(Icons.tune, color: RegentaColors.ink),
              onPressed: widget.onConfiguracion,
            ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () => unawaited(_cargar()),
          ),
        ];

    return LayoutBuilder(builder: (context, afuera) {
      final escritorio = afuera.maxWidth >= kBreakpointEscritorio;
      return Scaffold(
        backgroundColor: RegentaColors.paper,
        // Encabezado común en web (design/comportamiento/Encabezado.md); en móvil, la barra.
        appBar: escritorio
            ? EncabezadoDeSeccion(
                subtitulo: 'Pedidos a proveedores y recepción de mercancía',
                titulo: 'Órdenes de Compra',
                acciones: acciones(conTexto: true),
              )
            : AppBar(
                backgroundColor: RegentaColors.surface,
                surfaceTintColor: RegentaColors.surface,
                elevation: 0,
                shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                title: Text('Órdenes de Compra',
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
                actions: [...acciones(conTexto: false), const SizedBox(width: 8)],
              ),
        floatingActionButton: FloatingActionButton.extended(
          key: const Key('compras-nueva-orden'),
          onPressed: () => unawaited(_nueva()),
          backgroundColor: RegentaColors.accent,
          foregroundColor: Colors.white,
          icon: const Icon(Icons.add),
          label: const Text('Nueva orden'),
        ),
        body: Column(children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
            decoration: const BoxDecoration(
              color: RegentaColors.surface,
              border: Border(bottom: BorderSide(color: RegentaColors.line)),
            ),
            child: Wrap(spacing: 10, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  for (final f in FiltroDeOrdenes.values) ...[
                    ChoiceChip(
                      key: Key('ordenes-filtro-${f.name}'),
                      label: Text(f.etiqueta),
                      selected: _filtro == f,
                      onSelected: (_) => setState(() => _filtro = f),
                    ),
                    const SizedBox(width: 6),
                  ],
                ]),
              ),
              SizedBox(
                width: 260,
                child: DropdownButtonFormField<String?>(
                  key: const Key('ordenes-filtro-proveedor'),
                  initialValue: _proveedor,
                  isExpanded: true,
                  decoration: decoracionDeCampo(),
                  items: [
                    const DropdownMenuItem<String?>(value: null, child: Text('Todos los proveedores')),
                    for (final e in proveedores.entries)
                      DropdownMenuItem<String?>(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)),
                  ],
                  onChanged: (v) => setState(() => _proveedor = v),
                ),
              ),
            ]),
          ),
          // HU-182 · Lo recibido en el celular que todavía no entró.
          _AvisoPorEnviar(
            recepciones: ref.watch(recepcionesPorEnviarProvider).valueOrNull ?? const [],
            onVer: () => unawaited(Navigator.of(context).push<void>(
                MaterialPageRoute(builder: (_) => const PantallaRecepcionesPorEnviar()))),
          ),
          if (_error != null)
            Padding(padding: const EdgeInsets.fromLTRB(14, 10, 14, 0), child: AvisoDeError(_error!)),
          Expanded(
            child: _ordenes == null
                ? const Center(child: CircularProgressIndicator())
                : visibles.isEmpty
                    ? Center(
                        child: Text(
                          todas.isEmpty ? 'Todavía no hay órdenes de compra.' : 'Ninguna orden con estos filtros.',
                          key: const Key('ordenes-vacio'),
                          style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                        ),
                      )
                    : ListView.builder(
                        key: const Key('ordenes-lista'),
                        padding: const EdgeInsets.fromLTRB(14, 6, 14, 90),
                        itemCount: visibles.length,
                        itemBuilder: (context, i) => _FilaOrden(orden: visibles[i], onTap: () => _abrir(visibles[i].id)),
                      ),
          ),
        ]),
      );
    });
  }
}

class _FilaOrden extends StatelessWidget {
  const _FilaOrden({required this.orden, required this.onTap});

  final OrdenDeCompraVista orden;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final o = orden;
    final fecha = o.fechaEsperada == null
        ? 'sin fecha'
        : 'para el ${o.fechaEsperada!.day.toString().padLeft(2, '0')}/${o.fechaEsperada!.month.toString().padLeft(2, '0')}';
    return InkWell(
      key: Key('orden-fila-${o.id}'),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('${o.numero} · ${o.proveedorNombre ?? 'Proveedor'}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Row(children: [
                ChipDeEstado(
                  texto: o.estadoVisible,
                  fondo: o.esperaAprobacion ? RegentaColors.warnSoft : RegentaColors.sunken,
                  tinta: o.esperaAprobacion ? RegentaColors.warn : RegentaColors.ink2,
                ),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(fecha,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                ),
              ]),
            ]),
          ),
          Text(formatearPesos(o.total),
              style: RegentaType.codigo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          const SizedBox(width: 4),
          const Icon(Icons.chevron_right, color: RegentaColors.faint),
        ]),
      ),
    );
  }
}

/// HU-182 · «2 recepciones por enviar · 1 con error». Sin ninguna, no se ve.
class _AvisoPorEnviar extends StatelessWidget {
  const _AvisoPorEnviar({required this.recepciones, required this.onVer});

  final List<RecepcionPorEnviar> recepciones;
  final VoidCallback onVer;

  @override
  Widget build(BuildContext context) {
    if (recepciones.isEmpty) return const SizedBox.shrink();
    final conError = recepciones.where((r) => r.conError).length;
    final porEnviar = recepciones.length - conError;
    final partes = [
      if (porEnviar > 0) porEnviar == 1 ? '1 recepción por enviar' : '$porEnviar recepciones por enviar',
      if (conError > 0) conError == 1 ? '1 con error' : '$conError con error',
    ];
    return Material(
      color: conError > 0 ? RegentaColors.critSoft : RegentaColors.warnSoft,
      child: InkWell(
        key: const Key('compras-por-enviar'),
        onTap: onVer,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 10, 8, 10),
          child: Row(children: [
            Icon(conError > 0 ? Icons.error_outline : Icons.cloud_off_outlined,
                size: 18, color: conError > 0 ? RegentaColors.crit : RegentaColors.warn),
            const SizedBox(width: 8),
            Expanded(
              child: Text(partes.join(' · '),
                  style: RegentaType.cuerpo.copyWith(
                      fontSize: 13, color: conError > 0 ? RegentaColors.crit : RegentaColors.warn)),
            ),
            const Text('Ver'),
            const Icon(Icons.chevron_right, color: RegentaColors.faint),
          ]),
        ),
      ),
    );
  }
}

/// Un atajo de la barra de Compras: con texto en web, solo ícono en el celular.
class _Atajo extends StatelessWidget {
  const _Atajo({
    required this.clave,
    required this.texto,
    required this.icono,
    required this.conTexto,
    required this.onPressed,
  });

  final Key clave;
  final String texto;
  final IconData icono;
  final bool conTexto;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    if (!conTexto) {
      return IconButton(key: clave, tooltip: texto, onPressed: onPressed, icon: Icon(icono, color: RegentaColors.ink));
    }
    return TextButton.icon(
      key: clave,
      onPressed: onPressed,
      icon: Icon(icono, size: 18, color: RegentaColors.ink),
      label: Text(texto, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink)),
    );
  }
}
