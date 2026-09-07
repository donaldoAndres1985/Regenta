import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

String _jwt(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256'})}.${parte(payload)}.firma';
}

void main() {
  ModulosActivos activos(List<String> codigos) => ModulosActivos(
        plan: 'PROFESIONAL',
        patron: 'VENTA_DIRECTA',
        codigos: codigos,
      );

  test('tiene() responde por codigo, sin importar el orden ni el caso', () {
    final m = activos(['VENTAS', 'INVENTARIO']);
    expect(m.tiene('VENTAS'), isTrue);
    expect(m.tiene('ventas'), isTrue);
    expect(m.tiene('FACTURACION'), isFalse);
  });

  test('criterio 2: una entrada de navegacion de un modulo inactivo no aparece', () {
    final catalogo = [
      const EntradaDeNavegacion(modulo: 'VENTAS', ruta: '/ventas', titulo: 'Ventas'),
      const EntradaDeNavegacion(modulo: 'FACTURACION', ruta: '/facturas', titulo: 'Facturas'),
      const EntradaDeNavegacion(modulo: 'INVENTARIO', ruta: '/inventario', titulo: 'Inventario'),
    ];

    final visibles = activos(['VENTAS', 'INVENTARIO']).visiblesDe(catalogo);

    expect(visibles.map((e) => e.ruta), ['/ventas', '/inventario']);
  });

  test('una entrada sin modulo (siempre visible, tipo Core) nunca se filtra', () {
    final catalogo = [
      const EntradaDeNavegacion(modulo: null, ruta: '/inicio', titulo: 'Inicio'),
      const EntradaDeNavegacion(modulo: 'VENTAS', ruta: '/ventas', titulo: 'Ventas'),
    ];
    expect(activos([]).visiblesDe(catalogo).map((e) => e.ruta), ['/inicio']);
  });

  test('se arma desde los claims del token', () {
    final claims = ClaimsDeSesion.deJwt(_jwt({
      'negocio_id': 'n',
      'sub': 'u',
      'plan': 'EMPRESARIAL',
      'patron': 'COMANDA',
      'modulos': ['MENU', 'MESAS', 'COMANDAS'],
    }));

    final m = ModulosActivos.deClaims(claims);

    expect(m.plan, 'EMPRESARIAL');
    expect(m.patron, 'COMANDA');
    expect(m.tiene('MESAS'), isTrue);
  });
}
