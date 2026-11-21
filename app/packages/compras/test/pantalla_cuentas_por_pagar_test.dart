import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeCuentasPorPagar {
  _RepoFake(this.cuentas);

  List<CuentaPorPagarVista> cuentas;
  final List<(String, String, DateTime)> facturas = [];
  ErrorDeApi? falla;

  @override
  Future<List<CuentaPorPagarVista>> listar() async => cuentas;

  @override
  Future<CuentaPorPagarVista> registrarFactura(String cuentaId, String numero, DateTime fecha) async {
    if (falla != null) throw falla!;
    facturas.add((cuentaId, numero, fecha));
    final c = cuentas.firstWhere((x) => x.id == cuentaId);
    return CuentaPorPagarVista(
      id: c.id,
      proveedorNombre: c.proveedorNombre,
      recepcionNumero: c.recepcionNumero,
      numeroFactura: numero.trim(),
      monto: c.monto,
      saldo: c.saldo,
      fechaVencimiento: fecha.add(const Duration(days: 30)),
    );
  }
}

final _remision = CuentaPorPagarVista(
  id: 'c-1',
  proveedorNombre: 'Andina',
  recepcionNumero: 'REC-12',
  enEsperaDeFactura: true,
  monto: 119000,
  saldo: 119000,
  fechaVencimiento: DateTime(2026, 12, 20),
);

final _conFactura = CuentaPorPagarVista(
  id: 'c-2',
  proveedorNombre: 'Pinturas del Valle',
  recepcionNumero: 'REC-3',
  numeroFactura: 'FV-77',
  monto: 50000,
  saldo: 50000,
  fechaVencimiento: DateTime(2026, 11, 1),
  vencida: true,
  diasDeMora: 19,
);

Future<_RepoFake> _montar(WidgetTester tester, {Size tamano = const Size(1440, 900)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake([_remision, _conFactura]);
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeCuentasPorPagarProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaCuentasPorPagar()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-170 C4 · «En espera de factura» deja solo las que llegaron con remisión, con su recepción',
      (tester) async {
    await _montar(tester);
    expect(find.text('En espera de factura (1)'), findsOneWidget);

    await tester.tap(find.byKey(const Key('cuentas-filtro-enEspera')));
    await tester.pumpAndSettle();

    expect(find.text('ANDINA'), findsOneWidget);
    expect(find.text('PINTURAS DEL VALLE'), findsNothing);
    expect(find.text('Remisión REC-12 · vence 20/12/2026'), findsOneWidget);
    expect(find.text('EN ESPERA DE FACTURA'), findsOneWidget);
  });

  testWidgets('HU-170 C5 / HU-169 C5 · Las vencidas se marcan con sus días de mora', (tester) async {
    await _montar(tester);

    expect(find.text('Factura FV-77 · vence 01/11/2026'), findsOneWidget);
    expect(find.text('VENCIDA 19 D'), findsOneWidget);
  });

  testWidgets('HU-170 C3 · Registrar la factura quita la marca de espera', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('cuenta-registrar-factura-c-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('factura-numero')), 'FV-300');
    await tester.tap(find.byKey(const Key('factura-guardar')));
    await tester.pumpAndSettle();

    expect(repo.facturas.single.$1, 'c-1');
    expect(repo.facturas.single.$2, 'FV-300');
    expect(find.text('EN ESPERA DE FACTURA'), findsNothing);
    expect(find.textContaining('Factura FV-300'), findsOneWidget);
    expect(find.text('Factura registrada. El vencimiento se calculó desde su fecha.'), findsOneWidget);
  });

  testWidgets('HU-170 C3 · Una factura repetida muestra el motivo y el diálogo sigue abierto', (tester) async {
    final repo = await _montar(tester);
    repo.falla = const RecursoDuplicado('Ya hay una cuenta con la factura FV-77 de ese proveedor');

    await tester.tap(find.byKey(const Key('cuenta-registrar-factura-c-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('factura-numero')), 'FV-77');
    await tester.tap(find.byKey(const Key('factura-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Ya hay una cuenta con la factura FV-77 de ese proveedor'), findsOneWidget);
    expect(find.byKey(const Key('factura-numero')), findsOneWidget);
  });

  testWidgets('HU-170 · Sin número, el diálogo lo pide antes de llamar al servidor', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('cuenta-registrar-factura-c-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('factura-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Escribe el número de la factura.'), findsOneWidget);
    expect(repo.facturas, isEmpty);
  });

  testWidgets('HU-170 · En el celular el listado cabe sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 844));

    expect(find.byKey(const Key('cuentas-lista')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
