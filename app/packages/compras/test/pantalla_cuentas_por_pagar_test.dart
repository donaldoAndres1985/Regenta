import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeCuentasPorPagar {
  _RepoFake(this.cuentas);

  List<CuentaPorPagarVista> cuentas;
  final List<(String, String, DateTime)> facturas = [];
  final List<Map<String, Object?>> pagos = [];
  ErrorDeApi? falla;
  bool cajaAbierta = true;
  final Map<String, List<PagoDeLaCuenta>> pagosPorCuenta = {};
  final List<(String, String)> anulados = [];
  ErrorDeApi? fallaAlAnular;

  @override
  Future<bool> tengoCajaAbierta() async => cajaAbierta;

  @override
  Future<List<PagoDeLaCuenta>> pagosDe(String cuentaId) async => [...?pagosPorCuenta[cuentaId]];

  @override
  Future<void> anularPago(String pagoId, String motivo) async {
    if (fallaAlAnular != null) throw fallaAlAnular!;
    anulados.add((pagoId, motivo));
    for (final entrada in pagosPorCuenta.entries) {
      pagosPorCuenta[entrada.key] = [
        for (final p in entrada.value)
          p.id == pagoId
              ? PagoDeLaCuenta(
                  id: p.id, monto: p.monto, metodo: p.metodo, fecha: p.fecha, desdeLaCaja: p.desdeLaCaja,
                  anulado: true, motivoAnulacion: motivo)
              : p,
      ];
    }
  }

  @override
  Future<void> pagar({
    required String proveedorId,
    required num monto,
    required String metodo,
    String? referencia,
    bool desdeLaCaja = false,
    Map<String, num>? aplicaciones,
  }) async {
    if (falla != null) throw falla!;
    pagos.add({
      'proveedorId': proveedorId,
      'monto': monto,
      'metodo': metodo,
      'desdeLaCaja': desdeLaCaja,
      'aplicaciones': aplicaciones,
    });
  }

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

Future<_RepoFake> _montar(WidgetTester tester,
    {Size tamano = const Size(1440, 900), List<CuentaPorPagarVista>? cuentas}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake(cuentas ?? [_remision, _conFactura]);
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

  // ------------------------------------------------------------- HU-171 · un pago a varias facturas

  final vieja = CuentaPorPagarVista(
    id: 'c-a', proveedorId: 'p-1', proveedorNombre: 'Andina', numeroFactura: 'FV-1',
    monto: 300000, saldo: 300000, fechaVencimiento: DateTime(2026, 10, 31),
  );
  final nueva = CuentaPorPagarVista(
    id: 'c-b', proveedorId: 'p-1', proveedorNombre: 'Andina', numeroFactura: 'FV-2',
    monto: 500000, saldo: 500000, fechaVencimiento: DateTime(2026, 12, 1),
  );

  test('HU-171 C1 · El reparto por omisión va de la más antigua a la más nueva', () {
    expect(repartirPorAntiguedad(400000, [vieja, nueva]), {'c-a': 300000, 'c-b': 100000});
    expect(repartirPorAntiguedad(200000, [vieja, nueva]), {'c-a': 200000});
  });

  Future<_RepoFake> abrirPago(WidgetTester tester, {bool cajaAbierta = true, Size? tamano}) async {
    final repo = await _montar(tester, cuentas: [nueva, vieja], tamano: tamano ?? const Size(1440, 1000));
    repo.cajaAbierta = cajaAbierta;
    await tester.tap(find.byKey(const Key('cuentas-pagar')));
    await tester.pumpAndSettle();
    return repo;
  }

  testWidgets('HU-171 C1 · 400.000 sin tocar el reparto: 300.000 a la más antigua y 100.000 a la otra',
      (tester) async {
    final repo = await abrirPago(tester);
    await tester.enterText(find.byKey(const Key('pago-monto')), '400000');
    await tester.pumpAndSettle();

    expect(tester.widget<Text>(find.byKey(const Key('pago-reparto-c-a'))).data, '\$300.000');
    expect(tester.widget<Text>(find.byKey(const Key('pago-reparto-c-b'))).data, '\$100.000');
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(repo.pagos.single['monto'], 400000);
    expect(repo.pagos.single['proveedorId'], 'p-1');
    expect(repo.pagos.single['aplicaciones'], isNull, reason: 'sin reparto a mano lo reparte el backend');
    expect(find.text('Pago registrado.'), findsOneWidget);
  });

  testWidgets('HU-171 C2 · Repartido a mano: todo a la segunda factura', (tester) async {
    final repo = await abrirPago(tester);
    await tester.enterText(find.byKey(const Key('pago-monto')), '400000');
    await tester.tap(find.byKey(const Key('pago-a-mano')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('pago-aplicar-c-a')), '0');
    await tester.enterText(find.byKey(const Key('pago-aplicar-c-b')), '400000');
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(repo.pagos.single['aplicaciones'], {'c-a': 0, 'c-b': 400000});
  });

  testWidgets('HU-171 C3 · Más de lo que se debe no se deja registrar', (tester) async {
    final repo = await abrirPago(tester);
    await tester.enterText(find.byKey(const Key('pago-monto')), '900000');
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(find.text('No se paga más de lo que se debe: le debes \$800.000.'), findsOneWidget);
    expect(repo.pagos, isEmpty);
  });

  testWidgets('HU-171 · Un reparto a mano que no suma el pago no se deja registrar', (tester) async {
    final repo = await abrirPago(tester);
    await tester.enterText(find.byKey(const Key('pago-monto')), '400000');
    await tester.tap(find.byKey(const Key('pago-a-mano')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('pago-aplicar-c-b')), '50000');
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('El reparto suma'), findsOneWidget);
    expect(repo.pagos, isEmpty);
  });

  testWidgets('HU-171 / HU-145 · En efectivo con la caja abierta, sale de la caja', (tester) async {
    final repo = await abrirPago(tester);
    await tester.enterText(find.byKey(const Key('pago-monto')), '300000');
    await tester.tap(find.byKey(const Key('pago-metodo')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Efectivo').last);
    await tester.pumpAndSettle();

    expect(tester.widget<CheckboxListTile>(find.byKey(const Key('pago-desde-la-caja'))).value, isTrue);
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(repo.pagos.single['metodo'], 'EFECTIVO');
    expect(repo.pagos.single['desdeLaCaja'], isTrue);
  });

  testWidgets('HU-171 C6 · Si el servidor rechaza el pago, el motivo se ve y el diálogo sigue abierto',
      (tester) async {
    final repo = await abrirPago(tester);
    repo.falla = const RecursoDuplicado('No tienes una caja abierta');
    await tester.enterText(find.byKey(const Key('pago-monto')), '100000');
    await tester.tap(find.byKey(const Key('pago-registrar')));
    await tester.pumpAndSettle();

    expect(find.text('No tienes una caja abierta'), findsOneWidget);
    expect(find.byKey(const Key('pago-monto')), findsOneWidget);
  });

  testWidgets('HU-171 · Pagar desde una fila deja elegido a su proveedor', (tester) async {
    await _montar(tester, cuentas: [nueva, vieja]);
    await tester.tap(find.byKey(const Key('cuenta-pagar-c-a')));
    await tester.pumpAndSettle();

    expect(find.text('Le debes \$800.000'), findsOneWidget);
    expect(find.byKey(const Key('pago-cuenta-c-a')), findsOneWidget);
    expect(find.byKey(const Key('pago-cuenta-c-b')), findsOneWidget);
  });

  testWidgets('HU-171 · En el celular el diálogo de pago cabe sin desbordarse', (tester) async {
    await abrirPago(tester, tamano: const Size(390, 844));
    await tester.enterText(find.byKey(const Key('pago-monto')), '400000');
    await tester.tap(find.byKey(const Key('pago-a-mano')));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });

  // ------------------------------------------------------------------ HU-172 · anular un pago

  final pagada = CuentaPorPagarVista(
    id: 'c-p', proveedorId: 'p-1', proveedorNombre: 'Andina', numeroFactura: 'FV-1',
    monto: 300000, saldo: 0, estado: 'PAGADA', fechaVencimiento: DateTime(2026, 10, 31),
  );
  final pagoDeLaCaja = PagoDeLaCuenta(
    id: 'pg-1', monto: 300000, metodo: 'EFECTIVO', fecha: DateTime(2026, 11, 20), desdeLaCaja: true,
  );

  Future<_RepoFake> abrirPagos(WidgetTester tester) async {
    final repo = await _montar(tester, cuentas: [pagada]);
    repo.pagosPorCuenta['c-p'] = [pagoDeLaCaja];
    await tester.tap(find.byKey(const Key('cuentas-filtro-todas')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('cuenta-ver-pagos-c-p')));
    await tester.pumpAndSettle();
    return repo;
  }

  testWidgets('HU-172 C1 · Anular un pago con su motivo', (tester) async {
    final repo = await abrirPagos(tester);
    expect(find.text('20/11/2026 · Efectivo · de la caja'), findsOneWidget);

    await tester.tap(find.byKey(const Key('pago-anular-pg-1')));
    await tester.pumpAndSettle();
    expect(find.textContaining('el efectivo regresa a la caja'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('anular-motivo')), 'Se registró dos veces');
    await tester.tap(find.byKey(const Key('anular-confirmar')));
    await tester.pumpAndSettle();

    expect(repo.anulados.single, ('pg-1', 'Se registró dos veces'));
  });

  testWidgets('HU-172 C2 · El pago anulado sigue visible, tachado y con su motivo', (tester) async {
    await abrirPagos(tester);
    await tester.tap(find.byKey(const Key('pago-anular-pg-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('anular-motivo')), 'Se registró dos veces');
    await tester.tap(find.byKey(const Key('anular-confirmar')));
    await tester.pumpAndSettle();

    expect(find.text('ANULADO'), findsOneWidget);
    expect(find.text('Motivo: Se registró dos veces'), findsOneWidget);
    expect(find.byKey(const Key('pago-anular-pg-1')), findsNothing, reason: 'no se anula dos veces');
  });

  testWidgets('HU-172 · Sin motivo no se anula', (tester) async {
    final repo = await abrirPagos(tester);
    await tester.tap(find.byKey(const Key('pago-anular-pg-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anular-confirmar')));
    await tester.pumpAndSettle();

    expect(find.text('Escribe por qué se anula.'), findsOneWidget);
    expect(repo.anulados, isEmpty);
  });

  testWidgets('HU-172 C4 / C5 · Si el servidor rechaza (sin caja abierta, sin permiso), se ve el motivo',
      (tester) async {
    final repo = await abrirPagos(tester);
    repo.fallaAlAnular = const SinPermiso('Sin permiso');
    await tester.tap(find.byKey(const Key('pago-anular-pg-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('anular-motivo')), 'Error');
    await tester.tap(find.byKey(const Key('anular-confirmar')));
    await tester.pumpAndSettle();

    expect(find.text('No tienes permiso para anular pagos a proveedores.'), findsOneWidget);
    expect(find.byKey(const Key('pago-anular-pg-1')), findsOneWidget);
  });

  testWidgets('HU-172 · Una cuenta sin abonos no muestra «Ver pagos»', (tester) async {
    await _montar(tester, cuentas: [vieja]);

    expect(find.byKey(const Key('cuenta-ver-pagos-c-a')), findsNothing);
  });
}
