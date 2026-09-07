import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// HU-106 criterio 4: fuera del archivo de tema, ningun widget escribe un color
/// a mano. Los colores salen de RegentaColors, que es lo unico que comparte los
/// valores con los mockups.
///
/// Este test hace de lint: recorre todo `lib/` de todos los paquetes y de la
/// app y falla si encuentra `Color(0x...)` o `Color.fromARGB(` fuera de
/// `packages/core/lib/src/tema/`.
void main() {
  test('no hay colores literales fuera de packages/core/lib/src/tema', () {
    final raiz = Directory('../..'); // desde packages/core -> raiz de app/
    final permitido = p('packages/core/lib/src/tema');
    final literal = RegExp(r'Color\(\s*0x|Color\.fromARGB\(|Color\.fromRGBO\(');

    final infractores = <String>[];

    for (final ent in raiz.listSync(recursive: true).whereType<File>()) {
      final ruta = ent.path.replaceAll('\\', '/');
      if (!ruta.endsWith('.dart')) continue;
      if (!ruta.contains('/lib/')) continue;
      if (ruta.contains('/$permitido/') || ruta.contains(permitido)) continue;
      if (ruta.contains('/.dart_tool/')) continue;

      final lineas = ent.readAsLinesSync();
      for (var i = 0; i < lineas.length; i++) {
        if (literal.hasMatch(lineas[i])) {
          infractores.add('$ruta:${i + 1}  ${lineas[i].trim()}');
        }
      }
    }

    expect(
      infractores,
      isEmpty,
      reason: 'usa RegentaColors en vez de un color a mano:\n${infractores.join('\n')}',
    );
  });
}

String p(String s) => s.replaceAll('\\', '/');
