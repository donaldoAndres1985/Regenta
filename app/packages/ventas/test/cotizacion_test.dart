import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _FakeRepoCotizaciones implements RepositorioDeVentas {
  SolicitudDeCotizacionParaEnviar? ultimaCotizacionCreada;
  String? ultimaCotizacionConvertida;
  final List<CotizacionDelNegocio> cotizaciones = [];

  @override
  Future<CotizacionDelNegocio> crearCotizacion(
    SolicitudDeCotizacionParaEnviar solicitud,
  ) async {
    ultimaCotizacionCreada = solicitud;
    return const CotizacionDelNegocio(
      id: 'cot-99',
      numero: 'COT-099',
      estado: 'ABIERTA',
      total: 30000,
    );
  }

  @override
  Future<List<CotizacionDelNegocio>> listarCotizaciones() async => cotizaciones;

  @override
  Future<CotizacionDelNegocio> verCotizacion(String cotizacionId) async {
    return cotizaciones.firstWhere(
      (c) => c.id == cotizacionId,
      orElse: () => CotizacionDelNegocio(id: cotizacionId, numero: 'COT-TEST', estado: 'ABIERTA', total: 0),
    );
  }

  @override
  Future<ResultadoDeConversion> convertirCotizacion(
    String cotizacionId,
    String bodegaId,
  ) async {
    ultimaCotizacionConvertida = cotizacionId;
    return const ResultadoDeConversion(
      ventaId: 'v-convertida-1',
      numeroVenta: 'FV-888',
      preciosDesactualizados: false,
    );
  }

  @override
  Future<VentaEnProceso> verVenta(String ventaId) async {
    return const VentaEnProceso(
      id: 'v-convertida-1',
      numero: 'FV-888',
      estado: 'BORRADOR',
      total: 30000,
      lineas: [
        LineaDeVentaResumen(
          linea: 1,
          productoId: 'p-1',
          sku: 'SKU-COT',
          nombre: 'Cemento Gris',
          cantidad: 3,
          precioUnitario: 10000,
          total: 30000,
        ),
      ],
    );
  }

  @override
  Future<List<VentaEnProceso>> listarBorradoresAbiertos(String bodegaId) async => const [];

  @override
  Future<List<ProductoBuscado>> buscar(String termino) async {
    return [
      const ProductoBuscado(
        id: 'p-1',
        sku: 'SKU-COT',
        nombre: 'Cemento Gris',
        precioVenta: 10000,
        existencia: 50,
        nivelStock: NivelStock.normal,
      ),
    ];
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  testWidgets('Guardar carrito como cotización y cargar cotización convertida',
      (tester) async {
    final repo = _FakeRepoCotizaciones();
    repo.cotizaciones.add(
      const CotizacionDelNegocio(
        id: 'cot-55',
        numero: 'COT-055',
        estado: 'ABIERTA',
        total: 30000,
        clienteNombre: 'Ferretería La Palma',
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeVentasProvider.overrideWithValue(repo),
          bodegaDeVentaProvider.overrideWithValue('bodega-central'),
        ],
        child: const MaterialApp(
          home: PantallaPos(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // 1. Agregar un producto al carrito
    final posNotifier =
        ProviderScope.containerOf(tester.element(find.byType(PantallaPos)))
            .read(controladorDelPosProvider.notifier);
    posNotifier.agregar(
      const ProductoBuscado(
        id: 'p-1',
        sku: 'SKU-COT',
        nombre: 'Cemento Gris',
        precioVenta: 10000,
        existencia: 50,
        nivelStock: NivelStock.normal,
      ),
    );
    await tester.pumpAndSettle();

    // 2. Tocar botón "Cotizar" en el pie
    await tester.tap(find.byKey(const Key('pos-boton-cotizar')));
    await tester.pumpAndSettle();

    expect(repo.ultimaCotizacionCreada, isNotNull);
    expect(repo.ultimaCotizacionCreada?.lineas.first.nombre, 'Cemento Gris');
    expect(find.text('Cotización #COT-099 creada exitosamente'), findsOneWidget);
    // Verificar que el carrito de ventas se limpia automáticamente tras cotizar
    expect(
      ProviderScope.containerOf(tester.element(find.byType(PantallaPos)))
          .read(controladorDelPosProvider)
          .lineas,
      isEmpty,
    );

    // 3. Abrir diálogo de cargar cotización desde el AppBar
    await tester.tap(find.byKey(const Key('pos-boton-cargar-cotizacion')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('dialogo-cargar-cotizacion')), findsOneWidget);
    expect(find.byKey(const Key('campo-buscar-cotizacion')), findsOneWidget);
    expect(find.text('Cotización #COT-055'), findsOneWidget);
    expect(find.text('Ferretería La Palma'), findsOneWidget);

    // Probar filtro de búsqueda
    await tester.enterText(find.byKey(const Key('campo-buscar-cotizacion')), 'Palma');
    await tester.pumpAndSettle();
    expect(find.text('Cotización #COT-055'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('campo-buscar-cotizacion')), 'Inexistente');
    await tester.pumpAndSettle();
    expect(find.text('No se encontraron cotizaciones con ese filtro'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('campo-buscar-cotizacion')), '');
    await tester.pumpAndSettle();
    expect(find.text('Cotización #COT-055'), findsOneWidget);

    // 4. Convertir cotización
    await tester.tap(find.byKey(const Key('boton-convertir-cotizacion-cot-55')));
    await tester.pumpAndSettle();

    expect(repo.ultimaCotizacionConvertida, 'cot-55');
    expect(find.text('Cotización #COT-055 convertida en venta #FV-888'), findsOneWidget);
  });
}
