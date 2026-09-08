/// El estado del documento electrónico frente a la DIAN.
enum EstadoFacturaVista {
  borrador,
  generada,
  firmada,
  enviada,
  aceptada,
  rechazada,
  anulada,
  contingencia,
}

EstadoFacturaVista _estado(String? valor) => switch ((valor ?? '').toUpperCase()) {
      'GENERADA' => EstadoFacturaVista.generada,
      'FIRMADA' => EstadoFacturaVista.firmada,
      'ENVIADA' => EstadoFacturaVista.enviada,
      'ACEPTADA' => EstadoFacturaVista.aceptada,
      'RECHAZADA' => EstadoFacturaVista.rechazada,
      'ANULADA' => EstadoFacturaVista.anulada,
      'CONTINGENCIA' => EstadoFacturaVista.contingencia,
      _ => EstadoFacturaVista.borrador,
    };

/// Una línea de la factura, como se ve.
class LineaVista {
  const LineaVista({
    required this.descripcion,
    this.codigo,
    this.cantidad = 0,
    this.precioUnitario = 0,
    this.total = 0,
  });

  final String descripcion;
  final String? codigo;
  final num cantidad;
  final num precioUnitario;
  final num total;

  factory LineaVista.desdeJson(Map<String, dynamic> j) => LineaVista(
        descripcion: (j['descripcion'] ?? '') as String,
        codigo: j['codigo'] as String?,
        cantidad: (j['cantidad'] ?? 0) as num,
        precioUnitario: (j['precioUnitario'] ?? 0) as num,
        total: (j['total'] ?? 0) as num,
      );
}

/// Un impuesto o retención de la factura.
class ImpuestoVista {
  const ImpuestoVista({
    required this.nombre,
    this.porcentaje = 0,
    this.valor = 0,
    this.esRetencion = false,
  });

  final String nombre;
  final num porcentaje;
  final num valor;
  final bool esRetencion;

  factory ImpuestoVista.desdeJson(Map<String, dynamic> j) => ImpuestoVista(
        nombre: (j['nombre'] ?? '') as String,
        porcentaje: (j['porcentaje'] ?? 0) as num,
        valor: (j['valor'] ?? 0) as num,
        esRetencion: (j['esRetencion'] ?? false) as bool,
      );
}

/// Un paso de la trazabilidad DIAN (una fila de `transmisiones`).
class PasoDeTrazabilidad {
  const PasoDeTrazabilidad({required this.evento, this.mensaje, this.codigoError, this.ocurridoEn});

  final String evento;
  final String? mensaje;
  final String? codigoError;
  final String? ocurridoEn;

  factory PasoDeTrazabilidad.desdeJson(Map<String, dynamic> j) => PasoDeTrazabilidad(
        evento: (j['evento'] ?? '') as String,
        mensaje: j['mensaje'] as String?,
        codigoError: j['codigo_error'] as String?,
        ocurridoEn: j['ocurrido_en']?.toString(),
      );
}

/// La factura para consultarla y enviarla (HU-058). El emisor y el adquiriente
/// vienen del snapshot congelado: no cambian si mañana editan al cliente.
class FacturaVista {
  const FacturaVista({
    required this.id,
    required this.numeroCompleto,
    required this.tipoDocumento,
    required this.estado,
    required this.fechaEmision,
    required this.emisor,
    required this.cliente,
    required this.lineas,
    required this.impuestos,
    this.subtotal = 0,
    this.descuentoTotal = 0,
    this.baseGravable = 0,
    this.impuestosTotal = 0,
    this.retencionesTotal = 0,
    this.total = 0,
    this.cufe,
    this.codigoRechazo,
    this.mensajeRechazo,
    this.trazabilidad = const [],
  });

  final String id;
  final String numeroCompleto;
  final String tipoDocumento;
  final EstadoFacturaVista estado;
  final String fechaEmision;
  final Map<String, dynamic> emisor;
  final Map<String, dynamic> cliente;
  final List<LineaVista> lineas;
  final List<ImpuestoVista> impuestos;
  final num subtotal;
  final num descuentoTotal;
  final num baseGravable;
  final num impuestosTotal;
  final num retencionesTotal;
  final num total;
  final String? cufe;
  final String? codigoRechazo;
  final String? mensajeRechazo;
  final List<PasoDeTrazabilidad> trazabilidad;

  bool get aceptada => estado == EstadoFacturaVista.aceptada;
  bool get rechazada => estado == EstadoFacturaVista.rechazada;

  /// La URL del QR de la DIAN se arma con el CUFE.
  String? get urlDelQr => cufe == null
      ? null
      : 'https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=$cufe';

  String get nombreEmisor => _nombre(emisor);
  String get nombreAdquiriente => _nombre(cliente);

  String? get documentoAdquiriente {
    final tipo = cliente['tipo_documento'] ?? cliente['tipoDocumento'];
    final numero = cliente['numero_documento'] ?? cliente['numeroDocumento'];
    if (numero == null) return null;
    return tipo == null ? '$numero' : '$tipo $numero';
  }

  static String _nombre(Map<String, dynamic> m) {
    final razon = m['razon_social'] ?? m['razonSocial'];
    if (razon != null && '$razon'.isNotEmpty) return '$razon';
    final nombres = m['nombres'] ?? '';
    final apellidos = m['apellidos'] ?? '';
    final compuesto = '$nombres $apellidos'.trim();
    return compuesto.isEmpty ? 'Consumidor final' : compuesto;
  }

  factory FacturaVista.desdeJson(Map<String, dynamic> j) => FacturaVista(
        id: j['id'] as String,
        numeroCompleto: (j['numeroCompleto'] ?? '') as String,
        tipoDocumento: (j['tipoDocumento'] ?? 'FACTURA_VENTA') as String,
        estado: _estado(j['estado'] as String?),
        fechaEmision: j['fechaEmision']?.toString() ?? '',
        emisor: ((j['emisor'] ?? const {}) as Map).cast<String, dynamic>(),
        cliente: ((j['cliente'] ?? const {}) as Map).cast<String, dynamic>(),
        lineas: ((j['lineas'] ?? const []) as List)
            .map((e) => LineaVista.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
        impuestos: ((j['impuestos'] ?? const []) as List)
            .map((e) => ImpuestoVista.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
        subtotal: (j['subtotal'] ?? 0) as num,
        descuentoTotal: (j['descuentoTotal'] ?? 0) as num,
        baseGravable: (j['baseGravable'] ?? 0) as num,
        impuestosTotal: (j['impuestosTotal'] ?? 0) as num,
        retencionesTotal: (j['retencionesTotal'] ?? 0) as num,
        total: (j['total'] ?? 0) as num,
        cufe: j['cufe'] as String?,
        codigoRechazo: j['codigoRechazo'] as String?,
        mensajeRechazo: j['mensajeRechazo'] as String?,
      );

  FacturaVista conTrazabilidad(List<PasoDeTrazabilidad> pasos) => FacturaVista(
        id: id,
        numeroCompleto: numeroCompleto,
        tipoDocumento: tipoDocumento,
        estado: estado,
        fechaEmision: fechaEmision,
        emisor: emisor,
        cliente: cliente,
        lineas: lineas,
        impuestos: impuestos,
        subtotal: subtotal,
        descuentoTotal: descuentoTotal,
        baseGravable: baseGravable,
        impuestosTotal: impuestosTotal,
        retencionesTotal: retencionesTotal,
        total: total,
        cufe: cufe,
        codigoRechazo: codigoRechazo,
        mensajeRechazo: mensajeRechazo,
        trazabilidad: pasos,
      );
}
