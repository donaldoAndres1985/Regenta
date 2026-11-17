import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/detallado.dart';

/// HU-161 · equivalencias de detallado: «1 cuñete = 5 galones». Se crean de a una (origen,
/// destino y factor) o se replican a los resultados de una búsqueda, uniendo cada producto con
/// su par por nombre (cambiando «cuñete» por «galón»).
class PantallaEquivalencias extends ConsumerStatefulWidget {
  const PantallaEquivalencias({super.key});

  @override
  ConsumerState<PantallaEquivalencias> createState() => _PantallaEquivalenciasState();
}

class _PantallaEquivalenciasState extends ConsumerState<PantallaEquivalencias> {
  List<EquivalenciaDeDetallado> _lista = const [];
  String? _error;

  static String _n(num v) => v == v.roundToDouble() ? '${v.round()}' : '$v';

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final l = await ref.read(repositorioDeInventarioProvider).equivalencias();
      if (mounted) setState(() => _lista = l);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  Future<void> _abrir(Widget pagina) async {
    await Navigator.of(context).push<void>(MaterialPageRoute(builder: (_) => pagina));
    await _cargar();
  }

  @override
  Widget build(BuildContext context) {
    final puede = ref.watch(puedeDetallarProvider);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Equivalencias de detallado')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        if (puede)
          Wrap(spacing: 8, children: [
            FilledButton.icon(
              key: const Key('equivalencia-nueva'),
              icon: const Icon(Icons.add),
              label: const Text('Nueva equivalencia'),
              onPressed: () => _abrir(const _NuevaEquivalencia()),
            ),
            OutlinedButton.icon(
              key: const Key('equivalencia-replicar'),
              icon: const Icon(Icons.copy_all),
              label: const Text('Replicar a varios productos'),
              onPressed: () => _abrir(const _ReplicarEquivalencia()),
            ),
          ]),
        if (_error != null) Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
        const SizedBox(height: 12),
        if (_lista.isEmpty)
          const Text('Todavía no hay equivalencias. Ej.: 1 Pintulux blanco cuñete = 5 Pintulux blanco galón.'),
        for (final e in _lista)
          ListTile(
            leading: const Icon(Icons.call_split),
            title: Text('1 ${e.origen} = ${_n(e.factor)} ${e.destino}',
                style: const TextStyle(color: RegentaColors.inkFuerte)),
          ),
      ]),
    );
  }
}

/// Un buscador que deja elegir un producto de los resultados.
class _ElegirProducto extends ConsumerStatefulWidget {
  const _ElegirProducto({required this.campo, required this.etiqueta, required this.alElegir});

  final String campo;
  final String etiqueta;
  final void Function(String id, String nombre) alElegir;

  @override
  ConsumerState<_ElegirProducto> createState() => _ElegirProductoState();
}

class _ElegirProductoState extends ConsumerState<_ElegirProducto> {
  List<(String, String)> _resultados = const [];
  String? _elegido;

  Future<void> _buscar(String t) async {
    final r = await ref.read(repositorioDeInventarioProvider).buscar(termino: t, limite: 20);
    if (mounted) setState(() => _resultados = [for (final p in r.productos) (p.id, p.nombre)]);
  }

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      TextField(
        key: Key('${widget.campo}-buscar'),
        textInputAction: TextInputAction.done,
        decoration: InputDecoration(labelText: widget.etiqueta, prefixIcon: const Icon(Icons.search)),
        onSubmitted: _buscar,
      ),
      if (_elegido != null)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Text('Elegido: $_elegido', style: const TextStyle(color: RegentaColors.accent)),
        ),
      for (final r in _resultados)
        ListTile(
          dense: true,
          title: Text(r.$2),
          onTap: () {
            setState(() {
              _elegido = r.$2;
              _resultados = const [];
            });
            widget.alElegir(r.$1, r.$2);
          },
        ),
    ]);
  }
}

class _NuevaEquivalencia extends ConsumerStatefulWidget {
  const _NuevaEquivalencia();

  @override
  ConsumerState<_NuevaEquivalencia> createState() => _NuevaEquivalenciaState();
}

class _NuevaEquivalenciaState extends ConsumerState<_NuevaEquivalencia> {
  String? _origen;
  String? _destino;
  final _factor = TextEditingController();
  String? _error;

  Future<void> _guardar() async {
    final factor = num.tryParse(_factor.text.trim().replaceAll(',', '.'));
    if (_origen == null || _destino == null || factor == null) {
      setState(() => _error = 'Elige el producto que se abre, el destino y cuántos salen.');
      return;
    }
    try {
      await ref.read(repositorioDeInventarioProvider).crearEquivalencia(_origen!, _destino!, factor);
      if (mounted) Navigator.of(context).pop();
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Nueva equivalencia')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        _ElegirProducto(
            campo: 'origen',
            etiqueta: 'Producto que se abre (ej. cuñete)',
            alElegir: (id, _) => _origen = id),
        const SizedBox(height: 12),
        _ElegirProducto(
            campo: 'destino',
            etiqueta: 'Producto que recibe (ej. galón)',
            alElegir: (id, _) => _destino = id),
        const SizedBox(height: 12),
        TextField(
          key: const Key('equivalencia-factor'),
          controller: _factor,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: '¿Cuántos salen de 1? (ej. 5)'),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
          ),
        const SizedBox(height: 16),
        FilledButton(key: const Key('equivalencia-guardar'), onPressed: _guardar, child: const Text('Guardar')),
      ]),
    );
  }
}

class _ReplicarEquivalencia extends ConsumerStatefulWidget {
  const _ReplicarEquivalencia();

  @override
  ConsumerState<_ReplicarEquivalencia> createState() => _ReplicarEquivalenciaState();
}

class _ReplicarEquivalenciaState extends ConsumerState<_ReplicarEquivalencia> {
  List<(String, String)> _origenes = const [];
  final _buscar = TextEditingController();
  final _reemplazar = TextEditingController();
  final _factor = TextEditingController();
  String? _mensaje;

  Future<void> _buscarOrigenes(String t) async {
    final r = await ref.read(repositorioDeInventarioProvider).buscar(termino: t, limite: 200);
    if (mounted) setState(() => _origenes = [for (final p in r.productos) (p.id, p.nombre)]);
  }

  Future<void> _aplicar() async {
    final factor = num.tryParse(_factor.text.trim().replaceAll(',', '.'));
    if (_origenes.isEmpty || factor == null) {
      setState(() => _mensaje = 'Busca los productos que se abren y escribe cuántos salen.');
      return;
    }
    try {
      final r = await ref.read(repositorioDeInventarioProvider).replicarEquivalencia(
          [for (final o in _origenes) o.$1], _buscar.text.trim(), _reemplazar.text.trim(), factor);
      if (!mounted) return;
      setState(() => _mensaje = '${r.creadas} equivalencias creadas.'
          '${r.sinPar.isEmpty ? '' : '\nSin par (no se creó nada): ${r.sinPar.join(', ')}'}');
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _mensaje = e.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Replicar equivalencia')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        TextField(
          key: const Key('replica-buscar-productos'),
          textInputAction: TextInputAction.done,
          decoration: const InputDecoration(
              labelText: 'Productos que se abren (ej. pintulux cuñete)', prefixIcon: Icon(Icons.search)),
          onSubmitted: _buscarOrigenes,
        ),
        for (final o in _origenes) ListTile(dense: true, title: Text(o.$2)),
        Row(children: [
          Expanded(
            child: TextField(
              key: const Key('replica-buscar'),
              controller: _buscar,
              decoration: const InputDecoration(labelText: 'En el nombre, cambiar…'),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: TextField(
              key: const Key('replica-reemplazar'),
              controller: _reemplazar,
              decoration: const InputDecoration(labelText: '…por'),
            ),
          ),
        ]),
        TextField(
          key: const Key('replica-factor'),
          controller: _factor,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: '¿Cuántos salen de 1?'),
        ),
        const SizedBox(height: 16),
        FilledButton(key: const Key('replica-aplicar'), onPressed: _aplicar, child: const Text('Replicar')),
        if (_mensaje != null)
          Padding(padding: const EdgeInsets.only(top: 12), child: Text(_mensaje!)),
      ]),
    );
  }
}
