import 'datos.dart';

/// HU-171 · El reparto por omisión de un pago, igual que el backend (`RepartoDePago`): de la cuenta
/// más antigua a la más nueva hasta agotar el monto. Devuelve cuenta → monto.
Map<String, num> repartirPorAntiguedad(num monto, List<CuentaPorPagarVista> abiertas) {
  final reparto = <String, num>{};
  var falta = monto;
  for (final c in abiertas) {
    if (falta <= 0) break;
    final parte = falta < c.saldo ? falta : c.saldo;
    if (parte > 0) {
      reparto[c.id] = parte;
      falta -= parte;
    }
  }
  return reparto;
}
