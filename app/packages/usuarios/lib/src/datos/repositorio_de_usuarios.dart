import 'package:regenta_core/regenta_core.dart';

import 'configuracion_negocio_vista.dart';

/// Acceso a los servicios de usuarios y configuración de negocio (HU-018).
class RepositorioDeUsuarios {
  RepositorioDeUsuarios(this._http);

  final ClienteHttp _http;

  Future<ConfiguracionNegocioVista> verConfiguracion() async {
    final res = await _http.get<Map<String, dynamic>>('/api/usuarios/configuracion');
    return ConfiguracionNegocioVista.desdeJson(res);
  }

  Future<ConfiguracionNegocioVista> actualizarConfiguracion(
    ConfiguracionNegocioVista config,
  ) async {
    final res = await _http.put<Map<String, dynamic>>(
      '/api/usuarios/configuracion',
      datos: config.aJson(),
    );
    return ConfiguracionNegocioVista.desdeJson(res);
  }
}
