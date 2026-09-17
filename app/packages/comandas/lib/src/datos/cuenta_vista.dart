/// Una de las cuentas en que se dividió la comanda (HU-089).
class CuentaVista {
  const CuentaVista({
    required this.id,
    required this.comandaId,
    required this.numeroDivision,
    required this.modoDivision,
    required this.subtotal,
    required this.impuestoTotal,
    required this.propina,
    required this.total,
    required this.pagado,
    required this.estado,
    required this.lineas,
    this.etiqueta,
  });

  final String id;
  final String comandaId;
  final int numeroDivision;
  final String? etiqueta;
  final String modoDivision;
  final num subtotal;
  final num impuestoTotal;
  final num propina;
  final num total;
  final num pagado;
  final String estado;
  final List<LineaDeCuentaVista> lineas;

  bool get pagada => estado == 'PAGADA';

  /// «Cuenta 1 · Camilo», o solo «Cuenta 1» sin etiqueta.
  String get nombre =>
      etiqueta == null || etiqueta!.isEmpty ? 'Cuenta $numeroDivision' : 'Cuenta $numeroDivision · $etiqueta';

  /// La proporción de [lineaId] que lleva esta cuenta, o `null` si no la marcó.
  num? proporcionDe(String lineaId) {
    for (final l in lineas) {
      if (l.lineaId == lineaId) return l.proporcion;
    }
    return null;
  }

  factory CuentaVista.desdeJson(Map<String, dynamic> json) => CuentaVista(
        id: json['id'] as String,
        comandaId: (json['comandaId'] ?? '') as String,
        numeroDivision: (json['numeroDivision'] as num?)?.toInt() ?? 1,
        etiqueta: json['etiqueta'] as String?,
        modoDivision: (json['modoDivision'] ?? 'POR_ITEM') as String,
        subtotal: (json['subtotal'] as num?) ?? 0,
        impuestoTotal: (json['impuestoTotal'] as num?) ?? 0,
        propina: (json['propina'] as num?) ?? 0,
        total: (json['total'] as num?) ?? 0,
        pagado: (json['pagado'] as num?) ?? 0,
        estado: (json['estado'] ?? 'ABIERTA') as String,
        lineas: (json['lineas'] as List<dynamic>? ?? const [])
            .map((e) => LineaDeCuentaVista.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
      );
}

class LineaDeCuentaVista {
  const LineaDeCuentaVista({required this.lineaId, required this.proporcion, required this.monto, this.nombre});

  final String lineaId;
  final String? nombre;
  final num proporcion;
  final num monto;

  factory LineaDeCuentaVista.desdeJson(Map<String, dynamic> json) => LineaDeCuentaVista(
        lineaId: json['lineaId'] as String,
        nombre: json['nombre'] as String?,
        proporcion: (json['proporcion'] as num?) ?? 1,
        monto: (json['monto'] as num?) ?? 0,
      );
}
