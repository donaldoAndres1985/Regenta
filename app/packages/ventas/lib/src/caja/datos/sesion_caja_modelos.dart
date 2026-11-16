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

/// HU-142 · los datos de la tirilla de cierre de una sesión cerrada.
class TirillaDeCierreVista {
  const TirillaDeCierreVista({
    required this.sesionId,
    required this.numero,
    required this.caja,
    required this.cajeroApertura,
    required this.cajeroCierre,
    required this.abiertaEn,
    required this.cerradaEn,
    required this.base,
    required this.lineas,
    required this.diferenciaTotal,
    required this.estado,
    this.observaciones,
  });

  final String sesionId;
  final String numero;
  final String caja;
  final String cajeroApertura;
  final String cajeroCierre;
  final DateTime? abiertaEn;
  final DateTime? cerradaEn;
  final num base;
  final List<LineaDeArqueoVista> lineas;
  final num diferenciaTotal;
  final String estado;
  final String? observaciones;

  factory TirillaDeCierreVista.desdeJson(Map<String, dynamic> j) => TirillaDeCierreVista(
        sesionId: j['sesionId'] as String,
        numero: (j['numero'] as String?) ?? '',
        caja: (j['caja'] as String?) ?? '',
        cajeroApertura: (j['cajeroApertura'] as String?) ?? '',
        cajeroCierre: (j['cajeroCierre'] as String?) ?? '',
        abiertaEn: DateTime.tryParse((j['abiertaEn'] as String?) ?? ''),
        cerradaEn: DateTime.tryParse((j['cerradaEn'] as String?) ?? ''),
        base: (j['base'] as num?) ?? 0,
        lineas: ((j['lineas'] as List?) ?? const [])
            .map((e) => LineaDeArqueoVista.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
        diferenciaTotal: (j['diferenciaTotal'] as num?) ?? 0,
        estado: (j['estado'] as String?) ?? '',
        observaciones: j['observaciones'] as String?,
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
    this.anulaA,
  });

  final String id;
  final String sesionId;
  final String tipo;
  final num monto;
  final String concepto;
  final String fecha;

  /// HU-141: si este movimiento es una anulación, el id del movimiento que compensa.
  final String? anulaA;

  /// I5: solo los ingresos, retiros y gastos se pueden anular.
  bool get esManual => const {'INGRESO', 'RETIRO', 'GASTO'}.contains(tipo.toUpperCase());

  factory MovimientoCajaVista.desdeJson(Map<String, dynamic> j) => MovimientoCajaVista(
        id: j['id'] as String,
        sesionId: (j['sesionId'] as String?) ?? '',
        tipo: (j['tipo'] as String?) ?? 'INGRESO',
        monto: (j['monto'] as num?) ?? 0,
        concepto: (j['concepto'] as String?) ?? '',
        fecha: (j['fecha'] as String?) ?? (j['ocurridoEn'] as String?) ?? '',
        anulaA: j['anulaA'] as String?,
      );
}
