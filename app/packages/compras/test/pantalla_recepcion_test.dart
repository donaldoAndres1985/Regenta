import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';

class _RepoFake implements RepositorioDeRecepciones {
  _RepoFake({this.sinRed = false});

  bool sinRed;
  final List<List<LineaParaRecibir>> registradas = [];

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
  Future<OrdenRecibible> verOrden(String ordenId) async => orden;

  @override
  Future<ResultadoDeRecepcion> registrar({
    required String ordenId,
    required String bodegaId,
    String? facturaProveedor,
    required List<LineaParaRecibir> lineas,
  }) async {
    registradas.add(lineas);
    if (sinRed) return const ResultadoDeRecepcion.encolada();
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
}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeRecepcionesProvider.overrideWithValue(repo),
      escanerDeRecepcionProvider.overrideWithValue(_EscanerFake()),
    ],
    child: MaterialApp(
      home: PantallaRecepcion(ordenId: 'oc-1', onRegistrada: onRegistrada),
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
}
