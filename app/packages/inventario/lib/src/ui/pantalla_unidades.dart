import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/ficha_de_producto.dart';

/// HU-155 · las unidades de medida del negocio: listar, crear, corregir (tocándola) y desactivar
/// las que no se usan.
class PantallaUnidades extends ConsumerStatefulWidget {
  const PantallaUnidades({super.key});

  @override
  ConsumerState<PantallaUnidades> createState() => _PantallaUnidadesState();
}

class _PantallaUnidadesState extends ConsumerState<PantallaUnidades> {
  List<UnidadDeMedida> _unidades = const [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final u = await ref.read(repositorioDeInventarioProvider).unidadesDeMedida();
      if (mounted) setState(() => _unidades = u);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  /// Crear una unidad nueva o, con [unidad], corregir una existente (código incluido).
  Future<void> _editar([UnidadDeMedida? unidad]) async {
    final codigo = TextEditingController(text: unidad?.codigo ?? '');
    final nombre = TextEditingController(text: unidad?.nombre ?? '');
    var decimales = unidad?.permiteDecimales ?? false;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: Text(unidad == null ? 'Nueva unidad' : 'Editar unidad'),
          content: Column(mainAxisSize: MainAxisSize.min, children: [
            TextField(
              key: const Key('unidad-codigo'),
              controller: codigo,
              maxLength: 10,
              decoration: const InputDecoration(labelText: 'Código (ej. GLN)'),
            ),
            TextField(
              key: const Key('unidad-nombre'),
              controller: nombre,
              decoration: const InputDecoration(labelText: 'Nombre (ej. Galón)'),
            ),
            CheckboxListTile(
              key: const Key('unidad-decimales'),
              value: decimales,
              title: const Text('Se vende con decimales (2,5)'),
              onChanged: (v) => setLocal(() => decimales = v ?? false),
            ),
          ]),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
            FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('Guardar')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    final repo = ref.read(repositorioDeInventarioProvider);
    try {
      if (unidad == null) {
        await repo.crearUnidad(codigo.text.trim(), nombre.text.trim(), decimales);
      } else {
        await repo.editarUnidad(unidad.id, codigo.text.trim(), nombre.text.trim(), decimales);
      }
      if (mounted) setState(() => _error = null);
      await _cargar();
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  Future<void> _desactivar(UnidadDeMedida u) async {
    try {
      await ref.read(repositorioDeInventarioProvider).desactivarUnidad(u.id);
      await _cargar();
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    final puedeEditar = ref.watch(puedeEditarProductosProvider);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Unidades de medida')),
      floatingActionButton: puedeEditar
          ? FloatingActionButton.extended(
              key: const Key('unidad-nueva'),
              onPressed: _editar,
              icon: const Icon(Icons.add),
              label: const Text('Nueva unidad'),
            )
          : null,
      body: Column(children: [
        if (_error != null)
          Padding(
            padding: const EdgeInsets.all(12),
            child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
          ),
        Expanded(
          child: ListView(children: [
            for (final u in _unidades)
              ListTile(
                key: Key('unidad-${u.id}'),
                onTap: puedeEditar ? () => _editar(u) : null,
                title: Text('${u.codigo} · ${u.nombre}',
                    style: const TextStyle(color: RegentaColors.inkFuerte)),
                subtitle: Text(
                  [
                    u.permiteDecimales ? 'Con decimales' : 'Solo enteros',
                    if (!u.activa) 'Inactiva',
                  ].join(' · '),
                  style: const TextStyle(color: RegentaColors.ink2),
                ),
                trailing: puedeEditar && u.activa
                    ? IconButton(
                        key: Key('desactivar-${u.id}'),
                        tooltip: 'Desactivar',
                        icon: const Icon(Icons.block),
                        onPressed: () => _desactivar(u),
                      )
                    : null,
              ),
          ]),
        ),
      ]),
    );
  }
}
