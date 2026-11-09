/// Una caja registrada en el negocio (punto de cobro, HU-059).
class CajaVista {
  const CajaVista({
    required this.id,
    required this.codigo,
    required this.nombre,
    required this.activa,
  });

  final String id;
  final String codigo;
  final String nombre;
  final bool activa;

  factory CajaVista.desdeJson(Map<String, dynamic> json) => CajaVista(
        id: json['id'] as String,
        codigo: (json['codigo'] ?? '') as String,
        nombre: (json['nombre'] ?? '') as String,
        activa: (json['activa'] ?? true) as bool,
      );
}

/// Estado de una sesión de caja.
enum EstadoTurnoCaja { abierta, cerrada, cuadrada, descuadrada }

/// Una sesión o turno de caja (HU-059).
class SesionCajaActiva {
  const SesionCajaActiva({
    required this.id,
    required this.numero,
    required this.cajaId,
    required this.usuarioId,
    required this.estado,
    required this.montoApertura,
    required this.abiertaEn,
    this.cerradaEn,
    this.montoEsperado,
    this.montoDeclarado,
    this.diferencia,
  });

  final String id;
  final String numero;
  final String cajaId;
  final String usuarioId;
  final EstadoTurnoCaja estado;
  final num montoApertura;
  final DateTime abiertaEn;
  final DateTime? cerradaEn;
  final num? montoEsperado;
  final num? montoDeclarado;
  final num? diferencia;

  bool get estaAbierta => estado == EstadoTurnoCaja.abierta;

  factory SesionCajaActiva.desdeJson(Map<String, dynamic> json) => SesionCajaActiva(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        cajaId: (json['cajaId'] ?? '') as String,
        usuarioId: (json['usuarioId'] ?? '') as String,
        estado: _parseEstado((json['estado'] ?? 'ABIERTA') as String),
        montoApertura: (json['montoApertura'] as num?) ?? 0,
        abiertaEn: DateTime.tryParse((json['abiertaEn'] ?? '') as String)?.toLocal() ??
            DateTime.now(),
        cerradaEn: DateTime.tryParse((json['cerradaEn'] ?? '') as String? ?? '')?.toLocal(),
        montoEsperado: json['montoEsperado'] as num?,
        montoDeclarado: json['montoDeclarado'] as num?,
        diferencia: json['diferencia'] as num?,
      );

  static EstadoTurnoCaja _parseEstado(String s) => switch (s.toUpperCase()) {
        'CERRADA' => EstadoTurnoCaja.cerrada,
        'CUADRADA' => EstadoTurnoCaja.cuadrada,
        'DESCUADRADA' => EstadoTurnoCaja.descuadrada,
        _ => EstadoTurnoCaja.abierta,
      };
}

/// Un movimiento en la sesión (HU-060, HU-061).
class MovimientoCajaVista {
  const MovimientoCajaVista({
    required this.id,
    required this.tipo,
    required this.signo,
    required this.monto,
    required this.metodoPago,
    required this.concepto,
    required this.creadoEn,
  });

  final String id;
  final String tipo; // APERTURA, VENTA, INGRESO, RETIRO, GASTO
  final int signo; // +1 o -1
  final num monto;
  final String metodoPago;
  final String concepto;
  final DateTime creadoEn;

  factory MovimientoCajaVista.desdeJson(Map<String, dynamic> json) => MovimientoCajaVista(
        id: (json['id'] ?? '') as String,
        tipo: (json['tipo'] ?? '') as String,
        signo: (json['signo'] as num?)?.toInt() ?? 1,
        monto: (json['monto'] as num?) ?? 0,
        metodoPago: (json['metodoPago'] ?? 'EFECTIVO') as String,
        concepto: (json['concepto'] ?? '') as String,
        creadoEn: DateTime.tryParse((json['creadoEn'] ?? '') as String)?.toLocal() ??
            DateTime.now(),
      );
}
