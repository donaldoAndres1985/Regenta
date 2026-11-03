import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// El tema de la app es Pastel Profesional, claro y oscuro, con los tokens
/// propios de Regenta colgados como extensión.
void main() {
  test('el tema claro usa el azul hortensia, el fondo lavanda y los radios pastel', () {
    final tema = PastelTheme.light();
    expect(tema.colorScheme.brightness, Brightness.light);
    expect(tema.colorScheme.primary, const Color(0xFF2E62C0));
    expect(tema.colorScheme.onSurface, const Color(0xFF25253A));
    expect(tema.scaffoldBackgroundColor, const Color(0xFFF8F8FD));

    final tk = tema.extension<RegentaTokens>()!;
    expect(tk.radioTarjeta, 20);
    expect(tk.radioControl, 12);
    expect(tk.vidrio, isFalse);
    expect(tk.modulo('inventario'), const Color(0xFFDDF5EE));
  });

  test('el tema oscuro trae su propia paleta y fondo', () {
    final tema = PastelTheme.dark();
    expect(tema.colorScheme.brightness, Brightness.dark);
    expect(tema.colorScheme.primary, const Color(0xFFA9C4FF));
    expect(tema.scaffoldBackgroundColor, const Color(0xFF15151E));
    expect(tema.extension<RegentaTokens>()!.success, const Color(0xFF7FDDB0));
  });

  test('cada estado de documento tiene su chip, y el anulado no es verde', () {
    final tk = PastelTheme.light().extension<RegentaTokens>()!;
    expect(tk.chip(EstadoDocumento.pagado),
        (fondo: const Color(0xFFDFECE6), texto: const Color(0xFF1D7550)));
    expect(tk.chip(EstadoDocumento.pendiente).texto, const Color(0xFF935700));
    expect(tk.chip(EstadoDocumento.anulado).texto, const Color(0xFF5D5D78));
  });

  test('en Venta directa el tema es exactamente la paleta Pastel', () {
    final tema = regentaTheme(PatronOperativo.ventaDirecta);
    expect(tema.colorScheme.primary, const Color(0xFF2E62C0));
    expect(tema.colorScheme.secondary, const Color(0xFF257A6A));
    expect(tema.colorScheme.secondaryContainer, const Color(0xFFE0ECEA));
  });

  test('titulos en Nunito y cuerpo en Nunito Sans, empaquetadas con el nucleo', () {
    final texto = PastelTheme.light().textTheme;
    expect(texto.headlineMedium!.fontFamily, 'packages/regenta_core/Nunito');
    expect(texto.bodyMedium!.fontFamily, 'packages/regenta_core/NunitoSans');

    final pubspec = File('pubspec.yaml').readAsStringSync();
    expect(pubspec, contains(RegExp(r'family: Nunito$', multiLine: true)));
    expect(pubspec, contains('family: NunitoSans'));
    expect(pubspec, isNot(contains('google_fonts')),
        reason: 'las fuentes no se piden a la red: la app es offline-first');
  });
}
