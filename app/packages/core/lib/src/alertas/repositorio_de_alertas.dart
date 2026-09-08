import '../http/cliente_http.dart';
import 'alerta_vista.dart';

/// Acceso a `servicio-alertas` para el centro de la app (HU-095).
class RepositorioDeAlertas {
  RepositorioDeAlertas(this._http);

  final ClienteHttp _http;

  Future<List<AlertaVista>> mias() async {
    final crudo = await _http.get<List<dynamic>>('/api/alertas/mias');
    return crudo
        .map((e) => AlertaVista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// Marca vistas las alertas nuevas del usuario (al abrir el centro).
  Future<void> marcarVistas() => _http.post('/api/alertas/mias/vistas');

  Future<AlertaVista> resolver(String alertaId) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/alertas/mias/${Uri.encodeComponent(alertaId)}/resolucion',
    ) as Map<String, dynamic>;
    return AlertaVista.desdeJson(cuerpo);
  }
}
