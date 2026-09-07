import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// Arma un JWT de mentira (sin firma real: el cliente no la valida, eso es del
/// gateway) con los claims dados.
String jwtDe(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256', 'typ': 'JWT'})}.${parte(payload)}.firma';
}

void main() {
  test('criterio 1: del token salen plan, patron, modulos, roles y permisos', () {
    final token = jwtDe({
      'sub': '22222222-2222-2222-2222-222222222222',
      'negocio_id': '11111111-1111-1111-1111-111111111111',
      'plan': 'PROFESIONAL',
      'patron': 'VENTA_DIRECTA',
      'modulos': ['VENTAS', 'INVENTARIO', 'FACTURACION'],
      'roles': ['ADMINISTRADOR'],
      'permisos': ['VENTAS_VENTA_CREAR'],
      'exp': 1893456000,
    });

    final claims = ClaimsDeSesion.deJwt(token);

    expect(claims.negocioId, '11111111-1111-1111-1111-111111111111');
    expect(claims.usuarioId, '22222222-2222-2222-2222-222222222222');
    expect(claims.plan, 'PROFESIONAL');
    expect(claims.patron, 'VENTA_DIRECTA');
    expect(claims.modulos, containsAll(['VENTAS', 'INVENTARIO', 'FACTURACION']));
    expect(claims.roles, ['ADMINISTRADOR']);
    expect(claims.permisos, ['VENTAS_VENTA_CREAR']);
  });

  test('un token con base64url sin padding se decodifica igual', () {
    final token = jwtDe({
      'negocio_id': 'n',
      'sub': 'u',
      'plan': 'BASICO',
      'patron': 'RESERVA',
      'modulos': <String>[],
      'roles': <String>[],
      'permisos': <String>[],
    });
    expect(ClaimsDeSesion.deJwt(token).patron, 'RESERVA');
  });

  test('un token que no tiene tres partes revienta con un error claro', () {
    expect(() => ClaimsDeSesion.deJwt('no-es-un-jwt'), throwsFormatException);
  });

  test('claims sin listas: modulos/roles/permisos quedan vacios, no null', () {
    final token = jwtDe({'negocio_id': 'n', 'sub': 'u', 'plan': 'BASICO', 'patron': 'COMANDA'});
    final claims = ClaimsDeSesion.deJwt(token);
    expect(claims.modulos, isEmpty);
    expect(claims.roles, isEmpty);
    expect(claims.permisos, isEmpty);
  });
}
