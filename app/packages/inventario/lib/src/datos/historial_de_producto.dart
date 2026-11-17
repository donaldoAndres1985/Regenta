/// HU-160 · un movimiento del historial de un producto. `cantidad` lleva el signo (salidas en
/// negativo) y `saldoPosterior` es lo que quedó en esa bodega.
class MovimientoDeHistorial {
  const MovimientoDeHistorial({
    required this.id,
    required this.ocurridoEn,
    required this.bodegaId,
    required this.tipo,
    required this.cantidad,
    required this.saldoPosterior,
    required this.motivo,
    required this.usuario,
  });

  final String id;
  final DateTime ocurridoEn;
  final String bodegaId;
  final String tipo;
  final num cantidad;
  final num saldoPosterior;
  final String motivo;
  final String usuario;

  factory MovimientoDeHistorial.desdeJson(Map<String, dynamic> j) => MovimientoDeHistorial(
        id: j['id'] as String,
        ocurridoEn: DateTime.parse(j['ocurridoEn'] as String).toLocal(),
        bodegaId: (j['bodegaId'] ?? '') as String,
        tipo: (j['tipo'] ?? '') as String,
        cantidad: (j['cantidad'] ?? 0) as num,
        saldoPosterior: (j['saldoPosterior'] ?? 0) as num,
        motivo: (j['motivo'] ?? '') as String,
        usuario: (j['usuario'] ?? '') as String,
      );

  /// El tipo en palabras: venta, compra, ajuste, traslado, detallado…
  String get tipoLegible => switch (tipo) {
        'ENTRADA_COMPRA' => 'Compra',
        'ENTRADA_AJUSTE' => 'Ajuste (entrada)',
        'SALIDA_AJUSTE' => 'Ajuste (salida)',
        'ENTRADA_DEVOLUCION' => 'Devolución',
        'ENTRADA_TRASLADO' => 'Traslado (entrada)',
        'SALIDA_TRASLADO' => 'Traslado (salida)',
        'SALIDA_VENTA' => 'Venta',
        'SALIDA_MERMA' => 'Merma',
        'SALIDA_CONSUMO' => 'Consumo',
        'ENTRADA_DETALLE' => 'Detallado (entrada)',
        'SALIDA_DETALLE' => 'Detallado (salida)',
        _ => tipo,
      };
}
