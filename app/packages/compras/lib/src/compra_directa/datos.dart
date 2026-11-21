import 'package:regenta_core/regenta_core.dart';

import '../proveedores/datos/producto_del_proveedor.dart';
import '../proveedores/datos/proveedor_del_negocio.dart';

/// Una bodega del negocio, para elegir a dónde entra la mercancía.
class BodegaParaComprar {
  const BodegaParaComprar({required this.id, required this.nombre, this.esDefault = false});

  final String id;
  final String nombre;
  final bool esDefault;

  factory BodegaParaComprar.desdeJson(Map<String, dynamic> json) => BodegaParaComprar(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? json['codigo'] ?? '') as String,
        esDefault: (json['esDefault'] as bool?) ?? false,
      );
}

/// Lo que se necesita de un producto para agregarlo a la factura (HU-167 C3): la tarifa de IVA y
/// la regla de lote del catálogo, y el costo y código sugeridos por este proveedor.
class ProductoParaComprar {
  const ProductoParaComprar({
    required this.productoId,
    required this.nombre,
    this.sku,
    this.tarifaIva = 'IVA_19',
    this.impuestoPct = 0,
    this.manejaLotes = false,
    this.perecedero = false,
    this.costoSugerido,
    this.codigoProveedor,
  });

  final String productoId;
  final String nombre;
  final String? sku;
  final String tarifaIva;
  final num impuestoPct;
  final bool manejaLotes;
  final bool perecedero;
  final num? costoSugerido;
  final String? codigoProveedor;

  bool get exigeLote => manejaLotes || perecedero;

  factory ProductoParaComprar.desdeJson(Map<String, dynamic> json) => ProductoParaComprar(
        productoId: json['productoId'] as String,
        nombre: (json['nombre'] ?? '') as String,
        sku: json['sku'] as String?,
        tarifaIva: (json['tarifaIva'] ?? 'IVA_19') as String,
        impuestoPct: (json['impuestoPct'] as num?) ?? 0,
        manejaLotes: (json['manejaLotes'] as bool?) ?? false,
        perecedero: (json['perecedero'] as bool?) ?? false,
        costoSugerido: json['costoSugerido'] as num?,
        codigoProveedor: json['codigoProveedor'] as String?,
      );
}

/// Lo que el servidor confirmó: el número de la recepción y lo que quedó debiendo.
class CompraRegistrada {
  const CompraRegistrada({required this.numero, required this.total, this.avisos = const []});

  final String numero;
  final num total;

  /// HU-169 C4 · Lo que no impidió registrar pero hay que saber (por ejemplo, el cupo superado).
  final List<String> avisos;
}

/// Acceso a la compra directa de `compras` (HU-167): se arma en borrador y se confirma.
class RepositorioDeCompraDirecta {
  RepositorioDeCompraDirecta(this._http);

  final ClienteHttp _http;

  Future<List<ProveedorDelNegocio>> proveedoresActivos() async {
    final res = await _http.get<List<dynamic>>('/api/compras/proveedores');
    return res.map((e) => ProveedorDelNegocio.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  Future<List<BodegaParaComprar>> bodegas() async {
    final res = await _http.get<List<dynamic>>('/api/inventario/bodegas');
    return res
        .map((e) => (e as Map).cast<String, dynamic>())
        .where((b) => (b['activa'] as bool?) ?? true)
        .map(BodegaParaComprar.desdeJson)
        .toList();
  }

  Future<List<ProductoEncontrado>> buscarProductos(String termino) async {
    final dynamic res =
        await _http.get<dynamic>('/api/inventario/productos/buscar', query: {'q': termino.trim()});
    final lista = (res is Map ? res['productos'] : res) as List<dynamic>? ?? const [];
    return lista.map((e) => ProductoEncontrado.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  /// HU-169 / HU-145 · Si el usuario tiene una caja abierta, para ofrecer «Pagar con dinero de la caja».
  /// Cualquier fallo (sin caja, sin permiso, sin señal) cuenta como que no la tiene.
  Future<bool> tengoCajaAbierta() async {
    try {
      await _http.get<dynamic>('/api/caja/sesiones/mia');
      return true;
    } on ErrorDeApi {
      return false;
    }
  }

  Future<ProductoParaComprar> paraComprar(String proveedorId, String productoId) async {
    final res = await _http.get<Map<String, dynamic>>(
        '/api/compras/proveedores/$proveedorId/para-comprar/$productoId');
    return ProductoParaComprar.desdeJson(res);
  }

  /// Crea la compra en borrador y la confirma. Si la confirmación falla, el borrador queda en el
  /// servidor y se corrige con [corregir] usando el id que trae el error.
  Future<CompraRegistrada> registrar(Map<String, dynamic> solicitud, {String? borradorId}) async {
    final dynamic creada = borradorId == null
        ? await _http.post<dynamic>('/api/compras/recepciones/directas', datos: solicitud)
        : await _http.put<dynamic>('/api/compras/recepciones/$borradorId', datos: solicitud);
    final id = ((creada as Map)['id']) as String;
    try {
      final dynamic confirmada = await _http.post<dynamic>('/api/compras/recepciones/$id/confirmacion');
      final mapa = (confirmada as Map).cast<String, dynamic>();
      return CompraRegistrada(
        numero: mapa['numero'] as String,
        total: (mapa['total'] as num?) ?? 0,
        avisos: [for (final a in (mapa['avisos'] as List<dynamic>? ?? const [])) a.toString()],
      );
    } on ErrorDeApi catch (fallo) {
      throw ConfirmacionFallida(id, fallo);
    }
  }
}

/// La compra quedó en borrador pero no se pudo confirmar: se reintenta corrigiendo ese borrador.
class ConfirmacionFallida implements Exception {
  const ConfirmacionFallida(this.borradorId, this.causa);

  final String borradorId;
  final ErrorDeApi causa;
}
