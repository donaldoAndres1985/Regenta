import 'package:dio/dio.dart';

import 'cliente_auth.dart';
import 'sesion.dart';

/// `ClienteAuth` sobre dio. Habla con el gateway; las rutas de `auth` son las
/// unicas publicas del sistema.
class ClienteAuthHttp implements ClienteAuth {
  ClienteAuthHttp(this._dio);

  static const String rutaLogin = '/api/usuarios/auth/login';
  static const String rutaRefresco = '/api/usuarios/auth/refrescar';

  final Dio _dio;

  @override
  Future<Sesion> entrar(Credenciales credenciales) async {
    final Response<dynamic> respuesta;
    try {
      respuesta = await _dio.post<dynamic>(rutaLogin, data: credenciales.toJson());
    } on DioException catch (e) {
      final codigo = e.response?.statusCode;
      if (codigo == 423) throw const CuentaBloqueada();
      if (codigo == 401 || codigo == 422) throw const CredencialesInvalidas();
      rethrow;
    }

    final cuerpo = (respuesta.data as Map).cast<String, dynamic>();
    if (cuerpo['debeElegirNegocio'] == true) {
      final negocios = (cuerpo['negocios'] as List<dynamic>)
          .map((n) => NegocioParaElegir.fromJson((n as Map).cast<String, dynamic>()))
          .toList();
      throw DebeElegirNegocio(negocios);
    }
    return _aSesion(cuerpo);
  }

  @override
  Future<Sesion> refrescar({
    required String tokenDeRefresco,
    String? dispositivoId,
    String? plataforma,
  }) async {
    try {
      final respuesta = await _dio.post<dynamic>(rutaRefresco, data: {
        'tokenDeRefresco': tokenDeRefresco,
        'dispositivoId': ?dispositivoId,
        'plataforma': ?plataforma,
      });
      return _aSesion((respuesta.data as Map).cast<String, dynamic>());
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) throw const RefrescoRechazado();
      rethrow;
    }
  }

  Sesion _aSesion(Map<String, dynamic> c) {
    final segundos = (c['expiraEnSegundos'] as num).toInt();
    return Sesion(
      tokenDeAcceso: c['tokenDeAcceso'] as String,
      tokenDeRefresco: c['tokenDeRefresco'] as String,
      expiraEn: DateTime.now().toUtc().add(Duration(seconds: segundos)),
      negocioId: c['negocioId'] as String,
      usuarioId: c['usuarioId'] as String,
      plan: c['plan'] as String,
      patron: c['patron'] as String,
      roles: (c['roles'] as List<dynamic>).cast<String>(),
      modulos: (c['modulos'] as List<dynamic>).cast<String>(),
    );
  }
}
