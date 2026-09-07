import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_menu/regenta_menu.dart';

void main() {
  test('el modulo menu declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'menu');
    expect(nucleoRequerido, isNotEmpty);
  });
}
