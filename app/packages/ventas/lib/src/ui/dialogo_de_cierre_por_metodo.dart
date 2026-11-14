import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/datos/sesion_caja_modelos.dart';
import 'formato.dart';

/// HU-140 · el cierre de caja por método de pago (R23 de `design/comportamiento/SesionDeCaja.md`).
///
/// Una fila por método: esperado, contado y diferencia (contado − esperado). Los métodos no
/// cuadrables no se cuentan. La diferencia de cada fila y la total se recalculan mientras se
/// escribe. Un cuadrable sin contado cuenta como 0 (I9b). No se confirma con efectivo en 0 (I10).
class DialogoDeCierrePorMetodo extends StatefulWidget {
  const DialogoDeCierrePorMetodo({
    super.key,
    required this.lineas,
    required this.onConfirmar,
  });

  final List<LineaDeArqueoVista> lineas;
  final ValueChanged<Map<String, num>> onConfirmar;

  static const nombres = <String, String>{
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
  State<DialogoDeCierrePorMetodo> createState() => _DialogoDeCierrePorMetodoState();
}

class _DialogoDeCierrePorMetodoState extends State<DialogoDeCierrePorMetodo> {
  late final Map<String, TextEditingController> _contados = {
    for (final l in widget.lineas.where((l) => l.cuadrable))
      l.metodo: TextEditingController(
        text: l.contado == null || l.contado == 0 ? '' : l.contado!.round().toString(),
      ),
  };

  @override
  void dispose() {
    for (final c in _contados.values) {
      c.dispose();
    }
    super.dispose();
  }

  num _contadoDe(LineaDeArqueoVista l) {
    if (!l.cuadrable) return l.esperado;
    final texto = _contados[l.metodo]!.text.replaceAll('.', '').replaceAll(',', '.').trim();
    return num.tryParse(texto) ?? 0;
  }

  num _diferenciaDe(LineaDeArqueoVista l) => _contadoDe(l) - l.esperado;

  num get _diferenciaTotal => widget.lineas.fold<num>(0, (s, l) => s + _diferenciaDe(l));

  num get _efectivo {
    final efectivo = widget.lineas.where((l) => l.metodo == 'EFECTIVO');
    return efectivo.isEmpty ? 0 : _contadoDe(efectivo.first);
  }

  void _confirmar() {
    widget.onConfirmar({
      for (final l in widget.lineas.where((l) => l.cuadrable)) l.metodo: _contadoDe(l),
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final l in widget.lineas) _fila(l),
        const Divider(color: RegentaColors.line, height: 24),
        Row(
          children: [
            Expanded(
              child: Text('Diferencia total',
                  style: RegentaType.seccion.copyWith(fontSize: 15)),
            ),
            _diferencia(_diferenciaTotal, const ValueKey('diferencia-total'), grande: true),
          ],
        ),
        const SizedBox(height: 16),
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: RegentaColors.accent,
              minimumSize: const Size(0, RegentaSpacing.hitTarget),
            ),
            onPressed: _efectivo > 0 ? _confirmar : null,
            child: const Text('Confirmar cierre'),
          ),
        ),
      ],
    );
  }

  Widget _fila(LineaDeArqueoVista l) {
    final nombre = DialogoDeCierrePorMetodo.nombres[l.metodo] ?? l.metodo;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            flex: 3,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(nombre,
                    style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w600)),
                Text('Esperado ${formatearPesos(l.esperado)} · ${l.movimientos} mov.',
                    style: RegentaType.etiqueta),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 3,
            child: l.cuadrable
                ? TextField(
                    key: ValueKey('contado-${l.metodo}'),
                    controller: _contados[l.metodo],
                    keyboardType: TextInputType.number,
                    style: RegentaType.dinero.copyWith(fontSize: 15),
                    decoration: const InputDecoration(
                      labelText: 'Contado',
                      prefixText: '\$ ',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    onChanged: (_) => setState(() {}),
                  )
                : Text('No se cuenta: se concilia aparte',
                    style: RegentaType.etiqueta.copyWith(color: RegentaColors.muted)),
          ),
          const SizedBox(width: 12),
          SizedBox(
            width: 110,
            child: Align(
              alignment: Alignment.centerRight,
              child: _diferencia(_diferenciaDe(l), ValueKey('diferencia-${l.metodo}')),
            ),
          ),
        ],
      ),
    );
  }

  Widget _diferencia(num valor, Key key, {bool grande = false}) {
    final color = valor < 0
        ? RegentaColors.crit
        : valor > 0
            ? RegentaColors.ok
            : RegentaColors.ink2;
    return Text(
      formatearPesos(valor),
      key: key,
      style: RegentaType.dinero.copyWith(fontSize: grande ? 17 : 14, color: color),
    );
  }
}
