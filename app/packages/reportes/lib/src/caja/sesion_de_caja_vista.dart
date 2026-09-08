/// El estado de un turno de caja en el reporte (HU-063).
enum EstadoSesionCaja { abierta, cerrada, cuadrada, descuadrada }

/// Una sesión de caja como la muestra el reporte del gerente (HU-063).
class SesionDeCajaVista {
  const SesionDeCajaVista({
    required this.id,
    required this.numero,
    required this.cajaId,
    required this.estado,
    required this.descuadrada,
    required this.abiertaEn,
    this.cerradaEn,
    this.montoEsperado,
    this.montoDeclarado,
    this.diferencia,
  });

  final String id;
  final String numero;
  final String cajaId;
  final EstadoSesionCaja estado;
  final bool descuadrada;
  final DateTime abiertaEn;
  final DateTime? cerradaEn;
  final num? montoEsperado;
  final num? montoDeclarado;
  final num? diferencia;

  bool get estaCerrada => estado != EstadoSesionCaja.abierta;

  factory SesionDeCajaVista.desdeJson(Map<String, dynamic> json) => SesionDeCajaVista(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        cajaId: (json['cajaId'] ?? '') as String,
        estado: _estado((json['estado'] ?? 'ABIERTA') as String),
        descuadrada: (json['descuadrada'] ?? false) as bool,
        abiertaEn: DateTime.tryParse((json['abiertaEn'] ?? '') as String)?.toLocal() ??
            DateTime.now(),
        cerradaEn: DateTime.tryParse((json['cerradaEn'] ?? '') as String? ?? '')?.toLocal(),
        montoEsperado: json['montoEsperado'] as num?,
        montoDeclarado: json['montoDeclarado'] as num?,
        diferencia: json['diferencia'] as num?,
      );

  static EstadoSesionCaja _estado(String s) => switch (s.toUpperCase()) {
        'CERRADA' => EstadoSesionCaja.cerrada,
        'CUADRADA' => EstadoSesionCaja.cuadrada,
        'DESCUADRADA' => EstadoSesionCaja.descuadrada,
        _ => EstadoSesionCaja.abierta,
      };
}
