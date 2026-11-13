/// Utilidades para búsqueda difusa multi-palabra (tokens en cualquier orden
/// y con cualquier texto intermedio) y configuración de modos de búsqueda y crédito.
library;

/// Estado global en memoria para las preferencias operativas de búsqueda.
class ConfiguradorDeBusqueda {
  /// `true`: búsqueda difusa multi-palabra (palabras en cualquier orden).
  /// `false`: búsqueda directa estándar (como antes, coincidencia exacta de subcadena más rápida).
  static bool busquedaDifusaHabilitada = true;
}

/// Estado global en memoria para las preferencias operativas de crédito.
class ConfiguradorDeCredito {
  /// `true`: permite crédito directo a cualquier cliente sin exigir cupo asignado.
  /// `false`: valida que el cliente tenga crédito habilitado y cupo disponible.
  static bool creditoSinCupoHabilitado = false;
}

/// Estado global en memoria para las preferencias operativas de inventario y stock.
class ConfiguradorDeStock {
  /// `true`: permite completar ventas aunque el stock registrado sea 0 o negativo.
  /// `false`: bloquea el cobro cuando hay líneas sin stock disponible.
  static bool politicaStockNegativoHabilitada = false;
}

/// Parámetros y datos del negocio parametrizables para la factura térmica / tiquete POS.
/// Inspirado en el diseño y campos del formato de tiquete de SISTEMA SARA (Factura_venta.jrxml).
class ConfiguradorDeTicket {
  static String razonSocial = 'FERRETERIA CENTRAL N°1';
  static String nombreComercial = '';
  static String nit = '';
  static String direccion = '';
  static String telefono = '';
  static String celular = '';
  static String email = '';
  static String ciudad = '';
  static String regimenFiscal = '';

  // Textos y leyendas parametrizables de la factura (idénticos a Factura_venta.jrxml de SISTEMA SARA)
  static String tipoComprobante = 'FACTURA DE VENTA';
  static String nombreVendedor = 'CAJA PRINCIPAL';
  static String mensajeAgradecimiento = 'GRACIAS POR SU COMPRA';
  static String mensajeGarantia = 'SIN FACTURAS NO SE HACEN CAMBIOS';
  static String mensajeDevolucion = 'NO SE HACEN DEVOLUCIONES DE DINERO';
  static String softwarePie = 'SARA - PUNTO DE VENTA - SOFTWARE POS';

  static String get nombreNegocioOComercial =>
      razonSocial.trim().isNotEmpty
          ? razonSocial.trim()
          : (nombreComercial.trim().isNotEmpty
              ? nombreComercial.trim()
              : 'FERRETERIA CENTRAL N°1');
}

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

/// Evalúa si [textoObjetivo] (junto con los [camposAdicionales] opcionales) coincide con
/// la búsqueda según el modo configurado en [ConfiguradorDeBusqueda.busquedaDifusaHabilitada]
/// o forzado con [forzarDifusa].
bool coincideConTokens(
  String? textoObjetivo,
  String consulta, {
  Iterable<String?>? camposAdicionales,
  bool? forzarDifusa,
}) {
  final normConsulta = normalizarParaBusqueda(consulta.trim());
  if (normConsulta.isEmpty) return true;

  final usarDifusa = forzarDifusa ?? ConfiguradorDeBusqueda.busquedaDifusaHabilitada;

  if (!usarDifusa) {
    // Búsqueda directa estándar como antes: subcadena corrida directa (más rápida)
    if (textoObjetivo != null && normalizarParaBusqueda(textoObjetivo).contains(normConsulta)) {
      return true;
    }
    if (camposAdicionales != null) {
      for (final campo in camposAdicionales) {
        if (campo != null && normalizarParaBusqueda(campo).contains(normConsulta)) {
          return true;
        }
      }
    }
    return false;
  }

  // Búsqueda difusa multi-palabra: todos los tokens en cualquier orden
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
