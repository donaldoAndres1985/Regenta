import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// Nunca hay red: es el vendedor en ruta.
class _SinSenal implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    throw DioException(
        requestOptions: options, type: DioExceptionType.connectionError, error: 'sin red');
  }

  @override
  void close({bool force = false}) {}
}

/// Vuelve la señal: el servidor acepta la venta.
class _ConSenal implements HttpClientAdapter {
  final List<String> rutas = [];
  final List<String> cuerpos = [];

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    rutas.add(options.path);
    cuerpos.add(options.data.toString());
    return ResponseBody.fromString(
      '{"id":"11111111-1111-1111-1111-111111111111","numero":"1042"}',
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late BaseLocal db;

  setUp(() => db = BaseLocal(NativeDatabase.memory()));
  tearDown(() => db.close());

  Dio dioCon(HttpClientAdapter adaptador) =>
      Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = adaptador;

  RepositorioDeVentas repoCon(HttpClientAdapter adaptador) =>
      RepositorioDeVentas(ClienteHttp(dioCon(adaptador), cola: ColaDeSalidaLocal(db)));

  const linea = LineaParaEnviar(
    productoId: '22222222-2222-2222-2222-222222222222',
    sku: 'SKU-1',
    nombre: 'Bandeja paisa',
    cantidad: 2,
    precioUnitario: 32000,
    impuestoPct: 19,
    costoUnitario: 12000,
  );

  test('criterio 1: sin señal, la venta se guarda en la cola con su propio id', () async {
    final repo = repoCon(_SinSenal());

    final resultado = await repo.confirmarVenta(bodegaId: 'bodega-1', lineas: const [linea]);

    expect(resultado.quedoEnLaCola, isTrue,
        reason: 'no se pierde ni se muestra un error: queda para subir después');
    expect(resultado.origenOfflineId, isNotEmpty);

    final encoladas = await db.select(db.operacionesPendientes).get();
    expect(encoladas, hasLength(1));
    expect(encoladas.single.ruta, '/api/ventas/offline');
    expect(encoladas.single.estado, 'PENDIENTE');
    expect(encoladas.single.cuerpo, contains(resultado.origenOfflineId));
    expect(encoladas.single.cuerpo, contains('SKU-1'));
  });

  test('con señal, la venta se crea de una y no pasa por la cola', () async {
    final adaptador = _ConSenal();
    final repo = repoCon(adaptador);

    final resultado = await repo.confirmarVenta(bodegaId: 'bodega-1', lineas: const [linea]);

    expect(resultado.quedoEnLaCola, isFalse);
    expect(resultado.venta!.numero, '1042');
    expect(adaptador.rutas, ['/api/ventas/offline']);
    expect(await db.select(db.operacionesPendientes).get(), isEmpty);
  });

  test('criterio 5: la cola sobrevive al apagón y otra instancia la sube al volver', () async {
    await repoCon(_SinSenal()).confirmarVenta(bodegaId: 'bodega-1', lineas: const [linea]);
    expect(await db.select(db.operacionesPendientes).get(), hasLength(1));

    // El celular se apagó y volvió a encender: el trabajador arranca de cero,
    // pero la base local es la misma y la cola sigue ahí.
    final adaptador = _ConSenal();
    final trabajador = TrabajadorDeSincronizacion(dio: dioCon(adaptador), db: db);
    await trabajador.ejecutarUnaPasada();

    expect(adaptador.rutas, ['/api/ventas/offline'],
        reason: 'la venta encolada se sube tal cual se había registrado');
    expect(await db.select(db.operacionesPendientes).get(), isEmpty,
        reason: 'ya subió: sale de la cola');
  });

  test('criterio 3: el reintento sube el mismo id offline, para que el servidor no duplique',
      () async {
    final resultado =
        await repoCon(_SinSenal()).confirmarVenta(bodegaId: 'bodega-1', lineas: const [linea]);

    final adaptador = _ConSenal();
    final trabajador = TrabajadorDeSincronizacion(dio: dioCon(adaptador), db: db);
    await trabajador.ejecutarUnaPasada();

    expect(adaptador.cuerpos.single, contains(resultado.origenOfflineId),
        reason: 'el id con que se creó en el celular viaja igual en la subida');
  });
}
