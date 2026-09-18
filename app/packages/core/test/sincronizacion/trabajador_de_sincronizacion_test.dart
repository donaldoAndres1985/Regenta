import 'package:dio/dio.dart';
import 'package:drift/drift.dart' show Value;
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// Responde segun la ruta pedida, para simular exito en unas operaciones y
/// fallo en otras dentro de la misma pasada.
class _Adaptador implements HttpClientAdapter {
  _Adaptador(this.porRuta);
  final Map<String, ({int codigo, Object? cuerpo})> porRuta;
  final llamadasPorRuta = <String, int>{};

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    llamadasPorRuta.update(options.path, (v) => v + 1, ifAbsent: () => 1);
    final r = porRuta[options.path];
    if (r == null) {
      throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
    }
    if (r.codigo >= 400) {
      throw DioException(
        requestOptions: options,
        type: DioExceptionType.badResponse,
        response: Response(requestOptions: options, statusCode: r.codigo, data: r.cuerpo),
      );
    }
    return ResponseBody.fromString('{}', r.codigo);
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late BaseLocal db;
  late ColaDeSalidaLocal cola;

  setUp(() {
    db = BaseLocal(NativeDatabase.memory());
    cola = ColaDeSalidaLocal(db);
  });
  tearDown(() => db.close());

  TrabajadorDeSincronizacion trabajadorCon(Map<String, ({int codigo, Object? cuerpo})> porRuta) {
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))
      ..httpClientAdapter = _Adaptador(porRuta);
    return TrabajadorDeSincronizacion(dio: dio, db: db);
  }

  test('una operacion que se aplica bien sale de la cola', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final trabajador = trabajadorCon({'/api/ventas': (codigo: 201, cuerpo: {})});

    final resumen = await trabajador.ejecutarUnaPasada();

    expect(await db.select(db.operacionesPendientes).get(), isEmpty);
    expect(resumen.pendientes, 0);
  });

  test('criterio 2: una subida fallida por red se reintenta con backoff, sin perderse', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final trabajador = trabajadorCon({}); // ninguna ruta responde -> error de red

    final antes = DateTime.now().toUtc();
    await trabajador.ejecutarUnaPasada();

    final fila = await db.select(db.operacionesPendientes).getSingle();
    expect(fila.estado, 'PENDIENTE', reason: 'sigue viva, no se pierde');
    expect(fila.intentos, 1);
    expect(fila.proximoIntentoEn, isNotNull);
    expect(fila.proximoIntentoEn!.isAfter(antes), isTrue,
        reason: 'el backoff empuja el proximo intento al futuro');
  });

  test('criterio 2: mientras no llega la hora del proximo intento, esa fila no se toca', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final adaptador = _Adaptador({}); // siempre falla si se llama
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = adaptador;
    final trabajador = TrabajadorDeSincronizacion(dio: dio, db: db);
    await trabajador.ejecutarUnaPasada(); // primer intento: falla, agenda el proximo al futuro

    await trabajador.ejecutarUnaPasada(); // segunda pasada, inmediatamente despues

    expect(adaptador.llamadasPorRuta['/api/ventas'], 1,
        reason: 'no se reintenta antes de que llegue proximoIntentoEn');
  });

  test('el backoff crece con cada intento fallido seguido', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final trabajador = trabajadorCon({});

    await trabajador.ejecutarUnaPasada();
    final fila1 = await db.select(db.operacionesPendientes).getSingle();
    final esperaUno = fila1.proximoIntentoEn!.difference(DateTime.now().toUtc());

    await db.update(db.operacionesPendientes).write(
        const OperacionesPendientesCompanion(proximoIntentoEn: Value(null)));
    await trabajador.ejecutarUnaPasada();
    final fila2 = await db.select(db.operacionesPendientes).getSingle();
    final esperaDos = fila2.proximoIntentoEn!.difference(DateTime.now().toUtc());

    expect(fila2.intentos, 2);
    expect(esperaDos, greaterThan(esperaUno));
  });

  test('criterio 4: un 409 (choca con lo que hay en el servidor) queda en CONFLICTO', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final trabajador = trabajadorCon({
      '/api/ventas': (codigo: 409, cuerpo: {'detail': 'ya existe'}),
    });

    final resumen = await trabajador.ejecutarUnaPasada();

    final fila = await db.select(db.operacionesPendientes).getSingle();
    expect(fila.estado, 'CONFLICTO');
    expect(resumen.conflictos, 1);
    expect(resumen.pendientes, 0);
  });

  test('un rechazo por validacion (422) no se reintenta para siempre: queda RECHAZADA', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    final trabajador = trabajadorCon({
      '/api/ventas': (codigo: 422, cuerpo: {'campos': {}}),
    });

    await trabajador.ejecutarUnaPasada();

    final fila = await db.select(db.operacionesPendientes).getSingle();
    expect(fila.estado, 'RECHAZADA');
  });

  test('criterio 4 (HU-102): una operacion en conflicto no bloquea las demas de la cola', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/clientes'));
    final trabajador = trabajadorCon({
      '/api/ventas': (codigo: 409, cuerpo: {}),
      '/api/clientes': (codigo: 201, cuerpo: {}),
    });

    await trabajador.ejecutarUnaPasada();

    final filas = await db.select(db.operacionesPendientes).get();
    expect(filas, hasLength(1), reason: 'la de clientes se aplico y salio de la cola');
    expect(filas.single.ruta, '/api/ventas');
    expect(filas.single.estado, 'CONFLICTO');
  });

  test('criterio 3: el resumen cuenta pendientes y conflictos por separado', () async {
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/clientes'));
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/compras'));
    final trabajador = trabajadorCon({
      '/api/ventas': (codigo: 409, cuerpo: {}),
      // /api/clientes y /api/compras: sin red, quedan PENDIENTE
    });

    final resumen = await trabajador.ejecutarUnaPasada();

    expect(resumen.conflictos, 1);
    expect(resumen.pendientes, 2);
  });
}
