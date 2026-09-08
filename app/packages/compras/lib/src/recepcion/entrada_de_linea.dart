import '../datos/orden_recibible.dart';

/// Lo que la persona va capturando de una línea mientras recibe: cuánto llegó y,
/// donde la categoría lo exige, el lote y el vencimiento (HU-051).
class EntradaDeLinea {
  const EntradaDeLinea({
    this.recibido,
    this.costo,
    this.lote = '',
    this.vencimiento,
  });

  final num? recibido;
  final num? costo;
  final String lote;
  final DateTime? vencimiento;

  bool get tieneAlgo =>
      recibido != null || lote.isNotEmpty || vencimiento != null;

  /// Falta el lote o el vencimiento en un producto que los exige.
  bool loteIncompleto(LineaRecibible linea) =>
      linea.exigeLote &&
      (recibido != null && recibido! > 0) &&
      (lote.trim().isEmpty || vencimiento == null);

  /// Se está recibiendo por encima de lo que falta más el 5% de tolerancia.
  bool excedeTolerancia(LineaRecibible linea) =>
      recibido != null && recibido! > linea.faltante * 1.05;

  String? problema(LineaRecibible linea) {
    if (recibido == null || recibido == 0) return null;
    if (recibido! < 0) return 'La cantidad recibida no puede ser negativa';
    if (excedeTolerancia(linea)) {
      return 'La línea ${linea.linea} recibe más de lo pedido más el 5%';
    }
    if (loteIncompleto(linea)) {
      return 'El producto exige lote y vencimiento al recibirlo';
    }
    return null;
  }

  EntradaDeLinea copiar({
    Object? recibido = _sinCambio,
    Object? costo = _sinCambio,
    String? lote,
    Object? vencimiento = _sinCambio,
  }) =>
      EntradaDeLinea(
        recibido: recibido == _sinCambio ? this.recibido : recibido as num?,
        costo: costo == _sinCambio ? this.costo : costo as num?,
        lote: lote ?? this.lote,
        vencimiento:
            vencimiento == _sinCambio ? this.vencimiento : vencimiento as DateTime?,
      );

  static const _sinCambio = Object();
}
