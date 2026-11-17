/// HU-156 · una fracción: medida, factor (lo que descuenta del producto) y precio con IVA
/// incluido, escrito a mano (no proporcional, I13). `origen`: TABLA o PROPIO (HU-157).
class FraccionDeTabla {
  const FraccionDeTabla({
    required this.medida,
    required this.factor,
    required this.precio,
    this.origen = 'TABLA',
  });

  final String medida;
  final num factor;
  final num precio;
  final String origen;

  factory FraccionDeTabla.desdeJson(Map<String, dynamic> j) => FraccionDeTabla(
        medida: (j['medida'] ?? '') as String,
        factor: (j['factor'] ?? 0) as num,
        precio: (j['precio'] ?? 0) as num,
        origen: (j['origen'] ?? 'TABLA') as String,
      );

  Map<String, dynamic> aJson() => {'medida': medida, 'factor': factor, 'precio': precio};
}

/// HU-156 · una tabla de fracciones compartida por varios productos.
class TablaDeFracciones {
  const TablaDeFracciones({
    required this.id,
    required this.nombre,
    required this.fracciones,
    this.productos = 0,
  });

  final String? id;
  final String nombre;
  final List<FraccionDeTabla> fracciones;

  /// Cuántos productos la usan.
  final int productos;

  factory TablaDeFracciones.desdeJson(Map<String, dynamic> j) => TablaDeFracciones(
        id: j['id'] as String?,
        nombre: (j['nombre'] ?? '') as String,
        fracciones: [
          for (final f in ((j['fracciones'] ?? const []) as List).cast<Map<String, dynamic>>())
            FraccionDeTabla.desdeJson(f),
        ],
        productos: ((j['productos'] ?? 0) as num).toInt(),
      );

  Map<String, dynamic> aJson() => {
        'nombre': nombre,
        'fracciones': [for (final f in fracciones) f.aJson()],
      };
}
