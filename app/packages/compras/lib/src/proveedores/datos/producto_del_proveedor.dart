/// Un producto en el catálogo de un proveedor (HU-166): su código allá, el último costo y si
/// este proveedor es el preferido para ese producto.
class ProductoDelProveedor {
  const ProductoDelProveedor({
    required this.productoId,
    this.sku,
    this.nombre,
    this.codigoProveedor,
    this.costoUltimo,
    this.diasEntrega,
    this.cantidadMinima = 1,
    this.preferido = false,
  });

  final String productoId;
  final String? sku;
  final String? nombre;
  final String? codigoProveedor;
  final num? costoUltimo;
  final int? diasEntrega;
  final num cantidadMinima;
  final bool preferido;

  String get nombreVisible => (nombre == null || nombre!.isEmpty) ? 'Producto sin nombre' : nombre!;

  factory ProductoDelProveedor.desdeJson(Map<String, dynamic> json) => ProductoDelProveedor(
        productoId: json['productoId'] as String,
        sku: json['sku'] as String?,
        nombre: json['nombre'] as String?,
        codigoProveedor: json['codigoProveedor'] as String?,
        costoUltimo: json['costoUltimo'] as num?,
        diasEntrega: (json['diasEntrega'] as num?)?.toInt(),
        cantidadMinima: (json['cantidadMinima'] as num?) ?? 1,
        preferido: (json['preferido'] as bool?) ?? false,
      );

  ProductoDelProveedor copiar({bool? preferido}) => ProductoDelProveedor(
        productoId: productoId,
        sku: sku,
        nombre: nombre,
        codigoProveedor: codigoProveedor,
        costoUltimo: costoUltimo,
        diasEntrega: diasEntrega,
        cantidadMinima: cantidadMinima,
        preferido: preferido ?? this.preferido,
      );

  /// Lo que pide `POST /api/compras/proveedores/{id}/productos`.
  Map<String, dynamic> aJson() => {
        'productoId': productoId,
        'codigoProveedor': (codigoProveedor?.trim().isEmpty ?? true) ? null : codigoProveedor!.trim(),
        'costoUltimo': costoUltimo,
        'diasEntrega': diasEntrega,
        'cantidadMinima': cantidadMinima,
        'preferido': preferido,
      };
}

/// Un producto del catálogo del negocio encontrado al buscar, para agregarlo al proveedor.
class ProductoEncontrado {
  const ProductoEncontrado({required this.id, required this.nombre, this.sku});

  final String id;
  final String nombre;
  final String? sku;

  factory ProductoEncontrado.desdeJson(Map<String, dynamic> json) => ProductoEncontrado(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? '') as String,
        sku: json['sku'] as String?,
      );
}
