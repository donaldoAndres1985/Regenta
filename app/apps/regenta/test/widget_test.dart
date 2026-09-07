import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/main.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  testWidgets('la app arranca y muestra la version del nucleo', (tester) async {
    await tester.pumpWidget(const RegentaApp());

    expect(find.text('Regenta'), findsOneWidget);
    expect(find.text('Nucleo $versionDelNucleo'), findsOneWidget);
  });
}
