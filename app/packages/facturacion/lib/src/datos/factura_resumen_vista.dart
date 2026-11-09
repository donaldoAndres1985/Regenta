/// Resumen de factura para la lista (HU-053).
class FacturaResumenVista {
  const FacturaResumenVista({
    required this.id,
    required this.numeroCompleto,
    required this.origenTipo,
    required this.estado,
    required this.total,
  });

  final String id;
  final String numeroCompleto;
  final String origenTipo;
  final String estado;
  final num total;

  factory FacturaResumenVista.desdeJson(Map<String, dynamic> j) => FacturaResumenVista(
        id: j['id'] as String,
        numeroCompleto: (j['numeroCompleto'] as String?) ?? '',
        origenTipo: (j['origenTipo'] as String?) ?? '',
        estado: (j['estado'] as String?) ?? 'BORRADOR',
        total: (j['total'] as num?) ?? 0,
      );
}
