import 'package:meta/meta.dart';

import '../negocio/claims_de_sesion.dart';
import 'ruta_protegida.dart';

/// Lo que la guardia necesita saber del que navega: si tiene sesion y que
/// modulos y permisos lleva el token.
@immutable
class EstadoDeAcceso {
  const EstadoDeAcceso({
    required this.haySesion,
    this.modulos = const {},
    this.permisos = const {},
  });

  final bool haySesion;
  final Set<String> modulos;
  final Set<String> permisos;

  factory EstadoDeAcceso.deClaims(ClaimsDeSesion? claims) => claims == null
      ? const EstadoDeAcceso(haySesion: false)
      : EstadoDeAcceso(
          haySesion: true,
          modulos: claims.modulos.map((m) => m.toUpperCase()).toSet(),
          permisos: claims.permisos.toSet(),
        );
}

/// Decide a donde mandar cada navegacion. Esconder una ruta es cortesia: el
/// backend revalida con `@RequiereModulo` y `@RequierePermiso`.
class GuardiaDeRutas {
  GuardiaDeRutas(this._rutas);

  static const String login = '/login';
  static const String inicio = '/';

  final List<RutaProtegida> _rutas;

  /// Devuelve la ruta a la que redirigir, o null si el destino se deja pasar.
  String? redirigir({required String destino, required EstadoDeAcceso acceso}) {
    final esLogin = destino == login;

    if (!acceso.haySesion) {
      return esLogin ? null : login;
    }
    if (esLogin) {
      return inicio;
    }

    final protegida = _protegidaDe(destino);
    if (protegida == null) return null;

    if (protegida.modulo != null &&
        !acceso.modulos.contains(protegida.modulo!.toUpperCase())) {
      return inicio;
    }
    if (protegida.permiso != null && !acceso.permisos.contains(protegida.permiso)) {
      return inicio;
    }
    return null;
  }

  /// Casa el destino con una ruta protegida, contando los subcaminos:
  /// `/ventas/123` cae bajo la guardia de `/ventas`.
  RutaProtegida? _protegidaDe(String destino) {
    for (final r in _rutas) {
      if (destino == r.ruta || destino.startsWith('${r.ruta}/')) return r;
    }
    return null;
  }
}
