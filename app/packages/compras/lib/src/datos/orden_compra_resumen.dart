/// Una orden de compra en la lista (HU-047).
class OrdenCompraResumen {
  const OrdenCompraResumen({
    required this.id,
    required this.numero,
    required this.proveedorId,
    required this.estado,
    required this.total,
    required this.creadaEn,
  });

  final String id;
  final String numero;
  final String proveedorId;
  final String estado; // BORRADOR, APROBADA, ENVIADA, RECIBIDA, CANCELADA
  final num total;
  final DateTime creadaEn;

  factory OrdenCompraResumen.desdeJson(Map<String, dynamic> j) => OrdenCompraResumen(
        id: j['id'] as String,
        numero: (j['numero'] ?? '') as String,
        proveedorId: (j['proveedorId'] ?? '') as String,
        estado: (j['estado'] ?? 'BORRADOR') as String,
        total: (j['total'] as num?) ?? 0,
        creadaEn: DateTime.tryParse((j['creadaEn'] ?? '') as String)?.toLocal() ??
            DateTime.now(),
      );
}
