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

  /// Lista las categorías del negocio para selector o filtrado.
  Future<List<Map<String, dynamic>>> listarCategorias() async {
    try {
      final res = await _http.get<List<dynamic>>('/api/inventario/categorias');
      return res.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    } catch (_) {
      return const [];
    }
  }

  /// Crea un nuevo producto básico en el inventario.
  Future<Map<String, dynamic>> crearProducto({
    required String sku,
    required String nombre,
    required String categoriaId,
    required String unidadMedidaId,
    String? codigoBarras,
    num? precioVenta,
  }) async {
    final res = await _http.post<Map<String, dynamic>>(
      '/api/inventario/productos',
      datos: {
        'sku': sku,
        'nombre': nombre,
        'categoriaId': categoriaId,
        'unidadMedidaId': unidadMedidaId,
        if (codigoBarras != null && codigoBarras.isNotEmpty) 'codigoBarras': codigoBarras,
        if (precioVenta != null) 'precioVenta': precioVenta,
        'controlaStock': true,
      },
    );
    return res;
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
}
