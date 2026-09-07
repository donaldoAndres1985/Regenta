/// Espaciado. Los mockups usan estos valores, no una malla de 4/8.
abstract final class RegentaSpacing {
  static const double xs = 5;
  static const double sm = 9;
  static const double md = 14;
  static const double lg = 18;
  static const double xl = 22;
  static const double xxl = 30;

  /// Altura minima de toque en movil. No bajar de aqui: se usa de pie, con una
  /// mano y a veces con guantes.
  static const double hitTarget = 44;
  static const double controlWeb = 36;
  static const double radius = 5;
  static const double radiusCard = 6;
}

/// Punto de corte entre la composicion de movil y la de escritorio.
///
/// No es un tamano de dispositivo: es donde deja de funcionar la lista de una
/// columna y empieza a caber una tabla con panel lateral.
const double kBreakpointEscritorio = 900;
