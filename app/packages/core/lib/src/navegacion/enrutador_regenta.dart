import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../negocio/perfil_de_sesion.dart';
import 'guardia_de_rutas.dart';
import 'ruta_protegida.dart';

/// Arma el `GoRouter` de la app: las rutas protegidas mas `/login` y `/`, con la
/// [GuardiaDeRutas] en el `redirect` y el [PerfilDeSesion] como
/// `refreshListenable` para que un cambio de sesion o de modulos reevalue a
/// donde se puede estar.
GoRouter crearEnrutador({
  required List<RutaProtegida> rutas,
  required PerfilDeSesion perfil,
  required GoRouterWidgetBuilder login,
  required GoRouterWidgetBuilder inicio,
  String ubicacionInicial = '/',
  Listenable? tambienEscuchar,
}) {
  final guardia = GuardiaDeRutas(rutas);
  final refresco = tambienEscuchar == null
      ? perfil
      : Listenable.merge([perfil, tambienEscuchar]);

  return GoRouter(
    initialLocation: ubicacionInicial,
    refreshListenable: refresco,
    redirect: (context, state) => guardia.redirigir(
      destino: state.matchedLocation,
      acceso: EstadoDeAcceso.deClaims(perfil.claims),
    ),
    routes: [
      GoRoute(path: GuardiaDeRutas.login, builder: login),
      GoRoute(path: GuardiaDeRutas.inicio, builder: inicio),
      for (final r in rutas) ...[
        GoRoute(
          path: r.ruta,
          builder: (context, state) => r.builder(context, state.pathParameters),
        ),
        GoRoute(
          path: '${r.ruta}/:id',
          builder: (context, state) => r.builder(context, state.pathParameters),
        ),
      ],
    ],
  );
}
