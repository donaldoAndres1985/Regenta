import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  test('el nucleo expone su version de contrato', () {
    expect(versionDelNucleo, isNotEmpty);
  });
}
