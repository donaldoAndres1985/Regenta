import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/src/navegacion/catalogo.dart';
import 'package:regenta_core/regenta_core.dart';

String _jwt(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256'})}.${parte(payload)}.firma';
}

String _tokenCon(List<String> modulos,
        {String patron = 'VENTA_DIRECTA', List<String> permisos = const []}) =>
    _jwt({
      'negocio_id': 'n-1',
      'sub': 'u-1',
      'plan': 'PROFESIONAL',
      'patron': patron,
      'modulos': modulos,
      'permisos': permisos,
    });

String? _redirigir(String destino, String token) => GuardiaDeRutas(rutasDeLaApp)
    .redirigir(
      destino: destino,
      acceso: EstadoDeAcceso.deClaims(ClaimsDeSesion.deJwt(token)),
    );

void main() {
  test('Criterio 3: una ferretería ve sus módulos y no ve los de restaurante', () {
    final claims = ClaimsDeSesion.deJwt(_tokenCon(
      ['VENTAS', 'INVENTARIO', 'CLIENTES'],
      permisos: ['VENTAS_VENTA_VER', 'INVENTARIO_PRODUCTO_VER', 'CLIENTES_CLIENTE_VER'],
    ));
    final visibles = menuDe(claims).map((e) => e.titulo).toList();

    expect(visibles, contains('Vender'));
    expect(visibles, contains('Inventario'));
    expect(visibles, contains('Clientes'));
    expect(visibles, isNot(contains('Mesas')));
    expect(visibles, isNot(contains('Cocina')));
  });

  test('Criterio 3: un restaurante ve el salón y la cocina', () {
    final claims = ClaimsDeSesion.deJwt(_tokenCon(
      ['MESAS', 'COMANDAS'],
      patron: 'COMANDA',
      permisos: ['MESAS_MESA_VER', 'COMANDAS_COMANDA_VER'],
    ));
    final visibles = menuDe(claims).map((e) => e.titulo).toList();

    expect(visibles, contains('Mesas'));
    expect(visibles, contains('Cocina'));
    expect(visibles, isNot(contains('Inventario')));
  });

  test('Criterio 3: Inicio se ve siempre, no depende de ningún módulo', () {
    final claims = ClaimsDeSesion.deJwt(_tokenCon(const []));

    expect(menuDe(claims).map((e) => e.titulo), contains('Inicio'));
  });

  test('Criterio 3: un módulo sin el permiso de verlo no aparece en el menú', () {
    final claims = ClaimsDeSesion.deJwt(_tokenCon(['VENTAS', 'INVENTARIO'],
        permisos: ['VENTAS_VENTA_VER']));
    final visibles = menuDe(claims).map((e) => e.titulo).toList();

    expect(visibles, contains('Vender'));
    expect(visibles, isNot(contains('Inventario')));
  });

  test('Criterio 4: escribir a mano la ruta de un módulo que no tengo me deja fuera', () {
    expect(_redirigir('/mesas', _tokenCon(['VENTAS'])), GuardiaDeRutas.inicio);
    expect(_redirigir('/ventas', _tokenCon(['VENTAS'])), isNull);
  });

  test('Criterio 4: sin sesión, cualquier ruta lleva al login', () {
    final guardia = GuardiaDeRutas(rutasDeLaApp);
    const sinSesion = EstadoDeAcceso(haySesion: false);

    expect(guardia.redirigir(destino: '/ventas', acceso: sinSesion), GuardiaDeRutas.login);
    expect(guardia.redirigir(destino: '/', acceso: sinSesion), GuardiaDeRutas.login);
    expect(guardia.redirigir(destino: '/login', acceso: sinSesion), isNull);
  });

  test('Criterio 4: con sesión, el login redirige a Inicio', () {
    expect(_redirigir('/login', _tokenCon(['VENTAS'])), GuardiaDeRutas.inicio);
  });

  test('Cada ruta del catálogo declara el módulo que exige', () {
    for (final ruta in rutasDeLaApp) {
      expect(ruta.modulo, isNotNull, reason: '${ruta.ruta} no declara módulo');
      expect(ruta.ruta.startsWith('/'), isTrue);
    }
  });

  test('Cada entrada del menú apunta a una ruta que existe', () {
    final rutas = {for (final r in rutasDeLaApp) r.ruta};
    for (final entrada in menuCompleto) {
      if (entrada.ruta == GuardiaDeRutas.inicio) continue;
      expect(rutas, contains(entrada.ruta), reason: '${entrada.titulo} apunta a la nada');
    }
  });
}
