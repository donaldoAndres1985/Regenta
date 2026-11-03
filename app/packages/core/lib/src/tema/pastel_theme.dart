// Regenta · Propuesta 5 · Pastel Profesional
// Tema Material 3 listo para usar (claro y oscuro).
//
// Uso:
//   MaterialApp(
//     theme: PastelTheme.light(),
//     darkTheme: PastelTheme.dark(),
//     themeMode: ThemeMode.system,
//   );
//
// Requiere Flutter 3.27 o superior (CardThemeData, WidgetStateProperty).

import 'package:flutter/material.dart';

import 'patron_operativo.dart';
import 'regenta_tokens.dart';

abstract final class PastelTheme {
  /// Familias empaquetadas con el núcleo (ver `fonts/README.md`): cargan igual
  /// en Android y en Web, sin pedir la red. Se piden con `package:` para que
  /// resuelvan desde la app y desde cualquier módulo.
  static const familiaTitulos = 'Nunito';
  static const familiaCuerpo = 'NunitoSans';
  static const _paquete = 'regenta_core';

  /// Títulos: Nunito.
  static TextStyle titulo({
    double? fontSize,
    FontWeight? fontWeight,
    Color? color,
    double? letterSpacing,
    double? height,
    List<FontFeature>? fontFeatures,
    TextDecoration? decoration,
  }) =>
      TextStyle(
        fontFamily: familiaTitulos,
        package: _paquete,
        fontSize: fontSize,
        fontWeight: fontWeight,
        color: color,
        letterSpacing: letterSpacing,
        height: height,
        fontFeatures: fontFeatures,
        decoration: decoration,
      );

  /// Cuerpo y cifras: Nunito Sans.
  static TextStyle cuerpo({
    double? fontSize,
    FontWeight? fontWeight,
    Color? color,
    double? letterSpacing,
    double? height,
    List<FontFeature>? fontFeatures,
    TextDecoration? decoration,
  }) =>
      TextStyle(
        fontFamily: familiaCuerpo,
        package: _paquete,
        fontSize: fontSize,
        fontWeight: fontWeight,
        color: color,
        letterSpacing: letterSpacing,
        height: height,
        fontFeatures: fontFeatures,
        decoration: decoration,
      );

  /// El secundario sale del patrón operativo del negocio (HU-106); el primario
  /// es siempre el azul hortensia. En Venta directa coincide con el secundario
  /// de la paleta Pastel.
  static ThemeData light({PatronOperativo patron = PatronOperativo.ventaDirecta}) => _build(
        _lightScheme.copyWith(secondary: patron.color, secondaryContainer: patron.colorSuave),
        _lightTokens,
      );
  static ThemeData dark() => _build(_darkScheme, _darkTokens);

  /// Cifras de dinero y cantidades: siempre con dígitos tabulares
  /// para que las columnas del POS y las tablas cuadren.
  static TextStyle numeros(BuildContext context,
      {double size = 16, FontWeight weight = FontWeight.w600}) {
    return cuerpo(
      fontSize: size,
      fontWeight: weight,
      color: Theme.of(context).colorScheme.onSurface,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
  }

  static const ColorScheme _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF2E62C0),
    onPrimary: Color(0xFFFFFFFF),
    primaryContainer: Color(0xFFE2E9F6),
    onPrimaryContainer: Color(0xFF19366A),
    secondary: Color(0xFF257A6A),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: Color(0xFFE0ECEA),
    onSecondaryContainer: Color(0xFF14433A),
    tertiary: Color(0xFFC2416F),
    onTertiary: Color(0xFFFFFFFF),
    tertiaryContainer: Color(0xFFF6E4EB),
    onTertiaryContainer: Color(0xFF6B243D),
    error: Color(0xFFB3304F),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFF4E2E6),
    onErrorContainer: Color(0xFF621A2B),
    surface: Color(0xFFFFFFFF),
    onSurface: Color(0xFF25253A),
    onSurfaceVariant: Color(0xFF5D5D78),
    surfaceContainerLowest: Color(0xFFFFFFFF),
    surfaceContainerLow: Color(0xFFFCFCFE),
    surfaceContainer: Color(0xFFF8F8FD),
    surfaceContainerHigh: Color(0xFFF1F1F5),
    surfaceContainerHighest: Color(0xFFE9E9EE),
    outline: Color(0xFFA6A6B5),
    outlineVariant: Color(0xFFE7E7F2),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFF25253A),
    onInverseSurface: Color(0xFFF8F8FD),
    inversePrimary: Color(0xFFA9C4FF),
  );

  static const ColorScheme _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFFA9C4FF),
    onPrimary: Color(0xFF0B2A66),
    primaryContainer: Color(0xFF3F465D),
    onPrimaryContainer: Color(0xFFE1EAFF),
    secondary: Color(0xFF8ADBC8),
    onSecondary: Color(0xFF00382E),
    secondaryContainer: Color(0xFF384B50),
    onSecondaryContainer: Color(0xFFD6F2EC),
    tertiary: Color(0xFFFFA3C7),
    onTertiary: Color(0xFF4D0A26),
    tertiaryContainer: Color(0xFF543E50),
    onTertiaryContainer: Color(0xFFFFDFEB),
    error: Color(0xFFFF9AB0),
    onError: Color(0xFF15151E),
    errorContainer: Color(0xFF543C4A),
    onErrorContainer: Color(0xFFFFDCE3),
    surface: Color(0xFF1E1E2A),
    onSurface: Color(0xFFF2F2FA),
    onSurfaceVariant: Color(0xFFB5B5CC),
    surfaceContainerLowest: Color(0xFF1A1A24),
    surfaceContainerLow: Color(0xFF1E1E2A),
    surfaceContainer: Color(0xFF282836),
    surfaceContainerHigh: Color(0xFF353542),
    surfaceContainerHighest: Color(0xFF42424E),
    outline: Color(0xFF717183),
    outlineVariant: Color(0xFF33334A),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFFF2F2FA),
    onInverseSurface: Color(0xFF15151E),
    inversePrimary: Color(0xFF2E62C0),
  );

  static const RegentaTokens _lightTokens = RegentaTokens(
    success: Color(0xFF1D7550),
    successContainer: Color(0xFFDFECE6),
    warning: Color(0xFF935700),
    warningContainer: Color(0xFFF0E7DB),
    info: Color(0xFF2E62C0),
    infoContainer: Color(0xFFE2E9F6),
    anulado: Color(0xFF5D5D78),
    anuladoContainer: Color(0xFFEFEFF2),
    fondo: Color(0xFFF8F8FD),
    superficieElevada: Color(0xFFFFFFFF),
    heroGradient: LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFFC9D8FF), Color(0xFFDDF5EE), Color(0xFFFFE4EE)],
    ),
    modulos: {
      'ventas': Color(0xFFECEBFF),
      'inventario': Color(0xFFDDF5EE),
      'caja': Color(0xFFE0F0FF),
      'clientes': Color(0xFFFFE4EE),
      'compras': Color(0xFFFFF4C7),
      'reportes': Color(0xFFFFE9DC),
    },
    radioTarjeta: 20,
    radioControl: 12,
    sombraTarjeta: [BoxShadow(color: Color(0x14000000), blurRadius: 24, offset: Offset(0, 8))],
    vidrio: false,
  );

  static const RegentaTokens _darkTokens = RegentaTokens(
    success: Color(0xFF7FDDB0),
    successContainer: Color(0xFF354C4A),
    warning: Color(0xFFFFC877),
    warningContainer: Color(0xFF54473C),
    info: Color(0xFFA9C4FF),
    infoContainer: Color(0xFF3F465D),
    anulado: Color(0xFFB5B5CC),
    anuladoContainer: Color(0xFF363644),
    fondo: Color(0xFF15151E),
    superficieElevada: Color(0xFF282836),
    heroGradient: LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0x66C9D8FF), Color(0x66DDF5EE), Color(0x66FFE4EE)],
    ),
    modulos: {
      'ventas': Color(0xFFECEBFF),
      'inventario': Color(0xFFDDF5EE),
      'caja': Color(0xFFE0F0FF),
      'clientes': Color(0xFFFFE4EE),
      'compras': Color(0xFFFFF4C7),
      'reportes': Color(0xFFFFE9DC),
    },
    radioTarjeta: 20,
    radioControl: 12,
    sombraTarjeta: [BoxShadow(color: Color(0x14000000), blurRadius: 24, offset: Offset(0, 8))],
    vidrio: false,
  );

  static TextTheme _textTheme(ColorScheme cs) {
    TextStyle t(double size, FontWeight w, {double ls = 0, double h = 1.25}) =>
        titulo(fontSize: size, fontWeight: w, letterSpacing: ls, height: h);
    TextStyle b(double size, FontWeight w, {double ls = 0, double h = 1.45}) =>
        cuerpo(fontSize: size, fontWeight: w, letterSpacing: ls, height: h);
    return TextTheme(
      displaySmall: t(36, FontWeight.w700, ls: -0.8, h: 1.1),
      headlineMedium: t(28, FontWeight.w700, ls: -0.4, h: 1.15),
      headlineSmall: t(24, FontWeight.w700, ls: -0.2, h: 1.2),
      titleLarge: t(20, FontWeight.w600),
      titleMedium: b(16, FontWeight.w600, h: 1.3),
      titleSmall: b(14, FontWeight.w600, h: 1.3),
      bodyLarge: b(16, FontWeight.w400),
      bodyMedium: b(14, FontWeight.w400),
      bodySmall: b(12, FontWeight.w400, h: 1.4),
      labelLarge: b(14, FontWeight.w600, ls: 0.1),
      labelMedium: b(12, FontWeight.w600, ls: 0.2),
      labelSmall: b(11, FontWeight.w600, ls: 0.4),
    ).apply(bodyColor: cs.onSurface, displayColor: cs.onSurface);
  }

  static ThemeData _build(ColorScheme cs, RegentaTokens tk) {
    final text = _textTheme(cs);
    final rCtl = BorderRadius.circular(tk.radioControl);
    OutlineInputBorder borde(Color color, [double w = 1]) => OutlineInputBorder(
          borderRadius: rCtl,
          borderSide: BorderSide(color: color, width: w),
        );

    return ThemeData(
      useMaterial3: true,
      brightness: cs.brightness,
      colorScheme: cs,
      scaffoldBackgroundColor: tk.fondo,
      textTheme: text,
      extensions: [tk],
      materialTapTargetSize: MaterialTapTargetSize.padded,
      dividerTheme: DividerThemeData(color: cs.outlineVariant, thickness: 1, space: 1),
      cardTheme: CardThemeData(
        color: cs.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 1,
        shadowColor: cs.shadow.withValues(alpha: 0.12),
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(tk.radioTarjeta),
          side: BorderSide.none,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: cs.surfaceContainerLowest,
        isDense: false,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        labelStyle: text.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
        floatingLabelStyle: text.labelLarge?.copyWith(color: cs.primary),
        hintStyle: text.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
        helperStyle: text.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        errorStyle: text.bodySmall?.copyWith(color: cs.error),
        prefixIconColor: cs.onSurfaceVariant,
        border: borde(cs.outline),
        enabledBorder: borde(cs.outline),
        focusedBorder: borde(cs.primary, 2),
        errorBorder: borde(cs.error),
        focusedErrorBorder: borde(cs.error, 2),
        disabledBorder: borde(cs.outlineVariant),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: cs.primary,
          foregroundColor: cs.onPrimary,
          disabledBackgroundColor: cs.onSurface.withValues(alpha: 0.12),
          disabledForegroundColor: cs.onSurface.withValues(alpha: 0.50),
          minimumSize: const Size(64, 48),
          padding: const EdgeInsets.symmetric(horizontal: 20),
          textStyle: text.labelLarge,
          shape: const StadiumBorder(),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: cs.primary,
          minimumSize: const Size(64, 48),
          padding: const EdgeInsets.symmetric(horizontal: 20),
          side: BorderSide(color: cs.outline),
          textStyle: text.labelLarge,
          shape: const StadiumBorder(),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: cs.primary,
          minimumSize: const Size(48, 44),
          textStyle: text.labelLarge,
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
        elevation: 2,
        shape: const StadiumBorder(),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: cs.surfaceContainerLowest,
        selectedColor: cs.primaryContainer,
        labelStyle: text.labelMedium,
        side: BorderSide(color: cs.outlineVariant),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 72,
        elevation: 0,
        backgroundColor: cs.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: cs.primaryContainer,
        indicatorShape: const StadiumBorder(),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        iconTheme: WidgetStateProperty.resolveWith((s) => IconThemeData(
              size: 24,
              color: s.contains(WidgetState.selected) ? cs.onPrimaryContainer : cs.onSurfaceVariant,
            )),
        labelTextStyle: WidgetStateProperty.resolveWith((s) => text.labelMedium?.copyWith(
              color: s.contains(WidgetState.selected) ? cs.onSurface : cs.onSurfaceVariant,
              fontWeight: s.contains(WidgetState.selected) ? FontWeight.w700 : FontWeight.w500,
            )),
      ),
      navigationRailTheme: NavigationRailThemeData(
        backgroundColor: cs.surface,
        elevation: 0,
        minWidth: 84,
        minExtendedWidth: 248,
        groupAlignment: -1,
        labelType: NavigationRailLabelType.all,
        useIndicator: true,
        indicatorColor: cs.primaryContainer,
        indicatorShape: const StadiumBorder(),
        selectedIconTheme: IconThemeData(color: cs.onPrimaryContainer, size: 24),
        unselectedIconTheme: IconThemeData(color: cs.onSurfaceVariant, size: 24),
        selectedLabelTextStyle: text.labelMedium?.copyWith(color: cs.onSurface, fontWeight: FontWeight.w700),
        unselectedLabelTextStyle: text.labelMedium?.copyWith(color: cs.onSurfaceVariant),
      ),
      dataTableTheme: DataTableThemeData(
        headingRowColor: WidgetStatePropertyAll(cs.surfaceContainerLow),
        headingTextStyle: text.labelMedium?.copyWith(color: cs.onSurfaceVariant),
        dataTextStyle: text.bodyMedium?.copyWith(
          fontFeatures: const [FontFeature.tabularFigures()],
        ),
        dividerThickness: 1,
        headingRowHeight: 44,
        dataRowMinHeight: 52,
        dataRowMaxHeight: 60,
      ),
    );
  }
}
