import 'dart:async';

import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import 'datos.dart';

/// HU-180 C4 · Lo que ve quien no puede crear productos cuando el código no existe.
const mensajeSinPermisoDeCrear = 'Este producto no existe. Pide a un administrador que lo cree.';

/// Una opción de una lista de Inventario (categoría o unidad).
class OpcionDeCatalogo {
  const OpcionDeCatalogo(this.id, this.nombre);

  final String id;
  final String nombre;
}

/// HU-180 · Un término parece un código de barras si son solo dígitos (8 o más).
bool pareceCodigoDeBarras(String termino) => RegExp(r'^\d{8,}$').hasMatch(termino.trim());

/// HU-180 · El formulario corto para dar de alta un producto que llega por primera vez (I21): SKU,
/// nombre, código de barras (el escaneado ya puesto, C1), categoría, unidad, tarifa de IVA y precio con
/// IVA, más el código y el costo con que lo vende este proveedor. Devuelve el producto listo para la
/// línea, ya creado en Inventario y en el catálogo del proveedor (C3).
class FormularioDeProductoNuevo extends StatefulWidget {
  const FormularioDeProductoNuevo({
    super.key,
    required this.proveedorId,
    required this.termino,
    required this.repo,
  });

  final String proveedorId;
  final String termino;
  final RepositorioDeCompraDirecta repo;

  @override
  State<FormularioDeProductoNuevo> createState() => _FormularioDeProductoNuevoState();
}

class _FormularioDeProductoNuevoState extends State<FormularioDeProductoNuevo> {
  static const _tarifas = {'IVA_19': 'IVA 19 %', 'IVA_5': 'IVA 5 %', 'EXENTO': 'Exento', 'EXCLUIDO': 'Excluido'};

  late final TextEditingController _sku = TextEditingController();
  late final TextEditingController _nombre =
      TextEditingController(text: pareceCodigoDeBarras(widget.termino) ? '' : widget.termino.trim());
  late final TextEditingController _codigo =
      TextEditingController(text: pareceCodigoDeBarras(widget.termino) ? widget.termino.trim() : '');
  final TextEditingController _precio = TextEditingController();
  final TextEditingController _costo = TextEditingController();
  final TextEditingController _codigoProveedor = TextEditingController();
  List<OpcionDeCatalogo> _categorias = const [];
  List<OpcionDeCatalogo> _unidades = const [];
  String? _categoriaId;
  String? _unidadId;
  String? _tarifa;
  bool _manejaLotes = false;
  String? _error;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  @override
  void dispose() {
    for (final c in [_sku, _nombre, _codigo, _precio, _costo, _codigoProveedor]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _cargar() async {
    try {
      final categorias = await widget.repo.categorias();
      final unidades = await widget.repo.unidades();
      if (!mounted) return;
      setState(() {
        _categorias = categorias;
        _unidades = unidades;
        _unidadId = unidades.length == 1 ? unidades.first.id : null;
      });
    } on ErrorDeApi catch (fallo) {
      if (mounted) setState(() => _error = fallo.mensaje);
    }
  }

  static num? _valor(String t) => t.trim().isEmpty ? null : num.tryParse(t.replaceAll(RegExp(r'[^0-9.]'), ''));

  Future<void> _guardar() async {
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      final producto = await widget.repo.crearProducto(widget.proveedorId, {
        'sku': _sku.text.trim(),
        if (_codigo.text.trim().isNotEmpty) 'codigoBarras': _codigo.text.trim(),
        'nombre': _nombre.text.trim(),
        'categoriaId': _categoriaId,
        'unidadMedidaId': _unidadId,
        'tarifaIva': _tarifa,
        if (_valor(_precio.text) != null) 'precioVenta': _valor(_precio.text),
        'manejaLotes': _manejaLotes,
        'perecedero': false,
        if (_codigoProveedor.text.trim().isNotEmpty) 'codigoProveedor': _codigoProveedor.text.trim(),
        if (_valor(_costo.text) != null) 'costo': _valor(_costo.text),
      });
      if (mounted) Navigator.of(context).pop(producto);
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _guardando = false;
        // C2 · Los mensajes de Inventario tal cual, campo por campo si vienen así.
        _error = switch (fallo) {
          ErroresDeValidacion(:final porCampo) when porCampo.isNotEmpty => [
              for (final e in porCampo.entries)
                if (e.value.isNotEmpty) '${e.key}: ${e.value.first}',
            ].join('\n'),
          SinPermiso() => mensajeSinPermisoDeCrear,
          ErrorDeRed() => 'No hay conexión: el producto no se creó.',
          _ => fallo.mensaje,
        };
      });
    }
  }

  Widget _texto(String clave, String etiqueta, TextEditingController c, {bool numero = false}) => CampoConEtiqueta(
        etiqueta: etiqueta,
        hijo: TextField(
          key: Key('nuevo-$clave'),
          controller: c,
          keyboardType: numero ? TextInputType.number : TextInputType.text,
          style: estiloDeCampo,
          decoration: decoracionDeCampo(),
        ),
      );

  Widget _lista(String clave, String etiqueta, String? valor, Map<String, String> opciones, ValueChanged<String?> al) =>
      CampoConEtiqueta(
        etiqueta: etiqueta,
        hijo: DropdownButtonFormField<String>(
          key: Key('nuevo-$clave'),
          initialValue: valor,
          isExpanded: true,
          decoration: decoracionDeCampo(hint: 'Elige'),
          items: [
            for (final e in opciones.entries)
              DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)),
          ],
          onChanged: al,
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Material(
      color: RegentaColors.surface,
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        CabeceraDeDialogo(
          titulo: 'Crear producto',
          subtitulo: 'Queda en el catálogo y en el de este proveedor.',
          onCerrar: _guardando ? null : () => Navigator.of(context).pop(),
        ),
        Flexible(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(18, 14, 18, 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              _texto('sku', 'SKU *', _sku),
              _texto('nombre', 'Nombre *', _nombre),
              _texto('codigo', 'Código de barras', _codigo),
              _lista('categoria', 'Categoría *', _categoriaId, {for (final c in _categorias) c.id: c.nombre},
                  (v) => setState(() => _categoriaId = v)),
              _lista('unidad', 'Unidad *', _unidadId, {for (final u in _unidades) u.id: u.nombre},
                  (v) => setState(() => _unidadId = v)),
              _lista('tarifa', 'Tarifa de IVA *', _tarifa, _tarifas, (v) => setState(() => _tarifa = v)),
              _texto('precio', 'Precio de venta (IVA incl.)', _precio, numero: true),
              _texto('costo', 'Costo con este proveedor', _costo, numero: true),
              _texto('codigo-proveedor', 'Código del proveedor', _codigoProveedor),
              CheckboxListTile(
                key: const Key('nuevo-lotes'),
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                value: _manejaLotes,
                onChanged: (v) => setState(() => _manejaLotes = v ?? false),
                title: const Text('Maneja lotes'),
              ),
              if (_error != null) AvisoDeError(_error!, key: const Key('nuevo-error')),
            ]),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
          child: SizedBox(
            height: RegentaSpacing.hitTarget,
            child: FilledButton(
              key: const Key('nuevo-guardar'),
              onPressed: _guardando ? null : () => unawaited(_guardar()),
              child: Text(_guardando ? 'Creando…' : 'Crear y agregar'),
            ),
          ),
        ),
      ]),
    );
  }
}
