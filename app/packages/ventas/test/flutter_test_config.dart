import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-128: todas las pruebas del paquete corren con Archivo e IBM Plex Mono
/// de verdad, no con la fuente de pruebas. Así un texto que no cabe a 390 px
/// falla aquí y no en el celular de alguien.
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  TestWidgetsFlutterBinding.ensureInitialized();
  await cargarFuentesDeRegenta();
  await testMain();
}
