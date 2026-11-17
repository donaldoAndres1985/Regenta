import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-150 · editar un producto desde la ficha.
class _RepoFicha implements RepositorioDeInventario {
  _RepoFicha(this.ficha);

  FichaDeProducto ficha;
  EdicionDeProducto? guardada;
  ErrorDeApi? errorAlGuardar;

  @override
  Future<FichaDeProducto> verProducto(String id) async => ficha;

  @override
  Future<FichaDeProducto> editarProducto(String id, EdicionDeProducto edicion) async {
    if (errorAlGuardar != null) throw errorAlGuardar!;
    guardada = edicion;
    ficha = FichaDeProducto(
      id: id,
      sku: edicion.sku,
      nombre: edicion.nombre,
      categoriaId: edicion.categoriaId,
      unidadMedidaId: edicion.unidadMedidaId,
      tarifaIva: edicion.tarifaIva,
      precioVenta: edicion.precioVenta,
      costo: edicion.costo,
      version: edicion.version + 1,
    );
    return ficha;
  }

  @override
  Future<List<OpcionDeFicha>> categorias() async =>
      const [OpcionDeFicha(id: 'c1', nombre: 'Pinturas')];

  @override
  Future<List<OpcionDeFicha>> unidades() async =>
      const [OpcionDeFicha(id: 'u1', nombre: 'GLN · Galón')];

  List<(String, num)> existencias = const [];

  @override
  Future<List<(String bodega, num cantidad)>> existenciasDe(String id) async => existencias;

  @override
  Future<FichaDeProducto> cambiarActivo(String id, {required bool activo}) async {
    ficha = FichaDeProducto(
      id: id,
      sku: ficha.sku,
      nombre: ficha.nombre,
      categoriaId: ficha.categoriaId,
      unidadMedidaId: ficha.unidadMedidaId,
      tarifaIva: ficha.tarifaIva,
      precioVenta: ficha.precioVenta,
      costo: ficha.costo,
      version: ficha.version + 1,
      activo: activo,
    );
    return ficha;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

const _barniz = FichaDeProducto(
  id: 'p1',
  sku: '7707031403512',
  nombre: 'Barniz galón',
  categoriaId: 'c1',
  unidadMedidaId: 'u1',
  tarifaIva: 'IVA_19',
  tarifaPorRevisar: true,
  precioVenta: 35500,
  costo: 26500,
  version: 3,
);

Future<void> _montar(WidgetTester tester, _RepoFicha repo, {bool puedeEditar = true}) async {
  tester.view.physicalSize = const Size(1280, 1000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      puedeEditarProductosProvider.overrideWithValue(puedeEditar),
    ],
    child: const MaterialApp(home: PantallaFichaDeProducto(productoId: 'p1')),
  ));
  await tester.pumpAndSettle();
}

void main() {
  test('HU-150 C1 · \$ 38.000 al 19 % con costo \$ 26.500: base 31.932,77, IVA 6.067,23, margen 20,50 %, beneficio 17,01 %', () {
    final c = CalculoDeFicha(precio: 38000, tarifa: 'IVA_19', costo: 26500);

    expect(c.base, 31932.77);
    expect(c.iva, 6067.23);
    expect(c.margenPct, 20.50);
    expect(c.beneficioPct, 17.01);
  });

  testWidgets('HU-150 C1 · al cambiar el precio la ficha recalcula base, IVA y margen, y guarda', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);

    await tester.enterText(find.byKey(const Key('ficha-precio')), '38000');
    await tester.pump();

    expect(find.text(formatearPesos(31932.77)), findsOneWidget);
    expect(find.text(formatearPesos(6067.23)), findsOneWidget);
    expect(find.textContaining('20,5'), findsOneWidget);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(repo.guardada!.precioVenta, 38000);
    expect(repo.guardada!.version, 3, reason: 'manda la versión que leyó');
    expect(find.text('Producto guardado'), findsOneWidget);
  });

  testWidgets('HU-150 C3 · la marca «tarifa por revisar» se ve y desaparece al guardar', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);
    expect(find.text('Tarifa por revisar'), findsOneWidget);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Tarifa por revisar'), findsNothing);
  });

  testWidgets('HU-150 C5 · si alguien guardó antes, se muestra el mensaje del backend', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..errorAlGuardar = const ErroresDeValidacion({}, 'Alguien cambió este producto; recarga antes de guardar.');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Alguien cambió este producto; recarga antes de guardar.'), findsOneWidget);
  });

  testWidgets('HU-151 C1 · desactivar pide confirmación y avisa cuántas unidades hay y dónde', (tester) async {
    final repo = _RepoFicha(_barniz)..existencias = const [('Principal', 8)];
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-desactivar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('No aparecerá en el POS ni en las búsquedas'), findsOneWidget);
    expect(find.textContaining('Tiene 8 unidades en Principal'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
    await tester.pumpAndSettle();

    expect(repo.ficha.activo, isFalse);
    expect(find.text('Inactivo'), findsOneWidget);
    expect(find.byKey(const Key('ficha-reactivar')), findsOneWidget);
  });

  testWidgets('HU-151 C4 · un producto inactivo se reactiva', (tester) async {
    final repo = _RepoFicha(FichaDeProducto(
      id: 'p1',
      sku: 'X',
      nombre: 'Barniz galón',
      categoriaId: 'c1',
      unidadMedidaId: 'u1',
      tarifaIva: 'IVA_19',
      precioVenta: 35500,
      version: 1,
      activo: false,
    ));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-reactivar')));
    await tester.pumpAndSettle();

    expect(repo.ficha.activo, isTrue);
    expect(find.byKey(const Key('ficha-desactivar')), findsOneWidget);
  });

  testWidgets('HU-150 C4 · sin permiso de editar, la ficha se ve sin poder guardar', (tester) async {
    await _montar(tester, _RepoFicha(_barniz), puedeEditar: false);

    expect(find.byKey(const Key('ficha-guardar')), findsNothing);
    final precio = tester.widget<TextField>(
        find.descendant(of: find.byKey(const Key('ficha-precio')), matching: find.byType(TextField)));
    expect(precio.enabled, isFalse);
  });
}
