/// Resumen de una orden de compra para listados y navegación (HU-047).
class OrdenResumenVista {
  const OrdenResumenVista({
    required this.id,
    required this.numero,
    required this.estado,
    required this.total,
    this.fechaEmision,
    this.observaciones,
  });

  final String id;
  final String numero;
  final String estado;
  final num total;
  final String? fechaEmision;
  final String? observaciones;

  factory OrdenResumenVista.desdeJson(Map<String, dynamic> j) => OrdenResumenVista(
        id: j['id'] as String,
        numero: (j['numero'] as String?) ?? '',
        estado: (j['estado'] as String?) ?? 'BORRADOR',
        total: (j['total'] as num?) ?? 0,
        fechaEmision: j['fechaEmision'] as String?,
        observaciones: j['observaciones'] as String?,
      );
}
