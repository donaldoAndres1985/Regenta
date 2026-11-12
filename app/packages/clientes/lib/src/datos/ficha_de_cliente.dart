/// La ficha de un cliente tal como se crea y se edita (HU-021).
///
/// El listado ([ClienteEnLista]) trae lo justo para la fila; esto es lo que se
/// le manda al servidor en `POST /api/clientes` y `PUT /api/clientes/{id}`.
class FichaDeCliente {
  const FichaDeCliente({
    this.id,
    this.tipoPersona = 'NATURAL',
    this.tipoDocumento = 'CC',
    this.numeroDocumento,
    this.digitoVerificacion,
    this.nombres,
    this.apellidos,
    this.razonSocial,
    this.email,
    this.telefono,
    this.telefonoAlterno,
    this.segmento,
    this.notas,
    this.regimenFiscal,
  });

  /// `null` mientras el cliente no exista en el servidor.
  final String? id;
  final String tipoPersona;
  final String tipoDocumento;
  final String? numeroDocumento;
  final String? digitoVerificacion;
  final String? nombres;
  final String? apellidos;
  final String? razonSocial;
  final String? email;
  final String? telefono;
  final String? telefonoAlterno;
  final String? segmento;
  final String? notas;
  final String? regimenFiscal;

  bool get esJuridica => tipoPersona == 'JURIDICA';

  factory FichaDeCliente.desdeJson(Map<String, dynamic> json) => FichaDeCliente(
        id: json['id'] as String?,
        tipoPersona: (json['tipoPersona'] ?? 'NATURAL') as String,
        tipoDocumento: (json['tipoDocumento'] ?? 'CC') as String,
        numeroDocumento: json['numeroDocumento'] as String?,
        digitoVerificacion: json['digitoVerificacion'] as String?,
        nombres: json['nombres'] as String?,
        apellidos: json['apellidos'] as String?,
        razonSocial: json['razonSocial'] as String?,
        email: json['email'] as String?,
        telefono: json['telefono'] as String?,
        telefonoAlterno: json['telefonoAlterno'] as String?,
        segmento: json['segmento'] as String?,
        notas: json['notas'] as String?,
        regimenFiscal: json['regimenFiscal'] as String?,
      );

  /// El cuerpo de `SolicitudDeCliente`. Los vacíos viajan como `null`.
  Map<String, dynamic> aJson() => {
        'tipoPersona': tipoPersona,
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': _oNulo(numeroDocumento),
        'digitoVerificacion': _oNulo(digitoVerificacion),
        'nombres': _oNulo(nombres),
        'apellidos': _oNulo(apellidos),
        'razonSocial': _oNulo(razonSocial),
        'email': _oNulo(email),
        'telefono': _oNulo(telefono),
        'telefonoAlterno': _oNulo(telefonoAlterno),
        'segmento': _oNulo(segmento),
        'notas': _oNulo(notas),
        'regimenFiscal': _oNulo(regimenFiscal),
      };

  static String? _oNulo(String? valor) {
    final limpio = valor?.trim() ?? '';
    return limpio.isEmpty ? null : limpio;
  }
}
