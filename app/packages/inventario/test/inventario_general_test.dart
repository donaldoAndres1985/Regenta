import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-153 · inventario general con filtros y valor.
class _Repo implements RepositorioDeInventario {
  final List<ConsultaDeInventario> consultas = [];

  @override
  Future<ResultadoDeInventario> inventarioGeneral(ConsultaDeInventario consulta) async {
    consultas.add(consulta);
    return ResultadoDeInventario(
      filas: [
        FilaDeInventario.desdeJson({
          'id': 'p1',
          'sku': 'BAR-1',
          'nombre': 'Barniz galón',
          'categoria': 'Pinturas',
          'tarifaIva': 'IVA_19',
          'tarifaPorRevisar': true,
          'existencia': 3,
          'costo': 26500,
          'precio': 35500,
          'valorCosto': 79500,
          'valorPrecio': 106500,
          'activo': true,
        }),
      ],
      totalFilas: 1,
      valorCosto: 79500,
      valorPrecio: 106500,
    );
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_Repo> _montar(WidgetTester tester) async {
  tester.view.physicalSize = const Size(1440, 1000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final repo = _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeInventarioProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaInventarioGeneral()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-153 C1 · muestra cada producto con existencia, costo, precio y valores, y los totales', (tester) async {
    await _montar(tester);

    expect(find.text('BAR-1'), findsOneWidget);
    expect(find.text('Barniz galón'), findsOneWidget);
    expect(find.text('Tarifa por revisar'), findsWidgets);
    expect(find.text(formatearPesos(79500)), findsWidgets);
    expect(find.text(formatearPesos(106500)), findsWidgets);
  });

  testWidgets('HU-153 C2 · los filtros «tarifa por revisar», «existencia negativa» e «inactivos» se mandan al backend', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('filtro-por-revisar')));
    await tester.pumpAndSettle();
    expect(repo.consultas.last.tarifaPorRevisar, isTrue);

    await tester.tap(find.byKey(const Key('filtro-negativa')));
    await tester.pumpAndSettle();
    expect(repo.consultas.last.existenciaNegativa, isTrue);

    await tester.tap(find.byKey(const Key('filtro-inactivos')));
    await tester.pumpAndSettle();
    expect(repo.consultas.last.estado, 'INACTIVOS');
  });

  testWidgets('HU-153 C3 · el texto escrito se manda como búsqueda', (tester) async {
    final repo = await _montar(tester);

    await tester.enterText(find.byKey(const Key('inventario-general-buscar')), 'barniz trans');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    expect(repo.consultas.last.q, 'barniz trans');
  });
}
