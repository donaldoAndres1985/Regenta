/// Formatea un monto en pesos como en los mockups: «$32.000», separador de
/// miles con punto y sin decimales.
String formatearPesos(num monto) {
  final entero = monto.round().abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i != 0 && (entero.length - i) % 3 == 0) buffer.write('.');
    buffer.write(entero[i]);
  }
  return '${monto < 0 ? '-' : ''}\$$buffer';
}

/// «142 und», «0 und». La cantidad se muestra sin decimales si es entera.
String formatearCantidad(num cantidad) {
  final n = cantidad == cantidad.roundToDouble() ? cantidad.round() : cantidad;
  return '$n und';
}
