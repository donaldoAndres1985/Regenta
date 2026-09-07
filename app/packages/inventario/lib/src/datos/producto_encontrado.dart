/// Cómo está el stock de un producto frente a su mínimo. Define el color de la
/// cantidad en la lista (HU-035). Espeja el enum del backend.
enum NivelStock { normal, bajo, cero }

NivelStock _nivelDesde(String? crudo) => switch (crudo) {
      'BAJO' => NivelStock.bajo,
      'CERO' => NivelStock.cero,
      _ => NivelStock.normal,
    };

/// Una fila del resultado de búsqueda de inventario.
class ProductoEncontrado {
  const ProductoEncontrado({
    required this.id,
    required this.sku,
    required this.codigoBarras,
    required this.nombre,
    required this.categoriaNombre,
    required this.precioVenta,
    required this.stockTotal,
    required this.nivelStock,
  });

  final String id;
  final String sku;
  final String? codigoBarras;
  final String nombre;
  final String categoriaNombre;
  final num precioVenta;
  final num stockTotal;
  final NivelStock nivelStock;

  factory ProductoEncontrado.desdeJson(Map<String, dynamic> json) => ProductoEncontrado(
        id: json['id'] as String,
        sku: (json['sku'] ?? '') as String,
        codigoBarras: json['codigoBarras'] as String?,
        nombre: (json['nombre'] ?? '') as String,
        categoriaNombre: (json['categoriaNombre'] ?? '') as String,
        precioVenta: (json['precioVenta'] ?? 0) as num,
        stockTotal: (json['stockTotal'] ?? 0) as num,
        nivelStock: _nivelDesde(json['nivelStock'] as String?),
      );
}

/// El resultado completo de una búsqueda.
class ResultadoDeBusqueda {
  const ResultadoDeBusqueda({required this.productos, required this.total});

  final List<ProductoEncontrado> productos;
  final int total;

  factory ResultadoDeBusqueda.desdeJson(Map<String, dynamic> json) => ResultadoDeBusqueda(
        productos: ((json['productos'] ?? const []) as List<dynamic>)
            .map((e) => ProductoEncontrado.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
        total: (json['total'] ?? 0) as int,
      );
}
