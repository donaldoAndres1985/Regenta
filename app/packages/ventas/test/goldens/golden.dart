import 'dart:io';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

/// Las dos composiciones de `design/pantallas/`.
const movil = Size(390, 844);
const escritorio = Size(1440, 900);

/// HU-128. Un golden se compara solo donde se generó su referencia: Linux,
/// con la misma versión de Flutter que CI (ver `app/README.md`, "Golden
/// tests"). En Windows o macOS el texto se rasteriza distinto y el golden
/// fallaría por el sistema operativo, no por la pantalla; ahí se saltan y se
/// corren con `tool/goldens.sh`, que usa el mismo contenedor que CI.
void testGolden(String descripcion, WidgetTesterCallback cuerpo) {
  testWidgets(descripcion, cuerpo, tags: const ['golden'], skip: !Platform.isLinux);
}

Future<void> tamano(WidgetTester tester, Size size) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
}
