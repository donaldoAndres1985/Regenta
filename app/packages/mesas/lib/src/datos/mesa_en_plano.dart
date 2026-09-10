/// Una mesa tal como la pinta el plano del salón (HU-081). Si está en una sesión
/// viva, [sesionId] dice cuál — dos mesas con el mismo [sesionId] están unidas
/// (HU-083 criterio 4) — y [minutosAbierta] / [numComensales] alimentan el
/// cronómetro y el nº de comensales de la tarjeta (HU-084 criterio 1). El
/// consumo por mesa llega con E12.
class MesaEnPlano {
  const MesaEnPlano({
    required this.id,
    required this.codigo,
    required this.capacidad,
    required this.forma,
    required this.estado,
    required this.posX,
    required this.posY,
    required this.ancho,
    required this.alto,
    this.zonaId,
    this.nombre,
    this.sesionId,
    this.minutosAbierta,
    this.numComensales,
  });

  final String id;
  final String? zonaId;
  final String codigo;
  final String? nombre;
  final int capacidad;
  final String forma;
  final String estado;
  final int posX;
  final int posY;
  final int ancho;
  final int alto;
  final String? sesionId;
  final int? minutosAbierta;
  final int? numComensales;

  bool get ocupada => estado == 'OCUPADA' || estado == 'CUENTA_PEDIDA';

  factory MesaEnPlano.desdeJson(Map<String, dynamic> json) => MesaEnPlano(
        id: json['id'] as String,
        zonaId: json['zonaId'] as String?,
        codigo: (json['codigo'] ?? '') as String,
        nombre: json['nombre'] as String?,
        capacidad: (json['capacidad'] as num?)?.toInt() ?? 0,
        forma: (json['forma'] ?? 'CUADRADA') as String,
        estado: (json['estado'] ?? 'LIBRE') as String,
        posX: (json['posX'] as num?)?.toInt() ?? 0,
        posY: (json['posY'] as num?)?.toInt() ?? 0,
        ancho: (json['ancho'] as num?)?.toInt() ?? 80,
        alto: (json['alto'] as num?)?.toInt() ?? 80,
        sesionId: json['sesionId'] as String?,
        minutosAbierta: (json['minutosAbierta'] as num?)?.toInt(),
        numComensales: (json['numComensales'] as num?)?.toInt(),
      );

  MesaEnPlano conPosicion(int x, int y) => MesaEnPlano(
        id: id,
        zonaId: zonaId,
        codigo: codigo,
        nombre: nombre,
        capacidad: capacidad,
        forma: forma,
        estado: estado,
        posX: x < 0 ? 0 : x,
        posY: y < 0 ? 0 : y,
        ancho: ancho,
        alto: alto,
        sesionId: sesionId,
        minutosAbierta: minutosAbierta,
        numComensales: numComensales,
      );
}
