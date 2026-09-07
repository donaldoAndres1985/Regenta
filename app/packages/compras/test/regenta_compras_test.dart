import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';

void main() {
  test('el modulo compras declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'compras');
    expect(nucleoRequerido, isNotEmpty);
  });
}
