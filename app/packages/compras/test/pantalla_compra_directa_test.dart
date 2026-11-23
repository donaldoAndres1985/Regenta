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
  bool cajaAbierta = true;
  List<String> avisos = const [];

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
  Future<bool> tengoCajaAbierta() async => cajaAbierta;

  final List<Map<String, dynamic>> creados = [];
  ErrorDeApi? fallaAlCrearProducto;

  @override
  Future<List<OpcionDeCatalogo>> categorias() async => const [OpcionDeCatalogo('cat-1', 'Droguería')];

  @override
  Future<List<OpcionDeCatalogo>> unidades() async => const [OpcionDeCatalogo('und', 'Unidad')];

  @override
  Future<ProductoParaComprar> crearProducto(String proveedorId, Map<String, dynamic> datos) async {
    creados.add(datos);
    if (fallaAlCrearProducto != null) throw fallaAlCrearProducto!;
    final p = ProductoParaComprar(productoId: 'nuevo', nombre: datos['nombre'] as String, sku: datos['sku'] as String,
        impuestoPct: 19, costoSugerido: datos['costo'] as num?, codigoProveedor: datos['codigoProveedor'] as String?);
    productos['nuevo'] = p;
    return p;
  }

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
    return CompraRegistrada(numero: 'REC-7', total: 248000, avisos: avisos);
  }
}

Future<_RepoFake> _montar(WidgetTester tester,
    {Size tamano = const Size(1440, 1600), bool cajaAbierta = true, bool puedeCrear = true}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake()..cajaAbierta = cajaAbierta;
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeCompraDirectaProvider.overrideWithValue(repo),
      puedeCrearProductosProvider.overrideWithValue(puedeCrear),
    ],
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

  // ------------------------------------------------------------------ HU-169 · contado o crédito

  Future<void> deContado(WidgetTester tester) async {
    await tester.tap(find.text('De contado'));
    await tester.pumpAndSettle();
  }

  testWidgets('HU-169 C1 · Con un proveedor de 30 días la compra va a crédito y dice cuándo vence', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);

    expect(find.text('Nace una cuenta por pagar. Vence a 30 días de la factura.'), findsOneWidget);
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    final enviada = repo.enviadas.single;
    expect(enviada['condicionPago'], 'CREDITO');
    expect(enviada.containsKey('metodoPago'), isFalse);
  });

  testWidgets('HU-169 C2 / HU-145 C8 · De contado en efectivo con la caja abierta: «Pagar con dinero de la caja»',
      (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);
    await deContado(tester);

    final casilla = tester.widget<CheckboxListTile>(find.byKey(const Key('compra-desde-la-caja')));
    expect(casilla.value, isTrue, reason: 'con la caja abierta, el efectivo sale de ella por omisión');
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    final enviada = repo.enviadas.single;
    expect(enviada['condicionPago'], 'CONTADO');
    expect(enviada['metodoPago'], 'EFECTIVO');
    expect(enviada['pagarDesdeLaCaja'], isTrue);
  });

  testWidgets('HU-169 C3 · De contado por transferencia: sin casilla de caja y con referencia', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);
    await deContado(tester);
    await tester.tap(find.byKey(const Key('compra-metodo')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Transferencia').last);
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('compra-desde-la-caja')), findsNothing);
    await tester.enterText(find.byKey(const Key('compra-referencia-pago')), 'TRX-55');
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    final enviada = repo.enviadas.single;
    expect(enviada['metodoPago'], 'TRANSFERENCIA');
    expect(enviada['pagarDesdeLaCaja'], isFalse);
    expect(enviada['referenciaPago'], 'TRX-55');
  });

  testWidgets('HU-145 C3 · Sin caja abierta, el efectivo de contado se registra como pagado por fuera', (tester) async {
    final repo = await _montar(tester, cajaAbierta: false);
    await _facturaDeReferencia(tester);
    await deContado(tester);

    final casilla = tester.widget<CheckboxListTile>(find.byKey(const Key('compra-desde-la-caja')));
    expect(casilla.onChanged, isNull);
    expect(find.textContaining('No tienes una caja abierta'), findsOneWidget);
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(repo.enviadas.single['pagarDesdeLaCaja'], isFalse);
  });

  testWidgets('HU-169 C4 · Si la compra supera el cupo, se avisa y queda registrada', (tester) async {
    final repo = await _montar(tester);
    repo.avisos = const ['Esta compra supera el cupo del proveedor.'];
    await _facturaDeReferencia(tester);

    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('Compra REC-7 registrada'), findsOneWidget);
    expect(find.textContaining('Esta compra supera el cupo del proveedor.'), findsOneWidget);
  });

  testWidgets('HU-169 · En el celular la sección de pago cabe sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 2800));
    await _facturaDeReferencia(tester);
    await deContado(tester);

    expect(find.byKey(const Key('compra-desde-la-caja')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  // ------------------------------------------------------------------ HU-170 · remisión

  testWidgets('HU-170 C1 · A crédito sin número de factura se registra: llegó con remisión', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);
    await tester.enterText(find.byKey(const Key('compra-factura')), '');
    await tester.pumpAndSettle();

    expect(find.text('¿Llegó con remisión? Déjalo vacío'), findsOneWidget);
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(repo.enviadas.single.containsKey('facturaProveedor'), isFalse);
    expect(repo.enviadas.single['condicionPago'], 'CREDITO');
  });

  testWidgets('HU-170 C2 · De contado sin número de factura no se deja registrar', (tester) async {
    final repo = await _montar(tester);
    await _facturaDeReferencia(tester);
    await tester.enterText(find.byKey(const Key('compra-factura')), '');
    await tester.tap(find.text('De contado'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('compra-registrar')));
    await tester.pumpAndSettle();

    expect(find.text('Una compra de contado necesita el número de la factura del proveedor.'), findsOneWidget);
    expect(repo.enviadas, isEmpty);
  });

  group('HU-180 · Crear un producto desde la recepción', () {
    Future<_RepoFake> buscarCodigoDesconocido(WidgetTester tester, {bool puedeCrear = true}) async {
      final repo = await _montar(tester, puedeCrear: puedeCrear);
      await tester.tap(find.byKey(const Key('compra-proveedor')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Distribuidora Andina SAS').last);
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('compra-buscar')), '7701234567890');
      await tester.tap(find.byKey(const Key('compra-buscar-boton')));
      await tester.pumpAndSettle();
      return repo;
    }

    testWidgets('C1 · Un código que no existe ofrece «Crear producto» con el código de barras ya puesto',
        (tester) async {
      await buscarCodigoDesconocido(tester);

      await tester.tap(find.byKey(const Key('compra-crear-producto')));
      await tester.pumpAndSettle();

      expect(tester.widget<TextField>(find.byKey(const Key('nuevo-codigo'))).controller!.text, '7701234567890');
      expect(tester.widget<TextField>(find.byKey(const Key('nuevo-nombre'))).controller!.text, isEmpty);
    });

    testWidgets('C3 · Creado, queda en la factura con su costo y su código del proveedor', (tester) async {
      final repo = await buscarCodigoDesconocido(tester);
      await tester.tap(find.byKey(const Key('compra-crear-producto')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('nuevo-sku')), 'GUA-01');
      await tester.enterText(find.byKey(const Key('nuevo-nombre')), 'Guantes de nitrilo');
      await tester.tap(find.byKey(const Key('nuevo-categoria')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Droguería').last);
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('nuevo-tarifa')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('IVA 19 %').last);
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('nuevo-precio')), '11900');
      await tester.enterText(find.byKey(const Key('nuevo-costo')), '7000');
      await tester.enterText(find.byKey(const Key('nuevo-codigo-proveedor')), 'AND-55');
      await tester.tap(find.byKey(const Key('nuevo-guardar')));
      await tester.pumpAndSettle();

      final enviado = repo.creados.single;
      expect(enviado['sku'], 'GUA-01');
      expect(enviado['codigoBarras'], '7701234567890');
      expect(enviado['categoriaId'], 'cat-1');
      expect(enviado['unidadMedidaId'], 'und', reason: 'con una sola unidad queda elegida');
      expect(enviado['tarifaIva'], 'IVA_19');
      expect(enviado['precioVenta'], 11900);
      expect(find.text('Guantes de nitrilo'), findsOneWidget);
      expect(find.byKey(const Key('compra-crear-producto')), findsNothing);
    });

    testWidgets('C2 · Sin SKU o sin tarifa, se ven los mismos mensajes de Inventario', (tester) async {
      final repo = await buscarCodigoDesconocido(tester);
      repo.fallaAlCrearProducto = const ErroresDeValidacion({'sku': ['no debe estar vacío']}, 'Datos inválidos');
      await tester.tap(find.byKey(const Key('compra-crear-producto')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('nuevo-guardar')));
      await tester.pumpAndSettle();
      expect(find.text('sku: no debe estar vacío'), findsOneWidget);

      repo.fallaAlCrearProducto = const ErroresDeValidacion({}, 'Elige la tarifa de IVA del producto.');
      await tester.tap(find.byKey(const Key('nuevo-guardar')));
      await tester.pumpAndSettle();
      expect(find.text('Elige la tarifa de IVA del producto.'), findsOneWidget);
      expect(find.byKey(const Key('nuevo-guardar')), findsOneWidget, reason: 'el formulario sigue abierto');
    });

    testWidgets('C4 · Sin INVENTARIO_PRODUCTO_CREAR se explica que lo cree un administrador', (tester) async {
      await buscarCodigoDesconocido(tester, puedeCrear: false);

      expect(find.byKey(const Key('compra-crear-producto')), findsNothing);
      expect(find.text('Este producto no existe. Pide a un administrador que lo cree.'), findsOneWidget);
    });

    test('Un término de solo dígitos (8 o más) es un código de barras', () {
      expect(pareceCodigoDeBarras('7701234567890'), isTrue);
      expect(pareceCodigoDeBarras('guantes'), isFalse);
      expect(pareceCodigoDeBarras('1234'), isFalse);
    });
  });
}
