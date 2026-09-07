import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

void main() {
  test('el modulo reportes declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'reportes');
    expect(nucleoRequerido, isNotEmpty);
  });
}
