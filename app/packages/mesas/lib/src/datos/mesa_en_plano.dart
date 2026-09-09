/// Una mesa tal como la pinta el plano del salón (HU-081). Los campos de sesión
/// —tiempo abierto, consumo— los añade HU-082; aquí no vienen.
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
      );
}
