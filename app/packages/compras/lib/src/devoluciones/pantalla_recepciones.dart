import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';

String _fecha(DateTime? d) => d == null
    ? '—'
    : '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

String _cantidad(num n) => n == n.roundToDouble() ? '${n.round()}' : '$n';

String _mensajeDe(ErrorDeApi fallo) => switch (fallo) {
      SinPermiso() => 'No tienes permiso para hacer esto.',
      ErrorDeRed() => 'No hay conexión. Intenta de nuevo.',
      _ => fallo.mensaje,
    };

AppBar _barra(String titulo) => AppBar(
      backgroundColor: RegentaColors.surface,
      surfaceTintColor: RegentaColors.surface,
      elevation: 0,
      shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
      title: Text(titulo, style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
    );

/// HU-178 · Las recepciones del negocio, la más reciente primero, para devolver mercancía o anularlas.
class PantallaRecepciones extends ConsumerStatefulWidget {
  const PantallaRecepciones({super.key});

  @override
  ConsumerState<PantallaRecepciones> createState() => _PantallaRecepcionesState();
}

class _PantallaRecepcionesState extends ConsumerState<PantallaRecepciones> {
  List<RecepcionVista>? _recepciones;
  String? _error;

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() async {
    try {
      final todas = await ref.read(repositorioDeDevolucionesProvider).recepciones();
      if (mounted) setState(() => _recepciones = todas.where((r) => r.estado != 'BORRADOR').toList());
    } on ErrorDeApi catch (fallo) {
      if (mounted) {
        setState(() {
          _recepciones ??= const [];
          _error = _mensajeDe(fallo);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final lista = _recepciones;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: _barra('Recepciones'),
      body: lista == null
          ? const Center(child: CircularProgressIndicator())
          : Column(children: [
              if (_error != null) Padding(padding: const EdgeInsets.all(14), child: AvisoDeError(_error!)),
              Expanded(
                child: lista.isEmpty
                    ? Center(
                        child: Text('Todavía no hay recepciones confirmadas.',
                            key: const Key('recepciones-vacio'),
                            style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)))
                    : ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                        itemCount: lista.length,
                        itemBuilder: (context, i) {
                          final r = lista[i];
                          return InkWell(
                            key: Key('recepcion-fila-${r.id}'),
                            onTap: () async {
                              await Navigator.of(context).push<void>(MaterialPageRoute(
                                  builder: (_) => PantallaDeRecepcion(recepcionId: r.id)));
                              if (mounted) unawaited(_cargar());
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 12),
                              decoration: const BoxDecoration(
                                  border: Border(bottom: BorderSide(color: RegentaColors.line))),
                              child: Row(children: [
                                Expanded(
                                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                    Text('${r.numero} · ${r.proveedorNombre ?? 'Proveedor'}',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: RegentaType.item.copyWith(
                                            color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
                                    Text(
                                        '${r.facturaProveedor ?? 'sin factura'} · ${_fecha(r.fecha)}'
                                        '${r.estado == 'ANULADA' ? ' · anulada' : ''}',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                                  ]),
                                ),
                                Text(formatearPesos(r.total), style: RegentaType.codigo.copyWith(fontSize: 13)),
                                const Icon(Icons.chevron_right, color: RegentaColors.faint),
                              ]),
                            ),
                          );
                        },
                      ),
              ),
            ]),
    );
  }
}

/// HU-178 · Una recepción: lo recibido y lo ya devuelto por línea, sus devoluciones con la nota
/// crédito, y las acciones «Devolver al proveedor» y «Anular recepción» (HU-177).
class PantallaDeRecepcion extends ConsumerStatefulWidget {
  const PantallaDeRecepcion({super.key, required this.recepcionId});

  final String recepcionId;

  @override
  ConsumerState<PantallaDeRecepcion> createState() => _PantallaDeRecepcionState();
}

class _PantallaDeRecepcionState extends ConsumerState<PantallaDeRecepcion> {
  RecepcionVista? _recepcion;
  List<DevolucionVista> _devoluciones = const [];
  String? _error;

  RepositorioDeDevoluciones get _repo => ref.read(repositorioDeDevolucionesProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() async {
    try {
      final r = await _repo.recepcion(widget.recepcionId);
      final d = await _repo.devolucionesDe(widget.recepcionId);
      if (mounted) {
        setState(() {
          _recepcion = r;
          _devoluciones = d;
        });
      }
    } on ErrorDeApi catch (fallo) {
      if (mounted) setState(() => _error = _mensajeDe(fallo));
    }
  }

  Map<String, num> get _devuelto {
    final total = <String, num>{};
    for (final d in _devoluciones) {
      d.devueltoPorLinea.forEach((k, v) => total[k] = (total[k] ?? 0) + v);
    }
    return total;
  }

  Future<void> _devolver() async {
    final hecha = await showDialog<DevolucionVista>(
      context: context,
      barrierDismissible: false,
      builder: (_) => Dialog(
        clipBehavior: Clip.antiAlias,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 560, maxHeight: 760),
          child: DialogoDeDevolucion(recepcion: _recepcion!, devuelto: _devuelto, repo: _repo),
        ),
      ),
    );
    if (hecha == null || !mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text('Devolución ${hecha.numero}: nota crédito por ${formatearPesos(hecha.valorNota)}.'),
    ));
    unawaited(_cargar());
  }

  Future<void> _anular() async {
    final motivo = await showDialog<String>(context: context, builder: (_) => const _DialogoDeAnulacion());
    if (motivo == null || !mounted) return;
    try {
      final r = await _repo.anular(widget.recepcionId, motivo);
      if (!mounted) return;
      setState(() {
        _recepcion = r;
        _error = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('La recepción ${r.numero} quedó anulada.')));
    } on ErrorDeApi catch (fallo) {
      if (mounted) setState(() => _error = _mensajeDe(fallo));
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = _recepcion;
    final devuelto = _devuelto;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: _barra(r == null ? 'Recepción' : 'Recepción ${r.numero}'),
      body: r == null
          ? Center(
              child: _error == null
                  ? const CircularProgressIndicator()
                  : Padding(padding: const EdgeInsets.all(24), child: AvisoDeError(_error!)))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(14),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 900),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                    Text(r.proveedorNombre ?? 'Proveedor',
                        style: RegentaType.seccion.copyWith(fontSize: 18, color: RegentaColors.inkFuerte)),
                    Text('${r.facturaProveedor ?? 'sin factura'} · ${_fecha(r.fecha)} · ${formatearPesos(r.total)}',
                        style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
                    if (r.motivoAnulacion != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text('Anulada: ${r.motivoAnulacion}',
                            key: const Key('recepcion-anulada'),
                            style: RegentaType.cuerpo.copyWith(color: RegentaColors.warn)),
                      ),
                    const SizedBox(height: 12),
                    for (final l in r.lineas)
                      Container(
                        key: Key('recepcion-linea-${l.id}'),
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        decoration:
                            const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
                        child: Row(children: [
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(l.nombre,
                                  style: RegentaType.item
                                      .copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
                              Text(
                                  [if (l.codigoLote != null) 'lote ${l.codigoLote}',
                                        'entró a ${formatearPesos(l.costoEntrada)}']
                                      .join(' · '),
                                  style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                            ]),
                          ),
                          Text(
                              'recibido ${_cantidad(l.cantidad)}'
                              '${(devuelto[l.id] ?? 0) > 0 ? ' · devuelto ${_cantidad(devuelto[l.id]!)}' : ''}',
                              key: Key('recepcion-devuelto-${l.id}'),
                              style: RegentaType.codigo.copyWith(fontSize: 11.5)),
                        ]),
                      ),
                    if (_devoluciones.isNotEmpty) ...[
                      const SizedBox(height: 14),
                      Text('DEVOLUCIONES AL PROVEEDOR', style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
                      for (final d in _devoluciones)
                        Padding(
                          key: Key('devolucion-${d.id}'),
                          padding: const EdgeInsets.symmetric(vertical: 6),
                          child: Text(
                            '${d.numero} · ${d.motivo} · nota ${formatearPesos(d.valorNota)}'
                            '${d.aplicadoACuenta > 0 ? ' · rebajó ${formatearPesos(d.aplicadoACuenta)} de la factura' : ''}'
                            '${d.saldoAFavor > 0 ? ' · ${formatearPesos(d.saldoAFavor)} a favor' : ''}',
                            style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2),
                          ),
                        ),
                    ],
                    const SizedBox(height: 14),
                    if (_error != null) AvisoDeError(_error!, key: const Key('recepcion-error')),
                    if (r.confirmada) ...[
                      SizedBox(
                        height: RegentaSpacing.hitTarget,
                        child: FilledButton.icon(
                          key: const Key('recepcion-devolver'),
                          onPressed: () => unawaited(_devolver()),
                          icon: const Icon(Icons.assignment_return_outlined, size: 18),
                          label: const Text('Devolver al proveedor'),
                        ),
                      ),
                      const SizedBox(height: 8),
                      if (_devoluciones.isEmpty)
                        SizedBox(
                          height: RegentaSpacing.hitTarget,
                          child: OutlinedButton.icon(
                            key: const Key('recepcion-anular'),
                            onPressed: () => unawaited(_anular()),
                            icon: const Icon(Icons.block, size: 18),
                            label: const Text('Anular recepción'),
                          ),
                        ),
                    ],
                  ]),
                ),
              ),
            ),
    );
  }
}

/// HU-178 · Cuánto se devuelve de cada línea (de su lote, C6), con el motivo. Muestra el valor de la
/// nota crédito: la base de lo devuelto con sus descuentos más su IVA; el flete no se devuelve.
class DialogoDeDevolucion extends StatefulWidget {
  const DialogoDeDevolucion({super.key, required this.recepcion, required this.devuelto, required this.repo});

  final RecepcionVista recepcion;
  final Map<String, num> devuelto;
  final RepositorioDeDevoluciones repo;

  @override
  State<DialogoDeDevolucion> createState() => _DialogoDeDevolucionState();
}

class _DialogoDeDevolucionState extends State<DialogoDeDevolucion> {
  final TextEditingController _motivo = TextEditingController();
  late final Map<String, TextEditingController> _cantidades = {
    for (final l in widget.recepcion.lineas) l.id: TextEditingController(),
  };
  String? _error;
  bool _guardando = false;

  @override
  void dispose() {
    _motivo.dispose();
    for (final c in _cantidades.values) {
      c.dispose();
    }
    super.dispose();
  }

  num _quedan(LineaRecibidaVista l) => l.cantidad - (widget.devuelto[l.id] ?? 0);
  num _pedida(LineaRecibidaVista l) => num.tryParse(_cantidades[l.id]!.text.replaceAll(',', '.')) ?? 0;

  String? _validar() {
    if (_motivo.text.trim().isEmpty) return 'Escribe por qué se devuelve (averiado, vencido, errado…).';
    if (widget.recepcion.lineas.every((l) => _pedida(l) <= 0)) return 'Escribe cuánto devuelves de algún producto.';
    for (final l in widget.recepcion.lineas) {
      if (_pedida(l) > _quedan(l)) {
        return 'De «${l.nombre}» se pueden devolver ${_cantidad(_quedan(l))}.';
      }
    }
    return null;
  }

  Future<void> _devolver() async {
    final problema = _validar();
    if (problema != null) {
      setState(() => _error = problema);
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      final hecha = await widget.repo.devolver(widget.recepcion.id, _motivo.text,
          {for (final l in widget.recepcion.lineas) l.id: _pedida(l)});
      if (mounted) Navigator.of(context).pop(hecha);
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _guardando = false;
        _error = _mensajeDe(fallo);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: RegentaColors.surface,
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        CabeceraDeDialogo(
          titulo: 'Devolver al proveedor',
          subtitulo: 'Sale de la bodega al costo de entrada; la nota crédito rebaja la factura.',
          onCerrar: _guardando ? null : () => Navigator.of(context).pop(),
        ),
        Flexible(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(18, 14, 18, 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              for (final l in widget.recepcion.lineas)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(children: [
                    Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(l.nombre, style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
                        Text(
                            [if (l.codigoLote != null) 'lote ${l.codigoLote}',
                                  'se pueden devolver ${_cantidad(_quedan(l))}']
                                .join(' · '),
                            style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                      ]),
                    ),
                    SizedBox(
                      width: 110,
                      child: TextField(
                        key: Key('devolver-cantidad-${l.id}'),
                        controller: _cantidades[l.id],
                        enabled: _quedan(l) > 0,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        textAlign: TextAlign.right,
                        style: estiloDeCampo,
                        decoration: decoracionDeCampo(hint: '0'),
                      ),
                    ),
                  ]),
                ),
              CampoConEtiqueta(
                etiqueta: 'Motivo *',
                hijo: TextField(
                  key: const Key('devolver-motivo'),
                  controller: _motivo,
                  maxLength: 200,
                  style: estiloDeCampo,
                  decoration: decoracionDeCampo(hint: 'Averiado, vencido, errado…'),
                ),
              ),
              if (_error != null) AvisoDeError(_error!, key: const Key('devolver-error')),
            ]),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
          child: SizedBox(
            height: RegentaSpacing.hitTarget,
            child: FilledButton(
              key: const Key('devolver-registrar'),
              onPressed: _guardando ? null : () => unawaited(_devolver()),
              child: Text(_guardando ? 'Registrando…' : 'Registrar devolución'),
            ),
          ),
        ),
      ]),
    );
  }
}

/// HU-177 · El motivo para anular una recepción.
class _DialogoDeAnulacion extends StatefulWidget {
  const _DialogoDeAnulacion();

  @override
  State<_DialogoDeAnulacion> createState() => _DialogoDeAnulacionState();
}

class _DialogoDeAnulacionState extends State<_DialogoDeAnulacion> {
  final TextEditingController _motivo = TextEditingController();

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text('Anular recepción', style: RegentaType.seccion),
        content: SizedBox(
          width: 420,
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Text('Sale del inventario lo que entró y su factura queda anulada. Si ya se vendió parte o '
                'la factura tiene abonos, no se puede.',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
            const SizedBox(height: 12),
            TextField(
              key: const Key('anular-recepcion-motivo'),
              controller: _motivo,
              maxLength: 200,
              onChanged: (_) => setState(() {}),
              style: estiloDeCampo,
              decoration: decoracionDeCampo(hint: 'Motivo (obligatorio)'),
            ),
          ]),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Volver')),
          FilledButton(
            key: const Key('anular-recepcion-confirmar'),
            onPressed: _motivo.text.trim().isEmpty ? null : () => Navigator.of(context).pop(_motivo.text.trim()),
            child: const Text('Anular'),
          ),
        ],
      );
}
