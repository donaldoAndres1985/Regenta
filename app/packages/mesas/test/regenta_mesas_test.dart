import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_mesas/regenta_mesas.dart';

void main() {
  test('el modulo mesas declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'mesas');
    expect(nucleoRequerido, isNotEmpty);
  });
}
