import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/ficha_de_producto.dart';
import '../datos/tablas_de_fracciones.dart';
import 'formato.dart';

/// HU-150 · la ficha de un producto: se edita nombre, SKU, código de barras, categoría,
/// unidad, tarifa de IVA, precio (con IVA incluido) y costo. Mientras se escribe se ven la base,
/// el IVA, el margen y el beneficio. Sin `INVENTARIO_PRODUCTO_EDITAR` se ve sin poder guardar.
class PantallaFichaDeProducto extends ConsumerStatefulWidget {
  const PantallaFichaDeProducto({super.key, required this.productoId});

  final String productoId;

  @override
  ConsumerState<PantallaFichaDeProducto> createState() => _PantallaFichaDeProductoState();
}

class _PantallaFichaDeProductoState extends ConsumerState<PantallaFichaDeProducto> {
  final _nombre = TextEditingController();
  final _sku = TextEditingController();
  final _codigo = TextEditingController();
  final _precio = TextEditingController();
  final _costo = TextEditingController();

  FichaDeProducto? _ficha;

  /// HU-152: la ubicación en cada bodega, y lo que tenía al cargar (solo se guarda lo que cambió).
  List<UbicacionEnBodega> _ubicaciones = const [];

  /// HU-157: las fracciones del producto con su origen (de la tabla o propio).
  List<FraccionDeTabla> _fracciones = const [];
  final Map<String, TextEditingController> _ubicacionCtrl = {};
  List<OpcionDeFicha> _categorias = const [];
  List<OpcionDeFicha> _unidades = const [];
  String? _categoriaId;
  String? _unidadId;
  String _tarifa = 'IVA_19';
  String? _error;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    for (final c in [_nombre, _sku, _codigo, _precio, _costo, ..._ubicacionCtrl.values]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    final repo = ref.read(repositorioDeInventarioProvider);
    try {
      final ficha = await repo.verProducto(widget.productoId);
      final categorias = await repo.categorias();
      final unidades = await repo.unidades();
      final ubicaciones = await repo.ubicacionesDe(widget.productoId);
      final fracciones = await repo.fraccionesDe(widget.productoId);
      if (!mounted) return;
      setState(() {
        _fracciones = fracciones;
        _categorias = categorias;
        _unidades = unidades;
        _ubicaciones = ubicaciones;
        for (final u in ubicaciones) {
          _ubicacionCtrl[u.bodegaId] = TextEditingController(text: u.ubicacion ?? '');
        }
        _mostrar(ficha);
      });
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  void _mostrar(FichaDeProducto f) {
    _ficha = f;
    _nombre.text = f.nombre;
    _sku.text = f.sku;
    _codigo.text = f.codigoBarras ?? '';
    _precio.text = _sinDecimalesInutiles(f.precioVenta);
    _costo.text = _sinDecimalesInutiles(f.costo);
    _categoriaId = f.categoriaId;
    _unidadId = f.unidadMedidaId;
    _tarifa = f.tarifaIva;
  }

  static String _sinDecimalesInutiles(num v) =>
      v == v.roundToDouble() ? v.round().toString() : v.toString();

  static num _numero(String texto) => num.tryParse(texto.trim().replaceAll(',', '.')) ?? 0;

  Future<void> _guardar() async {
    final f = _ficha;
    if (f == null || _categoriaId == null || _unidadId == null) return;
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      final guardada = await ref.read(repositorioDeInventarioProvider).editarProducto(
            f.id,
            EdicionDeProducto(
              sku: _sku.text.trim(),
              codigoBarras: _codigo.text.trim(),
              nombre: _nombre.text.trim(),
              categoriaId: _categoriaId!,
              unidadMedidaId: _unidadId!,
              tarifaIva: _tarifa,
              precioVenta: _numero(_precio.text),
              costo: _numero(_costo.text),
              version: f.version,
            ),
          );
      // HU-152: solo las ubicaciones que cambiaron.
      final repo = ref.read(repositorioDeInventarioProvider);
      final nuevas = <UbicacionEnBodega>[];
      for (final u in _ubicaciones) {
        final texto = _ubicacionCtrl[u.bodegaId]!.text.trim();
        if (texto != (u.ubicacion ?? '')) await repo.fijarUbicacion(f.id, u.bodegaId, texto);
        nuevas.add(UbicacionEnBodega(
            bodegaId: u.bodegaId,
            bodegaNombre: u.bodegaNombre,
            ubicacion: texto.isEmpty ? null : texto));
      }
      if (!mounted) return;
      setState(() {
        _ubicaciones = nuevas;
        _mostrar(guardada);
      });
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Producto guardado')));
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  /// HU-157 C1 · precio propio de una fracción solo para este producto.
  Future<void> _precioPropio(FraccionDeTabla f) async {
    final ctrl = TextEditingController(text: _sinDecimalesInutiles(f.precio));
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Precio propio de ${f.medida}'),
        content: TextField(
          key: const Key('precio-propio-valor'),
          controller: ctrl,
          autofocus: true,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: 'Precio (con IVA incluido)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
          FilledButton(
              key: const Key('precio-propio-guardar'),
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Guardar')),
        ],
      ),
    );
    if (ok != true) return;
    await _cambiarFraccion(() => ref
        .read(repositorioDeInventarioProvider)
        .fijarPrecioPropio(widget.productoId, f.medida, _numero(ctrl.text)));
  }

  Future<void> _cambiarFraccion(Future<void> Function() accion) async {
    try {
      await accion();
      final fracciones =
          await ref.read(repositorioDeInventarioProvider).fraccionesDe(widget.productoId);
      if (mounted) setState(() => _fracciones = fracciones);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  Widget _seccionFracciones(bool puedeGestionar) {
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      const SizedBox(height: 16),
      Text('Fracciones', style: RegentaType.seccion.copyWith(color: RegentaColors.inkFuerte)),
      for (final f in _fracciones)
        ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(f.medida, style: const TextStyle(color: RegentaColors.inkFuerte)),
          subtitle: Text(f.origen == 'PROPIO' ? 'propio' : 'de la tabla',
              style: TextStyle(color: f.origen == 'PROPIO' ? RegentaColors.accent : RegentaColors.ink2)),
          trailing: Row(mainAxisSize: MainAxisSize.min, children: [
            Text(formatearPesos(f.precio),
                style: RegentaType.dinero.copyWith(color: RegentaColors.inkFuerte)),
            if (puedeGestionar) ...[
              IconButton(
                key: Key('precio-propio-${f.medida}'),
                tooltip: 'Precio propio para este producto',
                icon: const Icon(Icons.edit_outlined, size: 18),
                onPressed: () => _precioPropio(f),
              ),
              if (f.origen == 'PROPIO')
                IconButton(
                  key: Key('quitar-propio-${f.medida}'),
                  tooltip: 'Volver al precio de la tabla',
                  icon: const Icon(Icons.undo, size: 18),
                  onPressed: () => _cambiarFraccion(() => ref
                      .read(repositorioDeInventarioProvider)
                      .quitarPrecioPropio(widget.productoId, f.medida)),
                ),
            ],
          ]),
        ),
    ]);
  }

  /// HU-159 · sumar, restar o contar en una bodega, con tipo y motivo.
  Future<void> _ajustar() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => _DialogoDeAjuste(productoId: widget.productoId, bodegas: _ubicaciones),
    );
    if (ok == true && mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Existencia ajustada')));
    }
  }

  /// HU-151 C1 · desactivar pide confirmación y avisa la existencia que queda.
  Future<void> _desactivar() async {
    final f = _ficha;
    if (f == null) return;
    final repo = ref.read(repositorioDeInventarioProvider);
    final existencias = await repo.existenciasDe(f.id);
    if (!mounted) return;
    final aviso = existencias
        .map((e) => 'Tiene ${_sinDecimalesInutiles(e.$2)} unidades en ${e.$1}.')
        .join(' ');
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('¿Desactivar ${f.nombre}?'),
        content: Text(
          'No aparecerá en el POS ni en las búsquedas. Su historial de ventas, compras y '
          'movimientos se conserva.${aviso.isEmpty ? '' : '\n\n$aviso'}',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Desactivar'),
          ),
        ],
      ),
    );
    if (confirmado == true) await _cambiarActivo(false);
  }

  Future<void> _cambiarActivo(bool activo) async {
    final f = _ficha;
    if (f == null) return;
    try {
      final nueva =
          await ref.read(repositorioDeInventarioProvider).cambiarActivo(f.id, activo: activo);
      if (mounted) setState(() => _mostrar(nueva));
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    final puedeEditar = ref.watch(puedeEditarProductosProvider);
    final f = _ficha;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: Text(f?.nombre ?? 'Producto')),
      body: f == null
          ? Center(
              child: _error != null
                  ? Text(_error!, style: const TextStyle(color: RegentaColors.crit))
                  : const CircularProgressIndicator(),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (!f.activo)
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: Chip(
                      avatar: Icon(Icons.block, size: 18, color: RegentaColors.crit),
                      label: Text('Inactivo'),
                    ),
                  ),
                if (f.tarifaPorRevisar)
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: Chip(
                      avatar: Icon(Icons.flag_outlined, size: 18, color: RegentaColors.warn),
                      label: Text('Tarifa por revisar'),
                    ),
                  ),
                if (_error != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
                  ),
                _campo('Nombre', _nombre, puedeEditar),
                _campo('SKU', _sku, puedeEditar),
                _campo('Código de barras', _codigo, puedeEditar),
                _lista('Categoría', _categoriaId, _categorias, puedeEditar,
                    (v) => setState(() => _categoriaId = v)),
                _lista('Unidad', _unidadId, _unidades, puedeEditar,
                    (v) => setState(() => _unidadId = v)),
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: DropdownButtonFormField<String>(
                    key: const Key('ficha-tarifa'),
                    initialValue: _tarifa,
                    decoration: const InputDecoration(
                        labelText: 'Tarifa de IVA', border: OutlineInputBorder()),
                    items: [
                      for (final t in tarifasDeIva)
                        DropdownMenuItem(value: t.$1, child: Text(t.$2)),
                    ],
                    onChanged: puedeEditar ? (v) => setState(() => _tarifa = v!) : null,
                  ),
                ),
                _campo('Precio de venta (con IVA incluido)', _precio, puedeEditar,
                    clave: const Key('ficha-precio'), numero: true),
                _campo('Costo', _costo, puedeEditar, clave: const Key('ficha-costo'), numero: true),
                _resumen(),
                if (_fracciones.isNotEmpty) _seccionFracciones(ref.watch(puedeGestionarFraccionesProvider)),
                if (ref.watch(puedeAjustarExistenciasProvider) && _ubicaciones.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: OutlinedButton.icon(
                      key: const Key('ficha-ajustar'),
                      icon: const Icon(Icons.tune, size: 18),
                      label: const Text('Ajustar existencia'),
                      onPressed: _ajustar,
                    ),
                  ),
                if (_ubicaciones.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  Text('Ubicación en cada bodega',
                      style: RegentaType.seccion.copyWith(color: RegentaColors.inkFuerte)),
                  const SizedBox(height: 8),
                  for (final u in _ubicaciones)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: TextField(
                        key: Key('ubicacion-${u.bodegaId}'),
                        controller: _ubicacionCtrl[u.bodegaId],
                        enabled: puedeEditar,
                        maxLength: 60,
                        style: const TextStyle(color: RegentaColors.inkFuerte),
                        decoration: InputDecoration(
                          labelText: u.bodegaNombre,
                          hintText: 'Ej. Pasillo 3 – E2',
                          border: const OutlineInputBorder(),
                          counterText: '',
                        ),
                      ),
                    ),
                ],
                if (puedeEditar) ...[
                  const SizedBox(height: 16),
                  FilledButton(
                    key: const Key('ficha-guardar'),
                    style: FilledButton.styleFrom(
                      backgroundColor: RegentaColors.accent,
                      minimumSize: const Size.fromHeight(RegentaSpacing.hitTarget),
                    ),
                    onPressed: _guardando ? null : _guardar,
                    child: Text(_guardando ? 'Guardando…' : 'Guardar'),
                  ),
                  const SizedBox(height: 8),
                  // HU-151: nunca se borra; se desactiva y se puede reactivar.
                  if (f.activo)
                    OutlinedButton.icon(
                      key: const Key('ficha-desactivar'),
                      icon: const Icon(Icons.block, size: 18, color: RegentaColors.crit),
                      label: const Text('Desactivar producto'),
                      onPressed: _desactivar,
                    )
                  else
                    OutlinedButton.icon(
                      key: const Key('ficha-reactivar'),
                      icon: const Icon(Icons.check_circle_outline, size: 18),
                      label: const Text('Reactivar producto'),
                      onPressed: () => _cambiarActivo(true),
                    ),
                ],
              ],
            ),
    );
  }

  Widget _resumen() {
    final c = CalculoDeFicha(
        precio: _numero(_precio.text), tarifa: _tarifa, costo: _numero(_costo.text));
    String pct(num? v) => v == null ? '—' : '${v.toStringAsFixed(2).replaceAll('.', ',')} %';
    Widget fila(String etiqueta, String valor) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 3),
          child: Row(children: [
            Expanded(
                child: Text(etiqueta,
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2))),
            Text(valor, style: RegentaType.dinero.copyWith(color: RegentaColors.inkFuerte)),
          ]),
        );
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(children: [
        fila('Base (sin IVA)', formatearPesos(c.base)),
        fila('IVA', formatearPesos(c.iva)),
        fila('Margen sobre el costo', pct(c.margenPct)),
        fila('Beneficio sobre la venta', pct(c.beneficioPct)),
      ]),
    );
  }

  Widget _campo(String etiqueta, TextEditingController ctrl, bool habilitado,
      {Key? clave, bool numero = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        key: clave,
        controller: ctrl,
        enabled: habilitado,
        keyboardType: numero ? const TextInputType.numberWithOptions(decimal: true) : null,
        style: const TextStyle(color: RegentaColors.inkFuerte),
        decoration: InputDecoration(labelText: etiqueta, border: const OutlineInputBorder()),
        onChanged: (_) => setState(() {}),
      ),
    );
  }

  Widget _lista(String etiqueta, String? valor, List<OpcionDeFicha> opciones, bool habilitado,
      ValueChanged<String?> alCambiar) {
    final existe = opciones.any((o) => o.id == valor);
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: DropdownButtonFormField<String>(
        initialValue: existe ? valor : null,
        decoration: InputDecoration(labelText: etiqueta, border: const OutlineInputBorder()),
        items: [
          for (final o in opciones) DropdownMenuItem(value: o.id, child: Text(o.nombre)),
        ],
        onChanged: habilitado ? alCambiar : null,
      ),
    );
  }
}

/// HU-159 · el diálogo de ajuste: bodega, operación, cantidad, tipo y motivo. Si el backend
/// rechaza (por ejemplo, restar más de lo que hay), muestra su mensaje y no se cierra.
class _DialogoDeAjuste extends ConsumerStatefulWidget {
  const _DialogoDeAjuste({required this.productoId, required this.bodegas});

  final String productoId;
  final List<UbicacionEnBodega> bodegas;

  @override
  ConsumerState<_DialogoDeAjuste> createState() => _DialogoDeAjusteState();
}

class _DialogoDeAjusteState extends ConsumerState<_DialogoDeAjuste> {
  late String _bodegaId = widget.bodegas.first.bodegaId;
  String _operacion = 'SUMAR';
  String _tipo = 'CONTEO_FISICO';
  final _cantidad = TextEditingController();
  final _motivo = TextEditingController();
  String? _error;
  bool _enviando = false;

  @override
  void dispose() {
    _cantidad.dispose();
    _motivo.dispose();
    super.dispose();
  }

  Future<void> _aplicar() async {
    final cantidad = num.tryParse(_cantidad.text.trim().replaceAll(',', '.'));
    if (cantidad == null || cantidad < 0 || _motivo.text.trim().isEmpty) {
      setState(() => _error = 'Escribe la cantidad y el motivo.');
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await ref.read(repositorioDeInventarioProvider).ajustarExistencia(AjusteDeExistencia(
            productoId: widget.productoId,
            bodegaId: _bodegaId,
            operacion: _operacion,
            cantidad: cantidad,
            tipo: _tipo,
            motivo: _motivo.text.trim(),
          ));
      if (mounted) Navigator.of(context).pop(true);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Ajustar existencia'),
      content: SizedBox(
        width: 420,
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          DropdownButtonFormField<String>(
            initialValue: _bodegaId,
            decoration: const InputDecoration(labelText: 'Bodega'),
            items: [
              for (final b in widget.bodegas)
                DropdownMenuItem(value: b.bodegaId, child: Text(b.bodegaNombre)),
            ],
            onChanged: (v) => setState(() => _bodegaId = v!),
          ),
          const SizedBox(height: 12),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'SUMAR', label: Text('Sumar')),
              ButtonSegment(value: 'RESTAR', label: Text('Restar')),
              ButtonSegment(value: 'CONTEO', label: Text('Conteo físico')),
            ],
            selected: {_operacion},
            onSelectionChanged: (s) => setState(() => _operacion = s.first),
          ),
          const SizedBox(height: 12),
          TextField(
            key: const Key('ajuste-cantidad'),
            controller: _cantidad,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
                labelText: _operacion == 'CONTEO' ? 'Cantidad contada' : 'Cantidad'),
          ),
          DropdownButtonFormField<String>(
            key: const Key('ajuste-tipo'),
            initialValue: _tipo,
            decoration: const InputDecoration(labelText: 'Tipo'),
            items: [
              for (final t in tiposDeAjuste) DropdownMenuItem(value: t.$1, child: Text(t.$2)),
            ],
            onChanged: (v) => setState(() => _tipo = v!),
          ),
          TextField(
            key: const Key('ajuste-motivo'),
            controller: _motivo,
            decoration: const InputDecoration(labelText: 'Motivo'),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
            ),
        ]),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancelar')),
        FilledButton(
          key: const Key('ajuste-aplicar'),
          onPressed: _enviando ? null : _aplicar,
          child: const Text('Aplicar'),
        ),
      ],
    );
  }
}