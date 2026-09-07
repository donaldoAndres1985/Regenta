import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';

void main() {
  test('el modulo facturacion declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'facturacion');
    expect(nucleoRequerido, isNotEmpty);
  });
}
