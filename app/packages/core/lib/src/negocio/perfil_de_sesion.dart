import 'package:flutter/foundation.dart';

import 'claims_de_sesion.dart';
import 'modulos_activos.dart';

/// El perfil vigente del usuario, leido de su token. La navegacion lo escucha:
/// cuando el token se refresca con un modulo nuevo, el perfil avisa y el menu
/// se actualiza (HU-020, criterio 3).
class PerfilDeSesion extends ChangeNotifier {
  String? _token;
  ClaimsDeSesion? _claims;
  ModulosActivos _modulos = ModulosActivos.ninguno();

  ClaimsDeSesion? get claims => _claims;

  ModulosActivos get modulos => _modulos;

  /// Fija el token de acceso actual. Si es el mismo de antes, no hace nada.
  void fijarToken(String token) {
    if (token == _token) return;
    _token = token;
    _claims = ClaimsDeSesion.deJwt(token);
    _modulos = ModulosActivos.deClaims(_claims!);
    notifyListeners();
  }

  /// Al cerrar sesion.
  void limpiar() {
    if (_token == null && _claims == null) return;
    _token = null;
    _claims = null;
    _modulos = ModulosActivos.ninguno();
    notifyListeners();
  }
}
