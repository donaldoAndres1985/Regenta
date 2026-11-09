import 'package:regenta_core/regenta_core.dart';
import 'sesion_caja_modelos.dart';

/// Repositorio HTTP para operaciones de caja (HU-059, HU-060, HU-061).
class RepositorioDeCaja {
  RepositorioDeCaja(this._http);

  final ClienteHttp _http;

  /// Obtiene la lista de cajas del negocio.
  Future<List<CajaVista>> listarCajas() async {
    final res = await _http.get<List<dynamic>>('/api/caja/cajas');
    return res.map((e) => CajaVista.desdeJson(Map<String, dynamic>.from(e as Map))).toList();
  }

  /// Crea una nueva caja.
  Future<CajaVista> crearCaja(String codigo, String nombre) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/caja/cajas',
      datos: {'codigo': codigo, 'nombre': nombre},
    );
    return CajaVista.desdeJson(res);
  }

  /// Consulta la sesión activa de una caja. Retorna null si 404 (sin sesión abierta).
  Future<SesionCajaActiva?> consultarActiva(String cajaId) async {
    try {
      final res = await _http.get<Map<String, dynamic>>(
        '/api/caja/sesiones/activa',
        parametros: {'cajaId': cajaId},
      );
      return SesionCajaActiva.desdeJson(res);
    } on ErrorDeApi catch (e) {
      if (e.codigo == 404) return null;
      rethrow;
    }
  }

  /// Abre un turno/sesión de caja con una base inicial en efectivo.
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
    return SesionCajaActiva.desdeJson(res);
  }

  /// Lista los movimientos de la sesión activa.
  Future<List<MovimientoCajaVista>> movimientos(String sesionId) async {
    final res = await _http.get<List<dynamic>>('/api/caja/sesiones/$sesionId/movimientos');
    return res
        .map((e) => MovimientoCajaVista.desdeJson(Map<String, dynamic>.from(e as Map)))
        .toList();
  }

  /// Registra un ingreso manual de efectivo.
  Future<void> registrarIngreso({
    required String sesionId,
    required num monto,
    required String concepto,
  }) async {
    await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones/$sesionId/ingresos',
      datos: {
        'monto': monto,
        'concepto': concepto,
      },
    );
  }

  /// Registra un retiro o gasto de efectivo.
  Future<void> registrarRetiro({
    required String sesionId,
    required num monto,
    required String concepto,
    required String tipo, // RETIRO o GASTO
    String? autorizadoPor,
  }) async {
    await _http.post<Map<String, dynamic>>(
      '/api/caja/sesiones/$sesionId/retiros',
      datos: {
        'monto': monto,
        'concepto': concepto,
        'tipo': tipo,
        if (autorizadoPor != null) 'autorizadoPor': autorizadoPor,
      },
    );
  }

  /// Cierra la sesión declarando el dinero contado.
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
    return SesionCajaActiva.desdeJson(res);
  }
}
