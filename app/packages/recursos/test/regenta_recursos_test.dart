import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_recursos/regenta_recursos.dart';

void main() {
  test('el modulo recursos declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'recursos');
    expect(nucleoRequerido, isNotEmpty);
  });
}
