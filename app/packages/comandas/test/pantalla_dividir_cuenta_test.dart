import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_comandas/regenta_comandas.dart';

class _RepoComandasFake implements RepositorioDeComandas {
  _RepoComandasFake(this.comandaFija);
  ComandaVista comandaFija;
  ErrorDeApi? errorAlCargar;

  @override
  Future<ComandaVista> comanda(String comandaId) async {
    if (errorAlCargar != null) throw errorAlCargar!;
    return comandaFija;
  }

  @override
  Future<List<ItemDeCarta>> itemsDeCarta(String cartaId) async => const [];

  @override
  Future<List<GrupoModificadores>> gruposDeItem(String itemId) async => const [];

  @override
  Future<ComandaVista> agregarLinea(String comandaId,
          {required String itemMenuId,
          required num cantidad,
          List<String> modificadorIds = const [],
          String? notas}) async =>
      comandaFija;

  @override
  Future<ComandaVista> enviarACocina(String comandaId) async => comandaFija;

  @override
  Future<ComandaVista> avanzarLinea(String comandaId, String lineaId) async => comandaFija;
}

class _RepoCuentasFake implements RepositorioDeCuentas {
  _RepoCuentasFake({this.cuentasFijas = const []});

  static const _totalesPorLinea = {'l1': 40000.0, 'l2': 20000.0};

  List<CuentaVista> cuentasFijas;
  ErrorDeApi? errorAlCargar;
  final List<String> pagadas = [];
  final List<({String cuentaId, String metodo, num? propina, num? montoRecibido})> llamadasPago = [];

  @override
  Future<List<CuentaVista>> listar(String comandaId) async {
    if (errorAlCargar != null) throw errorAlCargar!;
    return cuentasFijas;
  }

  @override
  Future<CuentaVista> crear(String comandaId, {String? etiqueta}) async {
    final nueva = CuentaVista(
      id: 'c${cuentasFijas.length + 1}',
      comandaId: comandaId,
      numeroDivision: cuentasFijas.length + 1,
      etiqueta: etiqueta,
      modoDivision: 'POR_ITEM',
      subtotal: 0,
      impuestoTotal: 0,
      propina: 0,
      total: 0,
      pagado: 0,
      estado: 'ABIERTA',
      lineas: const [],
    );
    cuentasFijas = [...cuentasFijas, nueva];
    return nueva;
  }

  @override
  Future<List<CuentaVista>> marcarLinea(String comandaId, String cuentaId, String lineaId) async {
    // Reparte 1/N entre todas las cuentas que ya marcan esa línea, más esta.
    final marcandoYa = cuentasFijas.where((c) => c.proporcionDe(lineaId) != null || c.id == cuentaId).toList();
    final n = marcandoYa.length;
    final proporcion = 1 / n;
    final monto = (_totalesPorLinea[lineaId] ?? 0) * proporcion;
    cuentasFijas = [
      for (final c in cuentasFijas)
        if (marcandoYa.any((m) => m.id == c.id))
          _conLinea(c, lineaId, proporcion, monto)
        else
          c,
    ];
    return cuentasFijas;
  }

  @override
  Future<List<CuentaVista>> desmarcarLinea(String comandaId, String cuentaId, String lineaId) async {
    cuentasFijas = [
      for (final c in cuentasFijas)
        if (c.id == cuentaId)
          CuentaVista(
            id: c.id,
            comandaId: c.comandaId,
            numeroDivision: c.numeroDivision,
            etiqueta: c.etiqueta,
            modoDivision: c.modoDivision,
            subtotal: c.subtotal,
            impuestoTotal: c.impuestoTotal,
            propina: c.propina,
            total: 0,
            pagado: c.pagado,
            estado: c.estado,
            lineas: c.lineas.where((l) => l.lineaId != lineaId).toList(),
          )
        else
          c,
    ];
    return cuentasFijas;
  }

  @override
  Future<List<CuentaVista>> dividirEnPartesIguales(String comandaId, int numeroPartes) async {
    final total = 60000 / numeroPartes;
    cuentasFijas = [
      for (var i = 1; i <= numeroPartes; i++)
        CuentaVista(
          id: 'p$i',
          comandaId: comandaId,
          numeroDivision: i,
          modoDivision: 'PARTES_IGUALES',
          subtotal: total,
          impuestoTotal: 0,
          propina: 0,
          total: total,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: const [],
        ),
    ];
    return cuentasFijas;
  }

  @override
  Future<CuentaVista> registrarPago(
    String comandaId,
    String cuentaId, {
    required String metodo,
    num? montoRecibido,
    num? propina,
    String? referencia,
  }) async {
    pagadas.add(cuentaId);
    llamadasPago.add((cuentaId: cuentaId, metodo: metodo, propina: propina, montoRecibido: montoRecibido));
    cuentasFijas = [
      for (final c in cuentasFijas)
        if (c.id == cuentaId)
          CuentaVista(
            id: c.id,
            comandaId: c.comandaId,
            numeroDivision: c.numeroDivision,
            etiqueta: c.etiqueta,
            modoDivision: c.modoDivision,
            subtotal: c.subtotal,
            impuestoTotal: c.impuestoTotal,
            propina: c.propina,
            total: c.total,
            pagado: c.total,
            estado: 'PAGADA',
            lineas: c.lineas,
          )
        else
          c,
    ];
    return cuentasFijas.firstWhere((c) => c.id == cuentaId);
  }

  CuentaVista _conLinea(CuentaVista c, String lineaId, double proporcion, double monto) {
    final sinEsta = c.lineas.where((l) => l.lineaId != lineaId).toList();
    final lineas = [...sinEsta, LineaDeCuentaVista(lineaId: lineaId, proporcion: proporcion, monto: monto)];
    final total = lineas.fold<num>(0, (s, l) => s + l.monto);
    return CuentaVista(
      id: c.id,
      comandaId: c.comandaId,
      numeroDivision: c.numeroDivision,
      etiqueta: c.etiqueta,
      modoDivision: c.modoDivision,
      subtotal: total,
      impuestoTotal: c.impuestoTotal,
      propina: c.propina,
      total: total,
      pagado: c.pagado,
      estado: c.estado,
      lineas: lineas,
    );
  }
}

ComandaVista _comandaDePrueba() => const ComandaVista(
      id: 'c1',
      numero: 'CMD-0418',
      estado: 'EN_COCINA',
      numComensales: 4,
      subtotal: 60000,
      impuestoTotal: 0,
      propinaSugerida: 6000,
      total: 60000,
      lineas: [
        LineaVista(
            id: 'l1',
            linea: 1,
            nombre: 'Bandeja paisa',
            estado: 'ENTREGADA',
            curso: 'FUERTE',
            secuenciaEnvio: 1,
            cantidad: 1,
            total: 40000,
            modificadores: []),
        LineaVista(
            id: 'l2',
            linea: 2,
            nombre: 'Limonada de coco',
            estado: 'ENTREGADA',
            curso: 'BEBIDA',
            secuenciaEnvio: 1,
            cantidad: 1,
            total: 20000,
            modificadores: []),
      ],
    );

Future<void> _montar(
  WidgetTester tester,
  _RepoComandasFake repoComandas,
  _RepoCuentasFake repoCuentas, {
  Size size = const Size(1440, 900),
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        repositorioDeComandasProvider.overrideWithValue(repoComandas),
        repositorioDeCuentasProvider.overrideWithValue(repoCuentas),
      ],
      child: const MaterialApp(home: PantallaDividirCuenta(comandaId: 'c1')),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('sin cuentas todavía, invita a añadir una', (tester) async {
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), _RepoCuentasFake());

    expect(find.textContaining('Todavía no hay cuentas'), findsOneWidget);
    expect(find.byKey(const Key('dividir-anadir-cuenta')), findsOneWidget);
  });

  testWidgets('criterio 1: marcar una línea en una cuenta le suma el total de esa línea', (tester) async {
    final repoCuentas = _RepoCuentasFake(cuentasFijas: [
      const CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 0,
          impuestoTotal: 0,
          propina: 0,
          total: 0,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: []),
    ]);
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), repoCuentas);

    await tester.tap(find.byKey(const Key('dividir-check-c1-l1')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('dividir-total-c1')), findsOneWidget);
    expect((tester.widget(find.byKey(const Key('dividir-total-c1'))) as Text).data, contains('40.000'));
  });

  testWidgets('criterio 2: marcar la misma línea en dos cuentas la reparte al 50%', (tester) async {
    final repoCuentas = _RepoCuentasFake(cuentasFijas: const [
      CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 0,
          impuestoTotal: 0,
          propina: 0,
          total: 0,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: []),
      CuentaVista(
          id: 'c2',
          comandaId: 'c1',
          numeroDivision: 2,
          modoDivision: 'POR_ITEM',
          subtotal: 0,
          impuestoTotal: 0,
          propina: 0,
          total: 0,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: []),
    ]);
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), repoCuentas);

    await tester.tap(find.byKey(const Key('dividir-check-c1-l2')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('dividir-check-c2-l2')));
    await tester.pumpAndSettle();

    expect(find.text('1 / 2'), findsOneWidget);
  });

  testWidgets('criterio 3: dividir en partes iguales entre cuatro deja cada cuenta con la cuarta parte',
      (tester) async {
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), _RepoCuentasFake());

    await tester.tap(find.byKey(const Key('dividir-partes-iguales')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('dividir-partes-input')), '4');
    await tester.tap(find.byKey(const Key('dividir-partes-confirmar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('15.000'), findsNWidgets(4));
  });

  testWidgets('criterio 4: una cuenta pagada no deja marcarle ni desmarcarle líneas', (tester) async {
    final repoCuentas = _RepoCuentasFake(cuentasFijas: [
      CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 40000,
          impuestoTotal: 0,
          propina: 0,
          total: 40000,
          pagado: 40000,
          estado: 'PAGADA',
          lineas: const [LineaDeCuentaVista(lineaId: 'l1', proporcion: 1, monto: 40000)]),
    ]);
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), repoCuentas);

    final casilla = tester.widget<Checkbox>(find.byKey(const Key('dividir-check-c1-l1')));
    expect(casilla.onChanged, isNull);
    expect(find.byKey(const Key('dividir-cobrar-c1')), findsNothing);
  });

  testWidgets('criterio 5: cobrar la única cuenta abierta refleja la comanda cerrada', (tester) async {
    final comanda = _comandaDePrueba();
    final repoComandas = _RepoComandasFake(comanda);
    final repoCuentas = _RepoCuentasFake(cuentasFijas: [
      CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 60000,
          impuestoTotal: 0,
          propina: 0,
          total: 60000,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: const [
            LineaDeCuentaVista(lineaId: 'l1', proporcion: 1, monto: 40000),
            LineaDeCuentaVista(lineaId: 'l2', proporcion: 1, monto: 20000),
          ]),
    ]);
    await _montar(tester, repoComandas, repoCuentas);

    // Al cerrarse, el repo de comandas devuelve la comanda ya CERRADA.
    repoComandas.comandaFija = ComandaVista(
      id: comanda.id,
      numero: comanda.numero,
      estado: 'CERRADA',
      numComensales: comanda.numComensales,
      subtotal: comanda.subtotal,
      impuestoTotal: comanda.impuestoTotal,
      propinaSugerida: comanda.propinaSugerida,
      total: comanda.total,
      lineas: comanda.lineas,
    );

    await tester.tap(find.byKey(const Key('dividir-cobrar-c1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('cobrar-confirmar')));
    await tester.pumpAndSettle();

    expect(repoCuentas.pagadas, ['c1']);
    expect(find.byKey(const Key('dividir-cobrar-c1')), findsNothing);
  });

  testWidgets('HU-090 criterio 2: el diálogo de cobro prellena la propina sugerida y se puede cambiar',
      (tester) async {
    final repoCuentas = _RepoCuentasFake(cuentasFijas: [
      const CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 40000,
          impuestoTotal: 0,
          propina: 0,
          propinaSugerida: 4000,
          total: 40000,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: [LineaDeCuentaVista(lineaId: 'l1', proporcion: 1, monto: 40000)]),
    ]);
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), repoCuentas);

    await tester.tap(find.byKey(const Key('dividir-cobrar-c1')));
    await tester.pumpAndSettle();

    final campoPropina = tester.widget<TextField>(find.byKey(const Key('cobrar-propina')));
    expect(campoPropina.controller!.text, '4000');

    await tester.enterText(find.byKey(const Key('cobrar-propina')), '6000');
    await tester.enterText(find.byKey(const Key('cobrar-monto-recibido')), '50000');
    await tester.tap(find.byKey(const Key('cobrar-confirmar')));
    await tester.pumpAndSettle();

    expect(repoCuentas.llamadasPago, [(cuentaId: 'c1', metodo: 'EFECTIVO', propina: 6000, montoRecibido: 50000)]);
  });

  testWidgets('en móvil no hay rejilla de casillas; hay barra de cobro fija', (tester) async {
    final repoCuentas = _RepoCuentasFake(cuentasFijas: const [
      CuentaVista(
          id: 'c1',
          comandaId: 'c1',
          numeroDivision: 1,
          modoDivision: 'POR_ITEM',
          subtotal: 60000,
          impuestoTotal: 0,
          propina: 0,
          total: 60000,
          pagado: 0,
          estado: 'ABIERTA',
          lineas: [
            LineaDeCuentaVista(lineaId: 'l1', proporcion: 1, monto: 40000),
            LineaDeCuentaVista(lineaId: 'l2', proporcion: 1, monto: 20000),
          ]),
    ]);
    await _montar(tester, _RepoComandasFake(_comandaDePrueba()), repoCuentas, size: const Size(390, 844));

    expect(find.byKey(const Key('dividir-movil')), findsOneWidget);
    expect(find.byType(Checkbox), findsNothing);
    expect(find.byKey(const Key('dividir-falta-por-cobrar')), findsOneWidget);
    expect(find.byKey(const Key('dividir-cobrar-movil-c1')), findsOneWidget);
  });

  testWidgets('error al cargar ofrece reintentar', (tester) async {
    final repoComandas = _RepoComandasFake(_comandaDePrueba())..errorAlCargar = const ErrorDeRed('Sin conexión');
    await _montar(tester, repoComandas, _RepoCuentasFake());

    expect(find.byKey(const Key('dividir-reintentar')), findsOneWidget);

    repoComandas.errorAlCargar = null;
    await tester.tap(find.byKey(const Key('dividir-reintentar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('dividir-anadir-cuenta')), findsOneWidget);
  });
}
