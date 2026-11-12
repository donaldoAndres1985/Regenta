import 'package:regenta_core/regenta_core.dart';

import 'sesion_caja_modelos.dart';

/// Acceso a los endpoints de caja y turnos desde la app (HU-059, HU-061).
class RepositorioDeCaja {
  RepositorioDeCaja(this._http);

  final ClienteHttp _http;

  Future<List<CajaVista>> listarCajas() async {
    final res = await _http.get<List<dynamic>>('/api/caja/cajas');
    return res
        .map((e) => CajaVista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  Future<SesionCajaActiva?> consultarActiva(String cajaId) async {
    try {
      final dynamic res = await _http.get<dynamic>(
        '/api/caja/sesiones/activa?cajaId=$cajaId',
      );
      if (res == null) return null;
      return SesionCajaActiva.desdeJson((res as Map).cast<String, dynamic>());
    } on ErrorDeApi {
      return null;
    }
  }

  Future<SesionCajaActiva> abrirSesion({
    required String cajaId,
    required num montoApertura,
  }) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones',
      datos: {
        'cajaId': cajaId,
        'montoApertura': montoApertura,
      },
    );
    return SesionCajaActiva.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<List<MovimientoCajaVista>> movimientos(String sesionId) async {
    final res = await _http.get<List<dynamic>>(
      '/api/caja/sesiones/$sesionId/movimientos',
    );
    return res
        .map((e) => MovimientoCajaVista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  Future<MovimientoCajaVista> registrarIngreso({
    required String sesionId,
    required num monto,
    required String concepto,
  }) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones/$sesionId/ingresos',
      datos: {
        'monto': monto,
        'concepto': concepto,
      },
    );
    return MovimientoCajaVista.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<MovimientoCajaVista> registrarRetiro({
    required String sesionId,
    required num monto,
    required String concepto,
  }) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones/$sesionId/retiros',
      datos: {
        'monto': monto,
        'concepto': concepto,
      },
    );
    return MovimientoCajaVista.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<SesionCajaActiva> cerrarSesion({
    required String sesionId,
    required num montoDeclarado,
  }) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones/$sesionId/cierre',
      datos: {
        'montoDeclarado': montoDeclarado,
      },
    );
    return SesionCajaActiva.desdeJson((res as Map).cast<String, dynamic>());
  }
}
