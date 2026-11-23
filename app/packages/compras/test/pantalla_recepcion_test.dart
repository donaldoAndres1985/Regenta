import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeRecepciones {
  _RepoFake({this.sinRed = false});

  bool sinRed;
  final List<List<LineaParaRecibir>> registradas = [];
  final List<String?> ids = [];
  ErrorDeApi? rechazo;
  final List<FacturaDeRecepcion?> facturas = [];
  final List<Map<String, num>> preciosEnviados = [];
  Map<String, PrecioActual> precios = const {};

  @override
  Future<Map<String, PrecioActual>> preciosActuales(String bodegaId, Iterable<String> productoIds) async => precios;

  OrdenRecibible orden = OrdenRecibible(
    id: 'oc-1',
    numero: 'OC-0231',
    proveedorId: 'prov-1',
    bodegaId: 'bod-1',
    estado: 'APROBADA',
    lineas: const [
      LineaRecibible(
        id: 'l1',
        linea: 1,
        productoId: 'p-ace',
        nombre: 'Acetaminofén 500 mg x100',
        cantidadPedida: 120,
        cantidadRecibida: 0,
        costoUnitario: 8400,
        codigo: 'ACE-500',
        exigeLote: true,
      ),
      LineaRecibible(
        id: 'l2',
        linea: 2,
        productoId: 'p-gas',
        nombre: 'Gasa estéril 10x10',
        cantidadPedida: 200,
        cantidadRecibida: 0,
        costoUnitario: 1150,
        codigo: 'GAS-01',
        exigeLote: false,
      ),
    ],
  );

  @override
  Future<List<OrdenResumenVista>> listarOrdenes() async => const [];

  @override
  Future<OrdenRecibible> verOrden(String ordenId) async => orden;

  @override
  Future<ResultadoDeRecepcion> registrar({
    required String ordenId,
    required String bodegaId,
    String? facturaProveedor,
    FacturaDeRecepcion? factura,
    required List<LineaParaRecibir> lineas,
    Map<String, num> preciosNuevos = const {},
    String? idDispositivo,
    String? ordenNumero,
  }) async {
    ids.add(idDispositivo);
    if (rechazo != null) {
      final r = rechazo!;
      rechazo = null;
      throw r;
    }
    registradas.add(lineas);
    preciosEnviados.add(preciosNuevos);
    facturas.add(factura);
    if (sinRed) return ResultadoDeRecepcion.encolada(idDispositivo: idDispositivo);
    return const ResultadoDeRecepcion.confirmada('REC-1');
  }
}

class _EscanerFake implements EscanerDeCodigos {
  @override
  Future<bool> get disponible async => false;
  @override
  Future<String?> escanearUnCodigo(BuildContext context) async =>
      throw const EscaneoNoDisponible();
}

Future<void> _montar(
  WidgetTester tester,
  _RepoFake repo, {
  Size size = const Size(390, 900),
  void Function(String)? onRegistrada,
  bool puedeCambiarPrecios = false,
  RecepcionPorEnviar? correccion,
}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeRecepcionesProvider.overrideWithValue(repo),
      escanerDeRecepcionProvider.overrideWithValue(_EscanerFake()),
      puedeCambiarPreciosProvider.overrideWithValue(puedeCambiarPrecios),
    ],
    child: MaterialApp(
      home: PantallaRecepcion(ordenId: 'oc-1', onRegistrada: onRegistrada, correccion: correccion),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterio 1: al abrir la orden se ven las líneas con lo pedido y un campo grande para lo recibido',
      (tester) async {
    await _montar(tester, _RepoFake());

    expect(find.byKey(const Key('recepcion-movil')), findsOneWidget);
    expect(find.text('Acetaminofén 500 mg x100'), findsOneWidget);
    expect(find.textContaining('ped.'), findsWidgets);

    final campo = tester.getSize(find.byKey(const Key('campo-recibido')));
    expect(campo.height, greaterThanOrEqualTo(44));
  });

  testWidgets('Criterio 2: los campos de lote y vencimiento solo aparecen en la línea que los exige',
      (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo);

    // Línea 1 (Acetaminofén) exige lote.
    expect(find.byKey(const Key('campo-lote')), findsOneWidget);
    expect(find.byKey(const Key('campo-vence')), findsOneWidget);

    // Paso a la línea 2 (Gasa), que no maneja lotes.
    await tester.tap(find.bySemanticsLabel('Línea siguiente'));
    await tester.pumpAndSettle();
    expect(find.text('Gasa estéril 10x10'), findsOneWidget);
    expect(find.byKey(const Key('campo-lote')), findsNothing);
    expect(find.byKey(const Key('campo-vence')), findsNothing);
  });

  testWidgets('Criterio 3: escanear el código de un producto salta a esa línea',
      (tester) async {
    await _montar(tester, _RepoFake());
    // Arranca en la línea 1.
    expect(find.text('Acetaminofén 500 mg x100'), findsOneWidget);

    await tester.tap(find.bySemanticsLabel('Escanear código'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'GAS-01');
    await tester.tap(find.widgetWithText(FilledButton, 'Ir'));
    await tester.pumpAndSettle();

    expect(find.text('Gasa estéril 10x10'), findsOneWidget);
    expect(find.text('Acetaminofén 500 mg x100'), findsNothing);
  });

  testWidgets('Criterio 3: un código que no está en la orden avisa', (tester) async {
    await _montar(tester, _RepoFake());
    await tester.tap(find.bySemanticsLabel('Escanear código'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, 'NO-EXISTE');
    await tester.tap(find.widgetWithText(FilledButton, 'Ir'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Ningún renglón tiene el código'), findsOneWidget);
  });

  testWidgets('Criterio 4: sin señal, la recepción se guarda local y sube después',
      (tester) async {
    final repo = _RepoFake(sinRed: true);
    await _montar(tester, repo);

    await tester.enterText(find.byKey(const Key('campo-recibido')), '120');
    await tester.enterText(find.byKey(const Key('campo-lote')), 'L-2411');
    await tester.pump();
    // Elegir vencimiento.
    await tester.tap(find.byKey(const Key('campo-vence')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
    await tester.pumpAndSettle();

    expect(repo.registradas, hasLength(1));
    expect(find.textContaining('se guardó y subirá'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Guardada sin señal'), findsOneWidget);
  });

  testWidgets('Confirmar en línea manda al backend lo recibido y el lote', (tester) async {
    final repo = _RepoFake();
    String? numero;
    await _montar(tester, repo, onRegistrada: (n) => numero = n);

    await tester.enterText(find.byKey(const Key('campo-recibido')), '100');
    await tester.enterText(find.byKey(const Key('campo-lote')), 'L-2411');
    await tester.pump();
    await tester.tap(find.byKey(const Key('campo-vence')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
    await tester.pumpAndSettle();

    expect(repo.registradas, hasLength(1));
    final linea = repo.registradas.single.single;
    expect(linea.ordenLineaId, 'l1');
    expect(linea.cantidad, 100);
    expect(linea.codigoLote, 'L-2411');
    expect(linea.fechaVencimiento, isNotNull);
    expect(numero, 'REC-1');
  });

  testWidgets('El mismo widget se adapta: móvil una línea, escritorio la tabla',
      (tester) async {
    await _montar(tester, _RepoFake(), size: const Size(390, 900));
    expect(find.byType(LayoutBuilder), findsWidgets);
    expect(find.byKey(const Key('recepcion-movil')), findsOneWidget);
    expect(find.byKey(const Key('recepcion-escritorio')), findsNothing);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('recepcion-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('recepcion-movil')), findsNothing);
    expect(find.byType(PantallaRecepcion), findsOneWidget);
  });

  testWidgets('Recibir más de lo pedido + 5% no deja confirmar y avisa', (tester) async {
    await _montar(tester, _RepoFake());

    await tester.enterText(find.byKey(const Key('campo-recibido')), '200');
    await tester.pump();

    expect(find.textContaining('más de lo pedido'), findsOneWidget);
    final boton = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Confirmar recepción'));
    expect(boton.onPressed, isNull);
  });

  group('HU-175 · Recibir contra orden desde la web', () {
    _RepoFake soloGasa() => _RepoFake()
      ..orden = const OrdenRecibible(
        id: 'oc-1',
        numero: 'OC-0231',
        proveedorId: 'prov-1',
        bodegaId: 'bod-1',
        estado: 'ENVIADA',
        lineas: [
          LineaRecibible(
            id: 'l2', linea: 1, productoId: 'p-gas', nombre: 'Gasa estéril 10x10', cantidadPedida: 200,
            cantidadRecibida: 0, costoUnitario: 1150, codigo: 'GAS-01', impuestoPct: 19,
          ),
        ],
      );

    testWidgets('C4 · La factura se registra como en la compra directa: número, flete y de contado desde la caja',
        (tester) async {
      final repo = soloGasa();
      await _montar(tester, repo, size: const Size(1280, 1000));

      await tester.enterText(find.byKey(const Key('recepcion-numero-factura')), 'FV-100');
      await tester.enterText(find.byKey(const Key('recepcion-flete')), '10000');
      await tester.enterText(find.byKey(const Key('campo-recibido')), '200');
      await tester.tap(find.text('De contado'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('recepcion-desde-la-caja')));
      await tester.pumpAndSettle();

      expect(tester.widget<Text>(find.byKey(const Key('recepcion-subtotal'))).data, '\$230.000');
      expect(tester.widget<Text>(find.byKey(const Key('recepcion-iva'))).data, '\$43.700');
      expect(tester.widget<Text>(find.byKey(const Key('recepcion-total'))).data, '\$283.700');

      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.facturas.single!.aJson(), {
        'facturaProveedor': 'FV-100',
        'flete': 10000,
        'condicionPago': 'CONTADO',
        'metodoPago': 'EFECTIVO',
        'pagarDesdeLaCaja': true,
      });
    });

    testWidgets('C4 · A crédito y sin número (llegó con remisión) se puede confirmar', (tester) async {
      final repo = soloGasa();
      await _montar(tester, repo, size: const Size(1280, 1000));

      await tester.enterText(find.byKey(const Key('campo-recibido')), '60');
      await tester.pump();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.facturas.single!.aJson(), {'condicionPago': 'CREDITO'});
      expect(repo.registradas.single.single.cantidad, 60);
    });

    testWidgets('C4 · De contado sin número de factura no deja confirmar y lo dice', (tester) async {
      await _montar(tester, soloGasa(), size: const Size(1280, 1000));

      await tester.enterText(find.byKey(const Key('campo-recibido')), '60');
      await tester.tap(find.text('De contado'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('recepcion-factura-problema')), findsOneWidget);
      final boton = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      expect(boton.onPressed, isNull);
    });

    testWidgets('En el celular la factura no ocupa la pantalla: se recibe línea por línea', (tester) async {
      await _montar(tester, soloGasa());

      expect(find.byKey(const Key('recepcion-factura')), findsNothing);
      expect(tester.takeException(), isNull);
    });
  });

  group('HU-179 · Precio de venta al recibir', () {
    // A 10 × $10.000 y B 5 × $20.000 con flete $10.000: A entra a $10.500.
    _RepoFake referencia() => _RepoFake()
      ..orden = const OrdenRecibible(
        id: 'oc-1',
        numero: 'OC-1',
        proveedorId: 'prov-1',
        bodegaId: 'bod-1',
        estado: 'ENVIADA',
        lineas: [
          LineaRecibible(id: 'la', linea: 1, productoId: 'a', nombre: 'Acetaminofén', cantidadPedida: 10,
              cantidadRecibida: 0, costoUnitario: 10000, impuestoPct: 19),
          LineaRecibible(id: 'lb', linea: 2, productoId: 'b', nombre: 'Ibuprofeno', cantidadPedida: 5,
              cantidadRecibida: 0, costoUnitario: 20000, impuestoPct: 19),
        ],
      )
      ..precios = const {
        'a': PrecioActual(productoId: 'a', costoPromedio: 9000, precio: 14280, base: 12000),
      };

    Future<_RepoFake> recibirTodo(WidgetTester tester, {bool puede = true}) async {
      final repo = referencia();
      await _montar(tester, repo, size: const Size(1280, 1400), puedeCambiarPrecios: puede);
      await tester.enterText(find.byKey(const Key('campo-recibido')).first, '10');
      await tester.enterText(find.byKey(const Key('campo-recibido')).last, '5');
      await tester.enterText(find.byKey(const Key('recepcion-flete')), '10000');
      await tester.pumpAndSettle();
      return repo;
    }

    testWidgets('C1 · Costo 9.000 → 10.500 y margen 25,0 % → 12,5 % sobre la base', (tester) async {
      await recibirTodo(tester);

      expect(tester.widget<Text>(find.byKey(const Key('margen-costo-a'))).data, 'Costo \$9.000 → \$10.500');
      expect(tester.widget<Text>(find.byKey(const Key('margen-valor-a'))).data, 'Margen 25,0 % → 12,5 %');
      expect(find.byKey(const Key('margen-b')), findsNothing, reason: 'sin precio conocido no hay margen');
    });

    testWidgets('C2 · Con precio nuevo 15.470, se envía al confirmar', (tester) async {
      final repo = await recibirTodo(tester);

      await tester.enterText(find.byKey(const Key('precio-nuevo-a')), '15470');
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.preciosEnviados.single, {'a': 15470});
    });

    testWidgets('C3 · Sin precio nuevo no se envía ninguno', (tester) async {
      final repo = await recibirTodo(tester);

      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.preciosEnviados.single, isEmpty);
    });

    testWidgets('C4 · Sin INVENTARIO_PRODUCTO_EDITAR se ve el margen pero no el campo de precio', (tester) async {
      await recibirTodo(tester, puede: false);

      expect(find.byKey(const Key('margen-valor-a')), findsOneWidget);
      expect(find.byKey(const Key('precio-nuevo-a')), findsNothing);
    });

    test('C1 · El margen se mide sobre la base sin IVA', () {
      expect(formatearMargen(margenSobreBase(12000, 9000)), '25,0 %');
      expect(formatearMargen(margenSobreBase(12000, 10500)), '12,5 %');
      expect(margenSobreBase(0, 1), isNull);
    });
  });

  group('HU-182 · Recepción en el celular sin conexión', () {
    Future<void> capturarYConfirmar(WidgetTester tester) async {
      await tester.enterText(find.byKey(const Key('campo-recibido')), '120');
      await tester.enterText(find.byKey(const Key('campo-lote')), 'L-2411');
      await tester.pump();
      await tester.tap(find.byKey(const Key('campo-vence')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('OK'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();
    }

    testWidgets('C1 · Sin señal queda guardada con un identificador del celular y «por enviar»', (tester) async {
      final repo = _RepoFake(sinRed: true);
      await _montar(tester, repo);

      await capturarYConfirmar(tester);

      expect(repo.ids.single, matches(RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$')));
      expect(find.textContaining('«por enviar»'), findsOneWidget);
    });

    testWidgets('C2 · Si se confirma otra vez tras un rechazo, va con el mismo identificador', (tester) async {
      final repo = _RepoFake()..rechazo = const ErroresDeValidacion({}, 'La orden ya no se puede recibir');
      await _montar(tester, repo);

      await capturarYConfirmar(tester);
      expect(find.text('La orden ya no se puede recibir'), findsOneWidget);
      await tester.pump(const Duration(seconds: 5)); // que se vaya el aviso de abajo
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.ids, hasLength(2));
      expect(repo.ids.toSet(), hasLength(1));
      expect(repo.registradas, hasLength(1));
    });

    testWidgets('C3 · Al corregir vuelve lo anotado y el motivo, y se reenvía con el mismo identificador',
        (tester) async {
      final repo = _RepoFake();
      String? numero;
      final rechazada = RecepcionPorEnviar(
        id: 'id-del-celular',
        ordenId: 'oc-1',
        ordenNumero: 'OC-0231',
        guardadaEn: DateTime(2026, 11, 22, 10, 42),
        conError: true,
        error: 'Este producto maneja lotes: indica el código de lote que llegó',
        datos: const {
          'recepcion': {
            'ordenId': 'oc-1',
            'bodegaId': 'bod-1',
            'facturaProveedor': 'FV-88',
            'condicionPago': 'CREDITO',
            'lineas': [
              {'ordenLineaId': 'l1', 'cantidad': 120, 'exigeLote': true, 'fechaVencimiento': '2027-06-30'},
            ],
          },
        },
      );
      await _montar(tester, repo, correccion: rechazada, onRegistrada: (n) => numero = n);

      expect(find.byKey(const Key('recepcion-error-previo')), findsOneWidget);
      expect(find.textContaining('maneja lotes'), findsOneWidget);
      expect(find.widgetWithText(TextField, '120'), findsOneWidget);

      await tester.enterText(find.byKey(const Key('campo-lote')), 'L-2411');
      await tester.pump();
      await tester.tap(find.widgetWithText(FilledButton, 'Confirmar recepción'));
      await tester.pumpAndSettle();

      expect(repo.ids.single, 'id-del-celular');
      final linea = repo.registradas.single.single;
      expect(linea.cantidad, 120);
      expect(linea.codigoLote, 'L-2411');
      expect(linea.fechaVencimiento, DateTime(2027, 6, 30));
      expect(repo.facturas.single?.numero, 'FV-88');
      expect(numero, 'REC-1');
    });
  });
}
