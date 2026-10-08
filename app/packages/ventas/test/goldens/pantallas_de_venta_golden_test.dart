import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

import 'golden.dart';

/// HU-128. El POS y el selector de cliente contra su imagen de referencia, en
/// las dos composiciones de `design/pantallas/` (`POS*.html`,
/// `ClienteVenta*.html`). Un padding o un color que se mueva rompe el build.
///
/// Los datos son fijos y no salen de la red: un golden que depende de lo que
/// conteste un servidor falla por el servidor.

const _productos = [
  ProductoBuscado(
      id: 'p1', sku: 'CEM-050', nombre: 'Cemento gris 50 kg', precioVenta: 32000,
      nivelStock: NivelStock.normal, impuestoPct: 19),
  ProductoBuscado(
      id: 'p2', sku: 'MART-16', nombre: 'Martillo de uña 16 oz', precioVenta: 28500,
      nivelStock: NivelStock.bajo, impuestoPct: 19),
  ProductoBuscado(
      id: 'p3', sku: 'TOR-0612', nombre: 'Tornillo drywall 6 x 1/2" (caja x100)',
      precioVenta: 9800, nivelStock: NivelStock.normal, impuestoPct: 19),
];

class _VentasFijas implements RepositorioDeVentas {
  @override
  Future<List<ProductoBuscado>> buscar(String termino) async => _productos;

  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) async =>
      const CodigoResuelto(productoId: 'p1', factor: 1);

  @override
  Future<ProductoBuscado> verProducto(String productoId) async =>
      _productos.firstWhere((p) => p.id == productoId);

  @override
  Future<num?> montoParaIdentificarComprador() async => null;

  @override
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async =>
      const ResultadoDeCobro(origenOfflineId: 'o1', venta: VentaCreada(id: 'v1', numero: 'FV-1'));
}

class _EscanerApagado implements EscanerDeCodigos {
  @override
  Future<bool> get disponible async => false;
  @override
  Future<String?> escanearUnCodigo(BuildContext context) async =>
      throw const EscaneoNoDisponible();
}

class _ClientesFijos implements RepositorioDeClientesDeVenta {
  @override
  Future<List<ClienteDeLaVenta>> buscar(String termino) async => const [
        ClienteDeLaVenta(
            id: 'c1', nombre: 'Materiales Cruz S.A.S.', tipoDocumento: 'NIT',
            numeroDocumento: '900412883', digitoVerificacion: '1', condicion: 'crédito 30 d'),
        ClienteDeLaVenta(
            id: 'c2', nombre: 'Luz Marina Peña', tipoDocumento: 'CC', numeroDocumento: '52114908'),
        ClienteDeLaVenta(
            id: 'c3', nombre: 'Ferremax del Norte', tipoDocumento: 'NIT',
            numeroDocumento: '901556019', digitoVerificacion: '4'),
      ];

  @override
  Future<void> asignar({required String ventaId, required String clienteId}) async {}

  @override
  Future<void> quitar({required String ventaId}) async {}

  @override
  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo) async => ResultadoDeAlta(id: nuevo.id);
}

Future<void> _pos(WidgetTester tester, Size size) async {
  await tamano(tester, size);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeVentasProvider.overrideWithValue(_VentasFijas()),
      bodegaDeVentaProvider.overrideWithValue('bodega-1'),
      escanerProvider.overrideWithValue(_EscanerApagado()),
    ],
    child: MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: regentaTheme(PatronOperativo.ventaDirecta),
      home: const PantallaPos(),
    ),
  ));
  await tester.pump();
  final pos = ProviderScope.containerOf(tester.element(find.byType(PantallaPos)))
      .read(controladorDelPosProvider.notifier);
  pos
    ..agregar(_productos[0])
    ..agregar(_productos[0])
    ..agregar(_productos[1])
    ..agregar(_productos[2]);
  await tester.pumpAndSettle();
}

Future<void> _selector(WidgetTester tester, Size size) async {
  await tamano(tester, size);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeClientesDeVentaProvider.overrideWithValue(_ClientesFijos()),
      ventaEnCursoProvider.overrideWithValue('venta-1'),
      permisosDeLaSesionProvider.overrideWithValue(const {'CLIENTES_CLIENTE_CREAR'}),
    ],
    child: MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: regentaTheme(PatronOperativo.ventaDirecta),
      home: const PantallaClienteDeVenta(),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testGolden('POS en móvil (390×844) coincide con su referencia', (tester) async {
    await _pos(tester, movil);
    await expectLater(find.byType(PantallaPos), matchesGoldenFile('referencias/pos_movil.png'));
  });

  testGolden('POS en escritorio (1440×900) coincide con su referencia', (tester) async {
    await _pos(tester, escritorio);
    await expectLater(find.byType(PantallaPos), matchesGoldenFile('referencias/pos_web.png'));
  });

  testGolden('Selector de cliente en móvil (390×844) coincide con su referencia', (tester) async {
    await _selector(tester, movil);
    await expectLater(find.byType(PantallaClienteDeVenta),
        matchesGoldenFile('referencias/cliente_venta_movil.png'));
  });

  testGolden('Selector de cliente en escritorio (1440×900) coincide con su referencia',
      (tester) async {
    await _selector(tester, escritorio);
    await expectLater(find.byType(PantallaClienteDeVenta),
        matchesGoldenFile('referencias/cliente_venta_web.png'));
  });
}
