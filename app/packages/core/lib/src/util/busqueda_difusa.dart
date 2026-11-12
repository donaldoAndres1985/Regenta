/// Utilidades para búsqueda difusa multi-palabra (tokens en cualquier orden
/// y con cualquier texto intermedio).
library;

/// Normaliza un texto para comparaciones de búsqueda: minúsculas y sin acentos ni tildes.
String normalizarParaBusqueda(String texto) {
  var res = texto.toLowerCase();
  res = res.replaceAll(RegExp(r'[áàäâ]'), 'a');
  res = res.replaceAll(RegExp(r'[éèëê]'), 'e');
  res = res.replaceAll(RegExp(r'[íìïî]'), 'i');
  res = res.replaceAll(RegExp(r'[óòöô]'), 'o');
  res = res.replaceAll(RegExp(r'[úùüû]'), 'u');
  res = res.replaceAll(RegExp(r'[ñ]'), 'n');
  return res;
}

/// Extrae los tokens o palabras clave de una consulta (separados por espacios).
List<String> extraerTokensDeBusqueda(String consulta) {
  final norm = normalizarParaBusqueda(consulta.trim());
  if (norm.isEmpty) return const [];
  return norm.split(RegExp(r'\s+')).where((t) => t.isNotEmpty).toList();
}

/// Evalúa si [textoObjetivo] (junto con los [camposAdicionales] opcionales) contiene
/// TODOS los tokens de [consulta], en cualquier orden y con cualquier texto intermedio.
///
/// Ejemplo:
/// - consulta: "esmal negr"
/// - objetivo: "Esmalte pintulan negro" -> `true`
/// - objetivo: "esmalte pinte galon negro" -> `true`
/// - objetivo: "negro esmalte" -> `true`
/// - objetivo: "esmalte blanco" -> `false`
bool coincideConTokens(
  String? textoObjetivo,
  String consulta, {
  Iterable<String?>? camposAdicionales,
}) {
  final tokens = extraerTokensDeBusqueda(consulta);
  if (tokens.isEmpty) return true;

  final sb = StringBuffer();
  if (textoObjetivo != null && textoObjetivo.isNotEmpty) {
    sb.write(normalizarParaBusqueda(textoObjetivo));
    sb.write(' ');
  }
  if (camposAdicionales != null) {
    for (final campo in camposAdicionales) {
      if (campo != null && campo.isNotEmpty) {
        sb.write(normalizarParaBusqueda(campo));
        sb.write(' ');
      }
    }
  }

  final acumulado = sb.toString();
  if (acumulado.isEmpty) return false;

  return tokens.every((token) => acumulado.contains(token));
}
