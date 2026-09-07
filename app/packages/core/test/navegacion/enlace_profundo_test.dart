import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-107 criterio 4: un enlace profundo de una notificacion se resuelve a una
/// ruta; la guardia decide despues si se puede entrar.
void main() {
  test('un payload con ruta y id arma la ruta de la entidad', () {
    final ruta = resolverEnlaceProfundo({'ruta': '/ventas', 'id': '123'});
    expect(ruta, '/ventas/123');
  });

  test('un payload con solo ruta la devuelve tal cual', () {
    expect(resolverEnlaceProfundo({'ruta': '/alertas'}), '/alertas');
  });

  test('una ruta ya completa se respeta', () {
    expect(resolverEnlaceProfundo({'ruta': '/ventas/999'}), '/ventas/999');
  });

  test('un payload sin ruta no lleva a ningun lado', () {
    expect(resolverEnlaceProfundo({'titulo': 'Stock bajo'}), isNull);
    expect(resolverEnlaceProfundo(const {}), isNull);
  });

  test('una ruta que no empieza por / se normaliza', () {
    expect(resolverEnlaceProfundo({'ruta': 'ventas', 'id': '7'}), '/ventas/7');
  });
}
