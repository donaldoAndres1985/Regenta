import 'package:dio/dio.dart';

import 'cliente_auth.dart';
import 'motor_de_sesion.dart';

/// Interceptor de dio que mantiene el token al dia sin que las pantallas se
/// enteren (criterio 2) y que, ante un 401, refresca una vez y reintenta; si
/// aun asi falla, cierra la sesion (criterio 3).
///
/// No toca las rutas de `auth` —de ahi sale el token— ni las peticiones sin
/// sesion.
class InterceptorDeRefresco extends Interceptor {
  InterceptorDeRefresco(this._motor, {required Dio dio}) : _dio = dio;

  static const String _yaReintentada = 'regenta.reintentada';

  final MotorDeSesion _motor;
  final Dio _dio;

  bool _esRutaDeAuth(String path) => path.contains('/auth/');

  @override
  Future<void> onRequest(
      RequestOptions options, RequestInterceptorHandler handler) async {
    if (_esRutaDeAuth(options.path) || _motor.sesion == null) {
      return handler.next(options);
    }
    try {
      options.headers['Authorization'] = 'Bearer ${await _motor.accesoVigente()}';
      handler.next(options);
    } on RefrescoRechazado {
      handler.reject(DioException(
        requestOptions: options,
        error: const RefrescoRechazado(),
        type: DioExceptionType.cancel,
      ));
    }
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    final req = err.requestOptions;
    if (err.response?.statusCode != 401 ||
        _esRutaDeAuth(req.path) ||
        _motor.sesion == null) {
      return handler.next(err);
    }
    if (req.extra[_yaReintentada] == true) {
      await _motor.cerrar(MotivoDeCierre.refrescoRechazado);
      return handler.next(err);
    }
    try {
      final token = await _motor.accesoVigente(forzar: true);
      req.extra[_yaReintentada] = true;
      req.headers['Authorization'] = 'Bearer $token';
      handler.resolve(await _dio.fetch<dynamic>(req));
    } on RefrescoRechazado {
      handler.next(err); // accesoVigente ya cerro la sesion
    } on DioException catch (e) {
      handler.next(e);
    }
  }
}
