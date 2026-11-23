import '../../http/cliente_http.dart';

/// El registro del celular para recibir las alertas por push (HU-123). El
/// backend lo deja contra el usuario y el negocio de la sesión.
class RepositorioDeDispositivos {
  RepositorioDeDispositivos(this._http);

  final ClienteHttp _http;

  Future<void> registrar({
    required String tokenFcm,
    required String plataforma,
    String? modelo,
    String? versionApp,
  }) =>
      _http.post('/api/alertas/dispositivos', datos: {
        'tokenFcm': tokenFcm,
        'plataforma': plataforma,
        'modelo': ?modelo,
        'versionApp': ?versionApp,
      });
}
