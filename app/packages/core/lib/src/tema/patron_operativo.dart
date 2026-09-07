import 'package:flutter/painting.dart';

import 'regenta_colors.dart';

/// Los tres patrones operativos.
///
/// El patron viaja como claim del JWT y decide que paquetes carga la app y de
/// que color se pinta la navegacion (el color secundario del tema).
enum PatronOperativo {
  ventaDirecta(RegentaColors.venta, RegentaColors.ventaSoft, 0),
  reserva(RegentaColors.reserva, RegentaColors.reservaSoft, 1),
  comanda(RegentaColors.comanda, RegentaColors.comandaSoft, 2);

  const PatronOperativo(this.color, this.colorSuave, this.orden);

  final Color color;
  final Color colorSuave;
  final int orden;

  /// Resuelve el patron desde el valor del claim `patron` del JWT. Un valor
  /// desconocido no se traga en silencio: revienta.
  static PatronOperativo desdeClaim(String v) => switch (v) {
        'VENTA_DIRECTA' => PatronOperativo.ventaDirecta,
        'RESERVA' => PatronOperativo.reserva,
        'COMANDA' => PatronOperativo.comanda,
        _ => throw ArgumentError('Patron desconocido: $v'),
      };
}
