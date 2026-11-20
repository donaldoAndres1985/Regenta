import 'package:regenta_core/regenta_core.dart';

import 'producto_del_proveedor.dart';
import 'proveedor_del_negocio.dart';

/// Acceso a los proveedores de `compras` (HU-166). Todo pasa por el [ClienteHttp] del núcleo;
/// los errores llegan como [ErrorDeApi] con el mensaje del servidor.
class RepositorioDeProveedores {
  RepositorioDeProveedores(this._http);

  static const _base = '/api/compras/proveedores';

  final ClienteHttp _http;

  /// Activos e inactivos: el filtro de la pantalla los separa en memoria.
  Future<List<ProveedorDelNegocio>> listar() async {
    final res = await _http.get<List<dynamic>>(_base, query: {'estado': 'TODOS'});
    return res.map((e) => ProveedorDelNegocio.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  /// Crea el proveedor si no trae id; si lo trae, guarda los cambios.
  Future<ProveedorDelNegocio> guardar(ProveedorDelNegocio proveedor) async {
    final dynamic res = proveedor.id == null
        ? await _http.post<dynamic>(_base, datos: proveedor.aJson())
        : await _http.put<dynamic>('$_base/${proveedor.id}', datos: proveedor.aJson());
    return ProveedorDelNegocio.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<ProveedorDelNegocio> desactivar(String proveedorId) async {
    final dynamic res = await _http.delete<dynamic>('$_base/$proveedorId');
    return ProveedorDelNegocio.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<ProveedorDelNegocio> reactivar(String proveedorId) async {
    final dynamic res = await _http.post<dynamic>('$_base/$proveedorId/reactivacion');
    return ProveedorDelNegocio.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<List<ProductoDelProveedor>> productos(String proveedorId) async {
    final res = await _http.get<List<dynamic>>('$_base/$proveedorId/productos');
    return res.map((e) => ProductoDelProveedor.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  Future<ProductoDelProveedor> asociar(String proveedorId, ProductoDelProveedor producto) async {
    final dynamic res =
        await _http.post<dynamic>('$_base/$proveedorId/productos', datos: producto.aJson());
    return ProductoDelProveedor.desdeJson((res as Map).cast<String, dynamic>());
  }

  Future<void> quitar(String proveedorId, String productoId) async {
    await _http.delete<dynamic>('$_base/$proveedorId/productos/$productoId');
  }

  /// Busca en el catálogo de Inventario por nombre, SKU o código (mínimo tres caracteres).
  Future<List<ProductoEncontrado>> buscarProductos(String termino) async {
    final dynamic res =
        await _http.get<dynamic>('/api/inventario/productos/buscar', query: {'q': termino.trim()});
    final lista = (res is Map ? res['productos'] : res) as List<dynamic>? ?? const [];
    return lista.map((e) => ProductoEncontrado.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }
}
