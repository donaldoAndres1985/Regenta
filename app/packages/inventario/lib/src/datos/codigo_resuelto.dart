/// El producto al que apunta un código de barras, con su factor de conversión:
/// 1 para el código propio del producto, N para un alterno (caja de N).
class CodigoResuelto {
  const CodigoResuelto({
    required this.productoId,
    required this.sku,
    required this.nombre,
    required this.factor,
    required this.esAlterno,
  });

  final String productoId;
  final String sku;
  final String nombre;
  final num factor;
  final bool esAlterno;

  factory CodigoResuelto.desdeJson(Map<String, dynamic> json) => CodigoResuelto(
        productoId: json['productoId'] as String,
        sku: (json['sku'] ?? '') as String,
        nombre: (json['nombre'] ?? '') as String,
        factor: (json['factor'] ?? 1) as num,
        esAlterno: (json['esAlterno'] ?? false) as bool,
      );
}
