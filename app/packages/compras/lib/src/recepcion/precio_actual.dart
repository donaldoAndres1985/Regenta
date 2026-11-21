/// HU-179 · El costo promedio y el precio de venta actuales de un producto en la bodega donde llega.
class PrecioActual {
  const PrecioActual({required this.productoId, required this.costoPromedio, required this.precio, required this.base});

  final String productoId;
  final num costoPromedio;

  /// Con IVA incluido.
  final num precio;

  /// El precio sin IVA: sobre ella se mide el margen.
  final num base;

  factory PrecioActual.desdeJson(Map<String, dynamic> j) => PrecioActual(
        productoId: j['productoId'] as String,
        costoPromedio: (j['costoPromedio'] as num?) ?? 0,
        precio: (j['precio'] as num?) ?? 0,
        base: (j['base'] as num?) ?? 0,
      );
}

/// HU-179 C1 · El margen sobre la base sin IVA, en porcentaje: (base − costo) ÷ base. Nulo sin precio.
num? margenSobreBase(num base, num costo) => base <= 0 ? null : (base - costo) * 100 / base;

/// «25,0 %», con un decimal y coma.
String formatearMargen(num? margen) =>
    margen == null ? '—' : '${margen.toStringAsFixed(1).replaceAll('.', ',')} %';
