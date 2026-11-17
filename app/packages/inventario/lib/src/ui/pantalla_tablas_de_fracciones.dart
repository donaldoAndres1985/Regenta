import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/tablas_de_fracciones.dart';
import 'formato.dart';

/// HU-156 · tablas de fracciones compartidas: se arman con precios escritos a mano (no
/// proporcionales) y se les asignan productos por búsqueda (nombre o marca) o selección.
class PantallaTablasDeFracciones extends ConsumerStatefulWidget {
  const PantallaTablasDeFracciones({super.key});

  @override
  ConsumerState<PantallaTablasDeFracciones> createState() => _PantallaTablasDeFraccionesState();
}

class _PantallaTablasDeFraccionesState extends ConsumerState<PantallaTablasDeFracciones> {
  List<TablaDeFracciones> _tablas = const [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final t = await ref.read(repositorioDeInventarioProvider).tablasDeFracciones();
      if (mounted) setState(() => _tablas = t);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  Future<void> _editar([TablaDeFracciones? tabla]) async {
    final guardada = await Navigator.of(context).push<TablaDeFracciones>(MaterialPageRoute(
      builder: (_) => _EditorDeTabla(tabla: tabla),
    ));
    if (guardada != null) await _cargar();
  }

  Future<void> _asignar(TablaDeFracciones tabla) async {
    await Navigator.of(context).push<void>(MaterialPageRoute(
      builder: (_) => _AsignarProductos(tabla: tabla),
    ));
    await _cargar();
  }

  @override
  Widget build(BuildContext context) {
    final puede = ref.watch(puedeGestionarFraccionesProvider);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Tablas de fracciones')),
      floatingActionButton: puede
          ? FloatingActionButton.extended(
              key: const Key('tabla-nueva'),
              onPressed: _editar,
              icon: const Icon(Icons.add),
              label: const Text('Nueva tabla'),
            )
          : null,
      body: ListView(padding: const EdgeInsets.all(16), children: [
        if (_error != null) Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
        if (_tablas.isEmpty)
          const Padding(
            padding: EdgeInsets.all(24),
            child: Text('Todavía no hay tablas. Crea una (ej. «Esmalte Pintulux galón») y '
                'asígnale los productos que se venden fraccionados.'),
          ),
        for (final t in _tablas)
          Card(
            child: ListTile(
              title: Text(t.nombre, style: const TextStyle(color: RegentaColors.inkFuerte)),
              subtitle: Text(
                '${t.fracciones.map((f) => '${f.medida} ${formatearPesos(f.precio)}').join(' · ')}'
                '\n${t.productos} productos',
                style: const TextStyle(color: RegentaColors.ink2),
              ),
              isThreeLine: true,
              onTap: puede ? () => _editar(t) : null,
              trailing: puede
                  ? TextButton.icon(
                      key: Key('asignar-${t.id}'),
                      icon: const Icon(Icons.playlist_add),
                      label: const Text('Asignar productos'),
                      onPressed: () => _asignar(t),
                    )
                  : null,
            ),
          ),
      ]),
    );
  }
}

/// El editor de una tabla: nombre y filas de medida, factor y precio.
class _EditorDeTabla extends ConsumerStatefulWidget {
  const _EditorDeTabla({this.tabla});

  final TablaDeFracciones? tabla;

  @override
  ConsumerState<_EditorDeTabla> createState() => _EditorDeTablaState();
}

class _EditorDeTablaState extends ConsumerState<_EditorDeTabla> {
  late final _nombre = TextEditingController(text: widget.tabla?.nombre ?? '');
  late final List<(TextEditingController, TextEditingController, TextEditingController)> _filas = [
    for (final f in widget.tabla?.fracciones ?? const <FraccionDeTabla>[])
      (
        TextEditingController(text: f.medida),
        TextEditingController(text: '${f.factor}'),
        TextEditingController(text: '${f.precio}'),
      ),
    if ((widget.tabla?.fracciones ?? const []).isEmpty)
      (TextEditingController(), TextEditingController(), TextEditingController()),
  ];
  String? _error;

  static num _numero(String t) => num.tryParse(t.trim().replaceAll(',', '.')) ?? 0;

  Future<void> _guardar() async {
    final tabla = TablaDeFracciones(
      id: widget.tabla?.id,
      nombre: _nombre.text.trim(),
      fracciones: [
        for (final f in _filas)
          if (f.$1.text.trim().isNotEmpty)
            FraccionDeTabla(medida: f.$1.text.trim(), factor: _numero(f.$2.text), precio: _numero(f.$3.text)),
      ],
    );
    try {
      final guardada = await ref.read(repositorioDeInventarioProvider).guardarTablaDeFracciones(tabla);
      if (mounted) Navigator.of(context).pop(guardada);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget campo(Key key, TextEditingController c, String etiqueta, {bool numero = false}) => Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 8),
            child: TextField(
              key: key,
              controller: c,
              keyboardType: numero ? const TextInputType.numberWithOptions(decimal: true) : null,
              style: const TextStyle(color: RegentaColors.inkFuerte),
              decoration: InputDecoration(labelText: etiqueta, border: const OutlineInputBorder()),
            ),
          ),
        );
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: Text(widget.tabla == null ? 'Nueva tabla de fracciones' : widget.tabla!.nombre)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        TextField(
          key: const Key('tabla-nombre'),
          controller: _nombre,
          style: const TextStyle(color: RegentaColors.inkFuerte),
          decoration: const InputDecoration(
              labelText: 'Nombre (ej. Esmalte Pintulux galón)', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 12),
        const Text('El precio de cada fracción va con IVA incluido y no tiene que ser '
            'proporcional al del producto.', style: TextStyle(color: RegentaColors.ink2)),
        const SizedBox(height: 12),
        for (var i = 0; i < _filas.length; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(children: [
              campo(Key('fraccion-$i-medida'), _filas[i].$1, 'Medida (½ galón)'),
              campo(Key('fraccion-$i-factor'), _filas[i].$2, 'Factor (0,5)', numero: true),
              campo(Key('fraccion-$i-precio'), _filas[i].$3, 'Precio', numero: true),
            ]),
          ),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            key: const Key('fraccion-agregar'),
            icon: const Icon(Icons.add),
            label: const Text('Agregar fracción'),
            onPressed: () => setState(() =>
                _filas.add((TextEditingController(), TextEditingController(), TextEditingController()))),
          ),
        ),
        if (_error != null) Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
        const SizedBox(height: 12),
        FilledButton(
          key: const Key('tabla-guardar'),
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(RegentaSpacing.hitTarget)),
          onPressed: _guardar,
          child: const Text('Guardar tabla'),
        ),
      ]),
    );
  }
}

/// HU-156 C3/C5 · buscar productos (por nombre o marca), marcarlos y asignarlos a la tabla.
class _AsignarProductos extends ConsumerStatefulWidget {
  const _AsignarProductos({required this.tabla});

  final TablaDeFracciones tabla;

  @override
  ConsumerState<_AsignarProductos> createState() => _AsignarProductosState();
}

class _AsignarProductosState extends ConsumerState<_AsignarProductos> {
  List<(String id, String nombre)> _resultados = const [];
  final Set<String> _marcados = {};
  String? _error;

  Future<void> _buscar(String termino) async {
    final r = await ref.read(repositorioDeInventarioProvider).buscar(termino: termino, limite: 200);
    if (mounted) {
      setState(() {
        _resultados = [for (final p in r.productos) (p.id, p.nombre)];
        _marcados.clear();
      });
    }
  }

  Future<void> _asignar({bool confirmar = false}) async {
    final repo = ref.read(repositorioDeInventarioProvider);
    try {
      final n = await repo.asignarTablaDeFracciones(widget.tabla.id!, _marcados.toList(),
          confirmarCambio: confirmar);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('$n productos asignados a ${widget.tabla.nombre}')));
      Navigator.of(context).pop();
    } on RecursoDuplicado catch (e) {
      if (!mounted) return;
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          content: Text(e.mensaje),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
            FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('Cambiar de tabla')),
          ],
        ),
      );
      if (ok == true) await _asignar(confirmar: true);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: Text('Asignar a ${widget.tabla.nombre}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          TextField(
            key: const Key('asignar-buscar'),
            textInputAction: TextInputAction.done,
            style: const TextStyle(color: RegentaColors.inkFuerte),
            decoration: const InputDecoration(
              prefixIcon: Icon(Icons.search),
              hintText: 'Buscar por nombre o marca (ej. pintulux galon)',
              border: OutlineInputBorder(),
            ),
            onSubmitted: _buscar,
          ),
          const SizedBox(height: 8),
          Row(children: [
            TextButton(
              key: const Key('asignar-todos'),
              onPressed: _resultados.isEmpty
                  ? null
                  : () => setState(() => _marcados.addAll(_resultados.map((r) => r.$1))),
              child: Text('Marcar los ${_resultados.length} resultados'),
            ),
            const Spacer(),
            FilledButton(
              key: const Key('asignar-confirmar'),
              onPressed: _marcados.isEmpty ? null : _asignar,
              child: Text('Asignar ${_marcados.length}'),
            ),
          ]),
          if (_error != null) Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
          Expanded(
            child: ListView(children: [
              for (final r in _resultados)
                CheckboxListTile(
                  value: _marcados.contains(r.$1),
                  title: Text(r.$2, style: const TextStyle(color: RegentaColors.inkFuerte)),
                  onChanged: (v) => setState(() => v == true ? _marcados.add(r.$1) : _marcados.remove(r.$1)),
                ),
            ]),
          ),
        ]),
      ),
    );
  }
}
