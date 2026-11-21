import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

import 'ayudas_de_ordenes.dart';

/// Catálogos para el formulario de la orden (proveedores, bodegas, productos).
class _CatalogosFake implements RepositorioDeCompraDirecta {
  @override
  Future<List<ProveedorDelNegocio>> proveedoresActivos() async =>
      const [ProveedorDelNegocio(id: 'p-1', numeroDocumento: '900', razonSocial: 'Andina SAS', nombreComercial: 'Andina')];

  @override
  Future<List<BodegaParaComprar>> bodegas() async => const [BodegaParaComprar(id: 'b-1', nombre: 'Principal', esDefault: true)];

  @override
  Future<List<ProductoEncontrado>> buscarProductos(String termino) async =>
      const [ProductoEncontrado(id: 'cem', nombre: 'Cemento gris 50 kg', sku: 'CEM-50')];

  @override
  Future<ProductoParaComprar> paraComprar(String proveedorId, String productoId) async => const ProductoParaComprar(
      productoId: 'cem', nombre: 'Cemento gris 50 kg', sku: 'CEM-50', impuestoPct: 19, costoSugerido: 30000,
      codigoProveedor: 'CV-777');

  @override
  Future<bool> tengoCajaAbierta() async => false;

  @override
  Future<CompraRegistrada> registrar(Map<String, dynamic> solicitud, {String? borradorId}) => throw UnimplementedError();
}

const _cemento = LineaDeOrdenVista(
  productoId: 'cem', nombre: 'Cemento gris 50 kg', codigoProveedor: 'CV-777', cantidadPedida: 100,
  cantidadRecibida: 60, faltante: 40, costoUnitario: 30000, impuestoPct: 19, total: 3570000,
);

Future<(RepoOrdenesFake, CompartidorFake)> _detalle(WidgetTester tester, OrdenDeCompraVista o,
    {ValueChanged<String>? onRecibir, Size tamano = const Size(1440, 1000)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = RepoOrdenesFake([o]);
  final compartidor = CompartidorFake();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeOrdenesProvider.overrideWithValue(repo),
      compartidorDePdfProvider.overrideWithValue(compartidor),
      repositorioDeCompraDirectaProvider.overrideWithValue(_CatalogosFake()),
    ],
    child: MaterialApp(home: PantallaOrdenDeCompra(ordenId: o.id, onRecibir: onRecibir)),
  ));
  await tester.pumpAndSettle();
  return (repo, compartidor);
}

void main() {
  testWidgets('HU-174 C5 · De cada línea se ve lo pedido, lo recibido y lo que falta', (tester) async {
    await _detalle(tester, orden('a', 'OC-1', 'PARCIAL', lineas: const [_cemento]));

    expect(find.text('pedido 100 · recibido 60'), findsOneWidget);
    expect(find.text('faltan 40'), findsOneWidget);
    expect(find.textContaining('cód. CV-777'), findsOneWidget);
  });

  testWidgets('HU-174 C4 · Enviar al proveedor: sale el PDF a «Compartir» y la orden pasa a ENVIADA', (tester) async {
    final (repo, compartidor) = await _detalle(tester, orden('a', 'OC-1', 'APROBADA', lineas: const [_cemento]));

    await tester.tap(find.byKey(const Key('detalle-pdf')));
    await tester.pumpAndSettle();

    expect(repo.pdfsPedidos, ['a']);
    expect(compartidor.compartidos, ['orden-de-compra-OC-1.pdf']);
    expect(repo.enviadas, ['a']);
    expect(find.text('ENVIADA'), findsOneWidget);
    expect(find.text('La orden OC-1 quedó como enviada al proveedor.'), findsOneWidget);
  });

  testWidgets('HU-174 C3 · Una orden aprobada no ofrece editar sus líneas', (tester) async {
    await _detalle(tester, orden('a', 'OC-1', 'APROBADA', lineas: const [_cemento]));

    expect(find.byKey(const Key('detalle-editar')), findsNothing);
    expect(find.byKey(const Key('detalle-confirmar')), findsNothing);
  });

  testWidgets('HU-173 · Confirmar un borrador sin exigir aprobación lo deja APROBADA', (tester) async {
    await _detalle(tester, orden('a', 'OC-1', 'BORRADOR', lineas: const [_cemento]));
    expect(find.byKey(const Key('detalle-editar')), findsOneWidget);

    await tester.tap(find.byKey(const Key('detalle-confirmar')));
    await tester.pumpAndSettle();

    expect(find.text('APROBADA'), findsOneWidget);
    expect(find.text('Orden confirmada y aprobada.'), findsOneWidget);
  });

  testWidgets('HU-173 · Si el negocio exige aprobación, queda «Por aprobar» y se ofrece Aprobar', (tester) async {
    final (repo, _) = await _detalle(tester, orden('a', 'OC-1', 'BORRADOR', lineas: const [_cemento]));
    repo.exigeAprobacion = true;

    await tester.tap(find.byKey(const Key('detalle-confirmar')));
    await tester.pumpAndSettle();
    expect(find.text('POR APROBAR'), findsOneWidget);
    expect(find.text('Orden confirmada: queda esperando aprobación.'), findsOneWidget);

    repo.falla = const SinPermiso('Sin permiso');
    await tester.tap(find.byKey(const Key('detalle-aprobar')));
    await tester.pumpAndSettle();
    expect(find.text('No tienes permiso para hacer esto con la orden.'), findsOneWidget);
  });

  testWidgets('HU-174 · «Recibir mercancía» abre la recepción de esa orden', (tester) async {
    final recibidas = <String>[];
    await _detalle(tester, orden('a', 'OC-1', 'ENVIADA', lineas: const [_cemento]), onRecibir: recibidas.add);

    await tester.tap(find.byKey(const Key('detalle-recibir')));
    expect(recibidas, ['a']);
  });

  testWidgets('HU-174 C2 · Nueva orden: el costo llega sugerido por el proveedor y el IVA es el del producto',
      (tester) async {
    await tester.binding.setSurfaceSize(const Size(1440, 1200));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final repo = RepoOrdenesFake(const []);
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeOrdenesProvider.overrideWithValue(repo),
        repositorioDeCompraDirectaProvider.overrideWithValue(_CatalogosFake()),
      ],
      child: const MaterialApp(home: FormularioDeOrden()),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('orden-proveedor')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Andina').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('orden-buscar')), 'cemento');
    await tester.tap(find.byKey(const Key('orden-buscar-boton')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cemento gris 50 kg').last);
    await tester.pumpAndSettle();

    expect(tester.widget<TextField>(find.byKey(const Key('orden-costo-cem'))).controller!.text, '30000');
    expect(find.text('IVA 19%'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('orden-cantidad-cem')), '100');
    await tester.pumpAndSettle();
    expect(tester.widget<Text>(find.byKey(const Key('orden-total'))).data, '\$3.570.000');

    await tester.tap(find.byKey(const Key('orden-guardar')));
    await tester.pumpAndSettle();
    final creada = repo.creadas.single;
    expect(creada['proveedorId'], 'p-1');
    expect(creada['bodegaDestinoId'], 'b-1');
    final linea = (creada['lineas'] as List).single as Map<String, dynamic>;
    expect(linea['productoId'], 'cem');
    expect(linea['cantidad'], 100);
    expect(linea['costoUnitario'], 30000);
    expect(linea.containsKey('impuestoPct'), isFalse, reason: 'la tarifa la decide el backend');
  });

  testWidgets('HU-174 · En el celular el detalle cabe sin desbordarse', (tester) async {
    await _detalle(tester, orden('a', 'OC-1', 'APROBADA', lineas: const [_cemento]), tamano: const Size(390, 844));

    expect(tester.takeException(), isNull);
  });
}
