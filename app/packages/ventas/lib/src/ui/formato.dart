/// Formatea un monto en pesos: «$ 1.250.000», con espacio tras el signo,
/// separador de miles con punto y sin decimales.
String formatearPesos(num monto) {
  final entero = monto.round().abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i != 0 && (entero.length - i) % 3 == 0) buffer.write('.');
    buffer.write(entero[i]);
  }
  return '${monto < 0 ? '-' : ''}\$ $buffer';
}

/// «142 und», «0 und». La cantidad se muestra sin decimales si es entera.
String formatearCantidad(num cantidad) {
  final n = cantidad == cantidad.roundToDouble() ? cantidad.round() : cantidad;
  return '$n und';
}
