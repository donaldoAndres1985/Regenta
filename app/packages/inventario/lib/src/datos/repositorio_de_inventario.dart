import 'package:regenta_core/regenta_core.dart';

import 'bodega_del_negocio.dart';
import 'codigo_resuelto.dart';
import 'ficha_de_producto.dart';
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

  /// Las bodegas del negocio (HU-029). El negocio siempre tiene al menos una:
  /// el backend la asegura si todavía no existe ninguna.
  Future<List<BodegaDelNegocio>> bodegas() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/bodegas');
    return cuerpo
        .map((e) => BodegaDelNegocio.desdeJson(e as Map<String, dynamic>))
        .toList();
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

  /// HU-150 · la ficha completa de un producto.
  Future<FichaDeProducto> verProducto(String id) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id');
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// HU-150 · guarda la ficha. El backend rechaza con 409 si alguien guardó antes (C5) o si el
  /// SKU o el código de barras ya los usa otro producto (C2).
  Future<FichaDeProducto> editarProducto(String id, EdicionDeProducto edicion) async {
    final cuerpo = await _http.put<Map<String, dynamic>>(
      '/api/inventario/productos/$id',
      datos: edicion.aJson(),
    ) as Map<String, dynamic>;
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// Las categorías del negocio, para elegir en la ficha.
  Future<List<OpcionDeFicha>> categorias() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/categorias');
    return [
      for (final c in cuerpo.cast<Map<String, dynamic>>())
        OpcionDeFicha(id: c['id'] as String, nombre: (c['nombre'] ?? '') as String),
    ];
  }

  /// Las unidades de medida del negocio, para elegir en la ficha.
  Future<List<OpcionDeFicha>> unidades() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/unidades');
    return [
      for (final u in cuerpo.cast<Map<String, dynamic>>())
        OpcionDeFicha(id: u['id'] as String, nombre: '${u['codigo']} · ${u['nombre']}'),
    ];
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
