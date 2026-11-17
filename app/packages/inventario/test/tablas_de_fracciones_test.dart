import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-156 · tablas de fracciones y asignación de productos.
class _Repo implements RepositorioDeInventario {
  final List<TablaDeFracciones> tablas = [];
  TablaDeFracciones? creada;
  final List<(String, List<String>, bool)> asignaciones = [];
  bool pedirConfirmacion = false;

  @override
  Future<List<TablaDeFracciones>> tablasDeFracciones() async => List.of(tablas);

  @override
  Future<TablaDeFracciones> guardarTablaDeFracciones(TablaDeFracciones tabla) async {
    creada = tabla;
    final t = TablaDeFracciones(id: 't1', nombre: tabla.nombre, fracciones: tabla.fracciones);
    tablas.add(t);
    return t;
  }

  @override
  Future<ResultadoDeBusqueda> buscar(
      {String? termino, String? categoriaId, bool soloBajoMinimo = false, int? limite}) async =>
      ResultadoDeBusqueda.desdeJson({
        'productos': [
          for (var i = 0; i < 3; i++)
            {'id': 'p$i', 'sku': 'PIN-$i', 'nombre': 'Pintulux color $i galón', 'precioVenta': 60000,
             'stockTotal': 1, 'nivelStock': 'NORMAL', 'categoriaNombre': 'Pinturas'},
        ],
        'total': 3,
      });

  @override
  Future<int> asignarTablaDeFracciones(String tablaId, List<String> productoIds,
      {bool confirmarCambio = false}) async {
    if (pedirConfirmacion && !confirmarCambio) {
      // 409 del backend (ConflictoDeEstadoException); el núcleo lo traduce a RecursoDuplicado.
      throw const RecursoDuplicado('1 de estos productos ya tiene otra tabla de fracciones; confirma el cambio.');
    }
    asignaciones.add((tablaId, productoIds, confirmarCambio));
    return productoIds.length;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_Repo> _montar(WidgetTester tester, {_Repo? repo}) async {
  tester.view.physicalSize = const Size(1280, 1600);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final r = repo ?? _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(r),
      puedeGestionarFraccionesProvider.overrideWithValue(true),
    ],
    child: const MaterialApp(home: PantallaTablasDeFracciones()),
  ));
  await tester.pumpAndSettle();
  return r;
}

void main() {
  testWidgets('HU-156 C1 · se crea una tabla con precios escritos a mano, sin proporcionales', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('tabla-nueva')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('tabla-nombre')), 'Esmalte Pintulux galón');
    await tester.enterText(find.byKey(const Key('fraccion-0-medida')), '½ galón');
    await tester.enterText(find.byKey(const Key('fraccion-0-factor')), '0,5');
    await tester.enterText(find.byKey(const Key('fraccion-0-precio')), '34000');
    await tester.tap(find.byKey(const Key('fraccion-agregar')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('fraccion-1-medida')), '¼ galón');
    await tester.enterText(find.byKey(const Key('fraccion-1-factor')), '0.25');
    await tester.enterText(find.byKey(const Key('fraccion-1-precio')), '18000');
    await tester.tap(find.byKey(const Key('tabla-guardar')));
    await tester.pumpAndSettle();

    final t = repo.creada!;
    expect(t.nombre, 'Esmalte Pintulux galón');
    expect(t.fracciones.map((f) => (f.medida, f.factor, f.precio)),
        [('½ galón', 0.5, 34000), ('¼ galón', 0.25, 18000)]);
    expect(find.text('Esmalte Pintulux galón'), findsOneWidget);
  });

  testWidgets('HU-156 C3/C5 · se asignan los resultados de una búsqueda y, si alguno tenía otra tabla, se confirma', (tester) async {
    final repo = _Repo()
      ..pedirConfirmacion = true
      ..tablas.add(const TablaDeFracciones(id: 't1', nombre: 'Esmalte Pintulux galón', fracciones: []));
    await _montar(tester, repo: repo);

    await tester.tap(find.byKey(const Key('asignar-t1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('asignar-buscar')), 'pintulux galon');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('asignar-todos')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('asignar-confirmar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('ya tiene otra tabla'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Cambiar de tabla'));
    await tester.pumpAndSettle();

    expect(repo.asignaciones.single.$2, ['p0', 'p1', 'p2']);
    expect(repo.asignaciones.single.$3, isTrue);
  });
}
