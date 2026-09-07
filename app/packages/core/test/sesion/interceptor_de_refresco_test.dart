import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

import 'dobles.dart';

/// Adaptador que responde segun la cabecera Authorization que reciba: as� se
/// comprueba que el interceptor pone el token vigente y reintenta con el nuevo.
class AdaptadorPorToken implements HttpClientAdapter {
  AdaptadorPorToken(this.aceptado);

  /// El unico token de acceso que el "servicio" da por bueno.
  String aceptado;
  final List<String?> autorizaciones = [];

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    if (options.path == '/api/usuarios/auth/refrescar') {
      return ResponseBody.fromString(jsonEncode({'no': 'aplica'}), 200);
    }
    final auth = options.headers['Authorization'] as String?;
    autorizaciones.add(auth);
    final ok = auth == 'Bearer $aceptado';
    return ResponseBody.fromString(
      jsonEncode({'ok': ok}),
      ok ? 200 : 401,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late DateTime reloj;
  DateTime ahora() => reloj;
  setUp(() => reloj = DateTime.utc(2026, 1, 1, 12));

  test('criterio 2: pega el token vigente y lo refresca antes si esta por vencer', () async {
    final almacen = AlmacenEnMemoria()
      ..guardar(sesionDe(
          tokenDeAcceso: 'viejo', expiraEn: reloj.add(const Duration(seconds: 10))));
    final cliente = ClienteAuthFalso(
        alRefrescar: (_) => sesionDe(
            tokenDeAcceso: 'fresco', expiraEn: reloj.add(const Duration(minutes: 15))));
    final motor = MotorDeSesion(cliente: cliente, almacen: almacen, ahora: ahora);
    await motor.iniciar();

    final adaptador = AdaptadorPorToken('fresco');
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))
      ..httpClientAdapter = adaptador;
    dio.interceptors.add(InterceptorDeRefresco(motor, dio: dio));

    final r = await dio.get<Map<String, dynamic>>('/api/ventas/algo');

    expect(r.statusCode, 200);
    expect(cliente.refrescos, 1);
    expect(adaptador.autorizaciones, ['Bearer fresco']);
  });

  test('criterio 2/3: ante un 401 refresca una vez y reintenta; si aun falla, cierra', () async {
    final almacen = AlmacenEnMemoria()
      ..guardar(sesionDe(
          tokenDeAcceso: 'revocado', expiraEn: reloj.add(const Duration(minutes: 10))));
    // El refresco "funciona" pero da un token que el servicio tampoco acepta:
    // simula un token revocado en servidor.
    final cliente = ClienteAuthFalso(
        alRefrescar: (_) => sesionDe(
            tokenDeAcceso: 'tampoco', expiraEn: reloj.add(const Duration(minutes: 10))));
    final motor = MotorDeSesion(cliente: cliente, almacen: almacen, ahora: ahora);
    await motor.iniciar();

    final adaptador = AdaptadorPorToken('otro-distinto');
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))
      ..httpClientAdapter = adaptador;
    dio.interceptors.add(InterceptorDeRefresco(motor, dio: dio));

    await expectLater(dio.get<dynamic>('/api/ventas/algo'), throwsA(isA<DioException>()));

    expect(cliente.refrescos, 1, reason: 'un solo intento de refresco por peticion');
    expect(motor.estado, isA<SinSesion>());
    expect((motor.estado as SinSesion).motivo, MotivoDeCierre.refrescoRechazado);
  });

  test('sin sesion el interceptor no mete Authorization', () async {
    final motor = MotorDeSesion(
        cliente: ClienteAuthFalso(), almacen: AlmacenEnMemoria(), ahora: ahora);
    await motor.iniciar();

    final adaptador = AdaptadorPorToken('lo-que-sea');
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))
      ..httpClientAdapter = adaptador;
    dio.interceptors.add(InterceptorDeRefresco(motor, dio: dio));

    await dio.get<dynamic>('/actuator/health').catchError((_) => Response<dynamic>(
        requestOptions: RequestOptions(path: '/actuator/health')));

    expect(adaptador.autorizaciones.single, isNull);
  });
}
