/// "$2.140.000". Punto para los miles, sin decimales. Copiado del formato de
/// los mockups de clientes.
String formatearPesos(num valor) {
  final entero = valor.round().abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i != 0 && (entero.length - i) % 3 == 0) buffer.write('.');
    buffer.write(entero[i]);
  }
  final signo = valor < 0 ? '-' : '';
  return '$signo\$$buffer';
}

/// Las iniciales para el avatar de la fila: "Materiales Cruz S.A.S." -> "MC",
/// "Jorge Rendón Ospina" -> "JR".
String iniciales(String nombre) {
  final palabras = nombre
      .trim()
      .split(RegExp(r'\s+'))
      .where((p) => p.isNotEmpty && RegExp(r'[A-Za-zÀ-ÿ]').hasMatch(p))
      .toList();
  if (palabras.isEmpty) return '?';
  if (palabras.length == 1) {
    final p = palabras.first;
    return (p.length == 1 ? p : p.substring(0, 2)).toUpperCase();
  }
  return (palabras.first[0] + palabras[1][0]).toUpperCase();
}
