// GENERADO desde design/tokens — no editar a mano.
// Fuente de verdad: los mismos valores que usan los mockups de design/pantallas/.
// Si cambias un color aquí, cámbialo también en el mockup, o dejarán de coincidir.

import 'package:flutter/material.dart';

/// Paleta de Regenta. Neutros cálidos y un solo acento ocre.
abstract final class RegentaColors {
  static const paper = Color(0xFFFBFAF7);
  static const surface = Color(0xFFFFFFFF);
  static const sunken = Color(0xFFF3F1EB);
  static const ink = Color(0xFF1A1815);
  static const ink2 = Color(0xFF4A4740);
  static const muted = Color(0xFF7C776C);
  static const faint = Color(0xFFA39D91);
  static const line = Color(0xFFE4E0D8);
  static const line2 = Color(0xFFD2CCC0);
  static const accent = Color(0xFF9A5709);
  static const accentSoft = Color(0xFFF6EBD9);

  // Un color por patrón operativo: permite cruzar cualquier pantalla con su parte del modelo.
  static const core = Color(0xFF4A5C6D);
  static const coreSoft = Color(0xFFE7ECF0);
  static const venta = Color(0xFF9A5709);
  static const ventaSoft = Color(0xFFF6EBD9);
  static const reserva = Color(0xFF0C6473);
  static const reservaSoft = Color(0xFFDFEFF2);
  static const comanda = Color(0xFFA03325);
  static const comandaSoft = Color(0xFFF8E6E3);

  // Semánticos: separados del acento a propósito.
  static const ok = Color(0xFF1E6B45);
  static const okSoft = Color(0xFFE4F0E9);
  static const warn = Color(0xFF8A5B06);
  static const warnSoft = Color(0xFFFAF0DC);
  static const crit = Color(0xFFA0271B);
  static const critSoft = Color(0xFFF9E7E4);
  static const info = Color(0xFF0C6473);
  static const infoSoft = Color(0xFFDFEFF2);
}

/// Los tres patrones. El patrón viaja como claim del JWT y decide
/// qué paquetes carga la app y de qué color se pinta la navegación.
enum PatronOperativo {
  ventaDirecta(RegentaColors.venta, RegentaColors.ventaSoft, 0),
  reserva(RegentaColors.reserva, RegentaColors.reservaSoft, 1),
  comanda(RegentaColors.comanda, RegentaColors.comandaSoft, 2);

  const PatronOperativo(this.color, this.colorSuave, this.orden);
  final Color color;
  final Color colorSuave;
  final int orden;

  static PatronOperativo desdeClaim(String v) => switch (v) {
        'VENTA_DIRECTA' => PatronOperativo.ventaDirecta,
        'RESERVA' => PatronOperativo.reserva,
        'COMANDA' => PatronOperativo.comanda,
        _ => throw ArgumentError('Patrón desconocido: \$v'),
      };
}

/// Espaciado. Los mockups usan estos valores, no una malla de 4/8.
abstract final class RegentaSpacing {
  static const double xs = 5;
  static const double sm = 9;
  static const double md = 14;
  static const double lg = 18;
  static const double xl = 22;
  static const double xxl = 30;

  /// Altura mínima de toque en móvil. No bajar de aquí:
  /// se usa de pie, con una mano y a veces con guantes.
  static const double hitTarget = 44;
  static const double controlWeb = 36;
  static const double radius = 5;
  static const double radiusCard = 6;
}

/// Tipografía: Archivo para la interfaz, IBM Plex Mono para todo lo que
/// es código, dinero o identificador (SKU, NIT, CUFE, totales).
abstract final class RegentaType {
  static const ui = 'Archivo';
  static const mono = 'IBMPlexMono';

  static const tituloPantalla = TextStyle(
      fontFamily: ui, fontSize: 30, fontWeight: FontWeight.w700, letterSpacing: -0.6);
  static const seccion = TextStyle(
      fontFamily: ui, fontSize: 17, fontWeight: FontWeight.w600);
  static const item = TextStyle(
      fontFamily: ui, fontSize: 14, fontWeight: FontWeight.w500);
  static const cuerpo = TextStyle(fontFamily: ui, fontSize: 13);

  /// Cifras y códigos. tabular-nums para que las columnas cuadren.
  static const dinero = TextStyle(
      fontFamily: mono, fontSize: 22, fontWeight: FontWeight.w600,
      fontFeatures: [FontFeature.tabularFigures()]);
  static const codigo = TextStyle(fontFamily: mono, fontSize: 12);

  /// Etiqueta de campo: versalita monoespaciada.
  static const etiqueta = TextStyle(
      fontFamily: mono, fontSize: 9.5, fontWeight: FontWeight.w600,
      letterSpacing: 0.76, color: RegentaColors.muted);
}

/// Punto de corte entre la composición de móvil y la de escritorio.
/// No es un tamaño de dispositivo: es dónde deja de funcionar la lista
/// de una columna y empieza a caber una tabla con panel lateral.
const double kBreakpointEscritorio = 900;

ThemeData regentaTheme(PatronOperativo patron) {
  final base = ThemeData(useMaterial3: true, brightness: Brightness.light);
  return base.copyWith(
    scaffoldBackgroundColor: RegentaColors.paper,
    colorScheme: base.colorScheme.copyWith(
      primary: RegentaColors.accent,
      secondary: patron.color,
      surface: RegentaColors.surface,
      error: RegentaColors.crit,
      onSurface: RegentaColors.ink,
    ),
    dividerColor: RegentaColors.line,
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: RegentaColors.accent,
        minimumSize: const Size(0, RegentaSpacing.hitTarget),
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
        textStyle: const TextStyle(
            fontFamily: RegentaType.ui, fontSize: 14, fontWeight: FontWeight.w600),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: RegentaColors.surface,
      contentPadding: const EdgeInsets.symmetric(horizontal: 11, vertical: 12),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: const BorderSide(color: RegentaColors.line2),
      ),
    ),
  );
}
