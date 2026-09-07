import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  /// Fija el tamano de la ventana de prueba y lo restaura al terminar.
  Future<void> ventana(WidgetTester tester, double ancho) async {
    await tester.binding.setSurfaceSize(Size(ancho, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
  }

  Widget app(Widget hijo) => MaterialApp(home: Scaffold(body: hijo));

  testWidgets('criterio 1: por debajo de kBreakpointEscritorio, composicion movil',
      (tester) async {
    await ventana(tester, kBreakpointEscritorio - 1);
    late FormaDePantalla forma;

    await tester.pumpWidget(app(ComposicionAdaptativa(
      cuerpo: Builder(builder: (c) {
        forma = c.forma;
        return const Text('cuerpo', key: Key('cuerpo'));
      }),
      panelLateral: const Text('panel', key: Key('panel')),
    )));

    expect(forma, FormaDePantalla.movil);
    expect(find.byKey(const Key('cuerpo')), findsOneWidget);
    expect(find.byKey(const Key('panel')), findsNothing);
  });

  testWidgets('criterio 2: por encima, composicion de escritorio con panel lateral',
      (tester) async {
    await ventana(tester, kBreakpointEscritorio + 200);
    late FormaDePantalla forma;

    await tester.pumpWidget(app(ComposicionAdaptativa(
      cuerpo: Builder(builder: (c) {
        forma = c.forma;
        return const Text('cuerpo', key: Key('cuerpo'));
      }),
      panelLateral: const Text('panel', key: Key('panel')),
    )));

    expect(forma, FormaDePantalla.escritorio);
    expect(find.byKey(const Key('cuerpo')), findsOneWidget);
    expect(find.byKey(const Key('panel')), findsOneWidget);
  });

  testWidgets('criterio 3: un solo widget con LayoutBuilder; el mismo cuerpo en las dos ramas',
      (tester) async {
    var construccionesDelCuerpo = 0;
    final widget = ComposicionAdaptativa(
      cuerpo: Builder(builder: (_) {
        construccionesDelCuerpo++;
        return const SizedBox(height: 10);
      }),
      panelLateral: const Text('panel'),
    );

    await ventana(tester, 500);
    await tester.pumpWidget(app(widget));
    expect(find.byType(LayoutBuilder), findsWidgets);
    expect(find.text('panel'), findsNothing);

    await ventana(tester, 1300);
    await tester.pumpWidget(app(widget));
    expect(find.text('panel'), findsOneWidget);
    expect(construccionesDelCuerpo, greaterThan(0),
        reason: 'el mismo widget cuerpo, no dos arboles distintos');
  });

  testWidgets('sin panelLateral, escritorio muestra solo el cuerpo', (tester) async {
    await ventana(tester, 1200);
    await tester.pumpWidget(
        app(const ComposicionAdaptativa(cuerpo: Text('cuerpo', key: Key('cuerpo')))));
    expect(find.byKey(const Key('cuerpo')), findsOneWidget);
  });

  testWidgets('criterio 4: AreaDeToque nunca mide menos de 44 px de alto en movil',
      (tester) async {
    await ventana(tester, 400);
    await tester.pumpWidget(app(ComposicionAdaptativa(
      cuerpo: Align(
        alignment: Alignment.topLeft,
        child: AreaDeToque(onTap: () {}, child: const Text('x')),
      ),
    )));

    expect(tester.getSize(find.byType(AreaDeToque)).height,
        greaterThanOrEqualTo(RegentaSpacing.hitTarget));
  });

  testWidgets('AreaDeToque en escritorio puede ser mas compacta', (tester) async {
    await ventana(tester, 1200);
    await tester.pumpWidget(app(ComposicionAdaptativa(
      cuerpo: Align(
        alignment: Alignment.topLeft,
        child: AreaDeToque(onTap: () {}, child: const Text('x')),
      ),
    )));

    final alto = tester.getSize(find.byType(AreaDeToque)).height;
    expect(alto, greaterThanOrEqualTo(RegentaSpacing.controlWeb));
    expect(alto, lessThan(RegentaSpacing.hitTarget));
  });
}
