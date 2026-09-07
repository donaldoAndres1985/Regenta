import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

DioException _http(int codigo, {Object? cuerpo}) {
  final req = RequestOptions(path: '/api/ventas/algo');
  return DioException(
    requestOptions: req,
    type: DioExceptionType.badResponse,
    response: Response<dynamic>(
      requestOptions: req,
      statusCode: codigo,
      data: cuerpo,
    ),
  );
}

DioException _red() => DioException(
      requestOptions: RequestOptions(path: '/api/ventas/algo'),
      type: DioExceptionType.connectionError,
      error: 'sin red',
    );

void main() {
  test('criterio 1: un 401 se traduce a NoAutenticado', () {
    expect(mapearError(_http(401, cuerpo: {'detail': 'token vencido'})),
        isA<NoAutenticado>());
  });

  test('criterio 2: un 402 dice que el modulo no esta en el plan y ofrece ventas', () {
    final e = mapearError(_http(402, cuerpo: {
      'title': 'Modulo no contratado',
      'detail': 'FACTURACION no esta en el plan BASICO',
    }));
    expect(e, isA<ModuloFueraDelPlan>());
    e as ModuloFueraDelPlan;
    expect(e.mensaje, contains('FACTURACION'));
    expect(e.contactarVentas, isTrue);
  });

  test('criterio 3: un 403 dice que falta permiso, no que falta plan', () {
    final e = mapearError(_http(403, cuerpo: {'detail': 'te falta USUARIOS_USUARIO_CREAR'}));
    expect(e, isA<SinPermiso>());
    expect((e as SinPermiso).mensaje, isNot(contains('plan')));
    expect(e.mensaje, contains('permiso'));
  });

  test('criterio 4: un 422 trae los errores campo por campo', () {
    final e = mapearError(_http(422, cuerpo: {
      'title': 'Peticion invalida',
      'detail': 'Hay campos que no cumplen el contrato',
      'campos': {
        'email': 'no es un correo',
        'password': 'minimo 8 caracteres',
      },
    }));
    expect(e, isA<ErroresDeValidacion>());
    e as ErroresDeValidacion;
    expect(e.porCampo['email'], ['no es un correo']);
    expect(e.porCampo['password'], ['minimo 8 caracteres']);
    expect(e.deCampo('email'), 'no es un correo');
    expect(e.deCampo('telefono'), isNull);
  });

  test('un 409 se traduce a RecursoDuplicado', () {
    expect(mapearError(_http(409, cuerpo: {'detail': 'ya existe'})), isA<RecursoDuplicado>());
  });

  test('criterio 5: un error de red se traduce a ErrorDeRed', () {
    expect(mapearError(_red()), isA<ErrorDeRed>());
  });

  test('un 500 se traduce a ErrorDelServidor con su codigo', () {
    final e = mapearError(_http(503));
    expect(e, isA<ErrorDelServidor>());
    expect((e as ErrorDelServidor).codigo, 503);
  });

  test('un 402 sin cuerpo util igual es ModuloFueraDelPlan', () {
    expect(mapearError(_http(402)), isA<ModuloFueraDelPlan>());
  });
}
