import 'dart:convert';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

String jwtDe(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256'})}.${parte(payload)}.firma';
}

void main() {
  final rutas = [
    RutaProtegida(ruta: '/ventas', modulo: 'VENTAS', builder: (_, _) => const SizedBox()),
    RutaProtegida(
        ruta: '/facturas', modulo: 'FACTURACION', builder: (_, _) => const SizedBox()),
    RutaProtegida(
        ruta: '/usuarios',
        modulo: 'USUARIOS',
        permiso: 'USUARIOS_USUARIO_CREAR',
        builder: (_, _) => const SizedBox()),
  ];

  String? redirigir(String destino, EstadoDeAcceso acceso) =>
      GuardiaDeRutas(rutas).redirigir(destino: destino, acceso: acceso);

  const conSesion = EstadoDeAcceso(
    haySesion: true,
    modulos: {'VENTAS', 'INVENTARIO', 'USUARIOS'},
    permisos: {'VENTAS_VENTA_CREAR'},
  );
  const sinSesion = EstadoDeAcceso(haySesion: false);

  test('criterio 3: sin sesion, cualquier ruta protegida manda al login', () {
    expect(redirigir('/ventas', sinSesion), '/login');
    expect(redirigir('/', sinSesion), '/login');
  });

  test('sin sesion, el login se deja pasar', () {
    expect(redirigir('/login', sinSesion), isNull);
  });

  test('con sesion, entrar al login te devuelve al inicio', () {
    expect(redirigir('/login', conSesion), '/');
  });

  test('criterio 1: con sesion pero el modulo inactivo, redirige y no muestra', () {
    expect(redirigir('/facturas', conSesion), '/');
  });

  test('con sesion y el modulo activo, deja pasar', () {
    expect(redirigir('/ventas', conSesion), isNull);
    expect(redirigir('/', conSesion), isNull);
  });

  test('criterio 2: con el modulo activo pero sin el permiso, redirige', () {
    expect(redirigir('/usuarios', conSesion), '/');
  });

  test('con el modulo y el permiso, deja pasar', () {
    const admin = EstadoDeAcceso(
      haySesion: true,
      modulos: {'USUARIOS'},
      permisos: {'USUARIOS_USUARIO_CREAR'},
    );
    expect(redirigir('/usuarios', admin), isNull);
  });

  test('una ruta que no esta en la lista no la toca la guardia', () {
    expect(redirigir('/algo-raro', conSesion), isNull);
  });

  test('EstadoDeAcceso.deClaims lee modulos y permisos del token', () {
    final claims = ClaimsDeSesion.deJwt(jwtDe({
      'negocio_id': 'n',
      'sub': 'u',
      'plan': 'PROFESIONAL',
      'patron': 'VENTA_DIRECTA',
      'modulos': ['VENTAS'],
      'permisos': ['VENTAS_VENTA_CREAR'],
    }));
    final acceso = EstadoDeAcceso.deClaims(claims);
    expect(acceso.haySesion, isTrue);
    expect(acceso.modulos, contains('VENTAS'));
    expect(acceso.permisos, contains('VENTAS_VENTA_CREAR'));
  });

  test('EstadoDeAcceso.deClaims(null) es sin sesion', () {
    expect(EstadoDeAcceso.deClaims(null).haySesion, isFalse);
  });
}
