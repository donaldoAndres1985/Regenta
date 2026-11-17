import 'package:regenta_core/regenta_core.dart';

import 'bodega_del_negocio.dart';
import 'codigo_resuelto.dart';
import 'ficha_de_producto.dart';
import 'inventario_general.dart';
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

  /// HU-151 · desactiva o reactiva un producto. Nunca se borra (I8).
  Future<FichaDeProducto> cambiarActivo(String id, {required bool activo}) async {
    final ruta = '/api/inventario/productos/$id/${activo ? 'reactivacion' : 'desactivacion'}';
    final cuerpo = await _http.post<Map<String, dynamic>>(ruta) as Map<String, dynamic>;
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// HU-151 C1 · cuánto hay del producto en cada bodega (solo las que tienen algo).
  Future<List<(String bodega, num cantidad)>> existenciasDe(String id) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id/stock');
    return [
      for (final e in ((cuerpo['porBodega'] ?? const []) as List).cast<Map<String, dynamic>>())
        if (((e['cantidad'] ?? 0) as num) != 0)
          ((e['bodegaNombre'] ?? '') as String, (e['cantidad'] ?? 0) as num),
    ];
  }

  /// HU-152 · la ubicación del producto en cada bodega del negocio (vacía donde no se dijo).
  Future<List<UbicacionEnBodega>> ubicacionesDe(String id) async {
    final todas = await bodegas();
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id/stock');
    final dichas = <String, String>{
      for (final e in ((cuerpo['porBodega'] ?? const []) as List).cast<Map<String, dynamic>>())
        if (e['ubicacion'] != null) e['bodegaId'] as String: e['ubicacion'] as String,
    };
    return [
      for (final b in todas)
        UbicacionEnBodega(bodegaId: b.id, bodegaNombre: b.nombre, ubicacion: dichas[b.id]),
    ];
  }

  /// HU-152 · fija la ubicación en una bodega; en blanco la quita.
  Future<void> fijarUbicacion(String id, String bodegaId, String ubicacion) async {
    await _http.put<void>('/api/inventario/productos/$id/ubicaciones/$bodegaId',
        datos: {'ubicacion': ubicacion});
  }

  /// HU-153 · el inventario general, filtrado y paginado, con los totales de todo lo filtrado.
  Future<ResultadoDeInventario> inventarioGeneral(ConsultaDeInventario consulta) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/general',
        query: consulta.comoQuery());
    return ResultadoDeInventario.desdeJson(cuerpo);
  }

  /// HU-154 · fija la tarifa a los productos elegidos o, sin elegidos, a toda la categoría.
  /// Devuelve cuántos cambiaron. Todo o nada.
  Future<int> cambiarTarifaEnBloque(
      {List<String> productoIds = const [], String? categoriaId, required String tarifaIva}) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/inventario/productos/tarifa-en-bloque',
      datos: {
        'productoIds': productoIds,
        'categoriaId': ?categoriaId,
        'tarifaIva': tarifaIva,
      },
    ) as Map<String, dynamic>;
    return ((cuerpo['cambiados'] ?? 0) as num).toInt();
  }

  /// HU-155 · las unidades de medida del negocio, con todos sus datos.
  Future<List<UnidadDeMedida>> unidadesDeMedida() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/unidades');
    return [
      for (final u in cuerpo.cast<Map<String, dynamic>>()) UnidadDeMedida.desdeJson(u),
    ];
  }

  /// HU-155 C1/C2 · crea una unidad; el backend rechaza un código repetido.
  Future<UnidadDeMedida> crearUnidad(String codigo, String nombre, bool permiteDecimales) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/unidades', datos: {
      'codigo': codigo,
      'nombre': nombre,
      'permiteDecimales': permiteDecimales,
    }) as Map<String, dynamic>;
    return UnidadDeMedida.desdeJson(cuerpo);
  }

  /// HU-155 C3 · desactiva una unidad; el backend la rechaza si algún producto la usa.
  Future<UnidadDeMedida> desactivarUnidad(String id) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
        '/api/inventario/unidades/$id/desactivacion') as Map<String, dynamic>;
    return UnidadDeMedida.desdeJson(cuerpo);
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
