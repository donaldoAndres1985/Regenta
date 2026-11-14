/// Modelos para la gestión de puntos de cobro y sesiones de caja (HU-059, HU-061).
class CajaVista {
  const CajaVista({
    required this.id,
    required this.codigo,
    required this.nombre,
    this.activa = true,
  });

  final String id;
  final String codigo;
  final String nombre;
  final bool activa;

  factory CajaVista.desdeJson(Map<String, dynamic> j) => CajaVista(
        id: j['id'] as String,
        codigo: (j['codigo'] as String?) ?? '',
        nombre: (j['nombre'] as String?) ?? '',
        activa: (j['activa'] as bool?) ?? true,
      );
}

class SesionCajaActiva {
  const SesionCajaActiva({
    required this.id,
    required this.cajaId,
    required this.cajeroId,
    required this.fechaApertura,
    required this.montoApertura,
    required this.estado,
  });

  final String id;
  final String cajaId;
  final String cajeroId;
  final String fechaApertura;
  final num montoApertura;
  final String estado;

  factory SesionCajaActiva.desdeJson(Map<String, dynamic> j) => SesionCajaActiva(
        id: j['id'] as String,
        cajaId: (j['cajaId'] as String?) ?? '',
        cajeroId: (j['cajeroId'] as String?) ?? '',
        fechaApertura: (j['fechaApertura'] as String?) ?? '',
        montoApertura: (j['montoApertura'] as num?) ?? 0,
        estado: (j['estado'] as String?) ?? 'ABIERTA',
      );
}

/// HU-140 · una línea del arqueo por método de pago. Con la sesión abierta, el `contado` de un
/// método cuadrable puede venir nulo: todavía no se contó.
class LineaDeArqueoVista {
  const LineaDeArqueoVista({
    required this.metodo,
    required this.cuadrable,
    required this.esperado,
    required this.movimientos,
    this.contado,
  });

  final String metodo;
  final bool cuadrable;
  final num esperado;
  final num? contado;
  final int movimientos;

  factory LineaDeArqueoVista.desdeJson(Map<String, dynamic> j) => LineaDeArqueoVista(
        metodo: j['metodo'] as String,
        cuadrable: (j['cuadrable'] as bool?) ?? true,
        esperado: (j['esperado'] as num?) ?? 0,
        contado: j['contado'] as num?,
        movimientos: (j['movimientos'] as num?)?.toInt() ?? 0,
      );
}

class MovimientoCajaVista {
  const MovimientoCajaVista({
    required this.id,
    required this.sesionId,
    required this.tipo,
    required this.monto,
    required this.concepto,
    required this.fecha,
  });

  final String id;
  final String sesionId;
  final String tipo;
  final num monto;
  final String concepto;
  final String fecha;

  factory MovimientoCajaVista.desdeJson(Map<String, dynamic> j) => MovimientoCajaVista(
        id: j['id'] as String,
        sesionId: (j['sesionId'] as String?) ?? '',
        tipo: (j['tipo'] as String?) ?? 'INGRESO',
        monto: (j['monto'] as num?) ?? 0,
        concepto: (j['concepto'] as String?) ?? '',
        fecha: (j['fecha'] as String?) ?? '',
      );
}
