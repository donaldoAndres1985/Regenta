import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/ficha_de_producto.dart';
import '../datos/historial_de_producto.dart';

/// HU-160 · por qué cambió la existencia de un producto: cada movimiento con fecha, tipo,
/// cantidad con signo, saldo después, motivo y usuario, del más nuevo al más viejo. Se filtra
/// por días y por bodega. Es de solo lectura: el historial no se edita ni se borra (I11).
class PantallaHistorialDeProducto extends ConsumerStatefulWidget {
  const PantallaHistorialDeProducto({super.key, required this.productoId, required this.nombre});

  final String productoId;
  final String nombre;

  @override
  ConsumerState<PantallaHistorialDeProducto> createState() => _PantallaHistorialDeProductoState();
}

class _PantallaHistorialDeProductoState extends ConsumerState<PantallaHistorialDeProducto> {
  List<MovimientoDeHistorial> _movimientos = const [];
  List<UbicacionEnBodega> _bodegas = const [];
  int? _dias;
  String? _bodegaId;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
    ref.read(repositorioDeInventarioProvider).ubicacionesDe(widget.productoId).then((b) {
      if (mounted) setState(() => _bodegas = b);
    }).catchError((_) {});
  }

  Future<void> _cargar() async {
    final hoy = DateTime.now();
    try {
      final m = await ref.read(repositorioDeInventarioProvider).historialDe(
            widget.productoId,
            desde: _dias == null ? null : hoy.subtract(Duration(days: _dias!)),
            hasta: _dias == null ? null : hoy,
            bodegaId: _bodegaId,
          );
      if (mounted) setState(() => _movimientos = m);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    }
  }

  static String _fecha(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} '
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  static String _numero(num v) => v == v.roundToDouble() ? '${v.round()}' : '$v';

  @override
  Widget build(BuildContext context) {
    Widget chip(String texto, int? dias, Key key) => ChoiceChip(
          key: key,
          label: Text(texto),
          selected: _dias == dias,
          onSelected: (_) {
            setState(() => _dias = dias);
            _cargar();
          },
        );
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: Text('Historial · ${widget.nombre}')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Wrap(spacing: 8, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
            chip('Todo', null, const Key('historial-todo')),
            chip('Últimos 7 días', 7, const Key('historial-7-dias')),
            chip('Últimos 30 días', 30, const Key('historial-30-dias')),
            DropdownButton<String?>(
              key: const Key('historial-bodega'),
              value: _bodegaId,
              hint: const Text('Todas las bodegas'),
              items: [
                const DropdownMenuItem<String?>(value: null, child: Text('Todas las bodegas')),
                for (final b in _bodegas)
                  DropdownMenuItem<String?>(value: b.bodegaId, child: Text(b.bodegaNombre)),
              ],
              onChanged: (v) {
                setState(() => _bodegaId = v);
                _cargar();
              },
            ),
          ]),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
            ),
          const SizedBox(height: 8),
          Expanded(
            child: _movimientos.isEmpty
                ? const Center(child: Text('No hay movimientos con estos filtros.'))
                : ListView.separated(
                    itemCount: _movimientos.length,
                    separatorBuilder: (_, _) => const Divider(height: 1),
                    itemBuilder: (_, i) {
                      final m = _movimientos[i];
                      final entrada = m.cantidad >= 0;
                      return ListTile(
                        title: Text(m.tipoLegible,
                            style: const TextStyle(color: RegentaColors.inkFuerte)),
                        subtitle: Text(
                          '${_fecha(m.ocurridoEn)} · ${m.motivo}${m.usuario.isEmpty ? '' : ' · ${m.usuario}'}',
                          style: const TextStyle(color: RegentaColors.ink2),
                        ),
                        trailing: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              '${entrada ? '+' : ''}${_numero(m.cantidad)}',
                              style: RegentaType.dinero.copyWith(
                                  color: entrada ? RegentaColors.ok : RegentaColors.crit),
                            ),
                            Text('Saldo ${_numero(m.saldoPosterior)}',
                                style: RegentaType.etiqueta.copyWith(color: RegentaColors.ink2)),
                          ],
                        ),
                      );
                    },
                  ),
          ),
        ]),
      ),
    );
  }
}
