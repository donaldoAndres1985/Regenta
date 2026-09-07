import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

void main() {
  test('el modulo inventario declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'inventario');
    expect(nucleoRequerido, isNotEmpty);
  });
}
