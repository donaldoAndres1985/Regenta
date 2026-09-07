/// Nivel de stock del producto, tal como lo devuelve la búsqueda de Inventario.
enum NivelStock { normal, bajo, cero }

NivelStock _nivel(String? crudo) => switch (crudo) {
      'BAJO' => NivelStock.bajo,
      'CERO' => NivelStock.cero,
      _ => NivelStock.normal,
    };

/// Un producto encontrado por la búsqueda, listo para agregar al carrito.
class ProductoBuscado {
  const ProductoBuscado({
    required this.id,
    required this.sku,
    required this.nombre,
    required this.precioVenta,
    required this.nivelStock,
    this.impuestoPct = 0,
    this.costoUnitario = 0,
  });

  final String id;
  final String sku;
  final String nombre;
  final num precioVenta;
  final NivelStock nivelStock;
  final num impuestoPct;
  final num costoUnitario;

  bool get sinStock => nivelStock == NivelStock.cero;

  factory ProductoBuscado.desdeJson(Map<String, dynamic> json) => ProductoBuscado(
        id: json['id'] as String,
        sku: (json['sku'] ?? '') as String,
        nombre: (json['nombre'] ?? '') as String,
        precioVenta: (json['precioVenta'] ?? 0) as num,
        nivelStock: _nivel(json['nivelStock'] as String?),
      );
}

/// Lo que devuelve resolver un código de barras: el producto y su factor.
class CodigoResuelto {
  const CodigoResuelto({required this.productoId, required this.factor});
  final String productoId;
  final num factor;

  factory CodigoResuelto.desdeJson(Map<String, dynamic> json) => CodigoResuelto(
        productoId: json['productoId'] as String,
        factor: (json['factor'] ?? 1) as num,
      );
}
