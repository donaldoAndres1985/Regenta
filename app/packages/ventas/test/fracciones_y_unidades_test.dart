import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-158 reescribió este archivo a propósito: el selector ya no adivina la unidad por el
/// nombre ni ofrece una lista fija proporcional (½, ¼, metros, kilos…). Ahora muestra las
/// fracciones del catálogo (tabla o precio propio, HU-156/157) con su precio.
class _RepoFake implements RepositorioDeVentas {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Fracciones implements ConsultaDeFracciones {
  _Fracciones(this.porProducto);

  final Map<String, List<FraccionDeVenta>> porProducto;

  @override
  Future<List<FraccionDeVenta>> de(String productoId) async => porProducto[productoId] ?? const [];
}

const _rojo = ProductoBuscado(
  id: 'p-rojo',
  sku: 'PIN-R',
  nombre: 'Pintulux rojo galón',
  precioVenta: 60000,
  impuestoPct: 19,
  existencia: 15,
  nivelStock: NivelStock.normal,
  unidad: 'GLN',
);

const _brocha = ProductoBuscado(
  id: 'p-brocha',
  sku: 'BRO-3',
  nombre: 'Brocha 3 pulgadas',
  precioVenta: 8000,
  existencia: 10,
  nivelStock: NivelStock.normal,
);

Future<ProviderContainer> _montar(WidgetTester tester, ProductoBuscado producto) async {
  await tester.binding.setSurfaceSize(const Size(1200, 800));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final container = ProviderContainer(overrides: [
    repositorioDeVentasProvider.overrideWithValue(_RepoFake()),
    bodegaDeVentaProvider.overrideWithValue('bodega-1'),
    consultaDeFraccionesProvider.overrideWithValue(_Fracciones({
      'p-rojo': const [
        FraccionDeVenta(medida: '½ galón', factor: 0.5, precio: 34000),
        FraccionDeVenta(medida: '¼ galón', factor: 0.25, precio: 18000),
      ],
    })),
  ]);
  addTearDown(container.dispose);
  container.read(controladorDelPosProvider.notifier).agregar(producto);
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: const MaterialApp(home: Scaffold(body: PantallaPos())),
  ));
  await tester.pumpAndSettle();
  return container;
}

Future<void> _ctrlD(WidgetTester tester) async {
  await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
  await tester.sendKeyEvent(LogicalKeyboardKey.keyD);
  await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
  await tester.pumpAndSettle();
}

void main() {
  test('formatearCantidadExacta trunca ceros sobrantes y mantiene decimales significativos', () {
    expect(formatearCantidadExacta(1.0), '1');
    expect(formatearCantidadExacta(0.25), '0.25');
    expect(formatearCantidadExacta(0.50), '0.5');
    expect(formatearCantidadExacta(0.125), '0.125');
    expect(formatearCantidadExacta(0.03125), '0.03125');
    expect(formatearCantidadExacta(29.97), '29.97');
  });

  testWidgets('HU-158 · la fila muestra la unidad del catálogo, no una adivinada por el nombre', (tester) async {
    await _montar(tester, _rojo);

    expect(find.text('GLN'), findsOneWidget);
  });

  testWidgets('HU-158 C1 · Ctrl+D muestra solo las fracciones del producto, con su precio', (tester) async {
    await _montar(tester, _rojo);

    await _ctrlD(tester);

    expect(find.text('½ galón · \$ 34.000'), findsOneWidget);
    expect(find.text('¼ galón · \$ 18.000'), findsOneWidget);
    expect(find.textContaining('Cuñete'), findsNothing, reason: 'ya no hay lista fija');
  });

  testWidgets('HU-158 C3 · elegir ¼ convierte la línea a esa fracción, que cobra su precio', (tester) async {
    final container = await _montar(tester, _rojo);

    await _ctrlD(tester);
    await tester.tap(find.byKey(const Key('fraccion-¼ galón')));
    await tester.pumpAndSettle();

    final linea = container.read(controladorDelPosProvider).lineas.single;
    expect(linea.fraccion?.medida, '¼ galón');
    expect(linea.cantidad, 1);
    expect(linea.total, 18000);
    expect(find.text('¼ galón'), findsOneWidget, reason: 'la fila muestra la medida');
  });

  testWidgets('HU-158 C2 · un producto sin fracciones avisa que no se vende fraccionado', (tester) async {
    await _montar(tester, _brocha);

    await tester.tap(find.byIcon(Icons.pie_chart_outline));
    await tester.pumpAndSettle();

    expect(find.text('Este producto no se vende fraccionado.'), findsOneWidget);
  });
}
