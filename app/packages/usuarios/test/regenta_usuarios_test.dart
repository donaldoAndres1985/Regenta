import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_usuarios/regenta_usuarios.dart';

void main() {
  test('el modulo usuarios declara su nombre y cuelga del nucleo', () {
    expect(nombreDelModulo, 'usuarios');
    expect(nucleoRequerido, isNotEmpty);
  });
}
