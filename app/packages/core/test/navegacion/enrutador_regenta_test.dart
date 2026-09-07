import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

String _jwt(Map<String, dynamic> payload) {
  String p(Object o) => base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${p({'alg': 'HS256'})}.${p(payload)}.firma';
}

String tokenCon(List<String> modulos, {List<String> permisos = const []}) => _jwt({
      'negocio_id': 'n',
      'sub': 'u',
      'plan': 'PROFESIONAL',
      'patron': 'VENTA_DIRECTA',
      'modulos': modulos,
      'permisos': permisos,
    });

void main() {
  late PerfilDeSesion perfil;

  final rutas = [
    RutaProtegida(
      ruta: '/ventas',
      modulo: 'VENTAS',
      builder: (_, _) => const Text('PANTALLA VENTAS'),
    ),
    RutaProtegida(
      ruta: '/facturas',
      modulo: 'FACTURACION',
      builder: (_, _) => const Text('PANTALLA FACTURAS'),
    ),
  ];

  Widget appCon(GoRouter router) => MaterialApp.router(routerConfig: router);

  setUp(() => perfil = PerfilDeSesion());

  GoRouter configurar({String inicial = '/'}) => crearEnrutador(
        rutas: rutas,
        perfil: perfil,
        ubicacionInicial: inicial,
        login: (_, _) => const Text('PANTALLA LOGIN'),
        inicio: (_, _) => const Text('PANTALLA INICIO'),
      );

  testWidgets('criterio 3: sin sesion, ir a una ruta protegida cae en el login',
      (tester) async {
    await tester.pumpWidget(appCon(configurar(inicial: '/ventas')));
    await tester.pumpAndSettle();

    expect(find.text('PANTALLA LOGIN'), findsOneWidget);
    expect(find.text('PANTALLA VENTAS'), findsNothing);
  });

  testWidgets('con sesion y el modulo activo, la ruta se muestra', (tester) async {
    perfil.fijarToken(tokenCon(['VENTAS']));
    await tester.pumpWidget(appCon(configurar(inicial: '/ventas')));
    await tester.pumpAndSettle();

    expect(find.text('PANTALLA VENTAS'), findsOneWidget);
  });

  testWidgets('criterio 1: con sesion pero el modulo inactivo, redirige al inicio',
      (tester) async {
    perfil.fijarToken(tokenCon(['VENTAS'])); // sin FACTURACION
    await tester.pumpWidget(appCon(configurar(inicial: '/facturas')));
    await tester.pumpAndSettle();

    expect(find.text('PANTALLA INICIO'), findsOneWidget);
    expect(find.text('PANTALLA FACTURAS'), findsNothing);
  });

  testWidgets('al cerrar sesion en caliente, la vista protegida salta al login',
      (tester) async {
    perfil.fijarToken(tokenCon(['VENTAS']));
    await tester.pumpWidget(appCon(configurar(inicial: '/ventas')));
    await tester.pumpAndSettle();
    expect(find.text('PANTALLA VENTAS'), findsOneWidget);

    perfil.limpiar();
    await tester.pumpAndSettle();

    expect(find.text('PANTALLA LOGIN'), findsOneWidget);
  });
}
