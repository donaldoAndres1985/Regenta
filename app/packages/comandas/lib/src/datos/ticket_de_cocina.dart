/// Un ticket de cocina (HU-088): las líneas de una comanda que le tocan a una
/// sola estación, con su propio ciclo de vida y su demora.
class TicketDeCocina {
  const TicketDeCocina({
    required this.id,
    required this.comandaId,
    required this.comandaNumero,
    required this.estacionId,
    required this.secuencia,
    required this.estado,
    required this.minutosTranscurridos,
    required this.demorado,
    required this.lineas,
  });

  final String id;
  final String comandaId;
  final String comandaNumero;
  final String estacionId;
  final int secuencia;
  final String estado;
  final int minutosTranscurridos;
  final bool demorado;
  final List<LineaDeTicket> lineas;

  factory TicketDeCocina.desdeJson(Map<String, dynamic> json) => TicketDeCocina(
        id: json['id'] as String,
        comandaId: (json['comandaId'] ?? '') as String,
        comandaNumero: (json['comandaNumero'] ?? '') as String,
        estacionId: (json['estacionId'] ?? '') as String,
        secuencia: (json['secuencia'] as num?)?.toInt() ?? 0,
        estado: (json['estado'] ?? 'NUEVO') as String,
        minutosTranscurridos: (json['minutosTranscurridos'] as num?)?.toInt() ?? 0,
        demorado: (json['demorado'] as bool?) ?? false,
        lineas: (json['lineas'] as List<dynamic>? ?? const [])
            .map((e) => LineaDeTicket.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
      );
}

class LineaDeTicket {
  const LineaDeTicket({required this.id, required this.nombre, required this.cantidad, this.notas});

  final String id;
  final String nombre;
  final num cantidad;
  final String? notas;

  factory LineaDeTicket.desdeJson(Map<String, dynamic> json) => LineaDeTicket(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? '') as String,
        cantidad: (json['cantidad'] as num?) ?? 1,
        notas: json['notas'] as String?,
      );
}

/// Una estación de cocina, para las pestañas del encabezado (HU-077).
class EstacionDeCocina {
  const EstacionDeCocina({
    required this.id,
    required this.nombre,
    required this.codigo,
    required this.activa,
    required this.orden,
  });

  final String id;
  final String nombre;
  final String codigo;
  final bool activa;
  final int orden;

  factory EstacionDeCocina.desdeJson(Map<String, dynamic> json) => EstacionDeCocina(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? '') as String,
        codigo: (json['codigo'] ?? '') as String,
        activa: (json['activa'] as bool?) ?? true,
        orden: (json['orden'] as num?)?.toInt() ?? 0,
      );
}
