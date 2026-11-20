/// Un proveedor como lo devuelve `GET /api/compras/proveedores` (HU-166).
///
/// La [deuda] la calcula el backend con las cuentas por pagar abiertas; [superaCupo] solo
/// informa: el cupo lo controla el proveedor (I6 de `docs/dominio/compras.md`).
class ProveedorDelNegocio {
  const ProveedorDelNegocio({
    this.id,
    this.tipoDocumento = 'NIT',
    this.numeroDocumento = '',
    this.razonSocial = '',
    this.nombreComercial,
    this.contactoNombre,
    this.email,
    this.telefono,
    this.direccion,
    this.ciudad,
    this.diasCredito = 0,
    this.cupoCredito = 0,
    this.deuda = 0,
    this.superaCupo = false,
    this.notas,
    this.activo = true,
  });

  /// Nulo mientras el proveedor no se ha guardado.
  final String? id;
  final String tipoDocumento;
  final String numeroDocumento;
  final String razonSocial;
  final String? nombreComercial;
  final String? contactoNombre;
  final String? email;
  final String? telefono;
  final String? direccion;
  final String? ciudad;
  final int diasCredito;
  final num cupoCredito;
  final num deuda;
  final bool superaCupo;
  final String? notas;
  final bool activo;

  /// El nombre con que se le conoce: el comercial si lo tiene, si no la razón social.
  String get nombre {
    final comercial = nombreComercial?.trim() ?? '';
    return comercial.isNotEmpty ? comercial : razonSocial;
  }

  String get documentoLabel => '$tipoDocumento $numeroDocumento';

  bool get tieneDeuda => deuda > 0;

  /// Búsqueda por nombre (razón social o comercial) o por documento, sin tildes ni mayúsculas.
  bool coincideCon(String termino) {
    final t = _normalizar(termino);
    if (t.isEmpty) return true;
    return _normalizar(razonSocial).contains(t) ||
        _normalizar(nombreComercial ?? '').contains(t) ||
        _normalizar(numeroDocumento).contains(t);
  }

  factory ProveedorDelNegocio.desdeJson(Map<String, dynamic> json) => ProveedorDelNegocio(
        id: json['id'] as String?,
        tipoDocumento: (json['tipoDocumento'] ?? 'NIT') as String,
        numeroDocumento: (json['numeroDocumento'] ?? '') as String,
        razonSocial: (json['razonSocial'] ?? '') as String,
        nombreComercial: json['nombreComercial'] as String?,
        contactoNombre: json['contactoNombre'] as String?,
        email: json['email'] as String?,
        telefono: json['telefono'] as String?,
        direccion: json['direccion'] as String?,
        ciudad: json['ciudad'] as String?,
        diasCredito: (json['diasCredito'] as num?)?.toInt() ?? 0,
        cupoCredito: (json['cupoCredito'] as num?) ?? 0,
        deuda: (json['deuda'] as num?) ?? 0,
        superaCupo: (json['superaCupo'] as bool?) ?? false,
        notas: json['notas'] as String?,
        activo: (json['activo'] as bool?) ?? true,
      );

  /// Lo que piden `POST` y `PUT /api/compras/proveedores`.
  Map<String, dynamic> aJson() => {
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': numeroDocumento.trim(),
        'razonSocial': razonSocial.trim(),
        'nombreComercial': _vacioANulo(nombreComercial),
        'contactoNombre': _vacioANulo(contactoNombre),
        'email': _vacioANulo(email),
        'telefono': _vacioANulo(telefono),
        'direccion': _vacioANulo(direccion),
        'ciudad': _vacioANulo(ciudad),
        'diasCredito': diasCredito,
        'cupoCredito': cupoCredito,
        'notas': _vacioANulo(notas),
      };

  static String? _vacioANulo(String? v) {
    final t = v?.trim() ?? '';
    return t.isEmpty ? null : t;
  }

  static String _normalizar(String texto) {
    const con = 'áéíóúüñÁÉÍÓÚÜÑ';
    const sin = 'aeiouunAEIOUUN';
    final buffer = StringBuffer();
    for (final c in texto.trim().split('')) {
      final i = con.indexOf(c);
      buffer.write(i >= 0 ? sin[i] : c);
    }
    return buffer.toString().toLowerCase();
  }
}
