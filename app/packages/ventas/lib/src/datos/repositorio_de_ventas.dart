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
  });

  final String productoId;
  final String sku;
  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num impuestoPct;
  final num costoUnitario;

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

  Future<List<ProductoBuscado>> buscar(String termino) async {
    final q = termino.trim();
    if (q.length < 3) return const [];
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/buscar',
      query: {'q': q},
    );
    return ((cuerpo['productos'] ?? const []) as List<dynamic>)
        .map((e) => ProductoBuscado.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/codigo/${Uri.encodeComponent(codigo.trim())}',
    );
    return CodigoResuelto.desdeJson(cuerpo);
  }

  Future<ProductoBuscado> verProducto(String productoId) async {
    // La búsqueda por SKU exacto es la vía más directa que hay hoy; el POS la
    // usa para hidratar un producto escaneado.
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/buscar',
      query: {'q': productoId},
    );
    final lista = ((cuerpo['productos'] ?? const []) as List<dynamic>);
    return ProductoBuscado.desdeJson((lista.first as Map).cast<String, dynamic>());
  }

  /// Cobra la venta entera de una sola llamada (HU-043).
  ///
  /// Va siempre por el mismo camino, haya señal o no: el id de la venta lo
  /// pone el celular, así que si no hay red la operación queda en la cola y
  /// sube sola después, y si la subida se reintenta el servidor la reconoce
  /// por ese id y devuelve la que ya creó en vez de duplicarla. Armarla en
  /// tres llamadas —borrador, líneas, confirmación— dejaría ventas a medias
  /// justo cuando la conexión es mala, que es cuando más importa.
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
