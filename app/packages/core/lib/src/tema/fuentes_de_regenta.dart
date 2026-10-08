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
/// cada familia con el nombre con que la piden los estilos: las de un paquete
/// vienen como `packages/regenta_core/Archivo`, y [RegentaType] pide
/// `Archivo`. Se llama desde el `flutter_test_config.dart` de cada paquete.
Future<void> cargarFuentesDeRegenta() async {
  final manifiesto = json.decode(await rootBundle.loadString('FontManifest.json')) as List<dynamic>;
  for (final entrada in manifiesto.cast<Map<String, dynamic>>()) {
    final familia = (entrada['family'] as String).split('/').last;
    final cargador = FontLoader(familia);
    for (final fuente in (entrada['fonts'] as List<dynamic>).cast<Map<String, dynamic>>()) {
      cargador.addFont(rootBundle.load(fuente['asset'] as String));
    }
    await cargador.load();
  }
}
