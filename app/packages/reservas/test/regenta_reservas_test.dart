import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_reservas/regenta_reservas.dart';

void main() {
  test('el modulo reservas declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'reservas');
    expect(nucleoRequerido, isNotEmpty);
  });
}
