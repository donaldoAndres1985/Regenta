/// Una sesión de mesa tal como la devuelve la API (HU-082).
class SesionDeMesa {
  const SesionDeMesa({
    required this.id,
    required this.mesaPrincipalId,
    required this.estado,
    required this.numComensales,
    required this.minutosAbierta,
    this.duracionMin,
  });

  final String id;
  final String mesaPrincipalId;
  final String estado;
  final int numComensales;

  /// Minutos que lleva (o llevó) abierta — el cronómetro (criterio 1).
  final int minutosAbierta;

  /// Solo en una sesión cerrada: cuánto duró (criterio 5).
  final int? duracionMin;

  bool get estaViva => estado == 'ABIERTA' || estado == 'CUENTA_PEDIDA';

  factory SesionDeMesa.desdeJson(Map<String, dynamic> json) => SesionDeMesa(
        id: json['id'] as String,
        mesaPrincipalId: (json['mesaPrincipalId'] ?? '') as String,
        estado: (json['estado'] ?? 'ABIERTA') as String,
        numComensales: (json['numComensales'] as num?)?.toInt() ?? 0,
        minutosAbierta: (json['minutosAbierta'] as num?)?.toInt() ?? 0,
        duracionMin: (json['duracionMin'] as num?)?.toInt(),
      );
}
