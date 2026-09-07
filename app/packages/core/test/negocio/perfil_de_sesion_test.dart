import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

String _jwt(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256'})}.${parte(payload)}.firma';
}

String tokenCon(List<String> modulos, {String plan = 'PROFESIONAL'}) => _jwt({
      'negocio_id': 'n',
      'sub': 'u',
      'plan': plan,
      'patron': 'VENTA_DIRECTA',
      'modulos': modulos,
      'roles': ['ADMINISTRADOR'],
      'permisos': <String>[],
    });

void main() {
  test('sin token, no hay perfil', () {
    expect(PerfilDeSesion().claims, isNull);
    expect(PerfilDeSesion().modulos.tiene('VENTAS'), isFalse);
  });

  test('al fijar el token, el perfil conoce plan, patron y modulos', () {
    final perfil = PerfilDeSesion()..fijarToken(tokenCon(['VENTAS', 'INVENTARIO']));

    expect(perfil.claims!.plan, 'PROFESIONAL');
    expect(perfil.modulos.tiene('INVENTARIO'), isTrue);
    expect(perfil.modulos.tiene('FACTURACION'), isFalse);
  });

  test('criterio 3: al refrescar el token con un modulo nuevo, el perfil avisa', () {
    final perfil = PerfilDeSesion()..fijarToken(tokenCon(['VENTAS']));
    var avisos = 0;
    perfil.addListener(() => avisos++);

    perfil.fijarToken(tokenCon(['VENTAS', 'FACTURACION']));

    expect(perfil.modulos.tiene('FACTURACION'), isTrue);
    expect(avisos, 1);
  });

  test('fijar el mismo token no dispara avisos de mas', () {
    final t = tokenCon(['VENTAS']);
    final perfil = PerfilDeSesion()..fijarToken(t);
    var avisos = 0;
    perfil.addListener(() => avisos++);

    perfil.fijarToken(t);

    expect(avisos, 0);
  });

  test('limpiar deja el perfil sin claims y avisa', () {
    final perfil = PerfilDeSesion()..fijarToken(tokenCon(['VENTAS']));
    var avisos = 0;
    perfil.addListener(() => avisos++);

    perfil.limpiar();

    expect(perfil.claims, isNull);
    expect(avisos, 1);
  });
}
