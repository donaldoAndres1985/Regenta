import 'package:dio/dio.dart';

import 'resumen_del_negocio.dart';

/// Consulta el plan y los modulos del negocio contra el servicio de usuarios,
/// via gateway. Es la vista rica (limites, usuarios activos, origen de cada
/// modulo); para la navegacion basta con los claims del token.
class ClienteDeNegocioHttp {
  ClienteDeNegocioHttp(this._dio);

  static const String rutaResumen = '/api/usuarios/mi-negocio';
  static const String rutaModulos = '/api/usuarios/modulos';

  final Dio _dio;

  Future<ResumenDelNegocio> miNegocio() async {
    final r = await _dio.get<dynamic>(rutaResumen);
    return ResumenDelNegocio.fromJson((r.data as Map).cast<String, dynamic>());
  }

  Future<List<ModuloActivo>> modulos() async {
    final r = await _dio.get<dynamic>(rutaModulos);
    return (r.data as List<dynamic>)
        .map((m) => ModuloActivo.fromJson((m as Map).cast<String, dynamic>()))
        .toList();
  }
}
