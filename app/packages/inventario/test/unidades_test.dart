import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-155 · unidades de medida del negocio.
class _Repo implements RepositorioDeInventario {
  final List<UnidadDeMedida> lista = [
    const UnidadDeMedida(id: 'u1', codigo: 'GLN', nombre: 'Galón', permiteDecimales: true),
  ];
  (String, String, bool)? creada;

  @override
  Future<List<UnidadDeMedida>> unidadesDeMedida() async => List.of(lista);

  @override
  Future<UnidadDeMedida> crearUnidad(String codigo, String nombre, bool permiteDecimales) async {
    creada = (codigo, nombre, permiteDecimales);
    final u = UnidadDeMedida(
        id: 'u2', codigo: codigo.toUpperCase(), nombre: nombre, permiteDecimales: permiteDecimales);
    lista.add(u);
    return u;
  }

  @override
  Future<UnidadDeMedida> desactivarUnidad(String id) async =>
      throw const ErroresDeValidacion({}, 'No se puede desactivar GLN: la usan 37 productos.');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_Repo> _montar(WidgetTester tester) async {
  final repo = _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      puedeEditarProductosProvider.overrideWithValue(true),
    ],
    child: const MaterialApp(home: PantallaUnidades()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-155 C1 · se crea una unidad y aparece en la lista', (tester) async {
    final repo = await _montar(tester);
    expect(find.text('GLN · Galón'), findsOneWidget);

    await tester.tap(find.byKey(const Key('unidad-nueva')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('unidad-codigo')), 'kg');
    await tester.enterText(find.byKey(const Key('unidad-nombre')), 'Kilogramo');
    await tester.tap(find.byKey(const Key('unidad-decimales')));
    await tester.tap(find.widgetWithText(FilledButton, 'Guardar'));
    await tester.pumpAndSettle();

    expect(repo.creada, ('kg', 'Kilogramo', true));
    expect(find.text('KG · Kilogramo'), findsOneWidget);
  });

  testWidgets('HU-155 C3 · desactivar una unidad en uso muestra cuántos productos la usan', (tester) async {
    await _montar(tester);

    await tester.tap(find.byKey(const Key('desactivar-u1')));
    await tester.pumpAndSettle();

    expect(find.text('No se puede desactivar GLN: la usan 37 productos.'), findsOneWidget);
  });
}
