import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-106 criterio 1: el nucleo expone RegentaColors, RegentaType,
/// RegentaSpacing y PatronOperativo. La paleta es la de Pastel Profesional.
void main() {
  test('RegentaColors trae la paleta con sus hex exactos', () {
    expect(RegentaColors.paper, const Color(0xFFF8F8FD));
    expect(RegentaColors.ink, const Color(0xFF25253A));
    expect(RegentaColors.ink2, const Color(0xFF5D5D78));
    expect(RegentaColors.accent, const Color(0xFF2E62C0));
    expect(RegentaColors.line, const Color(0xFFE7E7F2));
    expect(RegentaColors.muted, const Color(0xFF717183));
    expect(RegentaColors.faint, const Color(0xFFA6A6B5),
        reason: 'faint es el gris de los placeholders, no un borde');
    expect(RegentaColors.crit, const Color(0xFFB3304F));
    expect(RegentaColors.ok, const Color(0xFF1D7550));
    expect(RegentaColors.warn, const Color(0xFF935700));
  });

  test('RegentaColors trae un color y su suave por patron', () {
    expect(RegentaColors.venta, const Color(0xFF257A6A));
    expect(RegentaColors.ventaSoft, const Color(0xFFE0ECEA));
    expect(RegentaColors.reserva, const Color(0xFF5B57C2));
    expect(RegentaColors.comanda, const Color(0xFFC2416F));
  });

  test('RegentaColors trae el color pastel de cada modulo', () {
    expect(RegentaColors.moduloVentas, const Color(0xFFECEBFF));
    expect(RegentaColors.moduloInventario, const Color(0xFFDDF5EE));
    expect(RegentaColors.moduloCaja, const Color(0xFFE0F0FF));
    expect(RegentaColors.moduloClientes, const Color(0xFFFFE4EE));
    expect(RegentaColors.moduloCompras, const Color(0xFFFFF4C7));
    expect(RegentaColors.moduloReportes, const Color(0xFFFFE9DC));
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
