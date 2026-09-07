import 'package:regenta_core/regenta_core.dart';

import 'producto_buscado.dart';

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
  RepositorioDeVentas(this._http);

  final ClienteHttp _http;

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

  /// Arma la venta de una: crea el borrador, agrega las líneas y confirma.
  Future<VentaCreada> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async {
    final cuerpo = <String, dynamic>{'bodegaId': bodegaId};
    if (clienteId != null) cuerpo['clienteId'] = clienteId;
    final creada = VentaCreada.desdeJson(await _http.post<Map<String, dynamic>>(
      '/api/ventas',
      datos: cuerpo,
    ) as Map<String, dynamic>);
    for (final linea in lineas) {
      await _http.post('/api/ventas/${creada.id}/lineas', datos: linea.aJson());
    }
    await _http.post('/api/ventas/${creada.id}/confirmacion');
    return creada;
  }
}
