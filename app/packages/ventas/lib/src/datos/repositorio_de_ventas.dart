import 'package:regenta_core/regenta_core.dart';
import 'package:uuid/uuid.dart';

import 'producto_buscado.dart';

/// Cómo terminó el cobro (HU-043): la venta quedó creada en el servidor, o
/// quedó guardada en la cola del celular porque no había señal. En los dos
/// casos la venta existe y tiene su id: lo que cambia es dónde está.
class ResultadoDeCobro {
  const ResultadoDeCobro({required this.origenOfflineId, this.venta});

  /// El id con el que el celular creó la venta. Es lo que hace que subirla dos
  /// veces no la duplique: el servidor la reconoce por aquí.
  final String origenOfflineId;

  /// La venta que devolvió el servidor, o null si todavía está en la cola.
  final VentaCreada? venta;

  bool get quedoEnLaCola => venta == null;
}

/// Lo que la venta guardada devuelve: su id y su número.
class VentaCreada {
  const VentaCreada({required this.id, required this.numero});
  final String id;
  final String numero;

  factory VentaCreada.desdeJson(Map<String, dynamic> json) => VentaCreada(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
      );
}

/// Una línea a mandar al backend al armar la venta.
class LineaParaEnviar {
  const LineaParaEnviar({
    required this.productoId,
    required this.sku,
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    this.impuestoPct = 0,
    this.costoUnitario = 0,
    this.descuentoPct = 0,
  });

  final String productoId;
  final String sku;
  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num impuestoPct;
  final num costoUnitario;
  final num descuentoPct;

  Map<String, dynamic> aJson() => {
        'productoId': productoId,
        'sku': sku,
        'nombre': nombre,
        'cantidad': cantidad,
        'precioUnitario': precioUnitario,
        'impuestoPct': impuestoPct,
        'costoUnitario': costoUnitario,
        'descuentoPct': descuentoPct,
      };
}

/// Motivos reconocidos por el backend (HU-042).
enum MotivoDevolucion {
  defectuoso('DEFECTUOSO', 'Producto defectuoso'),
  errorDespacho('ERROR_DESPACHO', 'Error en despacho'),
  arrepentimiento('ARREPENTIMIENTO', 'Arrepentimiento del cliente'),
  garantia('GARANTIA', 'Aplicación de garantía'),
  otro('OTRO', 'Otro motivo');

  const MotivoDevolucion(this.codigo, this.etiqueta);
  final String codigo;
  final String etiqueta;
}

class LineaDevueltaParaEnviar {
  const LineaDevueltaParaEnviar({required this.linea, required this.cantidad});
  final int linea;
  final num cantidad;

  Map<String, dynamic> aJson() => {'linea': linea, 'cantidad': cantidad};
}

class SolicitudDeDevolucionParaEnviar {
  const SolicitudDeDevolucionParaEnviar({
    required this.motivo,
    this.detalle,
    this.reintegraStock = true,
    this.bodegaDestinoId,
    required this.lineas,
  });

  final String motivo;
  final String? detalle;
  final bool reintegraStock;
  final String? bodegaDestinoId;
  final List<LineaDevueltaParaEnviar> lineas;

  Map<String, dynamic> aJson() => {
        'motivo': motivo,
        if (detalle != null && detalle!.trim().isNotEmpty) 'detalle': detalle!.trim(),
        'reintegraStock': reintegraStock,
        if (bodegaDestinoId != null) 'bodegaDestinoId': bodegaDestinoId,
        'lineas': [for (final l in lineas) l.aJson()],
      };
}

class DevolucionRegistrada {
  const DevolucionRegistrada({
    required this.id,
    required this.numero,
    required this.tipo,
    required this.motivo,
    required this.reintegraStock,
    required this.total,
  });

  final String id;
  final String numero;
  final String tipo;
  final String motivo;
  final bool reintegraStock;
  final num total;

  factory DevolucionRegistrada.desdeJson(Map<String, dynamic> json) => DevolucionRegistrada(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        tipo: (json['tipo'] ?? '') as String,
        motivo: (json['motivo'] ?? '') as String,
        reintegraStock: (json['reintegraStock'] ?? false) as bool,
        total: (json['total'] ?? 0) as num,
      );
}

/// Línea de cotización a enviar (HU-044).
class LineaDeCotizacionParaEnviar {
  const LineaDeCotizacionParaEnviar({
    required this.productoId,
    required this.sku,
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    this.unidad,
    this.descuentoPct = 0,
    this.impuestoCodigo,
    this.impuestoPct = 0,
    this.costoUnitario = 0,
  });

  final String productoId;
  final String sku;
  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final String? unidad;
  final num descuentoPct;
  final String? impuestoCodigo;
  final num impuestoPct;
  final num costoUnitario;

  Map<String, dynamic> aJson() => {
        'productoId': productoId,
        'sku': sku,
        'nombre': nombre,
        'cantidad': cantidad,
        'precioUnitario': precioUnitario,
        if (unidad != null) 'unidad': unidad,
        'descuentoPct': descuentoPct,
        if (impuestoCodigo != null) 'impuestoCodigo': impuestoCodigo,
        'impuestoPct': impuestoPct,
        'costoUnitario': costoUnitario,
      };
}

/// Solicitud para generar una cotización (HU-044).
class SolicitudDeCotizacionParaEnviar {
  const SolicitudDeCotizacionParaEnviar({
    this.clienteId,
    this.validaHasta,
    required this.lineas,
  });

  final String? clienteId;
  final String? validaHasta;
  final List<LineaDeCotizacionParaEnviar> lineas;

  Map<String, dynamic> aJson() => {
        if (clienteId != null) 'clienteId': clienteId,
        if (validaHasta != null) 'validaHasta': validaHasta,
        'lineas': [for (final l in lineas) l.aJson()],
      };
}

/// Cotización recibida del backend (HU-044).
class CotizacionDelNegocio {
  const CotizacionDelNegocio({
    required this.id,
    required this.numero,
    required this.estado,
    required this.total,
    this.validaHasta,
    this.ventaId,
    this.clienteId,
  });

  final String id;
  final String numero;
  final String estado;
  final num total;
  final String? validaHasta;
  final String? ventaId;
  final String? clienteId;

  bool get convertida => estado == 'CONVERTIDA' || ventaId != null;

  factory CotizacionDelNegocio.desdeJson(Map<String, dynamic> json) => CotizacionDelNegocio(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        estado: (json['estado'] ?? '') as String,
        total: (json['total'] ?? 0) as num,
        validaHasta: json['validaHasta'] as String?,
        ventaId: json['ventaId'] as String?,
        clienteId: json['clienteId'] as String?,
      );
}

/// Resultado de convertir una cotización en venta (HU-044).
class ResultadoDeConversion {
  const ResultadoDeConversion({
    required this.ventaId,
    required this.numeroVenta,
    required this.preciosDesactualizados,
  });

  final String ventaId;
  final String numeroVenta;
  final bool preciosDesactualizados;

  factory ResultadoDeConversion.desdeJson(Map<String, dynamic> json) => ResultadoDeConversion(
        ventaId: (json['ventaId'] ?? '') as String,
        numeroVenta: (json['numeroVenta'] ?? '') as String,
        preciosDesactualizados: (json['preciosDesactualizados'] ?? false) as bool,
      );
}

/// Una línea leída del backend con su snapshot.
class LineaDeVentaResumen {
  const LineaDeVentaResumen({
    required this.linea,
    required this.productoId,
    required this.sku,
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    required this.total,
  });

  final int linea;
  final String productoId;
  final String sku;
  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num total;

  factory LineaDeVentaResumen.desdeJson(Map<String, dynamic> json) => LineaDeVentaResumen(
        linea: ((json['linea'] ?? 1) as num).toInt(),
        productoId: (json['productoId'] ?? '') as String,
        sku: (json['sku'] ?? '') as String,
        nombre: (json['nombre'] ?? '') as String,
        cantidad: (json['cantidad'] ?? 0) as num,
        precioUnitario: (json['precioUnitario'] ?? 0) as num,
        total: (json['total'] ?? 0) as num,
      );
}

/// Una venta armada contra el flujo real por pasos (no el de una sola llamada
/// de `/offline`): lo que hace falta para seguirla mientras la saga de stock
/// la confirma.
class VentaEnProceso {
  const VentaEnProceso({
    required this.id,
    required this.numero,
    required this.estado,
    required this.total,
    this.notaCliente,
    this.facturada = false,
    this.clienteSnapshot,
    this.lineas = const [],
  });

  final String id;
  final String numero;

  /// BORRADOR | PENDIENTE_STOCK | CONFIRMADA | ANULADA | ... (EstadoVenta).
  final String estado;
  final num total;
  final String? notaCliente;
  final bool facturada;
  final String? clienteSnapshot;
  final List<LineaDeVentaResumen> lineas;

  bool get confirmada => estado == 'CONFIRMADA';
  bool get enEspera => estado == 'PENDIENTE_STOCK';
  bool get anulada => estado == 'ANULADA';
  bool get devuelta => estado == 'DEVUELTA' || estado == 'DEVUELTA_PARCIAL';

  /// Coincidencia difusa multi-palabra sobre número, cliente y nota en cualquier orden.
  bool coincideCon(String termino) => coincideConTokens(
        numero,
        termino,
        camposAdicionales: [
          clienteSnapshot,
          ?notaCliente,
        ],
      );

  factory VentaEnProceso.desdeJson(Map<String, dynamic> json) {
    final rawLineas = json['lineas'] as List<dynamic>? ?? const [];
    return VentaEnProceso(
      id: json['id'] as String,
      numero: (json['numero'] ?? '') as String,
      estado: (json['estado'] ?? '') as String,
      total: (json['total'] ?? 0) as num,
      notaCliente: json['notaCliente'] as String?,
      facturada: (json['facturada'] ?? false) as bool,
      clienteSnapshot: json['clienteSnapshot'] as String?,
      lineas: rawLineas
          .map((e) => LineaDeVentaResumen.desdeJson((e as Map).cast<String, dynamic>()))
          .toList(),
    );
  }
}

/// Un pago a registrar (HU-039): para EFECTIVO va [montoRecibido] y el cambio
/// lo calcula el backend; para el resto va [monto].
class PagoParaEnviar {
  const PagoParaEnviar({
    required this.metodo,
    this.monto,
    this.montoRecibido,
    this.referencia,
    this.franquicia,
  });

  final String metodo;
  final num? monto;
  final num? montoRecibido;
  final String? referencia;
  final String? franquicia;

  Map<String, dynamic> aJson() => {
        'metodo': metodo,
        if (monto != null) 'monto': monto,
        if (montoRecibido != null) 'montoRecibido': montoRecibido,
        if (referencia != null) 'referencia': referencia,
        if (franquicia != null) 'franquicia': franquicia,
      };
}

/// Lo que devuelve registrar un pago: cuánto quedó aplicado, el cambio si
/// sobró efectivo, y si con ese pago la venta ya quedó cubierta del todo.
class ResultadoDeUnPago {
  const ResultadoDeUnPago({
    required this.pagoId,
    required this.metodo,
    required this.montoAplicado,
    required this.cambio,
    required this.saldoPendiente,
    required this.cubierta,
  });

  final String pagoId;
  final String metodo;
  final num montoAplicado;
  final num cambio;
  final num saldoPendiente;
  final bool cubierta;

  factory ResultadoDeUnPago.desdeJson(Map<String, dynamic> json) => ResultadoDeUnPago(
        pagoId: (json['pagoId'] ?? '') as String,
        metodo: (json['metodo'] ?? '') as String,
        montoAplicado: (json['montoAplicado'] ?? 0) as num,
        cambio: (json['cambio'] ?? 0) as num,
        saldoPendiente: (json['saldoPendiente'] ?? 0) as num,
        cubierta: (json['cubierta'] ?? false) as bool,
      );
}

/// Acceso a `servicio-inventario` (búsqueda) y `servicio-ventas` (armado de la
/// venta) desde el POS. Se apoya en el [ClienteHttp] del núcleo.
class RepositorioDeVentas {
  RepositorioDeVentas(this._http, {Uuid? uuid}) : _uuid = uuid ?? const Uuid();

  final ClienteHttp _http;
  final Uuid _uuid;

  /// HU-137: el monto desde el cual hay que identificar al comprador, o `null`
  /// si el negocio no lo configuró. Se le pregunta a Ventas, que tiene su
  /// copia: la configuración del negocio pide un permiso que quien cobra no
  /// suele tener.
  Future<num?> montoParaIdentificarComprador() async {
    final cuerpo = await _http.get<dynamic>('/api/ventas/reglas-de-cobro');
    if (cuerpo is Map) {
      if (cuerpo['politicaStockNegativo'] is bool) {
        ConfiguradorDeStock.politicaStockNegativoHabilitada =
            cuerpo['politicaStockNegativo'] as bool;
      }
      return cuerpo['montoIdentificarComprador'] as num?;
    }
    return null;
  }

  Future<List<ProductoBuscado>> buscar(String termino) async {
    final q = termino.trim();
    if (q.length < 3) return const [];
    final cuerpo = await _http.get<dynamic>(
      '/api/inventario/productos/buscar',
      query: {'q': q},
    );
    if (cuerpo is Map) {
      final lista = (cuerpo['productos'] ?? cuerpo['content'] ?? const []) as List<dynamic>;
      return lista
          .map((e) => ProductoBuscado.desdeJson((e as Map).cast<String, dynamic>()))
          .toList();
    } else if (cuerpo is List) {
      return cuerpo
          .map((e) => ProductoBuscado.desdeJson((e as Map).cast<String, dynamic>()))
          .toList();
    }
    return const [];
  }

  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    final cuerpo = await _http.get<dynamic>(
      '/api/inventario/productos/codigo/${Uri.encodeComponent(codigo.trim())}',
    );
    return CodigoResuelto.desdeJson((cuerpo as Map).cast<String, dynamic>());
  }

  Future<ProductoBuscado> verProducto(String productoId) async {
    final cuerpo = await _http.get<dynamic>(
      '/api/inventario/productos/buscar',
      query: {'q': productoId},
    );
    List<dynamic> lista = const [];
    if (cuerpo is Map) {
      lista = (cuerpo['productos'] ?? cuerpo['content'] ?? const []) as List<dynamic>;
    } else if (cuerpo is List) {
      lista = cuerpo;
    }
    return ProductoBuscado.desdeJson((lista.first as Map).cast<String, dynamic>());
  }

  /// Cobra la venta entera de una sola llamada (HU-043).
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async {
    final origenOfflineId = _uuid.v4();
    final cuerpo = <String, dynamic>{
      'origenOfflineId': origenOfflineId,
      'ocurridoEn': DateTime.now().toUtc().toIso8601String(),
      'bodegaId': bodegaId,
      'lineas': [for (final linea in lineas) linea.aJson()],
    };
    if (clienteId != null) cuerpo['clienteId'] = clienteId;

    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/ventas/offline',
      datos: cuerpo,
      encolable: true,
    );
    if (respuesta is Encolado) {
      return ResultadoDeCobro(origenOfflineId: origenOfflineId);
    }
    return ResultadoDeCobro(
      origenOfflineId: origenOfflineId,
      venta: VentaCreada.desdeJson(respuesta as Map<String, dynamic>),
    );
  }

  // ---------------------------------------------------------------------
  // El flujo real por pasos (Etapa 0 del cobro): crear borrador, agregar
  // líneas, confirmar, pagar y cerrar. Se usa cuando hay conexión; sin
  // conexión se cae a [confirmarVenta] de arriba, que es seguro sin señal.
  // ---------------------------------------------------------------------

  Future<VentaEnProceso> crearBorrador({
    required String bodegaId,
    String? clienteId,
    String? notaCliente,
  }) async {
    final cuerpo = <String, dynamic>{'bodegaId': bodegaId};
    if (clienteId != null) cuerpo['clienteId'] = clienteId;
    if (notaCliente != null && notaCliente.trim().isNotEmpty) {
      cuerpo['notaCliente'] = notaCliente.trim();
    }
    final respuesta = await _http.post<Map<String, dynamic>>('/api/ventas', datos: cuerpo);
    return VentaEnProceso.desdeJson(respuesta as Map<String, dynamic>);
  }

  Future<void> agregarLineaAlBorrador(String ventaId, LineaParaEnviar linea) async {
    await _http.post<Map<String, dynamic>>('/api/ventas/$ventaId/lineas', datos: linea.aJson());
  }

  Future<void> confirmarBorrador(String ventaId) async {
    await _http.post<void>('/api/ventas/$ventaId/confirmacion');
  }

  Future<VentaEnProceso> verVenta(String ventaId) async {
    final respuesta = await _http.get<Map<String, dynamic>>('/api/ventas/$ventaId');
    return VentaEnProceso.desdeJson(respuesta);
  }

  /// La confirmación arranca una saga asíncrona con Inventario (HU-038): la
  /// venta pasa por `PENDIENTE_STOCK` antes de llegar a `CONFIRMADA` (o
  /// volver a `BORRADOR` si no hay stock). Se sondea hasta que salga de ahí.
  Future<VentaEnProceso> esperarConfirmacion(
    String ventaId, {
    int intentos = 12,
    Duration intervalo = const Duration(milliseconds: 300),
  }) async {
    for (var i = 0; i < intentos; i++) {
      final venta = await verVenta(ventaId);
      if (!venta.enEspera) return venta;
      await Future.delayed(intervalo);
    }
    return verVenta(ventaId);
  }

  /// HU-040: confirma la venta a crédito (exige cliente ya asignado al
  /// borrador). Deja la misma saga de HU-038 en marcha: también hay que
  /// sondear con [esperarConfirmacion] después de llamarla.
  Future<void> confirmarACredito(String ventaId, {bool autorizado = false}) async {
    final omitirCupo = ConfiguradorDeCredito.creditoSinCupoHabilitado;
    await _http.post<Map<String, dynamic>>(
      '/api/ventas/$ventaId/credito',
      datos: {
        'autorizado': autorizado,
        if (omitirCupo) 'sinCupo': true,
      },
    );
  }

  Future<ResultadoDeUnPago> registrarPago(String ventaId, PagoParaEnviar pago) async {
    final respuesta =
        await _http.post<Map<String, dynamic>>('/api/ventas/$ventaId/pagos', datos: pago.aJson());
    return ResultadoDeUnPago.desdeJson(respuesta as Map<String, dynamic>);
  }

  Future<void> cerrarVenta(String ventaId) async {
    await _http.post<void>('/api/ventas/$ventaId/cierre');
  }

  /// Las "ventas en espera" que el cajero dejó a medio armar, para recomponer
  /// las pestañas del POS al volver a abrir.
  Future<List<VentaEnProceso>> listarBorradoresAbiertos(String bodegaId) async {
    final respuesta = await _http.get<dynamic>(
      '/api/ventas/borradores',
      query: {'bodegaId': bodegaId},
    );
    final lista = respuesta is List ? respuesta : const [];
    return lista
        .map((e) => VentaEnProceso.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// HU-041: anula una venta confirmada enviando el motivo obligatorio.
  /// Reintegra automáticamente el stock reservado en el inventario.
  Future<void> anularVenta(String ventaId, String motivo) async {
    await _http.post<void>(
      '/api/ventas/$ventaId/anulacion',
      datos: {'motivo': motivo.trim()},
    );
  }

  /// HU-042: registra una devolución total o parcial contra la venta.
  Future<DevolucionRegistrada> registrarDevolucion(
    String ventaId,
    SolicitudDeDevolucionParaEnviar solicitud,
  ) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/ventas/$ventaId/devoluciones',
      datos: solicitud.aJson(),
    );
    return DevolucionRegistrada.desdeJson(respuesta as Map<String, dynamic>);
  }

  /// HU-044: crea una cotización con las líneas proporcionadas.
  Future<CotizacionDelNegocio> crearCotizacion(
    SolicitudDeCotizacionParaEnviar solicitud,
  ) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/cotizaciones',
      datos: solicitud.aJson(),
    );
    return CotizacionDelNegocio.desdeJson(respuesta as Map<String, dynamic>);
  }

  /// HU-044: lista las cotizaciones registradas para el negocio.
  Future<List<CotizacionDelNegocio>> listarCotizaciones() async {
    final respuesta = await _http.get<dynamic>('/api/cotizaciones');
    final lista = respuesta is List ? respuesta : const [];
    return lista
        .map((e) => CotizacionDelNegocio.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// HU-044: convierte una cotización en una venta en borrador para la bodega dada.
  Future<ResultadoDeConversion> convertirCotizacion(
    String cotizacionId,
    String bodegaId,
  ) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/cotizaciones/$cotizacionId/conversion',
      datos: {'bodegaId': bodegaId},
    );
    return ResultadoDeConversion.desdeJson(respuesta as Map<String, dynamic>);
  }

  /// Lista o busca ventas por consecutivo o nombre de cliente.
  Future<List<VentaEnProceso>> buscarVentas({String? q}) async {
    final respuesta = await _http.get<dynamic>(
      '/api/ventas',
      query: {
        if (q != null && q.trim().isNotEmpty) 'q': q.trim(),
      },
    );
    final lista = respuesta is List ? respuesta : const [];
    return lista
        .map((e) => VentaEnProceso.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }
}
