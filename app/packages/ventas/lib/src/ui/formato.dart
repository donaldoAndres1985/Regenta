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

/// «142 und», «0.25 und». La cantidad se muestra sin ceros sobrantes.
String formatearCantidad(num cantidad) {
  final s = formatearCantidadExacta(cantidad);
  return '$s und';
}

/// Formatea una cantidad numérica truncando los ceros sobrantes tras el punto decimal:
/// ej. 1.0 -> «1», 0.2500 -> «0.25», 0.03125 -> «0.03125», 0.2563 -> «0.2563».
String formatearCantidadExacta(num cantidad) {
  if (cantidad == cantidad.truncateToDouble()) {
    return cantidad.toInt().toString();
  }
  final texto = cantidad.toStringAsFixed(5);
  return texto.replaceAll(RegExp(r'0+$'), '').replaceAll(RegExp(r'\.$'), '');
}
