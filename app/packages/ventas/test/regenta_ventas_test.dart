import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

void main() {
  test('el modulo ventas declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'ventas');
    expect(nucleoRequerido, isNotEmpty);
  });
}
