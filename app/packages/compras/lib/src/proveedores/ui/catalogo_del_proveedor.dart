import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../../ui/formato.dart';
import '../datos/producto_del_proveedor.dart';
import '../datos/proveedor_del_negocio.dart';
import '../datos/repositorio_de_proveedores.dart';
import '../lista/proveedores.dart';
import 'piezas.dart';

/// Abre el catálogo de un proveedor: qué productos le compramos, con su código y último costo,
/// y de cuáles es el preferido (HU-166 C4: marcarlo desmarca al preferido anterior).
Future<void> abrirCatalogoDelProveedor(BuildContext context, ProveedorDelNegocio proveedor) {
  return showDialog<void>(
    context: context,
    builder: (_) => Dialog(
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 620, maxHeight: 760),
        child: CatalogoDelProveedor(proveedor: proveedor),
      ),
    ),
  );
}

class CatalogoDelProveedor extends ConsumerStatefulWidget {
  const CatalogoDelProveedor({super.key, required this.proveedor});

  final ProveedorDelNegocio proveedor;

  @override
  ConsumerState<CatalogoDelProveedor> createState() => _CatalogoDelProveedorState();
}

class _CatalogoDelProveedorState extends ConsumerState<CatalogoDelProveedor> {
  final TextEditingController _buscador = TextEditingController();
  List<ProductoDelProveedor> _productos = const [];
  List<ProductoEncontrado> _resultados = const [];
  bool _cargando = true;
  bool _buscando = false;
  String? _error;

  RepositorioDeProveedores get _repo => ref.read(repositorioDeProveedoresProvider);
  String get _proveedorId => widget.proveedor.id!;
  bool get _soloLectura => !widget.proveedor.activo;

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  @override
  void dispose() {
    _buscador.dispose();
    super.dispose();
  }

  Future<void> _cargar() async {
    final motivo = await _intentar(() async {
      final productos = await _repo.productos(_proveedorId);
      if (mounted) setState(() => _productos = productos);
    });
    if (mounted) setState(() => _cargando = false);
    if (motivo != null && mounted) setState(() => _error = motivo);
  }

  Future<String?> _intentar(Future<void> Function() accion) async {
    try {
      await accion();
      return null;
    } on ErroresDeValidacion catch (fallo) {
      return fallo.mensaje;
    } on ErrorDeRed {
      return 'No hay conexión. Intenta de nuevo.';
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
  }

  Future<void> _buscar() async {
    final termino = _buscador.text.trim();
    if (termino.length < 3) {
      setState(() => _error = 'Escribe al menos tres letras del nombre, el SKU o el código.');
      return;
    }
    setState(() {
      _buscando = true;
      _error = null;
    });
    final motivo = await _intentar(() async {
      final encontrados = await _repo.buscarProductos(termino);
      final yaEstan = _productos.map((p) => p.productoId).toSet();
      if (mounted) {
        setState(() => _resultados = encontrados.where((e) => !yaEstan.contains(e.id)).toList());
      }
    });
    if (!mounted) return;
    setState(() {
      _buscando = false;
      _error = motivo ?? (_resultados.isEmpty ? 'No hay productos nuevos con «$termino».' : null);
    });
  }

  Future<void> _agregar(ProductoEncontrado encontrado) async {
    final nuevo = await _pedirCondiciones(context, encontrado);
    if (nuevo == null || !mounted) return;
    await _guardar(nuevo);
    if (mounted && _error == null) {
      setState(() {
        _resultados = const [];
        _buscador.clear();
      });
    }
  }

  Future<void> _alternarPreferido(ProductoDelProveedor p) => _guardar(p.copiar(preferido: !p.preferido));

  Future<void> _guardar(ProductoDelProveedor producto) async {
    setState(() => _error = null);
    final motivo = await _intentar(() async {
      await _repo.asociar(_proveedorId, producto);
      final productos = await _repo.productos(_proveedorId);
      if (mounted) setState(() => _productos = productos);
    });
    if (mounted) setState(() => _error = motivo);
  }

  Future<void> _quitar(ProductoDelProveedor p) async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: const Text('¿Quitar este producto?'),
        content: Text('${p.nombreVisible} deja de estar en el catálogo de ${widget.proveedor.nombre}. '
            'Las órdenes y compras ya hechas no cambian.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(dialogo).pop(true), child: const Text('Quitar')),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;
    final motivo = await _intentar(() async {
      await _repo.quitar(_proveedorId, p.productoId);
      if (mounted) setState(() => _productos = _productos.where((x) => x.productoId != p.productoId).toList());
    });
    if (mounted) setState(() => _error = motivo);
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: RegentaColors.surface,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          CabeceraDeDialogo(
            titulo: 'Productos de ${widget.proveedor.nombre}',
            subtitulo: _soloLectura
                ? 'Proveedor desactivado: solo consulta'
                : 'La estrella marca el proveedor preferido del producto',
            onCerrar: () => Navigator.of(context).pop(),
          ),
          if (!_soloLectura) _barraDeBusqueda(),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 10, 18, 0),
              child: AvisoDeError(_error!, key: const Key('catalogo-error')),
            ),
          if (_resultados.isNotEmpty) _listaDeResultados(),
          Flexible(child: _listaDeProductos()),
        ],
      ),
    );
  }

  Widget _barraDeBusqueda() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 0),
      child: Row(children: [
        Expanded(
          child: TextField(
            key: const Key('catalogo-buscar'),
            controller: _buscador,
            onSubmitted: (_) => unawaited(_buscar()),
            style: estiloDeCampo,
            decoration: decoracionDeCampo(
              hint: 'Agregar producto: nombre, SKU o código',
              prefijo: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
            ),
          ),
        ),
        const SizedBox(width: 8),
        SizedBox(
          height: RegentaSpacing.hitTarget,
          child: OutlinedButton(
            onPressed: _buscando ? null : () => unawaited(_buscar()),
            child: Text(_buscando ? 'Buscando…' : 'Buscar'),
          ),
        ),
      ]),
    );
  }

  Widget _listaDeResultados() {
    return Container(
      key: const Key('catalogo-resultados'),
      margin: const EdgeInsets.fromLTRB(18, 8, 18, 0),
      constraints: const BoxConstraints(maxHeight: 180),
      decoration: BoxDecoration(
        border: Border.all(color: RegentaColors.line2),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: ListView(
        shrinkWrap: true,
        children: [
          for (final r in _resultados)
            ListTile(
              dense: true,
              title: Text(r.nombre, style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
              subtitle: r.sku == null ? null : Text(r.sku!, style: RegentaType.codigo.copyWith(fontSize: 11)),
              trailing: const Icon(Icons.add_circle_outline, color: RegentaColors.accent),
              onTap: () => unawaited(_agregar(r)),
            ),
        ],
      ),
    );
  }

  Widget _listaDeProductos() {
    if (_cargando) {
      return const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()));
    }
    if (_productos.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(28),
        child: Text(
          'Este proveedor todavía no tiene productos. Búscalos arriba para agregarlos con su costo.',
          textAlign: TextAlign.center,
          style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
        ),
      );
    }
    return ListView.builder(
      key: const Key('catalogo-lista'),
      shrinkWrap: true,
      padding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
      itemCount: _productos.length,
      itemBuilder: (context, i) {
        final p = _productos[i];
        final detalle = [
          if (p.sku != null) p.sku!,
          if (p.codigoProveedor != null) 'cód. ${p.codigoProveedor}',
          if (p.diasEntrega != null) 'entrega ${p.diasEntrega} d',
        ].join(' · ');
        return Container(
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
          child: Row(children: [
            IconButton(
              key: Key('catalogo-preferido-${p.productoId}'),
              tooltip: p.preferido ? 'Preferido para este producto' : 'Marcar como preferido',
              onPressed: _soloLectura ? null : () => unawaited(_alternarPreferido(p)),
              icon: Icon(
                p.preferido ? Icons.star : Icons.star_border,
                color: p.preferido ? RegentaColors.warn : RegentaColors.faint,
              ),
            ),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(p.nombreVisible,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
                if (detalle.isNotEmpty)
                  Text(detalle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
              ]),
            ),
            const SizedBox(width: 8),
            Text(p.costoUltimo == null ? '—' : formatearPesos(p.costoUltimo!),
                style: RegentaType.codigo.copyWith(fontSize: 13, color: RegentaColors.ink)),
            if (!_soloLectura)
              IconButton(
                tooltip: 'Quitar del catálogo',
                onPressed: () => unawaited(_quitar(p)),
                icon: const Icon(Icons.delete_outline, size: 18, color: RegentaColors.faint),
              ),
          ]),
        );
      },
    );
  }
}

/// Pide el código del proveedor, el costo y si es el preferido antes de agregar un producto.
Future<ProductoDelProveedor?> _pedirCondiciones(BuildContext context, ProductoEncontrado producto) {
  final codigo = TextEditingController();
  final costo = TextEditingController();
  final dias = TextEditingController();
  var preferido = false;
  return showDialog<ProductoDelProveedor>(
    context: context,
    builder: (dialogo) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        title: Text(producto.nombre, maxLines: 2, overflow: TextOverflow.ellipsis),
        content: SizedBox(
          width: 360,
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            CampoConEtiqueta(
              etiqueta: 'Costo (sin IVA)',
              hijo: TextField(
                key: const Key('condiciones-costo'),
                controller: costo,
                autofocus: true,
                keyboardType: TextInputType.number,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(),
              ),
            ),
            Row(children: [
              Expanded(
                child: CampoConEtiqueta(
                  etiqueta: 'Código del proveedor',
                  hijo: TextField(controller: codigo, style: estiloDeCampo, decoration: decoracionDeCampo()),
                ),
              ),
              const SizedBox(width: 10),
              SizedBox(
                width: 110,
                child: CampoConEtiqueta(
                  etiqueta: 'Días entrega',
                  hijo: TextField(
                    controller: dias,
                    keyboardType: TextInputType.number,
                    style: estiloDeCampo,
                    decoration: decoracionDeCampo(),
                  ),
                ),
              ),
            ]),
            CheckboxListTile(
              key: const Key('condiciones-preferido'),
              contentPadding: EdgeInsets.zero,
              value: preferido,
              onChanged: (v) => setState(() => preferido = v ?? false),
              title: const Text('Proveedor preferido para este producto'),
              controlAffinity: ListTileControlAffinity.leading,
            ),
          ]),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('condiciones-agregar'),
            onPressed: () => Navigator.of(dialogo).pop(ProductoDelProveedor(
              productoId: producto.id,
              sku: producto.sku,
              nombre: producto.nombre,
              codigoProveedor: codigo.text,
              costoUltimo: num.tryParse(costo.text.replaceAll(RegExp(r'[^0-9.]'), '')),
              diasEntrega: int.tryParse(dias.text.trim()),
              preferido: preferido,
            )),
            child: const Text('Agregar'),
          ),
        ],
      ),
    ),
  );
}
