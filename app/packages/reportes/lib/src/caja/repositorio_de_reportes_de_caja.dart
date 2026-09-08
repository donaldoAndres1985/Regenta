import 'package:regenta_core/regenta_core.dart';

import 'sesion_de_caja_vista.dart';

/// Acceso a `servicio-caja` para el reporte de cierre (HU-063).
class RepositorioDeReportesDeCaja {
  RepositorioDeReportesDeCaja(this._http);

  final ClienteHttp _http;

  Future<List<SesionDeCajaVista>> sesiones({
    required DateTime desde,
    required DateTime hasta,
    String? estado,
    String? cajaId,
  }) async {
    final crudo = await _http.get<List<dynamic>>(
      '/api/caja/reportes/sesiones',
      query: {
        'desde': _fecha(desde),
        'hasta': _fecha(hasta),
        'estado': ?estado,
        'cajaId': ?cajaId,
      },
    );
    return crudo
        .map((e) => SesionDeCajaVista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  static String _fecha(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-'
      '${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';
}
