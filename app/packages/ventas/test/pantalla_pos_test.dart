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

  @override
  Future<VentaCreada> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async {
    ventasConfirmadas.add(lineas);
    return const VentaCreada(id: 'v1', numero: 'FV-1');
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
    {Size size = const Size(390, 844), void Function(String)? onCobrada}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeVentasProvider.overrideWithValue(repo),
      bodegaDeVentaProvider.overrideWithValue('bodega-1'),
      escanerProvider.overrideWithValue(_EscanerFake()),
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
}
