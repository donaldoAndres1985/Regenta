import 'package:flutter/material.dart';

import 'patron_operativo.dart';
import 'regenta_colors.dart';
import 'regenta_spacing.dart';
import 'regenta_type.dart';

/// El tema Material de Regenta. El primario es siempre el ocre; el secundario
/// lo pone el patron operativo del negocio.
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
