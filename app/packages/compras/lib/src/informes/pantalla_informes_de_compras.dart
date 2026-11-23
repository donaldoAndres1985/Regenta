import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';

/// HU-183 · Cuánto se compra, a quién y cómo cambian los costos: compras por proveedor (C1) y por
/// producto (C2) en un periodo, el historial de costos de un producto (C3) y cualquiera de ellos en
/// PDF o Excel (C4).
///
/// HU-184 · Cuánto se debe y desde cuándo (antigüedad, C1), qué falta por recibir (C2), lo que nunca
/// llegó de las órdenes cerradas (C3) y los pagos del periodo por proveedor y método (C4).
class PantallaInformesDeCompras extends ConsumerStatefulWidget {
  const PantallaInformesDeCompras({super.key, this.hoy});

  /// Para las pruebas: el día desde el que se arman «Este mes» y «Mes pasado».
  final DateTime? hoy;

  @override
  ConsumerState<PantallaInformesDeCompras> createState() => _PantallaInformesDeComprasState();
}

class _PantallaInformesDeComprasState extends ConsumerState<PantallaInformesDeCompras>
    with SingleTickerProviderStateMixin {
  late final TabController _pestanas = TabController(length: InformeDeCompras.values.length, vsync: this);
  late final DateTime _hoy = widget.hoy ?? DateTime.now();
  late PeriodoDeInforme _periodo = PeriodoDeInforme.esteMes(_hoy);

  List<CompraPorProveedorVista>? _porProveedor;
  List<CompraPorProductoVista>? _porProducto;
  CompraPorProductoVista? _producto;
  List<CostoHistoricoVista>? _historial;
  InformeDeAntiguedadVista? _antiguedad;
  List<PendientePorRecibirVista>? _pendientes;
  List<FaltanteDeOrdenVista>? _faltantes;
  InformeDePagosVista? _pagos;
  String? _error;
  bool _descargando = false;

  RepositorioDeInformesDeCompras get _repo => ref.read(repositorioDeInformesDeComprasProvider);

  @override
  void initState() {
    super.initState();
    _pestanas.addListener(() {
      if (!_pestanas.indexIsChanging && mounted) setState(() {});
    });
    unawaited(_cargar());
  }

  @override
  void dispose() {
    _pestanas.dispose();
    super.dispose();
  }

  Future<void> _cargar() async {
    setState(() {
      _error = null;
      _porProveedor = null;
      _porProducto = null;
      _antiguedad = null;
      _pendientes = null;
      _faltantes = null;
      _pagos = null;
    });
    try {
      final periodo = _periodo;
      final proveedores = await _repo.porProveedor(periodo);
      final productos = await _repo.porProducto(periodo);
      final antiguedad = await _repo.antiguedad();
      final pendientes = await _repo.pendientesPorRecibir();
      final faltantes = await _repo.faltantes();
      final pagos = await _repo.pagos(periodo);
      if (!mounted || periodo != _periodo) return;
      setState(() {
        _porProveedor = proveedores;
        _porProducto = productos;
        _antiguedad = antiguedad;
        _pendientes = pendientes;
        _faltantes = faltantes;
        _pagos = pagos;
      });
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _porProveedor ??= const [];
        _porProducto ??= const [];
        _antiguedad ??= const InformeDeAntiguedadVista();
        _pendientes ??= const [];
        _faltantes ??= const [];
        _pagos ??= const InformeDePagosVista();
        _error = _motivo(fallo, 'no se pudieron cargar los informes');
      });
    }
  }

  Future<void> _verHistorial(CompraPorProductoVista p) async {
    setState(() {
      _producto = p;
      _historial = null;
      _error = null;
    });
    _pestanas.animateTo(2);
    try {
      final historial = await _repo.historialDeCostos(p.productoId);
      if (mounted && _producto == p) setState(() => _historial = historial);
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _historial = const [];
        _error = _motivo(fallo, 'no se pudo cargar el historial');
      });
    }
  }

  String _motivo(ErrorDeApi fallo, String que) {
    if (fallo is ErrorDeRed) return 'No hay conexión: $que.';
    if (fallo is SinPermiso) return 'No tienes permiso para ver los informes de compras.';
    return fallo.mensaje;
  }

  void _avisar(String texto) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texto)));

  void _elegirPeriodo(PeriodoDeInforme p) {
    if (p == _periodo) return;
    setState(() => _periodo = p);
    unawaited(_cargar());
  }

  Future<void> _otroPeriodo() async {
    final elegido = await showDateRangePicker(
      context: context,
      firstDate: DateTime(_hoy.year - 5),
      lastDate: _hoy,
      initialDateRange: DateTimeRange(start: _periodo.desde, end: _periodo.hasta),
      helpText: 'Periodo del informe',
    );
    if (elegido != null) _elegirPeriodo(PeriodoDeInforme(elegido.start, elegido.end));
  }

  InformeDeCompras get _informeVisible => InformeDeCompras.values[_pestanas.index];

  /// C4 · Descarga lo que se está viendo, en PDF o Excel.
  Future<void> _descargar(String formato) async {
    final informe = _informeVisible;
    final producto = _producto;
    if (informe == InformeDeCompras.historialDeCostos && producto == null) {
      _avisar('Elige primero un producto en «Por producto».');
      return;
    }
    setState(() => _descargando = true);
    try {
      final bytes = await _repo.archivo(
        informe,
        formato,
        periodo: informe.conPeriodo ? _periodo : null,
        productoId: informe == InformeDeCompras.historialDeCostos ? producto!.productoId : null,
      );
      final sufijo = informe == InformeDeCompras.historialDeCostos
          ? producto!.productoId.substring(0, 8)
          : informe.conPeriodo
              ? '${fechaIso(_periodo.desde)}-a-${fechaIso(_periodo.hasta)}'
              : fechaIso(_hoy);
      await ref
          .read(guardadorDeInformesProvider)
          .guardar('${informe.archivo}-$sufijo.${formato.toLowerCase()}', bytes);
    } on ErrorDeApi catch (fallo) {
      if (mounted) _avisar(_motivo(fallo, 'el archivo no se descargó'));
    } finally {
      if (mounted) setState(() => _descargando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        title: Text('Informes de compras',
            overflow: TextOverflow.ellipsis,
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        actions: [
          PopupMenuButton<String>(
            key: const Key('informes-descargar'),
            enabled: !_descargando,
            tooltip: 'Descargar',
            icon: _descargando
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.download_outlined, color: RegentaColors.ink),
            onSelected: (f) => unawaited(_descargar(f)),
            itemBuilder: (_) => const [
              PopupMenuItem(
                key: Key('informes-descargar-pdf'),
                value: 'PDF',
                child: ListTile(leading: Icon(Icons.picture_as_pdf_outlined), title: Text('PDF')),
              ),
              PopupMenuItem(
                key: Key('informes-descargar-xlsx'),
                value: 'XLSX',
                child: ListTile(leading: Icon(Icons.table_chart_outlined), title: Text('Excel')),
              ),
            ],
          ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () => unawaited(_producto != null && _pestanas.index == 2 ? _verHistorial(_producto!) : _cargar()),
          ),
          const SizedBox(width: 8),
        ],
        bottom: TabBar(
          controller: _pestanas,
          isScrollable: true,
          tabAlignment: TabAlignment.start,
          labelColor: RegentaColors.inkFuerte,
          unselectedLabelColor: RegentaColors.muted,
          indicatorColor: RegentaColors.accent,
          tabs: const [
            Tab(key: Key('informes-tab-proveedor'), text: 'Por proveedor'),
            Tab(key: Key('informes-tab-producto'), text: 'Por producto'),
            Tab(key: Key('informes-tab-historial'), text: 'Historial de costos'),
            Tab(key: Key('informes-tab-antiguedad'), text: 'Antigüedad'),
            Tab(key: Key('informes-tab-por-recibir'), text: 'Por recibir'),
            Tab(key: Key('informes-tab-faltantes'), text: 'Faltantes'),
            Tab(key: Key('informes-tab-pagos'), text: 'Pagos'),
          ],
        ),
      ),
      body: Column(children: [
        if (_informeVisible.conPeriodo) _SelectorDePeriodo(
          periodo: _periodo,
          hoy: _hoy,
          onElegir: _elegirPeriodo,
          onOtro: () => unawaited(_otroPeriodo()),
        ),
        if (_error != null)
          Padding(padding: const EdgeInsets.fromLTRB(14, 10, 14, 0), child: AvisoDeError(_error!)),
        Expanded(
          child: TabBarView(controller: _pestanas, children: [
            _ListaPorProveedor(filas: _porProveedor),
            _ListaPorProducto(filas: _porProducto, onHistorial: (p) => unawaited(_verHistorial(p))),
            _Historial(producto: _producto, entradas: _historial),
            _Antiguedad(informe: _antiguedad),
            _PorRecibir(ordenes: _pendientes),
            _Faltantes(ordenes: _faltantes),
            _Pagos(informe: _pagos),
          ]),
        ),
      ]),
    );
  }
}

class _SelectorDePeriodo extends StatelessWidget {
  const _SelectorDePeriodo({required this.periodo, required this.hoy, required this.onElegir, required this.onOtro});

  final PeriodoDeInforme periodo;
  final DateTime hoy;
  final ValueChanged<PeriodoDeInforme> onElegir;
  final VoidCallback onOtro;

  @override
  Widget build(BuildContext context) {
    final esteMes = PeriodoDeInforme.esteMes(hoy);
    final mesPasado = PeriodoDeInforme.mesPasado(hoy);
    final otro = periodo != esteMes && periodo != mesPasado;
    return Container(
      width: double.infinity,
      color: RegentaColors.surface,
      padding: const EdgeInsets.fromLTRB(14, 8, 14, 8),
      child: Wrap(spacing: 8, runSpacing: 6, crossAxisAlignment: WrapCrossAlignment.center, children: [
        ChoiceChip(
          key: const Key('informes-este-mes'),
          label: const Text('Este mes'),
          selected: periodo == esteMes,
          onSelected: (_) => onElegir(esteMes),
        ),
        ChoiceChip(
          key: const Key('informes-mes-pasado'),
          label: const Text('Mes pasado'),
          selected: periodo == mesPasado,
          onSelected: (_) => onElegir(mesPasado),
        ),
        ChoiceChip(
          key: const Key('informes-otro-periodo'),
          avatar: const Icon(Icons.date_range, size: 16),
          label: Text(otro ? '${fechaCorta(periodo.desde)} – ${fechaCorta(periodo.hasta)}' : 'Otro periodo'),
          selected: otro,
          onSelected: (_) => onOtro(),
        ),
      ]),
    );
  }
}

/// Un texto centrado cuando no hay nada que mostrar.
class _Vacio extends StatelessWidget {
  const _Vacio(this.texto, {required this.clave});

  final String texto;
  final Key clave;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(texto,
              key: clave, textAlign: TextAlign.center, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
        ),
      );
}

/// C1 · Una tarjeta por proveedor: el neto arriba y, debajo, directas, con orden y devoluciones.
class _ListaPorProveedor extends StatelessWidget {
  const _ListaPorProveedor({required this.filas});

  final List<CompraPorProveedorVista>? filas;

  @override
  Widget build(BuildContext context) {
    final f = filas;
    if (f == null) return const Center(child: CircularProgressIndicator());
    if (f.isEmpty) {
      return const _Vacio('No hubo compras en este periodo.', clave: Key('informes-proveedor-vacio'));
    }
    final total = f.fold<num>(0, (s, c) => s + c.neto.total);
    return ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
      _Total(etiqueta: 'Total comprado', valor: total, clave: const Key('informes-proveedor-total')),
      const SizedBox(height: 12),
      for (final c in f) _TarjetaDeProveedor(compra: c),
    ]);
  }
}

class _Total extends StatelessWidget {
  const _Total({required this.etiqueta, required this.valor, required this.clave});

  final String etiqueta;
  final num valor;
  final Key clave;

  @override
  Widget build(BuildContext context) => Row(children: [
        Expanded(child: Text(etiqueta, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2))),
        Text(formatearPesos(valor),
            key: clave,
            style: RegentaType.item.copyWith(
                color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700, fontSize: 18)),
      ]);
}

class _TarjetaDeProveedor extends StatelessWidget {
  const _TarjetaDeProveedor({required this.compra});

  final CompraPorProveedorVista compra;

  @override
  Widget build(BuildContext context) {
    final c = compra;
    final recepciones = c.recepciones == 1 ? '1 recepción' : '${c.recepciones} recepciones';
    return Container(
      key: Key('informes-proveedor-${c.proveedorId}'),
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(c.proveedor,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
              const SizedBox(height: 2),
              Text(recepciones, style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
            ]),
          ),
          Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text(formatearPesos(c.neto.total),
                key: Key('informes-proveedor-neto-${c.proveedorId}'),
                style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
            Text('Base ${formatearPesos(c.neto.base)} · IVA ${formatearPesos(c.neto.iva)}',
                style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
          ]),
        ]),
        const Divider(height: 18, color: RegentaColors.line),
        _Renglon('Directas', c.directas, clave: Key('informes-directas-${c.proveedorId}')),
        _Renglon('Con orden', c.conOrden, clave: Key('informes-con-orden-${c.proveedorId}')),
        if (!c.devoluciones.esCero)
          _Renglon('Devoluciones', c.devoluciones, resta: true, clave: Key('informes-devoluciones-${c.proveedorId}')),
      ]),
    );
  }
}

class _Renglon extends StatelessWidget {
  const _Renglon(this.etiqueta, this.importe, {required this.clave, this.resta = false});

  final String etiqueta;
  final ImporteDeCompra importe;
  final bool resta;
  final Key clave;

  @override
  Widget build(BuildContext context) {
    final color = resta ? RegentaColors.crit : RegentaColors.ink2;
    final signo = resta ? '− ' : '';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(children: [
        SizedBox(width: 96, child: Text(etiqueta, style: RegentaType.cuerpo.copyWith(fontSize: 13, color: color))),
        Expanded(
          child: Text('$signo${formatearPesos(importe.base)} + IVA ${formatearPesos(importe.iva)}',
              overflow: TextOverflow.ellipsis,
              style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        ),
        Text('$signo${formatearPesos(importe.total)}',
            key: clave, style: RegentaType.cuerpo.copyWith(fontSize: 13, color: color, fontWeight: FontWeight.w600)),
      ]),
    );
  }
}

/// C2 · Cantidad, costo total y costo promedio por producto; tocar uno abre su historial (C3).
class _ListaPorProducto extends StatelessWidget {
  const _ListaPorProducto({required this.filas, required this.onHistorial});

  final List<CompraPorProductoVista>? filas;
  final ValueChanged<CompraPorProductoVista> onHistorial;

  @override
  Widget build(BuildContext context) {
    final f = filas;
    if (f == null) return const Center(child: CircularProgressIndicator());
    if (f.isEmpty) {
      return const _Vacio('No entró ningún producto en este periodo.', clave: Key('informes-producto-vacio'));
    }
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(0, 4, 0, 24),
      itemCount: f.length,
      separatorBuilder: (_, _) => const Divider(height: 1, color: RegentaColors.line),
      itemBuilder: (_, i) {
        final p = f[i];
        return ListTile(
          key: Key('informes-producto-${p.productoId}'),
          tileColor: RegentaColors.surface,
          onTap: () => onHistorial(p),
          title: Text(p.producto,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
          subtitle: Text(
            '${formatearCantidad(p.cantidad)} · promedio '
            '${p.costoPromedio == null ? '—' : formatearPesos(p.costoPromedio!)}',
            key: Key('informes-producto-detalle-${p.productoId}'),
            style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted),
          ),
          trailing: Row(mainAxisSize: MainAxisSize.min, children: [
            Text(formatearPesos(p.costoTotal),
                key: Key('informes-producto-total-${p.productoId}'),
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
            const Icon(Icons.chevron_right, color: RegentaColors.faint),
          ]),
        );
      },
    );
  }
}

/// C3 · Cada costo de entrada del producto, del más reciente al más antiguo.
class _Historial extends StatelessWidget {
  const _Historial({required this.producto, required this.entradas});

  final CompraPorProductoVista? producto;
  final List<CostoHistoricoVista>? entradas;

  @override
  Widget build(BuildContext context) {
    final p = producto;
    if (p == null) {
      return const _Vacio('Elige un producto en «Por producto» para ver cómo ha cambiado su costo.',
          clave: Key('informes-historial-sin-producto'));
    }
    final e = entradas;
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Container(
        color: RegentaColors.surface,
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
        child: Text(p.producto,
            key: const Key('informes-historial-producto'),
            style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
      ),
      Expanded(
        child: e == null
            ? const Center(child: CircularProgressIndicator())
            : e.isEmpty
                ? const _Vacio('Este producto no tiene entradas confirmadas.', clave: Key('informes-historial-vacio'))
                : ListView.separated(
                    padding: const EdgeInsets.only(bottom: 24),
                    itemCount: e.length,
                    separatorBuilder: (_, _) => const Divider(height: 1, color: RegentaColors.line),
                    itemBuilder: (_, i) {
                      final h = e[i];
                      final anterior = i + 1 < e.length ? e[i + 1].costoEntrada : null;
                      return ListTile(
                        key: Key('informes-historial-${h.recepcionId}'),
                        tileColor: RegentaColors.surface,
                        title: Text(h.proveedor,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
                        subtitle: Text(
                          '${fechaCorta(h.fecha)} · ${h.recepcion} · ${h.conOrden ? 'con orden' : 'directa'} · '
                          '${formatearCantidad(h.cantidad)}',
                          style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted),
                        ),
                        trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                          if (anterior != null && anterior != h.costoEntrada)
                            Icon(h.costoEntrada > anterior ? Icons.arrow_upward : Icons.arrow_downward,
                                size: 16, color: h.costoEntrada > anterior ? RegentaColors.crit : RegentaColors.ok),
                          const SizedBox(width: 4),
                          Text(formatearPesos(h.costoEntrada),
                              key: Key('informes-historial-costo-${h.recepcionId}'),
                              style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
                        ]),
                      );
                    },
                  ),
      ),
    ]);
  }
}

/// Una tarjeta blanca con borde, como las del resto de Compras.
class _Tarjeta extends StatelessWidget {
  const _Tarjeta({required this.clave, required this.child, this.borde = RegentaColors.line});

  final Key clave;
  final Widget child;
  final Color borde;

  @override
  Widget build(BuildContext context) => Container(
        key: clave,
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        decoration: BoxDecoration(
          color: RegentaColors.surface,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: borde),
        ),
        child: child,
      );
}

TextStyle get _titulo => RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700);

TextStyle get _detalle => RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted);

/// HU-184 C1 · Las cinco columnas de mora de un saldo; las vencidas en rojo cuando tienen saldo.
class _ColumnasDeMora extends StatelessWidget {
  const _ColumnasDeMora({required this.tramos, required this.prefijo});

  final TramosDeMora tramos;
  final String prefijo;

  @override
  Widget build(BuildContext context) {
    final columnas = tramos.columnas;
    return Row(children: [
      for (var i = 0; i < columnas.length; i++)
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(columnas[i].$1, style: _detalle),
            const SizedBox(height: 2),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(
                columnas[i].$2 == 0 ? '—' : formatearPesos(columnas[i].$2),
                key: Key('$prefijo-$i'),
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: i > 0 && columnas[i].$2 != 0 ? RegentaColors.crit : RegentaColors.ink,
                ),
              ),
            ),
          ]),
        ),
    ]);
  }
}

/// HU-184 C1 · Cuánto se debe a cada proveedor por días de mora, con el total de cada columna arriba.
class _Antiguedad extends StatelessWidget {
  const _Antiguedad({required this.informe});

  final InformeDeAntiguedadVista? informe;

  @override
  Widget build(BuildContext context) {
    final a = informe;
    if (a == null) return const Center(child: CircularProgressIndicator());
    if (a.proveedores.isEmpty) {
      return const _Vacio('No se le debe nada a ningún proveedor.', clave: Key('informes-antiguedad-vacio'));
    }
    return ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
      _Total(etiqueta: 'Total por pagar', valor: a.totales.total, clave: const Key('informes-antiguedad-total')),
      const SizedBox(height: 8),
      _Tarjeta(
        clave: const Key('informes-antiguedad-totales'),
        child: _ColumnasDeMora(tramos: a.totales, prefijo: 'informes-antiguedad-columna'),
      ),
      for (final p in a.proveedores)
        _Tarjeta(
          clave: Key('informes-antiguedad-${p.proveedorId}'),
          borde: p.moraMaxima > 0 ? RegentaColors.crit : RegentaColors.line,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Row(children: [
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(p.proveedor, maxLines: 1, overflow: TextOverflow.ellipsis, style: _titulo),
                  const SizedBox(height: 2),
                  Text(
                    '${p.cuentas == 1 ? '1 factura' : '${p.cuentas} facturas'}'
                    '${p.moraMaxima > 0 ? ' · la más vieja con ${p.moraMaxima} días de mora' : ' · al día'}',
                    style: _detalle,
                  ),
                ]),
              ),
              Text(formatearPesos(p.saldo.total), key: Key('informes-antiguedad-saldo-${p.proveedorId}'), style: _titulo),
            ]),
            const Divider(height: 18, color: RegentaColors.line),
            _ColumnasDeMora(tramos: p.saldo, prefijo: 'informes-antiguedad-${p.proveedorId}'),
          ]),
        ),
    ]);
  }
}

/// HU-184 C2 / C3 · Una línea con lo pedido, lo recibido y lo que falta.
class _LineaQueFalta extends StatelessWidget {
  const _LineaQueFalta({required this.linea, required this.clave, required this.queFalta});

  final LineaPendienteVista linea;
  final Key clave;
  final String queFalta;

  @override
  Widget build(BuildContext context) {
    final l = linea;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(l.producto,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink)),
            Text('Pedido ${formatearCantidad(l.pedida)} · llegó ${formatearCantidad(l.recibida)}', style: _detalle),
          ]),
        ),
        Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Text('$queFalta ${formatearCantidad(l.faltante)}',
              key: clave,
              style: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          Text(formatearPesos(l.valor), style: _detalle),
        ]),
      ]),
    );
  }
}

/// HU-184 C2 · Lo que falta por recibir, orden por orden; las atrasadas primero y en rojo.
class _PorRecibir extends StatelessWidget {
  const _PorRecibir({required this.ordenes});

  final List<PendientePorRecibirVista>? ordenes;

  @override
  Widget build(BuildContext context) {
    final o = ordenes;
    if (o == null) return const Center(child: CircularProgressIndicator());
    if (o.isEmpty) {
      return const _Vacio('No hay nada pendiente por recibir.', clave: Key('informes-por-recibir-vacio'));
    }
    final atrasadas = o.where((x) => x.atrasada).length;
    return ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
      _Total(
        etiqueta: atrasadas == 0
            ? 'Por recibir'
            : 'Por recibir · ${atrasadas == 1 ? '1 orden atrasada' : '$atrasadas órdenes atrasadas'}',
        valor: o.fold<num>(0, (s, x) => s + x.valorPendiente),
        clave: const Key('informes-por-recibir-total'),
      ),
      const SizedBox(height: 12),
      for (final orden in o)
        _Tarjeta(
          clave: Key('informes-por-recibir-${orden.ordenId}'),
          borde: orden.atrasada ? RegentaColors.crit : RegentaColors.line,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Row(children: [
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${orden.numero} · ${orden.proveedor}',
                      maxLines: 1, overflow: TextOverflow.ellipsis, style: _titulo),
                  const SizedBox(height: 2),
                  Text(
                    orden.fechaEsperada == null
                        ? 'Sin fecha esperada'
                        : 'Se esperaba el ${fechaCorta(orden.fechaEsperada!)}',
                    key: Key('informes-por-recibir-fecha-${orden.ordenId}'),
                    style: _detalle,
                  ),
                ]),
              ),
              if (orden.atrasada)
                ChipDeEstado(
                  key: Key('informes-atrasada-${orden.ordenId}'),
                  texto: orden.diasDeAtraso == 1 ? 'ATRASADA · 1 DÍA' : 'ATRASADA · ${orden.diasDeAtraso} DÍAS',
                  fondo: RegentaColors.critSoft,
                  tinta: RegentaColors.crit,
                ),
            ]),
            const Divider(height: 18, color: RegentaColors.line),
            for (final l in orden.lineas)
              _LineaQueFalta(
                linea: l,
                queFalta: 'Faltan',
                clave: Key('informes-por-recibir-${orden.ordenId}-${l.linea}'),
              ),
          ]),
        ),
    ]);
  }
}

/// HU-184 C3 · Lo que nunca llegó de las órdenes cerradas con faltante.
class _Faltantes extends StatelessWidget {
  const _Faltantes({required this.ordenes});

  final List<FaltanteDeOrdenVista>? ordenes;

  @override
  Widget build(BuildContext context) {
    final o = ordenes;
    if (o == null) return const Center(child: CircularProgressIndicator());
    if (o.isEmpty) {
      return const _Vacio('Ninguna orden se cerró con faltante.', clave: Key('informes-faltantes-vacio'));
    }
    return ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
      _Total(
        etiqueta: 'Nunca llegó',
        valor: o.fold<num>(0, (s, x) => s + x.valorFaltante),
        clave: const Key('informes-faltantes-total'),
      ),
      const SizedBox(height: 12),
      for (final orden in o)
        _Tarjeta(
          clave: Key('informes-faltantes-${orden.ordenId}'),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text('${orden.numero} · ${orden.proveedor}', maxLines: 1, overflow: TextOverflow.ellipsis, style: _titulo),
            const SizedBox(height: 2),
            Text(
              [
                if (orden.cerradaEn != null) 'Cerrada el ${fechaCorta(orden.cerradaEn!)}',
                if (orden.motivo != null && orden.motivo!.isNotEmpty) orden.motivo!,
              ].join(' · '),
              key: Key('informes-faltantes-motivo-${orden.ordenId}'),
              style: _detalle,
            ),
            const Divider(height: 18, color: RegentaColors.line),
            for (final l in orden.lineas)
              _LineaQueFalta(
                linea: l,
                queFalta: 'No llegó',
                clave: Key('informes-faltantes-${orden.ordenId}-${l.linea}'),
              ),
          ]),
        ),
    ]);
  }
}

/// HU-184 C4 · Los pagos del periodo: total, por método y por proveedor y método.
class _Pagos extends StatelessWidget {
  const _Pagos({required this.informe});

  final InformeDePagosVista? informe;

  @override
  Widget build(BuildContext context) {
    final p = informe;
    if (p == null) return const Center(child: CircularProgressIndicator());
    if (p.filas.isEmpty) {
      return const _Vacio('No hubo pagos a proveedores en este periodo.', clave: Key('informes-pagos-vacio'));
    }
    return ListView(padding: const EdgeInsets.fromLTRB(14, 12, 14, 24), children: [
      _Total(etiqueta: 'Total pagado', valor: p.total, clave: const Key('informes-pagos-total')),
      const SizedBox(height: 8),
      Wrap(spacing: 8, runSpacing: 6, children: [
        for (final e in p.porMetodo.entries)
          Chip(
            key: Key('informes-pagos-metodo-${e.key}'),
            label: Text('${nombreDelMetodo(e.key)} ${formatearPesos(e.value)}'),
          ),
      ]),
      const SizedBox(height: 12),
      for (final f in p.filas)
        _Tarjeta(
          clave: Key('informes-pagos-${f.proveedorId}-${f.metodo}'),
          child: Row(children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(f.proveedor, maxLines: 1, overflow: TextOverflow.ellipsis, style: _titulo),
                const SizedBox(height: 2),
                Text('${nombreDelMetodo(f.metodo)} · ${f.pagos == 1 ? '1 pago' : '${f.pagos} pagos'}', style: _detalle),
              ]),
            ),
            Text(formatearPesos(f.total), key: Key('informes-pagos-total-${f.proveedorId}-${f.metodo}'), style: _titulo),
          ]),
        ),
    ]);
  }
}
