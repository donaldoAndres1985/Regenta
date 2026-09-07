import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-106 criterio 2: Archivo e IBM Plex Mono se empaquetan con el nucleo, asi
/// que cargan igual en Android y en Web, sin pedir la red.
void main() {
  final pubspec = File('pubspec.yaml').readAsStringSync();

  test('el pubspec del nucleo declara la familia Archivo con sus pesos', () {
    expect(pubspec, contains('family: Archivo'));
    for (final peso in [400, 500, 600, 700]) {
      expect(pubspec, contains('weight: $peso'),
          reason: 'Archivo necesita el peso $peso (lo usan los mockups)');
    }
    expect(pubspec, contains('fonts/Archivo-Regular.ttf'));
  });

  test('el pubspec del nucleo declara la familia IBMPlexMono con sus pesos', () {
    expect(pubspec, contains('family: IBMPlexMono'));
    for (final peso in [400, 500, 600]) {
      expect(pubspec, contains('weight: $peso'));
    }
    expect(pubspec, contains('fonts/IBMPlexMono-Regular.ttf'));
  });

  test('los ficheros de fuente referenciados existen de verdad', () {
    final refs = RegExp(r'asset:\s*(fonts/[^\s]+\.ttf)')
        .allMatches(pubspec)
        .map((m) => m.group(1)!)
        .toSet();
    expect(refs, isNotEmpty);
    for (final ref in refs) {
      expect(File(ref).existsSync(), isTrue, reason: 'falta $ref');
    }
  });

  testWidgets('un Text con un estilo del nucleo usa la familia empaquetada',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: regentaTheme(PatronOperativo.ventaDirecta),
      home: Scaffold(
        body: Column(
          children: [
            Text('Titulo', style: RegentaType.tituloPantalla),
            Text(r'$ 12.500', style: RegentaType.dinero),
          ],
        ),
      ),
    ));

    final titulo = tester.widget<Text>(find.text('Titulo'));
    expect(titulo.style!.fontFamily, 'Archivo');
    final dinero = tester.widget<Text>(find.text(r'$ 12.500'));
    expect(dinero.style!.fontFamily, 'IBMPlexMono');
  });
}
