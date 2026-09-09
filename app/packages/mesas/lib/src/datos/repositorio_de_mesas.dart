import 'package:regenta_core/regenta_core.dart';

import 'mesa_en_plano.dart';
import 'plano_del_salon.dart';

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
}
