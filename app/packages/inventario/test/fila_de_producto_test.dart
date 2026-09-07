import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

ProductoEncontrado _p(NivelStock nivel, num stock) => ProductoEncontrado(
      id: 'p1',
      sku: 'CEM-050',
      codigoBarras: null,
      nombre: 'Cemento gris 50 kg',
      categoriaNombre: 'Cementos',
      precioVenta: 32000,
      stockTotal: stock,
      nivelStock: nivel,
    );

Color _colorDeTexto(WidgetTester tester, String texto) =>
    (tester.widget<Text>(find.text(texto)).style!.color)!;

void main() {
  testWidgets('la fila muestra nombre, SKU, categoría, precio y cantidad', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: FilaDeProducto(producto: _p(NivelStock.normal, 142))),
    ));

    expect(find.text('Cemento gris 50 kg'), findsOneWidget);
    expect(find.text('CEM-050'), findsOneWidget);
    expect(find.text('Cementos'), findsOneWidget);
    expect(find.text('\$32.000'), findsOneWidget);
    expect(find.text('142 und'), findsOneWidget);
  });

  testWidgets('el color de la cantidad depende del nivel de stock', (tester) async {
    for (final (nivel, stock, esperado) in <(NivelStock, num, Color)>[
      (NivelStock.normal, 142, RegentaColors.ok),
      (NivelStock.bajo, 12, RegentaColors.warn),
      (NivelStock.cero, 0, RegentaColors.crit),
    ]) {
      await tester.pumpWidget(MaterialApp(
        home: Scaffold(body: FilaDeProducto(producto: _p(nivel, stock))),
      ));
      expect(_colorDeTexto(tester, '$stock und'), esperado, reason: '$nivel');
    }
  });

  test('formatearPesos usa el punto de miles como los mockups', () {
    expect(formatearPesos(32000), '\$32.000');
    expect(formatearPesos(1900), '\$1.900');
    expect(formatearPesos(142000), '\$142.000');
  });
}
