// Regenta · Propuesta 1 · Aurora
// Tema Material 3 listo para usar (claro y oscuro).
//
// pubspec.yaml:
//   dependencies:
//     google_fonts: ^6.2.1
//
// Uso:
//   MaterialApp(
//     theme: AuroraTheme.light(),
//     darkTheme: AuroraTheme.dark(),
//     themeMode: ThemeMode.system,
//   );
//
// Requiere Flutter 3.27 o superior (CardThemeData, WidgetStateProperty).

import 'dart:ui' show FontFeature, lerpDouble;

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'patron_operativo.dart';

/// Estados de documento que se pintan como chip (ventas, facturas, compras).
enum EstadoDocumento { pagado, pendiente, anulado }

/// Tokens propios de Regenta que Material 3 no cubre:
/// colores de estado, degradados, color por módulo y forma.
/// Uso: `final t = Theme.of(context).extension<RegentaTokens>()!;`
@immutable
class RegentaTokens extends ThemeExtension<RegentaTokens> {
  const RegentaTokens({
    required this.success,
    required this.successContainer,
    required this.warning,
    required this.warningContainer,
    required this.info,
    required this.infoContainer,
    required this.anulado,
    required this.anuladoContainer,
    required this.fondo,
    required this.superficieElevada,
    required this.heroGradient,
    required this.modulos,
    required this.radioTarjeta,
    required this.radioControl,
    required this.sombraTarjeta,
    required this.vidrio,
  });

  final Color success;
  final Color successContainer;
  final Color warning;
  final Color warningContainer;
  final Color info;
  final Color infoContainer;
  final Color anulado;
  final Color anuladoContainer;
  final Color fondo;
  final Color superficieElevada;
  final LinearGradient heroGradient;

  /// Color por módulo: ventas, inventario, caja, clientes, compras, reportes.
  final Map<String, Color> modulos;
  final double radioTarjeta;
  final double radioControl;
  final List<BoxShadow> sombraTarjeta;

  /// Si la dirección usa superficies tipo vidrio (BackdropFilter).
  /// En web, úsalo solo en barras de navegación, nunca en listas.
  final bool vidrio;

  /// Colores (fondo, texto) del chip para un estado de documento.
  /// El estado anulado además debe tachar el monto: no depende solo del color.
  ({Color fondo, Color texto}) chip(EstadoDocumento e) => switch (e) {
        EstadoDocumento.pagado => (fondo: successContainer, texto: success),
        EstadoDocumento.pendiente => (fondo: warningContainer, texto: warning),
        EstadoDocumento.anulado => (fondo: anuladoContainer, texto: anulado),
      };

  Color modulo(String nombre) => modulos[nombre] ?? success;

  @override
  RegentaTokens copyWith({
    Color? success,
    Color? successContainer,
    Color? warning,
    Color? warningContainer,
    Color? info,
    Color? infoContainer,
    Color? anulado,
    Color? anuladoContainer,
    Color? fondo,
    Color? superficieElevada,
    LinearGradient? heroGradient,
    Map<String, Color>? modulos,
    double? radioTarjeta,
    double? radioControl,
    List<BoxShadow>? sombraTarjeta,
    bool? vidrio,
  }) {
    return RegentaTokens(
      success: success ?? this.success,
      successContainer: successContainer ?? this.successContainer,
      warning: warning ?? this.warning,
      warningContainer: warningContainer ?? this.warningContainer,
      info: info ?? this.info,
      infoContainer: infoContainer ?? this.infoContainer,
      anulado: anulado ?? this.anulado,
      anuladoContainer: anuladoContainer ?? this.anuladoContainer,
      fondo: fondo ?? this.fondo,
      superficieElevada: superficieElevada ?? this.superficieElevada,
      heroGradient: heroGradient ?? this.heroGradient,
      modulos: modulos ?? this.modulos,
      radioTarjeta: radioTarjeta ?? this.radioTarjeta,
      radioControl: radioControl ?? this.radioControl,
      sombraTarjeta: sombraTarjeta ?? this.sombraTarjeta,
      vidrio: vidrio ?? this.vidrio,
    );
  }

  @override
  RegentaTokens lerp(ThemeExtension<RegentaTokens>? other, double t) {
    if (other is! RegentaTokens) return this;
    Color l(Color a, Color b) => Color.lerp(a, b, t)!;
    return RegentaTokens(
      success: l(success, other.success),
      successContainer: l(successContainer, other.successContainer),
      warning: l(warning, other.warning),
      warningContainer: l(warningContainer, other.warningContainer),
      info: l(info, other.info),
      infoContainer: l(infoContainer, other.infoContainer),
      anulado: l(anulado, other.anulado),
      anuladoContainer: l(anuladoContainer, other.anuladoContainer),
      fondo: l(fondo, other.fondo),
      superficieElevada: l(superficieElevada, other.superficieElevada),
      heroGradient: LinearGradient.lerp(heroGradient, other.heroGradient, t)!,
      modulos: t < 0.5 ? modulos : other.modulos,
      radioTarjeta: lerpDouble(radioTarjeta, other.radioTarjeta, t)!,
      radioControl: lerpDouble(radioControl, other.radioControl, t)!,
      sombraTarjeta: BoxShadow.lerpList(sombraTarjeta, other.sombraTarjeta, t)!,
      vidrio: t < 0.5 ? vidrio : other.vidrio,
    );
  }
}

abstract final class AuroraTheme {
  static ThemeData light({PatronOperativo patron = PatronOperativo.ventaDirecta}) =>
      _build(_lightScheme.copyWith(primary: const Color(0xFF5B3FD9), secondary: patron.color), _lightTokens);

  static ThemeData dark({PatronOperativo patron = PatronOperativo.ventaDirecta}) =>
      _build(_darkScheme.copyWith(secondary: patron.color), _darkTokens);

  /// Cifras de dinero y cantidades: siempre con dígitos tabulares
  /// para que las columnas del POS y las tablas cuadren.
  static TextStyle numeros(BuildContext context,
      {double size = 16, FontWeight weight = FontWeight.w600}) {
    return GoogleFonts.manrope(
      fontSize: size,
      fontWeight: weight,
      color: Theme.of(context).colorScheme.onSurface,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
  }

  static const ColorScheme _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF5B3FD9),
    onPrimary: Color(0xFFFFFFFF),
    primaryContainer: Color(0xFFE8E4FA),
    onPrimaryContainer: Color(0xFF322377),
    secondary: Color(0xFF0B7F7A),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: Color(0xFFDDEDEC),
    onSecondaryContainer: Color(0xFF064643),
    tertiary: Color(0xFFCC3359),
    onTertiary: Color(0xFFFFFFFF),
    tertiaryContainer: Color(0xFFF8E2E8),
    onTertiaryContainer: Color(0xFF701C31),
    error: Color(0xFFC42840),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFF7E1E4),
    onErrorContainer: Color(0xFF6C1623),
    surface: Color(0xFFFFFFFF),
    onSurface: Color(0xFF1B1838),
    onSurfaceVariant: Color(0xFF55527A),
    surfaceContainerLowest: Color(0xFFFFFFFF),
    surfaceContainerLow: Color(0xFFF9F8FD),
    surfaceContainer: Color(0xFFF3F2FB),
    surfaceContainerHigh: Color(0xFFECEBF3),
    surfaceContainerHighest: Color(0xFFE4E3EC),
    outline: Color(0xFFA2A0B6),
    outlineVariant: Color(0xFFE3E0F5),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFF1B1838),
    onInverseSurface: Color(0xFFF3F2FB),
    inversePrimary: Color(0xFFA897FF),
  );

  static const ColorScheme _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFFA897FF),
    onPrimary: Color(0xFF1C0F5E),
    primaryContainer: Color(0xFF3C3662),
    onPrimaryContainer: Color(0xFFE1DBFF),
    secondary: Color(0xFF4FD8CF),
    onSecondary: Color(0xFF00302D),
    secondaryContainer: Color(0xFF274656),
    onSecondaryContainer: Color(0xFFC1F1EE),
    tertiary: Color(0xFFFF8BA3),
    onTertiary: Color(0xFF4A0618),
    tertiaryContainer: Color(0xFF51344C),
    onTertiaryContainer: Color(0xFFFFD6DF),
    error: Color(0xFFFF7086),
    onError: Color(0xFF0F0E1D),
    errorContainer: Color(0xFF512D45),
    onErrorContainer: Color(0xFFFFCDD5),
    surface: Color(0xFF1A1830),
    onSurface: Color(0xFFF1EFFF),
    onSurfaceVariant: Color(0xFFB3AFD6),
    surfaceContainerLowest: Color(0xFF141326),
    surfaceContainerLow: Color(0xFF1A1830),
    surfaceContainer: Color(0xFF24223D),
    surfaceContainerHigh: Color(0xFF312F49),
    surfaceContainerHighest: Color(0xFF3E3D54),
    outline: Color(0xFF6E6B8B),
    outlineVariant: Color(0xFF2F2C4D),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFFF1EFFF),
    onInverseSurface: Color(0xFF0F0E1D),
    inversePrimary: Color(0xFF5B3FD9),
  );

  static final RegentaTokens _lightTokens = RegentaTokens(
    success: const Color(0xFF12784A),
    successContainer: const Color(0xFFDEECE6),
    warning: const Color(0xFF9A5800),
    warningContainer: const Color(0xFFF1E8DB),
    info: const Color(0xFF2459C7),
    infoContainer: const Color(0xFFE0E8F7),
    anulado: const Color(0xFF55527A),
    anuladoContainer: const Color(0xFFEEEEF2),
    fondo: const Color(0xFFF3F2FB),
    superficieElevada: const Color(0xFFFFFFFF),
    heroGradient: const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0xFF7B5CFF), Color(0xFF2BC8B7), Color(0xFFFF7A93)],
    ),
    modulos: const {
      'ventas': Color(0xFF7B5CFF),
      'inventario': Color(0xFF2BC8B7),
      'caja': Color(0xFFFF7A93),
      'clientes': Color(0xFFFFB547),
      'compras': Color(0xFF5B8CFF),
      'reportes': Color(0xFFB26BFF),
    },
    radioTarjeta: 24,
    radioControl: 14,
    sombraTarjeta: const [BoxShadow(color: Color(0x1F000000), blurRadius: 24, offset: Offset(0, 8))],
    vidrio: true,
  );

  static final RegentaTokens _darkTokens = RegentaTokens(
    success: const Color(0xFF4CD58E),
    successContainer: const Color(0xFF264547),
    warning: const Color(0xFFFFB547),
    warningContainer: const Color(0xFF513E36),
    info: const Color(0xFF7DA9FF),
    infoContainer: const Color(0xFF323B62),
    anulado: const Color(0xFFB3AFD6),
    anuladoContainer: const Color(0xFF32304B),
    fondo: const Color(0xFF0F0E1D),
    superficieElevada: const Color(0xFF24223D),
    heroGradient: const LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: [Color(0x667B5CFF), Color(0x662BC8B7), Color(0x66FF7A93)],
    ),
    modulos: const {
      'ventas': Color(0xFF7B5CFF),
      'inventario': Color(0xFF2BC8B7),
      'caja': Color(0xFFFF7A93),
      'clientes': Color(0xFFFFB547),
      'compras': Color(0xFF5B8CFF),
      'reportes': Color(0xFFB26BFF),
    },
    radioTarjeta: 24,
    radioControl: 14,
    sombraTarjeta: const [BoxShadow(color: Color(0x1F000000), blurRadius: 24, offset: Offset(0, 8))],
    vidrio: true,
  );

  static TextTheme _textTheme(ColorScheme cs) {
    TextStyle t(double size, FontWeight w, {double ls = 0, double h = 1.25}) =>
        GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: w, letterSpacing: ls, height: h);
    TextStyle b(double size, FontWeight w, {double ls = 0, double h = 1.45}) =>
        GoogleFonts.manrope(fontSize: size, fontWeight: w, letterSpacing: ls, height: h);
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
        elevation: 0,
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
