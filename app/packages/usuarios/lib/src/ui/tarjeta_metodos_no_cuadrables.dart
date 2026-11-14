import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../usuarios/proveedores.dart';

/// HU-139 · qué métodos de pago se cuentan al cerrar la caja (invariante I8 de caja).
///
/// Cada método tiene un interruptor "se cuenta al cerrar". El efectivo siempre se cuenta
/// (I10): su interruptor está fijo.
class TarjetaMetodosNoCuadrables extends ConsumerStatefulWidget {
  const TarjetaMetodosNoCuadrables({super.key});

  /// Los métodos de pago de caja, en el orden en que se muestran, con su nombre visible.
  static const metodos = <String, String>{
    'EFECTIVO': 'Efectivo',
    'TARJETA_DEBITO': 'Tarjeta débito',
    'TARJETA_CREDITO': 'Tarjeta crédito',
    'TRANSFERENCIA': 'Transferencia',
    'QR': 'Pago QR',
    'BONO': 'Bono',
    'CREDITO': 'Crédito (cartera)',
    'OTRO': 'Otro',
  };

  @override
  ConsumerState<TarjetaMetodosNoCuadrables> createState() =>
      _TarjetaMetodosNoCuadrablesState();
}

class _TarjetaMetodosNoCuadrablesState extends ConsumerState<TarjetaMetodosNoCuadrables> {
  Set<String>? _noCuadrables;
  Object? _error;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final metodos =
          await ref.read(repositorioDeConfigDeCajaProvider).verMetodosNoCuadrables();
      if (mounted) setState(() => _noCuadrables = metodos.toSet());
    } catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  Future<void> _guardar() async {
    setState(() => _guardando = true);
    try {
      final guardados = await ref
          .read(repositorioDeConfigDeCajaProvider)
          .fijarMetodosNoCuadrables(_noCuadrables!.toList()..sort());
      if (!mounted) return;
      setState(() => _noCuadrables = guardados.toSet());
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Métodos de cierre guardados'),
          backgroundColor: RegentaColors.ok,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error al guardar: $e'), backgroundColor: RegentaColors.crit),
      );
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: RegentaColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(10)),
        side: BorderSide(color: RegentaColors.line),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Cierre de caja', style: RegentaType.seccion.copyWith(fontSize: 16)),
            const SizedBox(height: 4),
            Text(
              'Qué métodos de pago cuenta el cajero al cerrar. Los que no se cuentan '
              '(tarjetas, bonos, crédito) se concilian aparte y no generan descuadre.',
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
            ),
            const Divider(color: RegentaColors.line, height: 24),
            ..._contenido(),
          ],
        ),
      ),
    );
  }

  List<Widget> _contenido() {
    if (_error != null) {
      return [
        Text(
          'No se pudo cargar la configuración de caja: $_error',
          style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit),
        ),
      ];
    }
    final noCuadrables = _noCuadrables;
    if (noCuadrables == null) {
      return const [Center(child: CircularProgressIndicator())];
    }
    return [
      for (final entrada in TarjetaMetodosNoCuadrables.metodos.entries)
        _interruptor(entrada.key, entrada.value, noCuadrables),
      const SizedBox(height: 12),
      Align(
        alignment: Alignment.centerRight,
        child: FilledButton.icon(
          style: FilledButton.styleFrom(
            backgroundColor: RegentaColors.accent,
            minimumSize: const Size(0, RegentaSpacing.hitTarget),
          ),
          onPressed: _guardando ? null : _guardar,
          icon: const Icon(Icons.save_outlined),
          label: const Text('Guardar métodos de cierre'),
        ),
      ),
    ];
  }

  Widget _interruptor(String metodo, String nombre, Set<String> noCuadrables) {
    final esEfectivo = metodo == 'EFECTIVO';
    return SwitchListTile(
      key: ValueKey('cuadrar-$metodo'),
      title: Text(
        nombre,
        style: const TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600),
      ),
      subtitle: esEfectivo
          ? Text(
              'El efectivo siempre se cuenta al cerrar.',
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
            )
          : null,
      value: esEfectivo || !noCuadrables.contains(metodo),
      onChanged: esEfectivo
          ? null
          : (seCuenta) => setState(() {
                if (seCuenta) {
                  noCuadrables.remove(metodo);
                } else {
                  noCuadrables.add(metodo);
                }
              }),
    );
  }
}
