import 'package:regenta_core/regenta_core.dart';

import 'ocupacion_vista.dart';

/// Acceso a `servicio-reservas` para el calendario de ocupación (HU-075).
class RepositorioDeCalendario {
  RepositorioDeCalendario(this._http);

  final ClienteHttp _http;

  Future<OcupacionDelCalendario> ocupacion({
    required DateTime desde,
    required DateTime hasta,
    String? tipoRecursoId,
  }) async {
    final crudo = await _http.get<Map<String, dynamic>>(
      '/api/reservas/calendario',
      query: {
        'desde': _fecha(desde),
        'hasta': _fecha(hasta),
        'tipoRecursoId': ?tipoRecursoId,
      },
    );
    return OcupacionDelCalendario.desdeJson(crudo);
  }

  static String _fecha(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-'
      '${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';
}
