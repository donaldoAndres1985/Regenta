import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

/// Cómo se pinta cada estado de mesa en el plano (Mesas.md R1). Los colores
/// salen de los tokens, que son los mismos del mockup.
typedef EstiloDeEstado = ({String etiqueta, Color fondo, Color borde});

EstiloDeEstado estiloDeEstado(String estado) => switch (estado) {
      'LIBRE' => (
          etiqueta: 'Libre',
          fondo: RegentaColors.okSoft,
          borde: RegentaColors.ok,
        ),
      'OCUPADA' => (
          etiqueta: 'Ocupada',
          fondo: RegentaColors.comandaSoft,
          borde: RegentaColors.comanda,
        ),
      'CUENTA_PEDIDA' => (
          etiqueta: 'Cuenta pedida',
          fondo: RegentaColors.warnSoft,
          borde: RegentaColors.warn,
        ),
      'RESERVADA' => (
          etiqueta: 'Reservada',
          fondo: RegentaColors.infoSoft,
          borde: RegentaColors.info,
        ),
      'SUCIA' => (
          etiqueta: 'Por limpiar',
          fondo: RegentaColors.sunken,
          borde: RegentaColors.muted,
        ),
      _ => (
          etiqueta: 'Bloqueada',
          fondo: RegentaColors.sunken,
          borde: RegentaColors.muted,
        ),
    };
