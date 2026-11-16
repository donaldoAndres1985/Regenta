import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// El diálogo de cierre se lee sobre el fondo del diálogo, sea claro u oscuro: los textos toman
/// su color del tema activo, no un color fijo pensado para fondo claro.
void main() {
  for (final (nombre, tema) in [('oscuro', ThemeData.dark()), ('claro', ThemeData.light())]) {
    testWidgets('Con tema $nombre, el método, el esperado y la diferencia total usan el color '
        'de texto del tema', (tester) async {
      await tester.pumpWidget(MaterialApp(
        theme: tema,
        home: Scaffold(
          body: DialogoDeCierrePorMetodo(
            lineas: const [
              LineaDeArqueoVista(metodo: 'EFECTIVO', cuadrable: true, esperado: 100000, movimientos: 3),
            ],
            onConfirmar: (_) {},
          ),
        ),
      ));

      final colorDelTema = tema.colorScheme.onSurface;
      final secundario = tema.colorScheme.onSurfaceVariant;
      expect(tester.widget<Text>(find.text('Efectivo')).style?.color, colorDelTema);
      expect(tester.widget<Text>(find.text('Diferencia total')).style?.color, colorDelTema);
      expect(tester.widget<Text>(find.textContaining('Esperado')).style?.color, secundario);
    });
  }
}
