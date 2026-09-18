import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/src/arranque/proveedores.dart';
import 'package:regenta/src/ui/pantalla_login.dart';
import 'package:regenta_core/regenta_core.dart';

/// El servidor de autenticación, sin red.
class _AuthFalso implements ClienteAuth {
  _AuthFalso({this.alEntrar, this.alEntrarLento});

  Sesion Function(Credenciales credenciales)? alEntrar;
  Future<Sesion> Function(Credenciales credenciales)? alEntrarLento;
  final List<Credenciales> recibidas = [];

  @override
  Future<Sesion> entrar(Credenciales credenciales) async {
    recibidas.add(credenciales);
    final lento = alEntrarLento;
    if (lento != null) return lento(credenciales);
    final hecho = alEntrar;
    if (hecho == null) throw const CredencialesInvalidas();
    return hecho(credenciales);
  }

  @override
  Future<Sesion> refrescar({
    required String tokenDeRefresco,
    String? dispositivoId,
    String? plataforma,
  }) async =>
      throw const RefrescoRechazado();
}

class _AlmacenEnMemoria implements AlmacenDeSesion {
  Sesion? _sesion;
  int borrados = 0;

  @override
  Future<void> guardar(Sesion sesion) async => _sesion = sesion;

  @override
  Future<Sesion?> leer() async => _sesion;

  @override
  Future<void> borrar() async {
    borrados++;
    _sesion = null;
  }
}

Sesion _sesionDe({String negocio = 'n-1'}) => Sesion(
      tokenDeAcceso: 'acceso',
      tokenDeRefresco: 'refresco',
      expiraEn: DateTime.now().toUtc().add(const Duration(minutes: 15)),
      negocioId: negocio,
      usuarioId: 'u-1',
      plan: 'PROFESIONAL',
      patron: 'VENTA_DIRECTA',
      roles: const ['ADMINISTRADOR'],
      modulos: const ['VENTAS'],
    );

Future<MotorDeSesion> _montar(
  WidgetTester tester,
  _AuthFalso auth, {
  AlmacenDeSesion? almacen,
  Size size = const Size(390, 844),
}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final motor = MotorDeSesion(cliente: auth, almacen: almacen ?? _AlmacenEnMemoria());
  await tester.pumpWidget(ProviderScope(
    overrides: [motorDeSesionProvider.overrideWithValue(motor)],
    child: const MaterialApp(home: PantallaLogin()),
  ));
  await tester.pumpAndSettle();
  return motor;
}

Future<void> _escribirCredenciales(WidgetTester tester,
    {String correo = 'donaldo@eltornillo.co', String clave = 'secreta'}) async {
  await tester.enterText(find.byKey(const Key('login-correo')), correo);
  await tester.enterText(find.byKey(const Key('login-clave')), clave);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('R1: entrar manda correo y contraseña, y el negocio no se escribe',
      (tester) async {
    final auth = _AuthFalso(alEntrar: (_) => _sesionDe());

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(auth.recibidas, hasLength(1));
    expect(auth.recibidas.single.email, 'donaldo@eltornillo.co');
    expect(auth.recibidas.single.password, 'secreta');
    expect(auth.recibidas.single.negocioId, isNull);
  });

  testWidgets('R1: con un campo vacío no se puede entrar', (tester) async {
    final auth = _AuthFalso(alEntrar: (_) => _sesionDe());

    await _montar(tester, auth);
    await tester.enterText(find.byKey(const Key('login-correo')), 'alguien@negocio.co');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(auth.recibidas, isEmpty);
  });

  testWidgets('R2: un correo en dos negocios pide elegir, sin volver a pedir la contraseña',
      (tester) async {
    var intentos = 0;
    final auth = _AuthFalso(alEntrar: (c) {
      intentos++;
      if (c.negocioId == null) {
        throw const DebeElegirNegocio([
          NegocioParaElegir(negocioId: 'n-1', nombreComercial: 'Ferretería El Tornillo'),
          NegocioParaElegir(negocioId: 'n-2', nombreComercial: 'Droguería La Salud'),
        ]);
      }
      return _sesionDe(negocio: c.negocioId!);
    });

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(find.text('Ferretería El Tornillo'), findsOneWidget);
    expect(find.text('Droguería La Salud'), findsOneWidget);
    expect(find.byKey(const Key('login-clave')), findsNothing);

    await tester.tap(find.text('Droguería La Salud'));
    await tester.pumpAndSettle();

    expect(intentos, 2);
    expect(auth.recibidas.last.negocioId, 'n-2');
    expect(auth.recibidas.last.password, 'secreta');
  });

  testWidgets('R3: un fallo de credenciales no dice cuál de los dos falló', (tester) async {
    final auth = _AuthFalso();

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(find.text('Correo o contraseña incorrectos'), findsOneWidget);
  });

  testWidgets('R4: la cuenta bloqueada se dice tal cual', (tester) async {
    final auth = _AuthFalso(alEntrar: (_) => throw const CuentaBloqueada());

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    expect(find.textContaining('bloqueada'), findsWidgets);
  });

  testWidgets('R3: al fallar, la contraseña se limpia y el correo se queda', (tester) async {
    final auth = _AuthFalso();

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.text('Entrar'));
    await tester.pumpAndSettle();

    final correo = tester.widget<TextField>(find.byKey(const Key('login-correo')));
    final clave = tester.widget<TextField>(find.byKey(const Key('login-clave')));
    expect(correo.controller?.text, 'donaldo@eltornillo.co');
    expect(clave.controller?.text, isEmpty);
  });

  testWidgets('El enlace de clave olvidada dice qué hacer, porque el flujo no existe',
      (tester) async {
    await _montar(tester, _AuthFalso());

    await tester.tap(find.text('¿Olvidaste tu clave?'));
    await tester.pumpAndSettle();

    expect(find.textContaining('administrador'), findsWidgets);
  });

  testWidgets('El login entra una sola vez aunque se toque dos veces seguidas',
      (tester) async {
    // Con un servidor instantáneo no se puede ver el doble envío: el primero
    // termina antes del segundo toque. Se frena a propósito.
    final puerta = Completer<Sesion>();
    final auth = _AuthFalso(alEntrarLento: (_) => puerta.future);

    await _montar(tester, auth);
    await _escribirCredenciales(tester);
    await tester.tap(find.byKey(const Key('login-entrar')));
    await tester.pump();

    // Mientras la petición está en curso el botón deja de decir «Entrar» y
    // queda deshabilitado: el segundo toque no llega a ninguna parte.
    expect(find.text('Entrar'), findsNothing);
    await tester.tap(find.byKey(const Key('login-entrar')), warnIfMissed: false);
    await tester.pump();

    expect(auth.recibidas, hasLength(1));

    puerta.complete(_sesionDe());
    await tester.pumpAndSettle();
  });
}
