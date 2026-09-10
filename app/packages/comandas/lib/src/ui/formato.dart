import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

/// Cómo se pinta cada estado de línea (Comanda.md). Los colores salen de los
/// tokens, que son los del mockup.
typedef EstiloDeLinea = ({String etiqueta, Color fondo, Color tinta});

EstiloDeLinea estiloDeLinea(String estado) => switch (estado) {
      'PENDIENTE' => (
          etiqueta: 'Pendiente',
          fondo: RegentaColors.sunken,
          tinta: RegentaColors.muted,
        ),
      'ENVIADA' => (
          etiqueta: 'Enviada',
          fondo: RegentaColors.warnSoft,
          tinta: RegentaColors.warn,
        ),
      'EN_PREPARACION' => (
          etiqueta: 'En preparación',
          fondo: RegentaColors.warnSoft,
          tinta: RegentaColors.warn,
        ),
      'LISTA' => (
          etiqueta: 'Lista',
          fondo: RegentaColors.okSoft,
          tinta: RegentaColors.ok,
        ),
      'ENTREGADA' => (
          etiqueta: 'Entregada',
          fondo: RegentaColors.infoSoft,
          tinta: RegentaColors.info,
        ),
      _ => (
          etiqueta: 'Anulada',
          fondo: RegentaColors.critSoft,
          tinta: RegentaColors.crit,
        ),
    };

String pesos(num v) {
  final entero = v.round().toString();
  final buf = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i > 0 && (entero.length - i) % 3 == 0) buf.write('.');
    buf.write(entero[i]);
  }
  return '\$$buf';
}
