import 'dart:convert';

import 'package:flutter/services.dart';

/// Carga a mano las fuentes que la app trae empaquetadas (HU-128): Archivo,
/// IBM Plex Mono y los íconos de Material.
///
/// En `flutter test` las fuentes del `pubspec` no se registran solas: sin
/// esto, todo texto se pinta con la fuente de pruebas (cuadritos del mismo
/// ancho) y los íconos salen como cajas. Un golden así no diría nada de la
/// tipografía, que es justo lo que más se despinta.
///
/// Lee el `FontManifest.json` del paquete que corre las pruebas y registra
/// cada familia con los dos nombres con que la piden los estilos: el completo
/// (`packages/regenta_core/Nunito`, como lo pide `PastelTheme`) y el corto
/// (`Archivo`, como lo pide [RegentaType]). Se llama desde el `flutter_test_config.dart` de cada paquete.
Future<void> cargarFuentesDeRegenta() async {
  final manifiesto = json.decode(await rootBundle.loadString('FontManifest.json')) as List<dynamic>;
  for (final entrada in manifiesto.cast<Map<String, dynamic>>()) {
    final completo = entrada['family'] as String;
    // Con los dos nombres: el corto lo pide RegentaType (`Archivo`); el
    // completo, el tema Pastel (`packages/regenta_core/Nunito`).
    for (final familia in {completo, completo.split('/').last}) {
      final cargador = FontLoader(familia);
      for (final fuente in (entrada['fonts'] as List<dynamic>).cast<Map<String, dynamic>>()) {
        cargador.addFont(rootBundle.load(fuente['asset'] as String));
      }
      await cargador.load();
    }
  }
}
