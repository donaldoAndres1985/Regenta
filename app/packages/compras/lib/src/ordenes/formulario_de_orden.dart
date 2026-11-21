import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../compra_directa/controlador.dart';
import '../compra_directa/datos.dart';
import '../proveedores/datos/producto_del_proveedor.dart';
import '../proveedores/datos/proveedor_del_negocio.dart';
import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';

/// Una línea de la orden mientras se arma.
class _Linea {
  _Linea(this.producto, {num cantidad = 1, num? costo})
      : cantidad = TextEditingController(text: '$cantidad'),
        costo = TextEditingController(text: costo == null ? '' : '${costo.round()}'),
        descuento = TextEditingController();

  final ProductoParaComprar producto;
  final TextEditingController cantidad;
  final TextEditingController costo;
  final TextEditingController descuento;

  num get cantidadNum => num.tryParse(cantidad.text.replaceAll(',', '.')) ?? 0;
  num? get costoNum => num.tryParse(costo.text.replaceAll(RegExp(r'[^0-9.]'), ''));
  num get descuentoNum => num.tryParse(descuento.text.replaceAll(',', '.')) ?? 0;
  num get base => cantidadNum * (costoNum ?? 0) * (1 - descuentoNum / 100);
  num get iva => base * producto.impuestoPct / 100;

  Map<String, dynamic> aJson() => {
        'productoId': producto.productoId,
        'cantidad': cantidadNum,
        if (costoNum != null) 'costoUnitario': costoNum,
        'descuentoPct': descuentoNum,
      };

  void dispose() {
    cantidad.dispose();
    costo.dispose();
    descuento.dispose();
  }
}

/// HU-174 · Armar una orden de compra nueva, o cambiar las líneas de una en borrador ([orden]).
/// Devuelve la orden guardada.
class FormularioDeOrden extends ConsumerStatefulWidget {
  const FormularioDeOrden({super.key, this.orden});

  final OrdenDeCompraVista? orden;

  @override
  ConsumerState<FormularioDeOrden> createState() => _FormularioDeOrdenState();
}

class _FormularioDeOrdenState extends ConsumerState<FormularioDeOrden> {
  final TextEditingController _buscador = TextEditingController();
  final TextEditingController _flete = TextEditingController();
  final TextEditingController _observaciones = TextEditingController();
  final List<_Linea> _lineas = [];
  List<ProveedorDelNegocio> _proveedores = const [];
  List<BodegaParaComprar> _bodegas = const [];
  List<ProductoEncontrado> _resultados = const [];
  String? _proveedorId;
  String? _bodegaId;
  DateTime? _fechaEsperada;
  String? _error;
  bool _cargando = true;
  bool _guardando = false;

  bool get _editando => widget.orden != null;
  RepositorioDeCompraDirecta get _catalogos => ref.read(repositorioDeCompraDirectaProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  @override
  void dispose() {
    _buscador.dispose();
    _flete.dispose();
    _observaciones.dispose();
    for (final l in _lineas) {
      l.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    try {
      final proveedores = (await _catalogos.proveedoresActivos()).where((p) => p.activo).toList();
      final bodegas = await _catalogos.bodegas();
      final o = widget.orden;
      if (o != null) {
        for (final l in o.lineas) {
          final p = await _catalogos.paraComprar(o.proveedorId, l.productoId);
          _lineas.add(_Linea(p, cantidad: l.cantidadPedida, costo: l.costoUnitario)
            ..descuento.text = l.descuentoPct == 0 ? '' : '${l.descuentoPct}');
        }
      }
      if (!mounted) return;
      setState(() {
        _proveedores = proveedores;
        _bodegas = bodegas;
        _proveedorId = o?.proveedorId;
        _bodegaId = o?.bodegaId ?? (bodegas.where((b) => b.esDefault).firstOrNull ?? bodegas.firstOrNull)?.id;
        _cargando = false;
      });
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = fallo.mensaje;
      });
    }
  }

  Future<void> _buscar() async {
    if (_proveedorId == null) {
      setState(() => _error = 'Elige primero el proveedor.');
      return;
    }
    if (_buscador.text.trim().length < 3) {
      setState(() => _error = 'Escribe al menos tres letras del nombre, el SKU o el código.');
      return;
    }
    try {
      final r = await _catalogos.buscarProductos(_buscador.text);
      if (!mounted) return;
      setState(() {
        _resultados = r;
        _error = r.isEmpty ? 'No hay productos con «${_buscador.text.trim()}».' : null;
      });
    } on ErrorDeApi catch (fallo) {
      setState(() => _error = fallo.mensaje);
    }
  }

  Future<void> _agregar(ProductoEncontrado p) async {
    if (_lineas.any((l) => l.producto.productoId == p.id)) {
      setState(() {
        _resultados = const [];
        _error = '«${p.nombre}» ya está en la orden.';
      });
      return;
    }
    try {
      // HU-174 C2 · costo sugerido del catálogo del proveedor y tarifa de IVA del producto.
      final producto = await _catalogos.paraComprar(_proveedorId!, p.id);
      if (!mounted) return;
      setState(() {
        _lineas.add(_Linea(producto, costo: producto.costoSugerido));
        _resultados = const [];
        _buscador.clear();
        _error = null;
      });
    } on ErrorDeApi catch (fallo) {
      setState(() => _error = fallo.mensaje);
    }
  }

  String? _validar() {
    if (_proveedorId == null) return 'Elige el proveedor.';
    if (_bodegaId == null) return 'Elige la bodega a la que llega la mercancía.';
    if (_lineas.isEmpty) return 'Agrega al menos un producto.';
    for (final l in _lineas) {
      if (l.cantidadNum <= 0) return 'La cantidad de «${l.producto.nombre}» debe ser mayor que cero.';
      if (l.costoNum == null) return 'Falta el costo de «${l.producto.nombre}».';
    }
    return null;
  }

  Future<void> _guardar() async {
    final problema = _validar();
    if (problema != null) {
      setState(() => _error = problema);
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    final repo = ref.read(repositorioDeOrdenesProvider);
    try {
      final lineas = [for (final l in _lineas) l.aJson()];
      final guardada = _editando
          ? await repo.editarLineas(widget.orden!.id, lineas)
          : await repo.crear({
              'proveedorId': _proveedorId,
              'bodegaDestinoId': _bodegaId,
              if (_fechaEsperada != null) 'fechaEsperada': _fechaEsperada!.toIso8601String().substring(0, 10),
              'flete': num.tryParse(_flete.text.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0,
              if (_observaciones.text.trim().isNotEmpty) 'observaciones': _observaciones.text.trim(),
              'lineas': lineas,
            });
      if (mounted) Navigator.of(context).pop(guardada);
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _guardando = false;
        _error = fallo is ErrorDeRed ? 'No hay conexión: la orden no se guardó.' : fallo.mensaje;
      });
    }
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final f = await showDatePicker(
      context: context,
      initialDate: _fechaEsperada ?? hoy.add(const Duration(days: 3)),
      firstDate: hoy,
      lastDate: DateTime(hoy.year + 1, hoy.month, hoy.day),
    );
    if (f != null) setState(() => _fechaEsperada = f);
  }

  @override
  Widget build(BuildContext context) {
    final base = _lineas.fold<num>(0, (s, l) => s + l.base);
    final iva = _lineas.fold<num>(0, (s, l) => s + l.iva);
    final flete = num.tryParse(_flete.text.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(_editando ? 'Líneas de ${widget.orden!.numero}' : 'Nueva orden de compra',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: _cargando
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(14),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 900),
                  child: Material(
                    type: MaterialType.transparency,
                    child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                      if (!_editando) ..._cabecera(),
                      _buscadorDeProductos(),
                      if (_resultados.isNotEmpty) _listaDeResultados(),
                      const SizedBox(height: 8),
                      for (var i = 0; i < _lineas.length; i++) _filaDeLinea(i),
                      const SizedBox(height: 10),
                      _fila('Base (sin IVA)', base, 'orden-base'),
                      _fila('IVA', iva, 'orden-iva'),
                      if (!_editando) _fila('Flete', flete, 'orden-flete-total'),
                      _fila('Total', base + iva + (_editando ? 0 : flete), 'orden-total', fuerte: true),
                      const SizedBox(height: 10),
                      if (_error != null) AvisoDeError(_error!, key: const Key('orden-error')),
                      SizedBox(
                        height: RegentaSpacing.hitTarget,
                        child: FilledButton(
                          key: const Key('orden-guardar'),
                          onPressed: _guardando ? null : () => unawaited(_guardar()),
                          child: Text(_guardando ? 'Guardando…' : (_editando ? 'Guardar líneas' : 'Guardar orden')),
                        ),
                      ),
                    ]),
                  ),
                ),
              ),
            ),
    );
  }

  List<Widget> _cabecera() => [
        CampoConEtiqueta(
          etiqueta: 'Proveedor *',
          hijo: DropdownButtonFormField<String>(
            key: const Key('orden-proveedor'),
            initialValue: _proveedorId,
            isExpanded: true,
            decoration: decoracionDeCampo(hint: 'Elige el proveedor'),
            items: [
              for (final p in _proveedores)
                DropdownMenuItem(value: p.id, child: Text(p.nombre, overflow: TextOverflow.ellipsis)),
            ],
            onChanged: (v) => setState(() {
              if (v != _proveedorId) {
                for (final l in _lineas) {
                  l.dispose();
                }
                _lineas.clear();
              }
              _proveedorId = v;
            }),
          ),
        ),
        Wrap(spacing: 10, children: [
          SizedBox(
            width: 220,
            child: CampoConEtiqueta(
              etiqueta: 'Bodega *',
              hijo: DropdownButtonFormField<String>(
                key: const Key('orden-bodega'),
                initialValue: _bodegaId,
                isExpanded: true,
                decoration: decoracionDeCampo(),
                items: [
                  for (final b in _bodegas)
                    DropdownMenuItem(value: b.id, child: Text(b.nombre, overflow: TextOverflow.ellipsis)),
                ],
                onChanged: (v) => setState(() => _bodegaId = v),
              ),
            ),
          ),
          SizedBox(
            width: 180,
            child: CampoConEtiqueta(
              etiqueta: 'Para cuándo',
              hijo: OutlinedButton.icon(
                key: const Key('orden-fecha'),
                onPressed: () => unawaited(_elegirFecha()),
                style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44), alignment: Alignment.centerLeft),
                icon: const Icon(Icons.event, size: 18),
                label: Text(_fechaEsperada == null ? 'Sin fecha' : _fechaEsperada!.toIso8601String().substring(0, 10)),
              ),
            ),
          ),
          SizedBox(
            width: 160,
            child: CampoConEtiqueta(
              etiqueta: 'Flete',
              hijo: TextField(
                key: const Key('orden-flete'),
                controller: _flete,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() {}),
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: '0'),
              ),
            ),
          ),
        ]),
        CampoConEtiqueta(
          etiqueta: 'Observaciones para el proveedor',
          hijo: TextField(
            key: const Key('orden-observaciones'),
            controller: _observaciones,
            maxLines: 2,
            style: estiloDeCampo,
            decoration: decoracionDeCampo(),
          ),
        ),
      ];

  Widget _buscadorDeProductos() => Row(children: [
        Expanded(
          child: TextField(
            key: const Key('orden-buscar'),
            controller: _buscador,
            enabled: _proveedorId != null,
            onSubmitted: (_) => unawaited(_buscar()),
            style: estiloDeCampo,
            decoration: decoracionDeCampo(
              hint: _proveedorId == null ? 'Elige el proveedor para agregar productos' : 'Producto: nombre, SKU o código',
              prefijo: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
            ),
          ),
        ),
        const SizedBox(width: 8),
        SizedBox(
          height: RegentaSpacing.hitTarget,
          child: OutlinedButton(
            key: const Key('orden-buscar-boton'),
            onPressed: _proveedorId == null ? null : () => unawaited(_buscar()),
            child: const Text('Buscar'),
          ),
        ),
      ]);

  Widget _listaDeResultados() => Container(
        key: const Key('orden-resultados'),
        margin: const EdgeInsets.only(top: 8),
        constraints: const BoxConstraints(maxHeight: 200),
        decoration: BoxDecoration(
          border: Border.all(color: RegentaColors.line2),
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        ),
        child: ListView(shrinkWrap: true, children: [
          for (final r in _resultados)
            ListTile(
              dense: true,
              title: Text(r.nombre),
              subtitle: r.sku == null ? null : Text(r.sku!),
              trailing: const Icon(Icons.add_circle_outline, color: RegentaColors.accent),
              onTap: () => unawaited(_agregar(r)),
            ),
        ]),
      );

  Widget _filaDeLinea(int i) {
    final l = _lineas[i];
    final id = l.producto.productoId;
    Widget campo(String clave, String etiqueta, TextEditingController c) => SizedBox(
          width: 140,
          child: CampoConEtiqueta(
            etiqueta: etiqueta,
            hijo: TextField(
              key: Key('orden-$clave-$id'),
              controller: c,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              onChanged: (_) => setState(() {}),
              style: estiloDeCampo,
              decoration: decoracionDeCampo(),
            ),
          ),
        );
    return Container(
      key: Key('orden-linea-$id'),
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          Expanded(
            child: Text(l.producto.nombre,
                style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
          ),
          Text('IVA ${l.producto.impuestoPct}%',
              key: Key('orden-iva-$id'), style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
          IconButton(
            tooltip: 'Quitar',
            icon: const Icon(Icons.delete_outline, size: 18, color: RegentaColors.faint),
            onPressed: () => setState(() => _lineas.removeAt(i).dispose()),
          ),
        ]),
        Wrap(spacing: 10, children: [
          campo('cantidad', 'Cantidad', l.cantidad),
          campo('costo', 'Costo (sin IVA)', l.costo),
          campo('descuento', 'Descuento %', l.descuento),
        ]),
      ]),
    );
  }

  Widget _fila(String etiqueta, num valor, String clave, {bool fuerte = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(children: [
          Expanded(
            child: Text(etiqueta,
                style: RegentaType.cuerpo.copyWith(fontWeight: fuerte ? FontWeight.w800 : FontWeight.w500)),
          ),
          Text(formatearPesos(valor),
              key: Key(clave),
              style: RegentaType.codigo.copyWith(fontSize: fuerte ? 15 : 13, fontWeight: fuerte ? FontWeight.w800 : FontWeight.w600)),
        ]),
      );
}
