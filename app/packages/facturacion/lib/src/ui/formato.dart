/// "$1.984.920". Punto para los miles, sin decimales. Negativos con "-$".
String formatearPesos(num valor) {
  final entero = valor.round().abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i != 0 && (entero.length - i) % 3 == 0) buffer.write('.');
    buffer.write(entero[i]);
  }
  return '${valor < 0 ? '-' : ''}\$$buffer';
}
