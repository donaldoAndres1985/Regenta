import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// Repositorio en memoria con lo mínimo del backend de HU-167.
class _RepoFake implements RepositorioDeCompraDirecta {
  final List<Map<String, dynamic>> enviadas = [];
  final List<String?> borradores = [];
  ErrorDeApi? fallaAlCrear;
  ErrorDeApi? fallaAlConfirmar;

  static const andina = ProveedorDelNegocio(
      id: 'p-1', numeroDocumento: '900123456', razonSocial: 'Distribuidora Andina SAS', diasCredito: 30);

  final productos = <String, ProductoParaComprar>{
    'a': const ProductoParaComprar(
        productoId: 'a', nombre: 'Acetaminofén 500 mg', sku: 'ACE-500', impuestoPct: 19, costoSugerido: 10000),
    'b': const ProductoParaComprar(
        productoId: 'b', nombre: 'Ibuprofeno 400 mg', sku: 'IBU-400', impuestoPct: 19, costoSugerido: 20000),
    'l': const ProductoParaComprar(
        productoId: 'l', nombre: 'Amoxicilina 500 mg', impuestoPct: 0, tarifaIva: 'EXCLUIDO',
        manejaLotes: true, perecedero: true, costoSugerido: 900),
  };

  @override
  Future<List<ProveedorDelNegocio>> proveedoresActivos() async => const [andina];

  @override
  Future<List<BodegaParaComprar>> bodegas() async =>
      const [BodegaParaComprar(id: 'b-1', nombre: 'Principal', esDefault: true)];

  @override
  Future<List<ProductoEncontrado>> buscarProductos(String termino) async => [
        for (final p in productos.values)
          if (p.nombre.toLowerCase().contains(termino.toLowerCase()))
            ProductoEncontrado(id: p.productoId, nombre: p.nombre, sku: p.sku),
      ];

  @override
  Future<ProductoParaComprar> paraComprar(String proveedorId, String productoId) async =>
      productos[productoId]!;

  @override
  Future<CompraRegistrada> registrar(Map<String, dynamic> solicitud, {String? borradorId}) async {
    enviadas.add(solicitud);
    borradores.add(borradorId);
    if (fallaAlCrear != null) throw fallaAlCrear!;
    if (fallaAlConfirmar != null) {
      final causa = fallaAlConfirmar!;
      fallaAlConfirmar = null;
      throw ConfirmacionFallida('rec-borrador', causa);
    }
    return const CompraRegistrada(numero: 'REC-7', total: 248000);
  }
}

Future<_RepoFake> _montar(WidgetTester tester, {Size tamano = const Size(1440, 1400)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake();
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeCompraDirectaProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaCompraDirecta()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

Future<void> _elegirProveedor(WidgetTester tester) async {
  await tester.tap(find.byKey(const Key('compra-proveedor')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Distribuidora Andina SAS').last);
  await tester.pumpAndSettle();
}

Future<void> _agregar(WidgetTester tester, String busqueda, String nombre) async {
  await tester.enterText(find.byKey(const Key('compra-buscar')), busqueda);
  await tester.tap(find.byKey(const Key('compra-buscar-boton')));
  await tester.pumpAndSettle();
  await tester.tap(find.text(nombre).last);
  await tester.pumpAndSettle();
}

/// Arma la factura de referencia: A 10 × $ 10.000, B 5 × $ 20.000, IVA 19 %, flete $ 10.000.
Future<void> _facturaDeReferencia(WidgetTester tester) async {
  await _elegirProveedor(tester);
  await tester.enterText(find.byKey(const Key('compra-factura')), 'FV-100');
  await tester.enterText(find.byKey(const Key('compra-flete')), '10000');
  await _agregar(tester, 'acet', 'Acetaminofén 500 mg');
  await _agregar(tester, 'ibup', 'Ibuprofeno 400 mg');
  await tester.enterText(find.byKey(const Key('linea-cantidad-a')), '10');
  await tester.enterText(find.byKey(const Key('linea-cantidad-b')), '5');
  await tester.pumpAndSettle();
}

void main() {
  group('Estado de la compra directa', () {
    const a = ProductoParaComprar(productoId: 'a', nombre: 'A', impuestoPct: 19);

    test('HU-167 C1 · base, IVA, flete y total como en el backend', () {
      const estado = EstadoDeCompraDirecta(flete: 10000, lineas: [
        LineaEnEdicion(producto: a, cantidad: 10, costo: 10000),
        LineaEnEdicion(producto: a, cantidad: 5, costo: 20000),
      ]);
      expect(estado.base, 200000);
      expect(estado.iva, 38000);
      expect(estado.total, 248000);
    });

    test('HU-168 C4 · El descuento global rebaja la base de cada línea antes del IVA', () {
      const estado = EstadoDeCompraDirecta(flete: 10000, descuentoGlobal: 4000, lineas: [
        LineaEnEdicion(producto: a, cantidad: 10, costo: 10000),
        LineaEnEdicion(producto: a, cantidad: 5, costo: 20000),
      ]);
      expect(estado.base, 200000);
      expect(estado.iva, closeTo(37240, 0.001));
      expect(estado.total, closeTo(243240, 0.001));
    });

    test('HU-168 · Un descuento global mayor que la factura no deja registrar', () {
      const estado = EstadoDeCompraDirecta(
        proveedorId: 'p', bodegaId: 'b', factura: 'F', descuentoGlobal: 999999,
        lineas: [LineaEnEdicion(producto: a, cantidad: 1, costo: 1000)],
      );
      expect(estado.falta, 'El descuento global no puede ser mayor que el valor de la factura.');
    });

    test('HU-167 · El descuento de línea baja base e IVA en proporción', () {
      const l = LineaEnEdicion(producto: a, cantidad: 10, costo: 10000, descuentoPct: 10);
      expect(l.base, 90000);
      expect(l.iva, closeTo(17100, 0.001));
    });
  });

  testWidgets('HU-167 C1 · La factura de referencia da 248.000 pesos y se registra sin orden', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);

    expect(find.text('\$200.000'), findsWidgets);
    expect(tester.widget<Text>(find.byKey(const Key('compra-iva'))).data, '\$38.000');
    expect(tester.widget<Text>(find.byKey(const Key('compra-total'))).data, '\$248.000');

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    final enviada = repo.enviadas.single;
    expect(enviada['proveedorId'], 'p-1');
    expect(enviada['bodegaId'], 'b-1');
    expect(enviada['facturaProveedor'], 'FV-100');
    expect(enviada['flete'], 10000);
    expect(enviada.containsKey('ordenId'), isFalse);
    expect((enviada['lineas'] as List).length, 2);
    expect(find.textContaining('Compra REC-7 registrada por \$248.000'), findsOneWidget);
  });

  testWidgets('HU-167 C3 · El costo llega sugerido por el proveedor y el IVA es el del catálogo', (tester) async {
    await _montar(tester);
    await _elegirProveedor(tester);
    await _agregar(tester, 'acet', 'Acetaminofén 500 mg');

    final costo = tester.widget<TextField>(find.byKey(const Key('linea-costo-a')));
    expect(costo.controller!.text, '10000');
    expect(find.text('ACE-500 · IVA 19 %'), findsOneWidget);
    expect(find.byKey(const Key('linea-lote-a')), findsNothing,
        reason: 'un producto sin lotes no muestra los campos de lote');
  });

  testWidgets('HU-167 · Un perecedero muestra lote y vencimiento y no deja registrar sin ellos', (tester) async {
    final repo = await _montar(tester);
    await _elegirProveedor(tester);
    await tester.enterText(find.byKey(const Key('compra-factura')), 'FV-200');
    await _agregar(tester, 'amox', 'Amoxicilina 500 mg');

    expect(find.byKey(const Key('linea-lote-l')), findsOneWidget);
    expect(find.byKey(const Key('linea-vencimiento-l')), findsOneWidget);

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(find.text('«Amoxicilina 500 mg» maneja lotes: escribe el código del lote.'), findsOneWidget);
    expect(repo.enviadas, isEmpty);
  });

  testWidgets('HU-167 C6 · Si la factura ya está registrada, se ve el motivo y no se pierde lo escrito',
      (tester) async {
    final repo = await _montar(tester);
    repo.fallaAlCrear = const RecursoDuplicado('La factura FV-100 de este proveedor ya está registrada');
    await _facturaDeReferencia(tester);

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(find.text('La factura FV-100 de este proveedor ya está registrada'), findsOneWidget);
    expect(find.text('Acetaminofén 500 mg'), findsOneWidget);
    expect(tester.widget<Text>(find.byKey(const Key('compra-total'))).data, '\$248.000');
  });

  testWidgets('HU-167 · Si la confirmación falla, el reintento corrige el mismo borrador', (tester) async {
    final repo = await _montar(tester);
    repo.fallaAlConfirmar = const RecursoDuplicado('Ese proveedor está desactivado');
    await _facturaDeReferencia(tester);

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();
    expect(find.text('Ese proveedor está desactivado'), findsOneWidget);

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(repo.borradores, [null, 'rec-borrador']);
    expect(find.textContaining('Compra REC-7 registrada'), findsOneWidget);
  });

  testWidgets('HU-167 · Sin proveedor no se busca: se pide elegirlo', (tester) async {
    await _montar(tester);

    expect(find.text('Elige el proveedor para buscar los productos de la factura.'), findsOneWidget);
    final buscador = tester.widget<TextField>(find.byKey(const Key('compra-buscar')));
    expect(buscador.enabled, isFalse);
  });

  testWidgets('HU-167 · En el celular la factura cabe sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 2400));
    expect(find.byKey(const Key('compra-directa-movil')), findsOneWidget);
    await _facturaDeReferencia(tester);

    expect(tester.takeException(), isNull);
    expect(tester.widget<Text>(find.byKey(const Key('compra-total'))).data, '\$248.000');
  });

  testWidgets('HU-168 C4 · El descuento global se ve en los totales y viaja al backend', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);
    await tester.enterText(find.byKey(const Key('compra-descuento-global')), '4000');
    await tester.pumpAndSettle();

    expect(tester.widget<Text>(find.byKey(const Key('compra-descuento'))).data, '-\$4.000');
    expect(tester.widget<Text>(find.byKey(const Key('compra-iva'))).data, '\$37.240');
    expect(tester.widget<Text>(find.byKey(const Key('compra-total'))).data, '\$243.240');

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();
    expect(repo.enviadas.single['descuentoGlobal'], 4000);
  });
}
