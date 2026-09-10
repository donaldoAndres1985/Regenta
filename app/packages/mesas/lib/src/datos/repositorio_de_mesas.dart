import 'package:regenta_core/regenta_core.dart';

import 'mesa_en_plano.dart';
import 'plano_del_salon.dart';
import 'sesion_de_mesa.dart';

/// Acceso al plano del salón contra `servicio-mesas` (HU-081). Sin copia local:
/// esa la trae HU-084 con el plano en tiempo real.
class RepositorioDeMesas {
  RepositorioDeMesas(this._http);

  final ClienteHttp _http;

  Future<PlanoDelSalon> plano() async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/mesas/plano');
    return PlanoDelSalon.desdeJson(cuerpo);
  }

  /// Crea una mesa (HU-081 criterio 1). Propaga [RecursoDuplicado] si el código
  /// ya existe (criterio 2).
  Future<MesaEnPlano> crearMesa({
    required String codigo,
    String? zonaId,
    String? nombre,
    int? capacidad,
    String? forma,
    int posX = 0,
    int posY = 0,
  }) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/mesas', datos: {
      'codigo': codigo,
      'zonaId': zonaId,
      'nombre': nombre,
      'capacidad': capacidad,
      'forma': forma,
      'posX': posX,
      'posY': posY,
    }) as Map<String, dynamic>;
    return MesaEnPlano.desdeJson(cuerpo);
  }

  /// Guarda la posición de la mesa en el plano (HU-081 criterio 3).
  Future<MesaEnPlano> moverMesa(String mesaId, int posX, int posY) async {
    final cuerpo = await _http.put<Map<String, dynamic>>(
      '/api/mesas/$mesaId/posicion',
      datos: {'posX': posX, 'posY': posY},
    ) as Map<String, dynamic>;
    return MesaEnPlano.desdeJson(cuerpo);
  }

  /// Elimina una mesa (soft). Propaga el 409 si tiene una sesión abierta
  /// (HU-081 criterio 4).
  Future<void> eliminarMesa(String mesaId) async {
    await _http.delete<void>('/api/mesas/$mesaId');
  }

  Future<void> crearZona({required String nombre, int? orden, String? color}) async {
    await _http.post<Map<String, dynamic>>('/api/mesas/zonas',
        datos: {'nombre': nombre, 'orden': orden, 'color': color});
  }

  // ---- Sesiones de mesa (HU-082) ----

  /// La sesión abierta de una mesa, o `null` si está libre.
  Future<SesionDeMesa?> sesionDe(String mesaId) async {
    try {
      final cuerpo = await _http.get<Map<String, dynamic>>('/api/mesas/$mesaId/sesion');
      return SesionDeMesa.desdeJson(cuerpo);
    } on ErrorDeApi {
      return null;
    }
  }

  /// Abre la mesa con el número de comensales (criterio 1). Propaga el 409 si ya
  /// tiene una sesión abierta (criterio 2).
  Future<SesionDeMesa> abrirSesion(String mesaId, int numComensales) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/mesas/$mesaId/sesiones',
      datos: {'numComensales': numComensales},
    ) as Map<String, dynamic>;
    return SesionDeMesa.desdeJson(cuerpo);
  }

  Future<void> pedirCuenta(String sesionId) async {
    await _http.post<Map<String, dynamic>>('/api/mesas/sesiones/$sesionId/cuenta');
  }

  /// Une una mesa libre a la sesión (HU-083 criterio 1). Propaga el 409 si la
  /// mesa ya está ocupada (criterio 2).
  Future<void> unirMesa(String sesionId, String mesaId) async {
    await _http.post<Map<String, dynamic>>('/api/mesas/sesiones/$sesionId/mesas/$mesaId');
  }

  /// Cierra la sesión; la mesa queda «por limpiar» (criterio 3).
  Future<void> cerrarSesion(String sesionId) async {
    await _http.post<Map<String, dynamic>>('/api/mesas/sesiones/$sesionId/cierre');
  }

  /// Marca la mesa limpia: de «por limpiar» a libre (criterio 4).
  Future<void> marcarLimpia(String mesaId) async {
    await _http.post<void>('/api/mesas/$mesaId/limpieza');
  }
}
