import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

class _Adaptador implements HttpClientAdapter {
  _Adaptador(this.responder);
  final ({int codigo, Object? cuerpo}) Function(RequestOptions o) responder;
  int llamadas = 0;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    llamadas++;
    final r = responder(options);
    return ResponseBody.fromString(jsonEncode(r.cuerpo ?? {}), r.codigo, headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }

  @override
  void close({bool force = false}) {}
}

/// Adaptador que siempre falla por red.
class _AdaptadorSinRed implements HttpClientAdapter {
  int llamadas = 0;
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    llamadas++;
    throw DioException(
        requestOptions: options, type: DioExceptionType.connectionError, error: 'sin red');
  }

  @override
  void close({bool force = false}) {}
}

class _ColaFalsa implements ColaDeSalida {
  final List<String> encoladas = [];
  @override
  Future<void> encolar(OperacionEncolable operacion) async =>
      encoladas.add('${operacion.metodo} ${operacion.ruta}');
}

void main() {
  ClienteHttp clienteCon(HttpClientAdapter adaptador, {ColaDeSalida? cola}) {
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = adaptador;
    return ClienteHttp(dio, cola: cola);
  }

  test('una respuesta 200 devuelve el cuerpo', () async {
    final cliente = clienteCon(_Adaptador((_) => (codigo: 200, cuerpo: {'ok': true})));
    final r = await cliente.get<Map<String, dynamic>>('/api/ventas/1');
    expect(r['ok'], true);
  });

  test('criterio 3: un 403 lanza SinPermiso', () async {
    final cliente = clienteCon(_Adaptador(
        (_) => (codigo: 403, cuerpo: {'detail': 'te falta el permiso VENTAS_VENTA_CREAR'})));
    await expectLater(cliente.get<dynamic>('/api/ventas/1'), throwsA(isA<SinPermiso>()));
  });

  test('criterio 4: un 422 lanza ErroresDeValidacion con los campos', () async {
    final cliente = clienteCon(_Adaptador((_) => (
          codigo: 422,
          cuerpo: {
            'campos': {'email': 'no es un correo'}
          }
        )));
    await expectLater(
      cliente.post<dynamic>('/api/usuarios', datos: {'email': 'x'}),
      throwsA(isA<ErroresDeValidacion>()
          .having((e) => e.deCampo('email'), 'email', 'no es un correo')),
    );
  });

  test('criterio 5: error de red + operacion encolable -> se encola, no se lanza', () async {
    final cola = _ColaFalsa();
    final adaptador = _AdaptadorSinRed();
    final cliente = clienteCon(adaptador, cola: cola);

    final r = await cliente.post<dynamic>('/api/ventas', datos: {'total': 100}, encolable: true);

    expect(r, isA<Encolado>());
    expect(cola.encoladas, ['POST /api/ventas']);
  });

  test('error de red y NO encolable -> lanza ErrorDeRed', () async {
    final cliente = clienteCon(_AdaptadorSinRed(), cola: _ColaFalsa());
    await expectLater(
      cliente.post<dynamic>('/api/ventas', datos: {}, encolable: false),
      throwsA(isA<ErrorDeRed>()),
    );
  });

  test('error de red, encolable, pero sin cola configurada -> lanza ErrorDeRed', () async {
    final cliente = clienteCon(_AdaptadorSinRed());
    await expectLater(
      cliente.post<dynamic>('/api/ventas', datos: {}, encolable: true),
      throwsA(isA<ErrorDeRed>()),
    );
  });
}
