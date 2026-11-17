import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/ficha_de_producto.dart';
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
      if (!mounted) return;
      setState(() {
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
