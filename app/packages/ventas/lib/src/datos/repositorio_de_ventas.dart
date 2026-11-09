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
      };
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
}
