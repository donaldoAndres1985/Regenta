import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-132 · R7 en el carrito del POS. Catálogo: martillo $ 10.000 y tornillo $ 2.000. La lista
/// «Mayorista» deja el martillo en $ 8.500 y el tornillo en $ 1.600, o $ 1.400 desde 100.
const _martillo = ProductoBuscado(
    id: 'p-martillo', sku: 'MAR', nombre: 'Martillo', precioVenta: 10000, nivelStock: NivelStock.normal, impuestoPct: 19);
const _tornillo = ProductoBuscado(
    id: 'p-tornillo', sku: 'TOR', nombre: 'Tornillo', precioVenta: 2000, nivelStock: NivelStock.normal, impuestoPct: 19);
const _mayorista = ClienteDeLaVenta(
    id: 'c-mayorista', nombre: 'Ferretería El Tornillo', tipoDocumento: 'NIT', numeroDocumento: '900111222');

class _Ventas implements RepositorioDeVentas {
  String? listaCobrada = 'sin cobrar';

  @override
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
    String? listaPreciosId,
  }) async {
    listaCobrada = listaPreciosId;
    return const ResultadoDeCobro(origenOfflineId: 'o-1');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Precios implements RepositorioDePreciosDeVenta {
  final List<({String? clienteId, String? listaPreciosId, List<LineaAPreciar> lineas})> previstas = [];
  final List<(String, String, num)> preciadas = [];
  PrevisionDePreciosVista respuesta = const PrevisionDePreciosVista();
  ErrorDeApi? falla;

  @override
  Future<PrevisionDePreciosVista> prever({
    String? clienteId,
    String? listaPreciosId,
    required List<LineaAPreciar> lineas,
  }) async {
    if (falla != null) throw falla!;
    previstas.add((clienteId: clienteId, listaPreciosId: listaPreciosId, lineas: lineas));
    return respuesta;
  }

  @override
  Future<num> precio({required String listaPreciosId, required String productoId, required num cantidad}) async {
    preciadas.add((listaPreciosId, productoId, cantidad));
    if (productoId == _martillo.id) return 8500;
    return cantidad >= 100 ? 1400 : 1600;
  }
}

PrevisionDePreciosVista _alMayorista() => const PrevisionDePreciosVista(
      listaNuevaId: 'l-mayorista',
      listaNuevaNombre: 'Mayorista',
      cambios: [CambioDePrecioVista(linea: 0, producto: 'Martillo', cantidad: 2, precioActual: 10000, precioNuevo: 8500)],
      conDescuentoAMano: 1,
      totalActual: 29000,
      totalNuevo: 26000,
    );

void main() {
  late _Ventas ventas;
  late _Precios precios;
  late ControladorDelPos pos;

  setUp(() {
    ventas = _Ventas();
    precios = _Precios();
    pos = ControladorDelPos(ventas, bodegaId: 'b-1', precios: precios, rebote: Duration.zero);
  });
  tearDown(() => pos.dispose());

  /// Martillo ×2 y tornillo ×1 con 10 % a mano.
  void conCarrito() {
    pos.agregar(_martillo);
    pos.cambiarCantidad(0, 1);
    pos.agregar(_tornillo);
    pos.aplicarDescuentoLinea(1, 10);
  }

  test('C2 · Antes de asignar se pregunta con las líneas del carrito: su precio y su descuento a mano', () async {
    conCarrito();
    precios.respuesta = _alMayorista();

    final p = await pos.preverCliente(_mayorista);

    final pedida = precios.previstas.single;
    expect(pedida.clienteId, 'c-mayorista');
    expect(pedida.listaPreciosId, isNull);
    expect(pedida.lineas.map((l) => (l.linea, l.productoId, l.cantidad, l.precioUnitario, l.descuentoPct)),
        [(0, 'p-martillo', 2, 10000, 0), (1, 'p-tornillo', 1, 2000, 10)]);
    expect(p!.hayCambios, isTrue);
    expect(p.cambiaLaLista, isTrue);
    // Prever no cambia nada.
    expect(pos.state.lineas[0].precioUnitario, 10000);
    expect(pos.state.listaPreciosId, isNull);
  });

  test('C1 / C3 · Aplicar: el carrito pasa a la lista y cambian solo las líneas previstas', () async {
    conCarrito();
    pos.fijarCliente(_mayorista);

    pos.aplicarPrevision(_alMayorista());

    expect(pos.state.listaPreciosId, 'l-mayorista');
    expect(pos.state.listaPreciosNombre, 'Mayorista');
    expect(pos.state.lineas[0].precioUnitario, 8500);
    expect(pos.state.lineas[1].precioUnitario, 2000);
    expect(pos.state.lineas[1].descuentoPct, 10);
    expect(pos.state.total, 17000 + 1800);
  });

  test('Con la lista puesta, lo que se agrega o cambia de cantidad toma el precio de la lista (y su tramo)',
      () async {
    pos.fijarCliente(_mayorista);
    pos.aplicarPrevision(const PrevisionDePreciosVista(listaNuevaId: 'l-mayorista', listaNuevaNombre: 'Mayorista'));

    pos.agregar(_tornillo);
    await Future<void>.delayed(Duration.zero);
    expect(pos.state.lineas.single.precioUnitario, 1600);

    pos.fijarCantidad(0, 100);
    await Future<void>.delayed(Duration.zero);
    expect(pos.state.lineas.single.precioUnitario, 1400);
    expect(precios.preciadas.last, ('l-mayorista', 'p-tornillo', 100));
  });

  test('Sin lista no se pregunta precio a nadie: queda el del catálogo', () async {
    pos.agregar(_tornillo);
    await Future<void>.delayed(Duration.zero);

    expect(precios.preciadas, isEmpty);
    expect(pos.state.lineas.single.precioUnitario, 2000);
  });

  test('C2 · Al cobrar se manda la lista aceptada; si se rechazó, ninguna', () async {
    conCarrito();
    pos.fijarCliente(_mayorista);
    await pos.cobrar();
    expect(ventas.listaCobrada, isNull, reason: 'rechazó el recálculo: el backend cobra el catálogo');

    conCarrito();
    pos.fijarCliente(_mayorista);
    pos.aplicarPrevision(_alMayorista());
    await pos.cobrar();
    expect(ventas.listaCobrada, 'l-mayorista');
  });

  test('Volver a consumidor final quita la lista y los precios vuelven al catálogo', () async {
    conCarrito();
    pos.fijarCliente(_mayorista);
    pos.aplicarPrevision(_alMayorista());

    pos.fijarCliente(null);
    pos.aplicarPrevision(const PrevisionDePreciosVista(
      listaActualId: 'l-mayorista',
      cambios: [CambioDePrecioVista(linea: 0, producto: 'Martillo', cantidad: 2, precioActual: 8500, precioNuevo: 10000)],
    ));

    expect(pos.state.listaPreciosId, isNull);
    expect(pos.state.lineas[0].precioDeLista, isNull);
    expect(pos.state.lineas[0].precioUnitario, 10000);
  });

  test('Cada venta abierta guarda su propia lista (R12)', () async {
    pos.fijarCliente(_mayorista);
    pos.aplicarPrevision(_alMayorista());

    pos.cambiarDeVenta(1);
    expect(pos.state.listaPreciosId, isNull);

    pos.cambiarDeVenta(0);
    expect(pos.state.listaPreciosId, 'l-mayorista');
  });

  test('Sin señal no se puede prever: se asigna sin recalcular', () async {
    conCarrito();
    precios.falla = const ErrorDeRed();

    expect(await pos.preverCliente(_mayorista), isNull);
  });

  test('El contrato de la previsión: listas, cambios y lo que no cambia', () {
    final p = PrevisionDePreciosVista.desdeJson({
      'listaActual': {'id': null, 'nombre': null},
      'listaNueva': {'id': 'l-mayorista', 'nombre': 'Mayorista'},
      'cambios': [
        {'linea': 0, 'productoId': 'p-martillo', 'producto': 'Martillo', 'cantidad': 2, 'precioActual': 10000.0, 'precioNuevo': 8500.0},
      ],
      'sinCambio': [
        {'linea': 1, 'producto': 'Tornillo', 'motivo': 'DESCUENTO_A_MANO'},
        {'linea': 2, 'producto': 'Pintura ¼', 'motivo': 'PRECIO_ESPECIAL'},
      ],
      'totalActual': 30000.0,
      'totalNuevo': 27000.0,
      'hayCambios': true,
    });

    expect(p.listaActualId, isNull);
    expect(p.listaNuevaNombre, 'Mayorista');
    expect(p.cambios.single.precioNuevo, 8500);
    expect(p.conDescuentoAMano, 1);
    expect(p.conPrecioEspecial, 1);
    expect(p.totalNuevo, 27000);
  });
}
