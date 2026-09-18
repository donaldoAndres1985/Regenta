/// Quién compra en esta venta (HU-113).
///
/// Es una copia deliberada de lo que `packages/clientes` sabe de un cliente:
/// un módulo no depende de otro módulo, solo del núcleo, y aquí hace falta
/// mucho menos —nombre y documento— porque es lo único que va a salir en la
/// factura.
class ClienteDeLaVenta {
  const ClienteDeLaVenta({
    required this.id,
    required this.nombre,
    required this.tipoDocumento,
    required this.numeroDocumento,
    this.digitoVerificacion,
    this.condicion,
  });

  final String id;
  final String nombre;
  final String tipoDocumento;
  final String numeroDocumento;
  final String? digitoVerificacion;

  /// Cómo paga, tal como lo pinta la lista: «contado», «crédito 30 d»,
  /// «cartera vencida». Es informativo: quien decide es el backend al cobrar.
  final String? condicion;

  /// `NIT 900.412.883-1`, como se lee en el mostrador.
  String get documento {
    final agrupado = _conPuntos(numeroDocumento);
    final dv = digitoVerificacion == null || digitoVerificacion!.isEmpty
        ? ''
        : '-$digitoVerificacion';
    return '$tipoDocumento $agrupado$dv';
  }

  /// Las iniciales del avatar. Dos letras, las del primer y último término.
  String get iniciales {
    final partes = nombre.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    if (partes.isEmpty) return '?';
    if (partes.length == 1) return partes.first.substring(0, 1).toUpperCase();
    return (partes.first.substring(0, 1) + partes.last.substring(0, 1)).toUpperCase();
  }

  bool get tieneCarteraVencida => (condicion ?? '').toLowerCase().contains('vencida');

  /// Busca por nombre o por documento, sin puntos: en el mostrador el NIT se
  /// escribe de las dos formas.
  bool coincideCon(String termino) {
    final t = termino.trim().toLowerCase();
    if (t.isEmpty) return true;
    final soloDigitos = t.replaceAll(RegExp('[^0-9]'), '');
    return nombre.toLowerCase().contains(t) ||
        (soloDigitos.isNotEmpty && numeroDocumento.contains(soloDigitos));
  }

  factory ClienteDeLaVenta.desdeJson(Map<String, dynamic> json) => ClienteDeLaVenta(
        id: (json['id'] ?? '') as String,
        nombre: (json['nombreDisplay'] ?? json['nombre'] ?? '') as String,
        tipoDocumento: (json['tipoDocumento'] ?? '') as String,
        numeroDocumento: (json['numeroDocumento'] ?? '') as String,
        digitoVerificacion: json['digitoVerificacion'] as String?,
        condicion: _condicionDe(json),
      );

  Map<String, dynamic> aJson() => {
        'id': id,
        'nombreDisplay': nombre,
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': numeroDocumento,
        if (digitoVerificacion != null) 'digitoVerificacion': digitoVerificacion,
        if (condicion != null) 'condicion': condicion,
      };

  static String _condicionDe(Map<String, dynamic> json) {
    if (json['carteraVencida'] == true) return 'cartera vencida';
    if (json['creditoHabilitado'] == true) {
      final dias = json['diasCredito'];
      return dias == null ? 'crédito' : 'crédito $dias d';
    }
    return 'contado';
  }

  static String _conPuntos(String numero) {
    final digitos = numero.replaceAll(RegExp('[^0-9]'), '');
    if (digitos.length < 4) return numero;
    final buffer = StringBuffer();
    for (var i = 0; i < digitos.length; i++) {
      if (i > 0 && (digitos.length - i) % 3 == 0) buffer.write('.');
      buffer.write(digitos[i]);
    }
    return buffer.toString();
  }
}

/// Lo mínimo para facturar (HU-114). El `id` lo pone el dispositivo para que
/// crear sin señal y reintentar la subida no termine en dos clientes.
class ClienteNuevo {
  const ClienteNuevo({
    required this.id,
    required this.tipoPersona,
    required this.tipoDocumento,
    required this.numeroDocumento,
    required this.nombre,
    required this.email,
    this.digitoVerificacion,
    this.telefono,
  });

  final String id;
  final String tipoPersona;
  final String tipoDocumento;
  final String numeroDocumento;
  final String nombre;
  final String email;
  final String? digitoVerificacion;
  final String? telefono;

  bool get esJuridica => tipoPersona == 'JURIDICA';

  Map<String, dynamic> aJson() => {
        'id': id,
        'tipoPersona': tipoPersona,
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': numeroDocumento,
        if (digitoVerificacion != null) 'digitoVerificacion': digitoVerificacion,
        if (esJuridica) 'razonSocial': nombre,
        if (!esJuridica) 'nombres': nombre,
        'email': email,
        if (telefono != null && telefono!.isNotEmpty) 'telefono': telefono,
      };
}

/// El resultado de crear: o llegó al servidor, o quedó en la cola.
class ResultadoDeAlta {
  const ResultadoDeAlta({required this.id, this.cliente, this.quedoEnLaCola = false});

  final String id;
  final ClienteDeLaVenta? cliente;
  final bool quedoEnLaCola;
}

/// El dígito de verificación del NIT, con la fórmula de la DIAN.
///
/// Está también en el backend, y la repetición es a propósito: el criterio 3
/// de HU-114 pide que se vea mientras se escribe, y crear un cliente sin señal
/// tiene que poder calcularlo sin preguntarle a nadie. El servidor lo vuelve a
/// calcular y el suyo es el que manda al guardar.
String? digitoDeVerificacion(String numeroDocumento) {
  const pesos = [3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71];
  final digitos = numeroDocumento.replaceAll(RegExp('[^0-9]'), '');
  if (digitos.isEmpty || digitos.length > pesos.length) return null;
  var suma = 0;
  for (var i = 0; i < digitos.length; i++) {
    final digito = int.parse(digitos[digitos.length - 1 - i]);
    suma += digito * pesos[i];
  }
  final resto = suma % 11;
  return (resto > 1 ? 11 - resto : resto).toString();
}
