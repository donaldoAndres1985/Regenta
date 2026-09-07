import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-106 criterio 1: el nucleo expone RegentaColors, RegentaType,
/// RegentaSpacing y PatronOperativo, con los valores literales de
/// design/tokens/regenta_theme.dart.
void main() {
  test('RegentaColors trae la paleta con sus hex exactos', () {
    expect(RegentaColors.paper, const Color(0xFFFBFAF7));
    expect(RegentaColors.ink, const Color(0xFF1A1815));
    expect(RegentaColors.accent, const Color(0xFF9A5709));
    expect(RegentaColors.line, const Color(0xFFE4E0D8));
    expect(RegentaColors.crit, const Color(0xFFA0271B));
  });

  test('RegentaColors trae un color y su suave por patron', () {
    expect(RegentaColors.venta, const Color(0xFF9A5709));
    expect(RegentaColors.ventaSoft, const Color(0xFFF6EBD9));
    expect(RegentaColors.reserva, const Color(0xFF0C6473));
    expect(RegentaColors.comanda, const Color(0xFFA03325));
  });

  test('RegentaSpacing trae la escala de los mockups, no una malla de 4/8', () {
    expect(RegentaSpacing.xs, 5);
    expect(RegentaSpacing.sm, 9);
    expect(RegentaSpacing.md, 14);
    expect(RegentaSpacing.lg, 18);
    expect(RegentaSpacing.xl, 22);
    expect(RegentaSpacing.xxl, 30);
    expect(RegentaSpacing.hitTarget, 44);
    expect(RegentaSpacing.radius, 5);
  });

  test('RegentaType trae las dos familias y los estilos con sus medidas', () {
    expect(RegentaType.ui, 'Archivo');
    expect(RegentaType.mono, 'IBMPlexMono');
    expect(RegentaType.tituloPantalla.fontSize, 30);
    expect(RegentaType.tituloPantalla.fontWeight, FontWeight.w700);
    expect(RegentaType.tituloPantalla.letterSpacing, -0.6);
    expect(RegentaType.dinero.fontFamily, 'IBMPlexMono');
    expect(RegentaType.dinero.fontFeatures, contains(const FontFeature.tabularFigures()));
    expect(RegentaType.etiqueta.fontSize, 9.5);
    expect(RegentaType.etiqueta.letterSpacing, 0.76);
  });

  test('el punto de corte movil/escritorio esta expuesto', () {
    expect(kBreakpointEscritorio, 900);
  });
}
