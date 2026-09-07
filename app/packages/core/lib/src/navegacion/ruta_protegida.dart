import 'package:flutter/widgets.dart';

/// Construye la pantalla de una ruta. `parametros` trae lo de la URL
/// (`/ventas/:id` -> `{'id': '123'}`).
typedef ConstructorDeRuta = Widget Function(
    BuildContext context, Map<String, String> parametros);

/// Una ruta que no es publica: exige sesion y, si se declara, un modulo activo
/// y un permiso.
@immutable
class RutaProtegida {
  const RutaProtegida({
    required this.ruta,
    required this.builder,
    this.modulo,
    this.permiso,
  });

  final String ruta;
  final ConstructorDeRuta builder;

  /// El modulo que hay que tener encendido. null = solo exige sesion.
  final String? modulo;

  /// El permiso que hay que tener. null = no exige ninguno en particular.
  final String? permiso;
}
