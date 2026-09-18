import 'dart:convert';

import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/main.dart';
import 'package:regenta/src/arranque/dependencias.dart';
import 'package:regenta_core/regenta_core.dart';

/// La carcasa de punta a punta: arrancar la app y ver dónde aterriza.
/// HU-119 criterios 1, 3 y 6.

class _AlmacenEnMemoria implements AlmacenDeSesion {
  _AlmacenEnMemoria([this._sesion]);

  Sesion? _sesion;

  @override
  Future<void> guardar(Sesion sesion) async => _sesion = sesion;

  @override
  Future<Sesion?> leer() async => _sesion;

  @override
  Future<void> borrar() async => _sesion = null;
}

String _jwt(Map<String, dynamic> payload) {
  String parte(Object o) =>
      base64Url.encode(utf8.encode(jsonEncode(o))).replaceAll('=', '');
  return '${parte({'alg': 'HS256'})}.${parte(payload)}.firma';
}

Sesion _sesionCon({
  required List<String> modulos,
  String patron = 'VENTA_DIRECTA',
  List<String> permisos = const [
    'VENTAS_VENTA_VER',
    'INVENTARIO_PRODUCTO_VER',
    'MESAS_MESA_VER',
    'COMANDAS_COMANDA_VER',
  ],
}) =>
    Sesion(
      tokenDeAcceso: _jwt({
        'negocio_id': 'n-1',
        'sub': 'u-1',
        'plan': 'PROFESIONAL',
        'patron': patron,
        'modulos': modulos,
        'roles': const ['ADMINISTRADOR'],
        'permisos': permisos,
      }),
      tokenDeRefresco: 'refresco',
      expiraEn: DateTime.now().toUtc().add(const Duration(minutes: 30)),
      negocioId: 'n-1',
      usuarioId: 'u-1',
      plan: 'PROFESIONAL',
      patron: patron,
      roles: const ['ADMINISTRADOR'],
      modulos: modulos,
    );

Future<DependenciasDeLaApp> _arrancar(WidgetTester tester, {Sesion? guardada}) async {
  await tester.binding.setSurfaceSize(const Size(390, 844));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final deps = DependenciasDeLaApp.paraPruebas(
    base: BaseLocal(NativeDatabase.memory()),
    almacen: _AlmacenEnMemoria(guardada),
  );
  addTearDown(deps.cerrar);
  await deps.motor.iniciar();
  await tester.pumpWidget(ProviderScope(
    overrides: deps.overrides(),
    child: RegentaApp(dependencias: deps),
  ));
  await tester.pumpAndSettle();
  return deps;
}

void main() {
  testWidgets('Criterio 1: sin sesión, la app abre en la pantalla de entrada',
      (tester) async {
    await _arrancar(tester);

    expect(find.text('Entrar'), findsOneWidget);
    expect(find.byKey(const Key('login-correo')), findsOneWidget);
  });

  testWidgets('Criterio 6: con sesión guardada, la app abre dentro y no pasa por el login',
      (tester) async {
    await _arrancar(tester, guardada: _sesionCon(modulos: ['VENTAS']));

    expect(find.text('Inicio'), findsOneWidget);
    expect(find.byKey(const Key('login-correo')), findsNothing);
  });

  testWidgets('Criterio 3: el menú muestra los módulos del negocio y no los demás',
      (tester) async {
    await _arrancar(tester, guardada: _sesionCon(modulos: ['VENTAS', 'INVENTARIO']));

    expect(find.text('Vender'), findsOneWidget);
    expect(find.text('Inventario'), findsOneWidget);
    expect(find.text('Mesas'), findsNothing);
  });

  testWidgets('Criterio 3: un restaurante ve el salón, no el inventario', (tester) async {
    await _arrancar(tester,
        guardada: _sesionCon(modulos: ['MESAS', 'COMANDAS'], patron: 'COMANDA'));

    expect(find.text('Mesas'), findsOneWidget);
    expect(find.text('Cocina'), findsOneWidget);
    expect(find.text('Inventario'), findsNothing);
  });

  testWidgets('El patrón operativo del negocio decide el color secundario del tema',
      (tester) async {
    await _arrancar(tester,
        guardada: _sesionCon(modulos: ['COMANDAS'], patron: 'COMANDA'));

    final material = tester.widget<MaterialApp>(find.byType(MaterialApp));
    expect(material.theme!.colorScheme.secondary, RegentaColors.comanda);
    expect(material.theme!.colorScheme.primary, RegentaColors.accent);
  });

  testWidgets('Criterio 6: cerrar sesión vuelve a la pantalla de entrada', (tester) async {
    final deps = await _arrancar(tester, guardada: _sesionCon(modulos: ['VENTAS']));

    await deps.motor.cerrar();
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('login-correo')), findsOneWidget);
    expect(deps.perfil.claims, isNull);
  });

  testWidgets('Un negocio sin módulos activos lo dice, no muestra una pantalla vacía',
      (tester) async {
    await _arrancar(tester, guardada: _sesionCon(modulos: const []));

    expect(find.textContaining('no tiene módulos activos'), findsOneWidget);
  });
}
