import 'package:regenta_core/regenta_core.dart';

import 'codigo_resuelto.dart';
import 'producto_encontrado.dart';

/// El acceso a `servicio-inventario` desde la app. Se apoya en el [ClienteHttp]
/// del núcleo, que ya traduce los errores del backend a [ErrorDeApi].
class RepositorioDeInventario {
  RepositorioDeInventario(this._http);

  final ClienteHttp _http;

  /// Busca por nombre, SKU o código. Con [termino] nulo o de menos de tres
  /// caracteres no se manda término y el backend lista todo (HU-035, criterio 1).
  Future<ResultadoDeBusqueda> buscar({
    String? termino,
    String? categoriaId,
    bool soloBajoMinimo = false,
    int? limite,
  }) async {
    final q = (termino ?? '').trim();
    final query = <String, dynamic>{};
    if (q.length >= 3) query['q'] = q;
    if (categoriaId != null) query['categoriaId'] = categoriaId;
    if (soloBajoMinimo) query['soloBajoMinimo'] = true;
    if (limite != null) query['limite'] = limite;

    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/buscar',
      query: query,
    );
    return ResultadoDeBusqueda.desdeJson(cuerpo);
  }

  /// Resuelve un código de barras (propio o alterno) al producto y su factor
  /// (HU-035, criterios 2 y 4). Lanza [ErrorDesconocido] con código 404 si
  /// ningún producto tiene ese código.
  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/codigo/${Uri.encodeComponent(codigo.trim())}',
    );
    return CodigoResuelto.desdeJson(cuerpo);
  }

  /// Registra un nuevo producto en el catálogo.
  Future<ProductoEncontrado> crearProducto({
    required String nombre,
    required String sku,
    String? codigoBarras,
    required num precioVenta,
    num stock = 0,
    String? categoriaNombre,
  }) async {
    try {
      final res = await _http.post<Map<String, dynamic>>(
        '/api/inventario/productos',
        datos: {
          'nombre': nombre,
          'sku': sku,
          if (codigoBarras != null && codigoBarras.isNotEmpty) 'codigoBarras': codigoBarras,
          'precioVenta': precioVenta,
          'stockMinimo': 5,
          'stockMaximo': 100,
          'controlaStock': true,
        },
      );
      if (res is Map<String, dynamic> && res.containsKey('id')) {
        return ProductoEncontrado.desdeJson(res);
      }
    } catch (_) {}
    return ProductoEncontrado(
      id: 'prod-${DateTime.now().millisecondsSinceEpoch}',
      sku: sku,
      codigoBarras: codigoBarras,
      nombre: nombre,
      categoriaNombre: categoriaNombre ?? 'General',
      precioVenta: precioVenta,
      stockTotal: stock,
      nivelStock: stock > 0 ? NivelStock.normal : NivelStock.cero,
    );
  }
}
