import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-140 C13 · la pantalla de cierre por método de pago.
const _lineas = [
  LineaDeArqueoVista(metodo: 'EFECTIVO', cuadrable: true, esperado: 100000, movimientos: 5),
  LineaDeArqueoVista(metodo: 'QR', cuadrable: true, esperado: 20000, movimientos: 1),
  LineaDeArqueoVista(metodo: 'TARJETA_DEBITO', cuadrable: false, esperado: 50000, movimientos: 2),
];

Future<Map<String, num>?> _montar(WidgetTester tester) async {
  Map<String, num>? confirmado;
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: DialogoDeCierrePorMetodo(
            lineas: _lineas,
            onConfirmar: (contados) => confirmado = contados,
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return confirmado;
}

String _textoDe(WidgetTester tester, String clave) =>
    tester.widget<Text>(find.byKey(ValueKey(clave))).data!;

void main() {
  testWidgets(
      'HU-140 C13 · Dado que escribo el contado de un método, '
      'su diferencia y la total se recalculan antes de confirmar', (tester) async {
    await _montar(tester);

    await tester.enterText(find.byKey(const ValueKey('contado-EFECTIVO')), '95000');
    await tester.pump();

    expect(_textoDe(tester, 'diferencia-EFECTIVO'), formatearPesos(-5000));
    // QR sin contado cuenta como 0 (I9b): -20.000. Débito no se cuadra: 0.
    expect(_textoDe(tester, 'diferencia-total'), formatearPesos(-25000));

    await tester.enterText(find.byKey(const ValueKey('contado-QR')), '20000');
    await tester.pump();

    expect(_textoDe(tester, 'diferencia-QR'), formatearPesos(0));
    expect(_textoDe(tester, 'diferencia-total'), formatearPesos(-5000));
  });

  testWidgets(
      'HU-140 C13 · Dado un método no cuadrable, se muestra sin campo editable', (tester) async {
    await _montar(tester);

    expect(find.byKey(const ValueKey('contado-TARJETA_DEBITO')), findsNothing);
    expect(find.text('No se cuenta: se concilia aparte'), findsOneWidget);
  });

  testWidgets(
      'HU-140 C4 · Dado un efectivo contado de 0, no se puede confirmar el cierre', (tester) async {
    await _montar(tester);

    final boton = find.widgetWithText(FilledButton, 'Confirmar cierre');
    expect(tester.widget<FilledButton>(boton).onPressed, isNull);

    await tester.enterText(find.byKey(const ValueKey('contado-EFECTIVO')), '95000');
    await tester.pump();
    expect(tester.widget<FilledButton>(boton).onPressed, isNotNull);
  });

  testWidgets(
      'HU-140 C13 · Dado el cierre confirmado, se envía lo contado de cada método cuadrable',
      (tester) async {
    Map<String, num>? enviado;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: DialogoDeCierrePorMetodo(
              lineas: _lineas,
              onConfirmar: (contados) => enviado = contados,
            ),
          ),
        ),
      ),
    );
    await tester.enterText(find.byKey(const ValueKey('contado-EFECTIVO')), '95000');
    await tester.enterText(find.byKey(const ValueKey('contado-QR')), '20000');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar cierre'));
    await tester.pump();

    expect(enviado, {'EFECTIVO': 95000, 'QR': 20000});
  });
}
