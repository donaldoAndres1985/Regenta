import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import 'cierres.dart';

String _motivo(ErrorDeApi fallo, String que) {
  if (fallo is ErrorDeRed) return 'No hay conexión: $que.';
  if (fallo is SinPermiso) return 'No tienes permiso para ver los cierres de caja.';
  return fallo.mensaje;
}

PreferredSizeWidget _barra(String titulo) => AppBar(
      backgroundColor: RegentaColors.surface,
      surfaceTintColor: RegentaColors.surface,
      elevation: 0,
      shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
      title: Text(titulo,
          overflow: TextOverflow.ellipsis, style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
    );

TextStyle get _titulo => RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700);

TextStyle get _detalle => RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted);

Widget _aviso(String texto, Key clave) => Padding(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: Text(texto,
            key: clave, textAlign: TextAlign.center, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
      ),
    );

Widget _avisoDeError(String texto) => Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(14, 10, 14, 0),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(color: RegentaColors.critSoft, borderRadius: BorderRadius.circular(8)),
      child: Text(texto,
          key: const Key('cierres-error'), style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.crit)),
    );

/// HU-143 C1 · Buscar los cierres pasados: por rango y, si se quiere, por cajero; el más reciente
/// primero y por páginas («Cargar más»). Tocar uno abre su detalle.
class PantallaCierresDeCaja extends ConsumerStatefulWidget {
  const PantallaCierresDeCaja({super.key, this.hoy});

  /// Para las pruebas: el día desde el que se arman los rangos.
  final DateTime? hoy;

  @override
  ConsumerState<PantallaCierresDeCaja> createState() => _PantallaCierresDeCajaState();
}

class _PantallaCierresDeCajaState extends ConsumerState<PantallaCierresDeCaja> {
  late final DateTime _hoy = () {
    final h = widget.hoy ?? DateTime.now();
    return DateTime(h.year, h.month, h.day);
  }();
  late DateTime _desde = _hoy.subtract(const Duration(days: 7));
  late DateTime _hasta = _hoy;
  String? _cajeroId;
  List<CajeroVista> _cajeros = const [];
  final List<CierreDeCajaVista> _cierres = [];
  int _pagina = 0;
  int _total = 0;
  bool _hayMas = false;
  bool _cargando = false;
  String? _error;

  RepositorioDeCierresDeCaja get _repo => ref.read(repositorioDeCierresDeCajaProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_buscar());
    unawaited(_cargarCajeros());
  }

  Future<void> _cargarCajeros() async {
    try {
      final cajeros = await _repo.cajeros();
      if (mounted) setState(() => _cajeros = cajeros);
    } on ErrorDeApi {
      // El filtro por cajero es opcional: sin la lista, se busca sin él.
    }
  }

  Future<void> _buscar({bool otraPagina = false}) async {
    final pagina = otraPagina ? _pagina + 1 : 0;
    setState(() {
      _cargando = true;
      _error = null;
      if (!otraPagina) _cierres.clear();
    });
    try {
      final p = await _repo.buscar(desde: _desde, hasta: _hasta, cajeroId: _cajeroId, pagina: pagina);
      if (!mounted) return;
      setState(() {
        _cierres.addAll(p.cierres);
        _pagina = pagina;
        _total = p.total;
        _hayMas = p.hayMas;
      });
    } on ErrorDeApi catch (fallo) {
      if (mounted) setState(() => _error = _motivo(fallo, 'no se pudieron buscar los cierres'));
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  void _rango(int dias) {
    setState(() {
      _desde = _hoy.subtract(Duration(days: dias));
      _hasta = _hoy;
    });
    unawaited(_buscar());
  }

  Future<void> _otroRango() async {
    final r = await showDateRangePicker(
      context: context,
      firstDate: DateTime(_hoy.year - 5),
      lastDate: _hoy,
      initialDateRange: DateTimeRange(start: _desde, end: _hasta),
      helpText: 'Cierres entre',
    );
    if (r == null) return;
    setState(() {
      _desde = r.start;
      _hasta = r.end;
    });
    unawaited(_buscar());
  }

  bool _esRango(int dias) => _hasta == _hoy && _desde == _hoy.subtract(Duration(days: dias));

  @override
  Widget build(BuildContext context) {
    final otro = !_esRango(0) && !_esRango(7) && !_esRango(30);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: _barra('Cierres de caja'),
      body: Column(children: [
        Container(
          width: double.infinity,
          color: RegentaColors.surface,
          padding: const EdgeInsets.fromLTRB(14, 8, 14, 8),
          child: Wrap(spacing: 8, runSpacing: 6, crossAxisAlignment: WrapCrossAlignment.center, children: [
            ChoiceChip(
                key: const Key('cierres-hoy'), label: const Text('Hoy'), selected: _esRango(0), onSelected: (_) => _rango(0)),
            ChoiceChip(
                key: const Key('cierres-7-dias'),
                label: const Text('Últimos 7 días'),
                selected: _esRango(7),
                onSelected: (_) => _rango(7)),
            ChoiceChip(
                key: const Key('cierres-30-dias'),
                label: const Text('Últimos 30 días'),
                selected: _esRango(30),
                onSelected: (_) => _rango(30)),
            ChoiceChip(
              key: const Key('cierres-otro-rango'),
              avatar: const Icon(Icons.date_range, size: 16),
              label: Text(otro ? '${fechaIso(_desde)} a ${fechaIso(_hasta)}' : 'Otro rango'),
              selected: otro,
              onSelected: (_) => unawaited(_otroRango()),
            ),
            DropdownButton<String?>(
              key: const Key('cierres-cajero'),
              value: _cajeroId,
              hint: const Text('Todos los cajeros'),
              underline: const SizedBox.shrink(),
              items: [
                const DropdownMenuItem<String?>(child: Text('Todos los cajeros')),
                for (final c in _cajeros)
                  DropdownMenuItem<String?>(key: Key('cierres-cajero-${c.id}'), value: c.id, child: Text(c.nombre)),
              ],
              onChanged: (id) {
                setState(() => _cajeroId = id);
                unawaited(_buscar());
              },
            ),
          ]),
        ),
        if (_error != null) _avisoDeError(_error!),
        Expanded(child: _lista()),
      ]),
    );
  }

  Widget _lista() {
    if (_cierres.isEmpty && _cargando) return const Center(child: CircularProgressIndicator());
    if (_cierres.isEmpty) {
      return _aviso('No hay cierres de caja en este rango.', const Key('cierres-vacio'));
    }
    return ListView(padding: const EdgeInsets.fromLTRB(14, 10, 14, 24), children: [
      Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text('${_cierres.length} de $_total cierres', key: const Key('cierres-conteo'), style: _detalle),
      ),
      for (final c in _cierres) _TarjetaDeCierre(cierre: c, onAbrir: () => _abrir(c)),
      if (_hayMas)
        Center(
          child: _cargando
              ? const Padding(padding: EdgeInsets.all(12), child: CircularProgressIndicator())
              : OutlinedButton(
                  key: const Key('cierres-cargar-mas'),
                  onPressed: () => unawaited(_buscar(otraPagina: true)),
                  child: const Text('Cargar más'),
                ),
        ),
    ]);
  }

  void _abrir(CierreDeCajaVista c) => unawaited(Navigator.of(context)
      .push<void>(MaterialPageRoute(builder: (_) => PantallaDetalleDeCierre(sesionId: c.sesionId))));
}

class _TarjetaDeCierre extends StatelessWidget {
  const _TarjetaDeCierre({required this.cierre, required this.onAbrir});

  final CierreDeCajaVista cierre;
  final VoidCallback onAbrir;

  @override
  Widget build(BuildContext context) {
    final c = cierre;
    final descuadre = c.diferenciaTotal != 0;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: c.descuadrado ? RegentaColors.crit : RegentaColors.line),
      ),
      child: InkWell(
        key: Key('cierre-${c.sesionId}'),
        borderRadius: BorderRadius.circular(10),
        onTap: onAbrir,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Row(children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('${c.numero} · ${c.caja}', maxLines: 1, overflow: TextOverflow.ellipsis, style: _titulo),
                const SizedBox(height: 2),
                Text('${c.cajero}${c.cerradaEn == null ? '' : ' · ${fechaYHora(c.cerradaEn!)}'}',
                    maxLines: 1, overflow: TextOverflow.ellipsis, style: _detalle),
                const SizedBox(height: 4),
                Text(c.estado, key: Key('cierre-estado-${c.sesionId}'),
                    style: RegentaType.etiqueta.copyWith(color: c.descuadrado ? RegentaColors.crit : RegentaColors.muted)),
              ]),
            ),
            Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
              Text(pesos(c.totalEsperado), key: Key('cierre-esperado-${c.sesionId}'), style: _titulo),
              Text(
                descuadre ? 'Diferencia ${pesos(c.diferenciaTotal)}' : 'Sin diferencia',
                key: Key('cierre-diferencia-${c.sesionId}'),
                style: RegentaType.codigo.copyWith(
                    fontSize: 11, color: descuadre ? RegentaColors.crit : RegentaColors.muted),
              ),
            ]),
          ]),
        ),
      ),
    );
  }
}

/// HU-143 C2 / C3 · Un cierre: el resumen con el arqueo por método y sus movimientos con el total.
/// Tocar un movimiento abre su detalle (C4 / C5).
class PantallaDetalleDeCierre extends ConsumerStatefulWidget {
  const PantallaDetalleDeCierre({super.key, required this.sesionId});

  final String sesionId;

  @override
  ConsumerState<PantallaDetalleDeCierre> createState() => _PantallaDetalleDeCierreState();
}

class _PantallaDetalleDeCierreState extends ConsumerState<PantallaDetalleDeCierre> {
  ResumenDeCierreVista? _resumen;
  MovimientosDeCierreVista? _movimientos;
  String? _error;

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() async {
    final repo = ref.read(repositorioDeCierresDeCajaProvider);
    try {
      final resumen = await repo.resumen(widget.sesionId);
      final movimientos = await repo.movimientos(widget.sesionId);
      if (mounted) {
        setState(() {
          _resumen = resumen;
          _movimientos = movimientos;
        });
      }
    } on ErrorDeApi catch (fallo) {
      if (mounted) setState(() => _error = _motivo(fallo, 'no se pudo abrir el cierre'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = _resumen;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: _barra(r == null ? 'Cierre de caja' : 'Cierre ${r.cierre.numero}'),
      body: _error != null
          ? Column(children: [_avisoDeError(_error!)])
          : r == null
              ? const Center(child: CircularProgressIndicator())
              : ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
                  _Encabezado(resumen: r),
                  const SizedBox(height: 12),
                  _Resumen(resumen: r),
                  const SizedBox(height: 12),
                  _Arqueo(resumen: r),
                  const SizedBox(height: 12),
                  _Movimientos(
                    movimientos: _movimientos ?? const MovimientosDeCierreVista(),
                    onAbrir: (m) => unawaited(mostrarDetalleDeMovimiento(context, ref, m.id)),
                  ),
                ]),
    );
  }
}

class _Seccion extends StatelessWidget {
  const _Seccion({required this.titulo, required this.child, this.clave});

  final String titulo;
  final Widget child;
  final Key? clave;

  @override
  Widget build(BuildContext context) => Container(
        key: clave,
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        decoration: BoxDecoration(
          color: RegentaColors.surface,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: RegentaColors.line),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(titulo.toUpperCase(), style: RegentaType.etiqueta.copyWith(color: RegentaColors.muted)),
          const SizedBox(height: 8),
          child,
        ]),
      );
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor, {this.clave, this.color, this.negrita = false});

  final String etiqueta;
  final String valor;
  final Key? clave;
  final Color? color;
  final bool negrita;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Row(children: [
          Expanded(child: Text(etiqueta, style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2))),
          Text(valor,
              key: clave,
              style: RegentaType.cuerpo.copyWith(
                  fontSize: 13,
                  color: color ?? RegentaColors.ink,
                  fontWeight: negrita ? FontWeight.w700 : FontWeight.w600)),
        ]),
      );
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.resumen});

  final ResumenDeCierreVista resumen;

  @override
  Widget build(BuildContext context) {
    final r = resumen;
    final c = r.cierre;
    return _Seccion(
      titulo: 'Turno',
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text('${c.numero} · ${c.caja}', style: _titulo),
        const SizedBox(height: 4),
        Text('Abrió ${c.cajero}${r.abiertaEn == null ? '' : ' el ${fechaYHora(r.abiertaEn!)}'}', style: _detalle),
        Text('Cerró ${r.cerradoPor ?? '—'}${c.cerradaEn == null ? '' : ' el ${fechaYHora(c.cerradaEn!)}'}',
            style: _detalle),
        const SizedBox(height: 4),
        Text(c.estado,
            key: const Key('cierre-detalle-estado'),
            style: RegentaType.etiqueta.copyWith(color: c.descuadrado ? RegentaColors.crit : RegentaColors.ok)),
        if (r.observaciones != null && r.observaciones!.isNotEmpty) ...[
          const SizedBox(height: 6),
          Text('«${r.observaciones}»', style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2)),
        ],
      ]),
    );
  }
}

/// C2 · Ventas al contado y a crédito, ingresos, retiros y gastos (y lo demás que haya).
class _Resumen extends StatelessWidget {
  const _Resumen({required this.resumen});

  final ResumenDeCierreVista resumen;

  @override
  Widget build(BuildContext context) {
    final r = resumen;
    return _Seccion(
      titulo: 'Resumen',
      clave: const Key('cierre-resumen'),
      child: Column(children: [
        _Fila('Base de apertura', pesos(r.montoApertura), clave: const Key('cierre-apertura')),
        _Fila('Ventas de contado', pesos(r.ventasContado), clave: const Key('cierre-ventas-contado')),
        _Fila('Ventas a crédito', pesos(r.ventasCredito), clave: const Key('cierre-ventas-credito')),
        if (r.devoluciones != 0) _Fila('Devoluciones', '−${pesos(r.devoluciones)}', clave: const Key('cierre-devoluciones')),
        if (r.recaudos != 0) _Fila('Recaudos de cartera', pesos(r.recaudos), clave: const Key('cierre-recaudos')),
        _Fila('Ingresos', pesos(r.ingresos), clave: const Key('cierre-ingresos')),
        _Fila('Retiros', '−${pesos(r.retiros)}', clave: const Key('cierre-retiros')),
        _Fila('Gastos', '−${pesos(r.gastos)}', clave: const Key('cierre-gastos')),
        if (r.pagosAProveedor != 0)
          _Fila('Pagos a proveedores', '−${pesos(r.pagosAProveedor)}', clave: const Key('cierre-pagos-proveedor')),
      ]),
    );
  }
}

/// C2 · El arqueo por método: esperado, contado y diferencia, con la fila de totales.
class _Arqueo extends StatelessWidget {
  const _Arqueo({required this.resumen});

  final ResumenDeCierreVista resumen;

  @override
  Widget build(BuildContext context) {
    final r = resumen;
    TextStyle celda({bool negrita = false, Color? color}) => RegentaType.codigo.copyWith(
        fontSize: 12, color: color ?? RegentaColors.ink, fontWeight: negrita ? FontWeight.w700 : FontWeight.w500);
    Widget fila(String metodo, String esperado, String contado, num? diferencia, {bool negrita = false, Key? clave}) =>
        Padding(
          key: clave,
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(children: [
            Expanded(flex: 3, child: Text(metodo, style: celda(negrita: negrita))),
            Expanded(flex: 2, child: Text(esperado, textAlign: TextAlign.right, style: celda(negrita: negrita))),
            Expanded(flex: 2, child: Text(contado, textAlign: TextAlign.right, style: celda(negrita: negrita))),
            Expanded(
              flex: 2,
              child: Text(
                diferencia == null ? '—' : pesos(diferencia),
                textAlign: TextAlign.right,
                style: celda(
                    negrita: true,
                    color: diferencia == null || diferencia == 0 ? RegentaColors.ink2 : RegentaColors.crit),
              ),
            ),
          ]),
        );
    return _Seccion(
      titulo: 'Arqueo por método',
      clave: const Key('cierre-arqueo'),
      child: Column(children: [
        Row(children: [
          Expanded(flex: 3, child: Text('Método', style: _detalle)),
          Expanded(flex: 2, child: Text('Esperado', textAlign: TextAlign.right, style: _detalle)),
          Expanded(flex: 2, child: Text('Contado', textAlign: TextAlign.right, style: _detalle)),
          Expanded(flex: 2, child: Text('Diferencia', textAlign: TextAlign.right, style: _detalle)),
        ]),
        const Divider(height: 12, color: RegentaColors.line),
        for (final l in r.arqueo)
          fila(nombreDelMetodoDeCaja(l.metodo), pesos(l.esperado), l.contado == null ? '—' : pesos(l.contado!),
              l.diferencia,
              clave: Key('cierre-arqueo-${l.metodo}')),
        const Divider(height: 12, color: RegentaColors.line),
        fila('Total', pesos(r.totalEsperado), pesos(r.totalContado), r.totalDiferencia,
            negrita: true, clave: const Key('cierre-arqueo-total')),
      ]),
    );
  }
}

/// C3 · Todos los movimientos y la fila final del total.
class _Movimientos extends StatelessWidget {
  const _Movimientos({required this.movimientos, required this.onAbrir});

  final MovimientosDeCierreVista movimientos;
  final ValueChanged<MovimientoDeCierreVista> onAbrir;

  @override
  Widget build(BuildContext context) {
    return _Seccion(
      titulo: 'Movimientos',
      clave: const Key('cierre-movimientos'),
      child: Column(children: [
        for (final m in movimientos.movimientos)
          InkWell(
            key: Key('movimiento-${m.id}'),
            onTap: () => onAbrir(m),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Row(children: [
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(
                      [m.tipoNombre, if (m.documento != null && m.documento!.isNotEmpty) m.documento!].join(' · '),
                      style: RegentaType.cuerpo.copyWith(
                        fontSize: 13,
                        color: RegentaColors.ink,
                        decoration: m.anulado ? TextDecoration.lineThrough : null,
                      ),
                    ),
                    Text(
                      [if (m.ocurridoEn != null) fechaYHora(m.ocurridoEn!), m.metodoNombre, if (m.anulado) 'anulado']
                          .join(' · '),
                      style: _detalle,
                    ),
                  ]),
                ),
                Text(pesos(m.monto),
                    key: Key('movimiento-monto-${m.id}'),
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: m.monto < 0 ? RegentaColors.crit : RegentaColors.ink)),
                const Icon(Icons.chevron_right, size: 18, color: RegentaColors.faint),
              ]),
            ),
          ),
        const Divider(height: 14, color: RegentaColors.line),
        _Fila('Total', pesos(movimientos.total), clave: const Key('cierre-movimientos-total'), negrita: true),
      ]),
    );
  }
}

/// C4 / C5 · El detalle de un movimiento en una hoja que sube desde abajo.
Future<void> mostrarDetalleDeMovimiento(BuildContext context, WidgetRef ref, String movimientoId) {
  final repo = ref.read(repositorioDeCierresDeCajaProvider);
  final abrirVenta = ref.read(abrirVentaDesdeCajaProvider);
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: RegentaColors.surface,
    builder: (hoja) => SafeArea(
      child: FutureBuilder<DetalleDeMovimientoVista>(
        future: repo.movimiento(movimientoId),
        builder: (_, s) {
          if (s.hasError) {
            final fallo = s.error;
            return _aviso(
                fallo is ErrorDeApi ? _motivo(fallo, 'no se pudo abrir el movimiento') : '$fallo',
                const Key('movimiento-error'));
          }
          if (!s.hasData) {
            return const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()));
          }
          final d = s.data!;
          return SingleChildScrollView(
            key: const Key('movimiento-detalle'),
            padding: const EdgeInsets.fromLTRB(18, 16, 18, 18),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, mainAxisSize: MainAxisSize.min, children: [
              Row(children: [
                Expanded(
                    child: Text(
                        [d.tipoNombre, if (d.documento != null && d.documento!.isNotEmpty) d.documento!].join(' · '),
                        key: const Key('movimiento-titulo'),
                        style: _titulo)),
                Text(pesos(d.monto), key: const Key('movimiento-monto'), style: _titulo),
              ]),
              const SizedBox(height: 10),
              if (d.ocurridoEn != null) _Fila('Fecha', fechaYHora(d.ocurridoEn!)),
              if (d.esMixto) ...[
                _Fila('Método', 'Pago mixto'),
                for (final p in d.pagos)
                  _Fila('   ${p.metodoNombre}', pesos(p.monto), clave: Key('movimiento-pago-${p.metodoNombre}')),
              ] else
                _Fila('Método', d.metodoNombre, clave: const Key('movimiento-metodo')),
              if (d.propina != 0) _Fila('Propina', pesos(d.propina)),
              if (d.concepto != null && d.concepto!.isNotEmpty) _Fila('Concepto', d.concepto!),
              _Fila('Registró', d.registradoPor ?? '—', clave: const Key('movimiento-registro')),
              if (d.autorizadoPor != null) _Fila('Autorizó', d.autorizadoPor!, clave: const Key('movimiento-autorizo')),
              if (d.anuladoPor != null) ...[
                _Fila('Anuló', d.anuladoPor!, clave: const Key('movimiento-anulo'), color: RegentaColors.crit),
                if (d.motivoAnulacion != null) _Fila('Motivo', d.motivoAnulacion!, color: RegentaColors.crit),
              ],
              if (d.ventaId != null && abrirVenta != null) ...[
                const SizedBox(height: 12),
                FilledButton.icon(
                  key: const Key('movimiento-ir-a-venta'),
                  style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
                  onPressed: () {
                    Navigator.of(hoja).pop();
                    abrirVenta(context, d.ventaId!, d.documento);
                  },
                  icon: const Icon(Icons.receipt_long_outlined, size: 18),
                  label: Text('Ver la venta ${d.documento ?? ''}'.trim()),
                ),
              ],
            ]),
          );
        },
      ),
    ),
  );
}
