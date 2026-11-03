import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';
import '../navegacion/catalogo.dart';
import 'barra_lateral.dart';

/// Envuelve todas las pantallas de la app: en escritorio, con sesión y fuera
/// del login, pone el menú principal fijo a la izquierda y la pantalla de la
/// ruta a su derecha. Así el menú se ve igual en Inicio, Ventas, Inventario...
class CarcasaConMenu extends ConsumerStatefulWidget {
  const CarcasaConMenu({
    super.key,
    required this.enrutador,
    required this.child,
  });

  final GoRouter enrutador;
  final Widget child;

  @override
  ConsumerState<CarcasaConMenu> createState() => _CarcasaConMenuState();
}

class _CarcasaConMenuState extends ConsumerState<CarcasaConMenu> {
  GoRouter get enrutador => widget.enrutador;
  Widget get child => widget.child;

  @override
  void initState() {
    super.initState();
    enrutador.routerDelegate.addListener(_alCambiarDeRuta);
  }

  @override
  void didUpdateWidget(CarcasaConMenu viejo) {
    super.didUpdateWidget(viejo);
    if (viejo.enrutador != enrutador) {
      viejo.enrutador.routerDelegate.removeListener(_alCambiarDeRuta);
      enrutador.routerDelegate.addListener(_alCambiarDeRuta);
    }
  }

  @override
  void dispose() {
    enrutador.routerDelegate.removeListener(_alCambiarDeRuta);
    super.dispose();
  }

  /// El enrutador avisa del cambio de ruta en plena construcción; el menú se
  /// repinta al terminar el frame para no reconstruirse a mitad de otro build.
  void _alCambiarDeRuta() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) setState(() {});
    });
    WidgetsBinding.instance.scheduleFrame();
  }

  @override
  Widget build(BuildContext context) {
    final claims = ref.watch(perfilComoNotifierProvider).claims;
    if (claims == null) return child;

    return LayoutBuilder(
      builder: (context, restricciones) =>
          restricciones.maxWidth < anchoConMenuFijo ? child : _conMenu(claims),
    );
  }

  Widget _conMenu(ClaimsDeSesion claims) {
    return Builder(
      builder: (context) {
        final ruta = enrutador.routerDelegate.currentConfiguration.uri.path;
        if (ruta == GuardiaDeRutas.login) return child;

        final entradas = menuDe(claims);
        final nombreNegocio = (claims.nombreNegocio?.isNotEmpty ?? false)
            ? claims.nombreNegocio!
            : 'Ferretería El Tornillo';
        final plan = claims.plan.isNotEmpty
            ? 'Plan ${claims.plan}'
            : 'Plan Profesional';

        return Material(
          color: RegentaColors.paper,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              BarraLateral(
                nombreNegocio: nombreNegocio,
                nombreUsuario: 'Donaldo G.',
                plan: plan,
                rutaActiva: _rutaDelMenu(ruta, entradas),
                entradas: entradas,
                onNavegar: enrutador.go,
                onCerrarSesion: () => ref.read(motorDeSesionProvider).cerrar(),
              ),
              Expanded(child: MenuFijo(child: child)),
            ],
          ),
        );
      },
    );
  }

  /// La entrada del menú que corresponde a la ruta abierta: `/facturas/42`
  /// marca Facturación.
  static String _rutaDelMenu(String ruta, List<EntradaDeNavegacion> entradas) {
    if (ruta == GuardiaDeRutas.inicio) return ruta;
    for (final e in entradas) {
      if (e.ruta != GuardiaDeRutas.inicio &&
          (ruta == e.ruta || ruta.startsWith('${e.ruta}/'))) {
        return e.ruta;
      }
    }
    return ruta;
  }
}

/// Marca que la pantalla de abajo ya tiene el menú principal fijo al lado:
/// Inicio la usa para no pintar su propio cajón y su barra superior.
class MenuFijo extends InheritedWidget {
  const MenuFijo({super.key, required super.child});

  static bool presente(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<MenuFijo>() != null;

  @override
  bool updateShouldNotify(MenuFijo oldWidget) => false;
}
