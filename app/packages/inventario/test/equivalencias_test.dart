import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-161 · equivalencias de detallado.
class _Repo implements RepositorioDeInventario {
  final List<EquivalenciaDeDetallado> lista = [];
  (String, String, num)? creada;
  (List<String>, String, String, num)? replica;
  ErrorDeApi? errorAlCrear;

  @override
  Future<List<EquivalenciaDeDetallado>> equivalencias() async => List.of(lista);

  @override
  Future<ResultadoDeBusqueda> buscar(
      {String? termino, String? categoriaId, bool soloBajoMinimo = false, int? limite}) async {
    final todos = [
      {'id': 'c1', 'nombre': 'Pintulux blanco cuñete'},
      {'id': 'c2', 'nombre': 'Pintulux verde cuñete'},
      {'id': 'g1', 'nombre': 'Pintulux blanco galón'},
    ].where((p) => (p['nombre'] as String).toLowerCase().contains((termino ?? '').split(' ').last));
    return ResultadoDeBusqueda.desdeJson({
      'productos': [
        for (final p in todos)
          {...p, 'sku': p['id'], 'precioVenta': 1, 'stockTotal': 1, 'nivelStock': 'NORMAL', 'categoriaNombre': ''},
      ],
      'total': todos.length,
    });
  }

  @override
  Future<EquivalenciaDeDetallado> crearEquivalencia(String origenId, String destinoId, num factor) async {
    if (errorAlCrear != null) throw errorAlCrear!;
    creada = (origenId, destinoId, factor);
    final e = EquivalenciaDeDetallado(
        id: 'e1', origenId: origenId, origen: 'Pintulux blanco cuñete', destinoId: destinoId,
        destino: 'Pintulux blanco galón', factor: factor);
    lista.add(e);
    return e;
  }

  @override
  Future<ResultadoDeReplica> replicarEquivalencia(
      List<String> origenIds, String buscar, String reemplazar, num factor) async {
    replica = (origenIds, buscar, reemplazar, factor);
    return const ResultadoDeReplica(creadas: 1, sinPar: ['Pintulux verde cuñete']);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_Repo> _montar(WidgetTester tester) async {
  tester.view.physicalSize = const Size(1280, 1400);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final repo = _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      puedeDetallarProvider.overrideWithValue(true),
    ],
    child: const MaterialApp(home: PantallaEquivalencias()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

Future<void> _elegir(WidgetTester tester, String campo, String termino, String nombre) async {
  await tester.enterText(find.byKey(Key('$campo-buscar')), termino);
  await tester.testTextInput.receiveAction(TextInputAction.done);
  await tester.pumpAndSettle();
  await tester.tap(find.text(nombre).last);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('HU-161 C1 · se guarda «1 cuñete = 5 galones» eligiendo origen y destino', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('equivalencia-nueva')));
    await tester.pumpAndSettle();
    await _elegir(tester, 'origen', 'pintulux cuñete', 'Pintulux blanco cuñete');
    await _elegir(tester, 'destino', 'pintulux galón', 'Pintulux blanco galón');
    await tester.enterText(find.byKey(const Key('equivalencia-factor')), '5');
    await tester.tap(find.byKey(const Key('equivalencia-guardar')));
    await tester.pumpAndSettle();

    expect(repo.creada, ('c1', 'g1', 5));
    expect(find.text('1 Pintulux blanco cuñete = 5 Pintulux blanco galón'), findsOneWidget);
  });

  testWidgets('HU-161 C2 · el rechazo del backend se muestra en el formulario', (tester) async {
    final repo = await _montar(tester)
      ..errorAlCrear = const ErroresDeValidacion({}, '«A» y «B» no se miden en la misma unidad.');

    await tester.tap(find.byKey(const Key('equivalencia-nueva')));
    await tester.pumpAndSettle();
    await _elegir(tester, 'origen', 'pintulux cuñete', 'Pintulux blanco cuñete');
    await _elegir(tester, 'destino', 'pintulux galón', 'Pintulux blanco galón');
    await tester.enterText(find.byKey(const Key('equivalencia-factor')), '5');
    await tester.tap(find.byKey(const Key('equivalencia-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('«A» y «B» no se miden en la misma unidad.'), findsOneWidget);
    expect(repo.creada, isNull);
  });

  testWidgets('HU-161 C3 · replicar a los cuñetes de una búsqueda muestra los que quedaron sin par', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('equivalencia-replicar')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('replica-buscar-productos')), 'pintulux cuñete');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('replica-buscar')), 'cuñete');
    await tester.enterText(find.byKey(const Key('replica-reemplazar')), 'galón');
    await tester.enterText(find.byKey(const Key('replica-factor')), '5');
    await tester.tap(find.byKey(const Key('replica-aplicar')));
    await tester.pumpAndSettle();

    expect(repo.replica!.$1, ['c1', 'c2']);
    expect(repo.replica!.$2, 'cuñete');
    expect(repo.replica!.$3, 'galón');
    expect(find.textContaining('1 equivalencias creadas'), findsOneWidget);
    expect(find.textContaining('Sin par (no se creó nada): Pintulux verde cuñete'), findsOneWidget);
  });
}
