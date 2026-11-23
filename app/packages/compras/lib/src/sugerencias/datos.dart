import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-181 · Un producto bajo su mínimo en una bodega, con cuánto pedir para llegar al objetivo
/// (redondeado al múltiplo que vende el proveedor, I30). Como lo devuelve `GET /api/compras/sugerencias`.
class SugerenciaVista {
  const SugerenciaVista({
    required this.id,
    required this.productoId,
    required this.nombre,
    required this.cantidadSugerida,
    this.bodegaId,
    this.existencia = 0,
    this.stockMinimo = 0,
    this.stockObjetivo = 0,
    this.proveedorId,
    this.costoEstimado,
    this.sinProveedor = false,
  });

  final String id;
  final String productoId;
  final String nombre;
  final String? bodegaId;
  final num existencia;
  final num stockMinimo;
  final num stockObjetivo;
  final num cantidadSugerida;
  final String? proveedorId;
  final num? costoEstimado;
  final bool sinProveedor;

  num get subtotal => (costoEstimado ?? 0) * cantidadSugerida;

  factory SugerenciaVista.desdeJson(Map<String, dynamic> json) => SugerenciaVista(
        id: json['id'] as String,
        productoId: json['productoId'] as String,
        nombre: (json['nombre'] ?? '') as String,
        bodegaId: json['bodegaId'] as String?,
        existencia: (json['existencia'] as num?) ?? 0,
        stockMinimo: (json['stockMinimo'] as num?) ?? 0,
        stockObjetivo: (json['stockObjetivo'] as num?) ?? 0,
        cantidadSugerida: (json['cantidadSugerida'] as num?) ?? 0,
        proveedorId: json['proveedorId'] as String?,
        costoEstimado: json['costoEstimado'] as num?,
        sinProveedor: (json['sinProveedor'] as bool?) ?? json['proveedorId'] == null,
      );
}

/// HU-181 · I31: lo que se le pediría a un proveedor para una bodega (una orden), o el grupo
/// «Sin proveedor», que junta los productos a los que hay que asignarles uno.
class GrupoDeSugerenciasVista {
  const GrupoDeSugerenciasVista({
    required this.sugerencias,
    this.proveedorId,
    this.proveedorNombre,
    this.bodegaId,
    this.sinProveedor = false,
    this.costoTotalEstimado = 0,
  });

  final String? proveedorId;
  final String? proveedorNombre;
  final String? bodegaId;
  final bool sinProveedor;
  final num costoTotalEstimado;
  final List<SugerenciaVista> sugerencias;

  /// Identifica el grupo en la pantalla (claves de los widgets).
  String get clave => sinProveedor ? 'sin-proveedor' : '$proveedorId-${bodegaId ?? 'sin-bodega'}';

  factory GrupoDeSugerenciasVista.desdeJson(Map<String, dynamic> json) => GrupoDeSugerenciasVista(
        proveedorId: json['proveedorId'] as String?,
        proveedorNombre: json['proveedorRazonSocial'] as String?,
        bodegaId: json['bodegaId'] as String?,
        sinProveedor: (json['sinProveedor'] as bool?) ?? false,
        costoTotalEstimado: (json['costoTotalEstimado'] as num?) ?? 0,
        sugerencias: [
          for (final s in (json['sugerencias'] as List<dynamic>? ?? const []))
            SugerenciaVista.desdeJson((s as Map).cast<String, dynamic>()),
        ],
      );
}

/// Un proveedor activo, para elegirlo al asignarle uno a un producto (HU-181 C5).
class ProveedorParaAsignar {
  const ProveedorParaAsignar({required this.id, required this.nombre});

  final String id;
  final String nombre;
}

/// La orden que nació de aceptar un grupo: su id para abrirla y su número para avisarlo.
class OrdenDesdeSugerencias {
  const OrdenDesdeSugerencias({required this.id, required this.numero});

  final String id;
  final String numero;
}

/// Acceso a las sugerencias de compra de `compras` (HU-050, HU-181).
class RepositorioDeSugerencias {
  RepositorioDeSugerencias(this._http);

  static const _base = '/api/compras/sugerencias';

  final ClienteHttp _http;

  Future<List<GrupoDeSugerenciasVista>> listar() async {
    final res = await _http.get<List<dynamic>>(_base);
    return [for (final e in res) GrupoDeSugerenciasVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  /// I31 · Crea una orden en borrador con las sugerencias del grupo y las pasa a `EN_ORDEN`.
  Future<OrdenDesdeSugerencias> aceptar(GrupoDeSugerenciasVista grupo) async {
    final dynamic res = await _http.post<dynamic>('$_base/aceptacion', datos: {
      'proveedorId': grupo.proveedorId,
      if (grupo.bodegaId != null) 'bodegaDestinoId': grupo.bodegaId,
      'sugerenciaIds': [for (final s in grupo.sugerencias) s.id],
    });
    final orden = (res as Map).cast<String, dynamic>();
    return OrdenDesdeSugerencias(id: orden['id'] as String, numero: (orden['numero'] ?? '') as String);
  }

  /// C5 · El proveedor queda como preferido del producto y la sugerencia pasa a su grupo.
  Future<void> asignarProveedor(String sugerenciaId, String proveedorId) async {
    await _http.put<dynamic>('$_base/$sugerenciaId/proveedor', datos: {'proveedorId': proveedorId});
  }

  Future<void> descartar(String sugerenciaId) async {
    await _http.post<dynamic>('$_base/$sugerenciaId/descarte');
  }

  /// Los proveedores activos, por nombre.
  Future<List<ProveedorParaAsignar>> proveedores() async {
    final res = await _http.get<List<dynamic>>('/api/compras/proveedores');
    final lista = [
      for (final e in res)
        if (((e as Map)['activo'] as bool?) ?? true)
          ProveedorParaAsignar(
            id: e['id'] as String,
            nombre: ((e['nombreComercial'] as String?)?.trim().isNotEmpty ?? false)
                ? e['nombreComercial'] as String
                : (e['razonSocial'] ?? '') as String,
          ),
    ]..sort((a, b) => a.nombre.toLowerCase().compareTo(b.nombre.toLowerCase()));
    return lista;
  }

  /// Los nombres de las bodegas (id → nombre). Si no se pueden leer, ninguno: se muestra sin bodega.
  Future<Map<String, String>> bodegas() async {
    try {
      final res = await _http.get<List<dynamic>>('/api/inventario/bodegas');
      return {
        for (final e in res) (e as Map)['id'] as String: (e['nombre'] ?? e['codigo'] ?? '') as String,
      };
    } on ErrorDeApi {
      return const {};
    }
  }
}

final repositorioDeSugerenciasProvider = Provider<RepositorioDeSugerencias>((ref) {
  throw UnimplementedError('Sobrescribe repositorioDeSugerenciasProvider al montar el ProviderScope.');
});
