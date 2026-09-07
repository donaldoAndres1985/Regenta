import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_comandas/regenta_comandas.dart';

void main() {
  test('el modulo comandas declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'comandas');
    expect(nucleoRequerido, isNotEmpty);
  });
}
