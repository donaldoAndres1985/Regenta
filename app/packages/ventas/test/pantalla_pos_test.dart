import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _RepoFake implements RepositorioDeVentas {
  List<ProductoBuscado> resultados = const [];
  final List<String> terminos = [];
  final List<List<LineaParaEnviar>> ventasConfirmadas = [];

  @override
  Future<List<ProductoBuscado>> buscar(String termino) async {
    terminos.add(termino);
    return termino.trim().length >= 3 ? resultados : const [];
  }

  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) async =>
      const CodigoResuelto(productoId: 'p1', factor: 1);

  @override
  Future<ProductoBuscado> verProducto(String productoId) async => resultados.first;

  /// Cuando es true, el cobro simula que no había señal y la venta quedó en la cola.
  bool sinSenal = false;

  /// HU-137: lo que contesta Ventas en `/api/ventas/reglas-de-cobro`.
  num? montoDelNegocio;

  @override
  Future<num?> montoParaIdentificarComprador() async => montoDelNegocio;

  @override
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async {
    ventasConfirmadas.add(lineas);
    return ResultadoDeCobro(
      origenOfflineId: 'offline-1',
      venta: sinSenal ? null : const VentaCreada(id: 'v1', numero: 'FV-1'),
    );
  }
}

class _EscanerFake implements EscanerDeCodigos {
  @override
  Future<bool> get disponible async => false;
  @override
  Future<String?> escanearUnCodigo(BuildContext context) async =>
      throw const EscaneoNoDisponible();
}

ProductoBuscado _prod({
  String id = 'p1',
  String nombre = 'Cemento gris 50 kg',
  NivelStock nivel = NivelStock.normal,
  num precio = 32000,
}) =>
    ProductoBuscado(
        id: id, sku: 'CEM-050', nombre: nombre, precioVenta: precio, nivelStock: nivel);

Future<void> _montar(WidgetTester tester, _RepoFake repo,
    {Size size = const Size(390, 844),
    void Function(String)? onCobrada,
    num? montoParaIdentificar}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeVentasProvider.overrideWithValue(repo),
      bodegaDeVentaProvider.overrideWithValue('bodega-1'),
      escanerProvider.overrideWithValue(_EscanerFake()),
      if (montoParaIdentificar != null)
        montoParaIdentificarCompradorProvider.overrideWithValue(montoParaIdentificar),
    ],
    child: MaterialApp(home: PantallaPos(onVentaCobrada: onCobrada)),
  ));
  await tester.pump();
}

Future<void> _agregar(WidgetTester tester, _RepoFake repo, ProductoBuscado p) async {
  repo.resultados = [p];
  await tester.enterText(find.byType(TextField).first, 'cem');
  await tester.pump(const Duration(milliseconds: 400));
  await tester.tap(find.text(p.nombre).first);
  await tester.pump();
}

void main() {
  testWidgets('Criterio 1: en móvil el escáner y los +/- miden al menos 44 px', (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(390, 844));

    final escaner = tester.getSize(find.bySemanticsLabel('Escanear código'));
    expect(escaner.width, greaterThanOrEqualTo(44));
    expect(escaner.height, greaterThanOrEqualTo(44));

    await _agregar(tester, repo, _prod());
    final mas = tester.getSize(find.bySemanticsLabel('Agregar uno').first);
    expect(mas.width, greaterThanOrEqualTo(44));
    expect(mas.height, greaterThanOrEqualTo(44));
  });

  testWidgets('Criterio 2: en escritorio se ven la rejilla de productos y el carrito a la vez',
      (tester) async {
    await _montar(tester, _RepoFake(), size: const Size(1280, 900));

    expect(find.byKey(const Key('pos-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('pos-resultados')), findsOneWidget);
    expect(find.byKey(const Key('pos-carrito')), findsOneWidget);
    expect(find.byKey(const Key('pos-movil')), findsNothing);
  });

  testWidgets('Criterio 3: agregar un producto sin stock avisa y deshabilita Cobrar antes de confirmar',
      (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(390, 844));

    await _agregar(tester, repo, _prod(nivel: NivelStock.cero));

    expect(find.text('Hay líneas sin stock'), findsOneWidget);
    final cobrar = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Cobrar'));
    expect(cobrar.onPressed, isNull);

    // Al quitar la línea, Cobrar se rehabilita.
    await tester.tap(find.bySemanticsLabel('Quitar uno').first);
    await tester.pump();
    expect(find.text('Hay líneas sin stock'), findsNothing);
  });

  testWidgets('HU-137 criterios 2 y 4: sobre el monto y sin cliente, se avisa antes de Cobrar y no se cobra',
      (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(390, 844), montoParaIdentificar: 50000);

    await _agregar(tester, repo, _prod());
    expect(find.byKey(const Key('aviso-identificar-comprador')), findsNothing,
        reason: 'bajo el monto, nada cambia');

    await tester.tap(find.bySemanticsLabel('Agregar uno').first); // 64.000 > 50.000
    await tester.pump();

    expect(find.byKey(const Key('aviso-identificar-comprador')), findsOneWidget);
    expect(find.textContaining('identifica al comprador'), findsOneWidget);
    final aviso = tester.getTopLeft(find.byKey(const Key('aviso-identificar-comprador')));
    final boton = tester.getTopLeft(find.widgetWithText(FilledButton, 'Cobrar'));
    expect(aviso.dy, lessThan(boton.dy), reason: 'el aviso va antes del botón, no después');
    final cobrar = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Cobrar'));
    expect(cobrar.onPressed, isNull);
  });

  testWidgets('HU-137 criterio 1: el monto que configuró el negocio llega solo, desde Ventas',
      (tester) async {
    final repo = _RepoFake()..montoDelNegocio = 50000;
    await _montar(tester, repo, size: const Size(390, 844));
    await tester.pump();

    await _agregar(tester, repo, _prod(precio: 64000));

    expect(find.byKey(const Key('aviso-identificar-comprador')), findsOneWidget);
    expect(repo.ventasConfirmadas, isEmpty);
  });

  testWidgets('HU-137 criterio 2: con el comprador identificado, la misma venta se cobra',
      (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(390, 844), montoParaIdentificar: 50000);
    await _agregar(tester, repo, _prod(precio: 64000));

    final contenedor = ProviderScope.containerOf(tester.element(find.byType(PantallaPos)));
    contenedor.read(controladorDelPosProvider.notifier).fijarCliente(const ClienteDeLaVenta(
        id: 'c-1', nombre: 'Materiales Cruz S.A.S.', tipoDocumento: 'NIT',
        numeroDocumento: '900412883'));
    await tester.pump();

    expect(find.byKey(const Key('aviso-identificar-comprador')), findsNothing);
    final cobrar = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Cobrar'));
    expect(cobrar.onPressed, isNotNull);
  });

  testWidgets('HU-137 criterio 3: sin monto configurado se cobra como siempre, por grande que sea',
      (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(390, 844));
    await _agregar(tester, repo, _prod(precio: 99000000));

    expect(find.byKey(const Key('aviso-identificar-comprador')), findsNothing);
    final cobrar = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Cobrar'));
    expect(cobrar.onPressed, isNotNull);
  });

  testWidgets('Criterio 4: el mismo widget se adapta con LayoutBuilder a móvil y a escritorio',
      (tester) async {
    await _montar(tester, _RepoFake(), size: const Size(390, 844));
    expect(find.byType(LayoutBuilder), findsWidgets);
    expect(find.byKey(const Key('pos-movil')), findsOneWidget);
    expect(find.byType(PantallaPos), findsOneWidget);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pump();
    expect(find.byKey(const Key('pos-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('pos-movil')), findsNothing);
    expect(find.byType(PantallaPos), findsOneWidget);
  });

  testWidgets('Cobrar arma la venta contra el backend con las líneas del carrito', (tester) async {
    final repo = _RepoFake();
    String? cobrada;
    await _montar(tester, repo, size: const Size(390, 844), onCobrada: (n) => cobrada = n);

    await _agregar(tester, repo, _prod());
    await tester.tap(find.bySemanticsLabel('Agregar uno').first); // cantidad 2
    await tester.pump();

    await tester.tap(find.widgetWithText(FilledButton, 'Cobrar'));
    await tester.pump();
    await tester.pump();

    expect(repo.ventasConfirmadas, hasLength(1));
    expect(repo.ventasConfirmadas.single.single.cantidad, 2);
    expect(cobrada, 'FV-1');
  });

  testWidgets('HU-043 criterio 1: sin señal, cobrar avisa que la venta quedó guardada',
      (tester) async {
    final repo = _RepoFake()..sinSenal = true;
    String? cobrada;
    await _montar(tester, repo, size: const Size(390, 844), onCobrada: (n) => cobrada = n);

    await _agregar(tester, repo, _prod());
    await tester.tap(find.widgetWithText(FilledButton, 'Cobrar'));
    await tester.pump();
    await tester.pump();

    expect(repo.ventasConfirmadas, hasLength(1), reason: 'la venta se cobró igual');
    expect(cobrada, isNull, reason: 'todavía no tiene número: lo asigna el servidor al subir');
    expect(find.textContaining('sube sola cuando vuelva la conexión'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Cobrar'), findsOneWidget,
        reason: 'el carrito quedó limpio y la pantalla lista para la siguiente venta');
  });
}
