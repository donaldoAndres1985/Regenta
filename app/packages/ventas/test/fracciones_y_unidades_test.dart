import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _RepoFake implements RepositorioDeVentas {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  group('formatearCantidadExacta y unidades', () {
    test('trunca ceros sobrantes y mantiene decimales significativos', () {
      expect(formatearCantidadExacta(1.0), '1');
      expect(formatearCantidadExacta(5.0), '5');
      expect(formatearCantidadExacta(0.25), '0.25');
      expect(formatearCantidadExacta(0.50), '0.5');
      expect(formatearCantidadExacta(0.125), '0.125');
      expect(formatearCantidadExacta(0.0625), '0.0625');
      expect(formatearCantidadExacta(0.03125), '0.03125');
      expect(formatearCantidadExacta(0.2563), '0.2563');
      expect(formatearCantidadExacta(29.97), '29.97');
    });

    test('detecta correctamente la unidad según el producto', () {
      expect(unidadParaProducto('MANTO REAL (GOMOFLAX) 1 MTS'), 'MTS');
      expect(unidadParaProducto('CABLE ELECTRICO 2.5 MM'), 'MTS');
      expect(unidadParaProducto('MANGUERA DE RIEGO 1/2'), 'MTS');
      expect(unidadParaProducto('PINTURA ESMALTE BLANCO 1 GL'), 'GL');
      expect(unidadParaProducto('VINILO TIPO 1 SUPERLAVABLE'), 'GL');
      expect(unidadParaProducto('CUÑETE DE PINTURA MARFIL 5 GAL'), 'GL');
      expect(unidadParaProducto('VALDE DE IMPERMEABILIZANTE'), 'GL');
      expect(unidadParaProducto('CEMENTO GRIS 50 KG'), 'KG');
      expect(unidadParaProducto('BROCHA 3 PULGADAS'), 'UND');
    });
  });

  group('Selector de fracciones adaptado al producto', () {
    testWidgets('en producto de metros muestra medidas de longitud y NO de galón', (tester) async {
      final repo = _RepoFake();
      const productoManto = ProductoBuscado(
        id: 'p-manto',
        sku: '100001104',
        nombre: 'MANTO REAL (GOMOFLAX) 1 MTS',
        precioVenta: 19500,
        impuestoPct: 0,
        existencia: 30,
        nivelStock: NivelStock.normal,
      );

      await tester.binding.setSurfaceSize(const Size(1200, 800));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      final container = ProviderContainer(
        overrides: [
          repositorioDeVentasProvider.overrideWithValue(repo),
          bodegaDeVentaProvider.overrideWithValue('bodega-1'),
        ],
      );
      addTearDown(container.dispose);

      container.read(controladorDelPosProvider.notifier).agregar(productoManto);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(home: Scaffold(body: PantallaPos())),
        ),
      );
      await tester.pumpAndSettle();

      // Debe mostrar la unidad MTS en la fila
      expect(find.text('MTS'), findsOneWidget);

      // Abrir selector de fracciones
      final botonFraccion = find.byIcon(Icons.pie_chart_outline);
      expect(botonFraccion, findsOneWidget);
      await tester.tap(botonFraccion);
      await tester.pumpAndSettle();

      // Debe mostrar medidas de longitud / metros
      expect(find.text('Medidas de longitud / metros (manto, cable, tubo...):'), findsOneWidget);
      expect(find.text('1/2 m · 50 cm (0.5 m)'), findsOneWidget);
      expect(find.text('1/4 m · 25 cm (0.25 m)'), findsOneWidget);

      // NO debe mostrar galones
      expect(find.text('Fracciones de galón y cuñete (5 galones):'), findsNothing);
      expect(find.text('1 Galón (1.0)'), findsNothing);

      // Al tocar 1/4 m (0.25)
      await tester.tap(find.text('1/4 m · 25 cm (0.25 m)'));
      await tester.pumpAndSettle();

      // La cantidad debe haber cambiado a 0.25 sin ceros redundantes
      expect(container.read(controladorDelPosProvider).lineas.first.cantidad, 0.25);
      expect(find.text('0.25'), findsOneWidget);
    });

    testWidgets('en producto de galón muestra fracciones de galón y cuñete', (tester) async {
      final repo = _RepoFake();
      const productoVinilo = ProductoBuscado(
        id: 'p-vinilo',
        sku: '100002200',
        nombre: 'VINILO BLANCO TIPO 1 GALON',
        precioVenta: 45000,
        impuestoPct: 19,
        existencia: 15,
        nivelStock: NivelStock.normal,
      );

      await tester.binding.setSurfaceSize(const Size(1200, 800));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      final container = ProviderContainer(
        overrides: [
          repositorioDeVentasProvider.overrideWithValue(repo),
          bodegaDeVentaProvider.overrideWithValue('bodega-1'),
        ],
      );
      addTearDown(container.dispose);

      container.read(controladorDelPosProvider.notifier).agregar(productoVinilo);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: const MaterialApp(home: Scaffold(body: PantallaPos())),
        ),
      );
      await tester.pumpAndSettle();

      // Debe mostrar la unidad GL en la fila
      expect(find.text('GL'), findsOneWidget);

      // Abrir selector de fracciones
      final botonFraccion = find.byIcon(Icons.pie_chart_outline);
      await tester.tap(botonFraccion);
      await tester.pumpAndSettle();

      // Debe mostrar fracciones de galón y cuñete de 5 galones
      expect(find.text('Fracciones de galón y cuñete (5 galones):'), findsOneWidget);
      expect(find.text('1 Cuñete · Balde (5.0 gal)'), findsOneWidget);
      expect(find.text('1/2 Cuñete (2.5 gal)'), findsOneWidget);
      expect(find.text('1/4 Galón (0.25)'), findsOneWidget);
      expect(find.text('1/32 Galón (0.03125)'), findsOneWidget);

      // NO debe mostrar longitud
      expect(find.text('Medidas de longitud / metros (manto, cable, tubo...):'), findsNothing);

      // Seleccionar cuñete de 5 galones
      await tester.tap(find.text('1 Cuñete · Balde (5.0 gal)'));
      await tester.pumpAndSettle();

      expect(container.read(controladorDelPosProvider).lineas.first.cantidad, 5.0);
      expect(find.text('5'), findsOneWidget);
    });
  });
}
