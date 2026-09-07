import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

import 'dobles.dart';

void main() {
  late DateTime reloj;
  DateTime ahora() => reloj;

  setUp(() => reloj = DateTime.utc(2026, 1, 1, 12));

  MotorDeSesion motorCon(ClienteAuth cliente, AlmacenDeSesion almacen) =>
      MotorDeSesion(cliente: cliente, almacen: almacen, ahora: ahora);

  group('criterio 2: el token a punto de expirar se refresca solo', () {
    test('accesoVigente refresca cuando queda menos que el margen', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.add(const Duration(seconds: 20))));
      final cliente = ClienteAuthFalso(
        alRefrescar: (_) => sesionDe(
            tokenDeAcceso: 'acceso-2',
            tokenDeRefresco: 'refresco-2',
            expiraEn: reloj.add(const Duration(minutes: 15))),
      );
      final motor = motorCon(cliente, almacen);
      await motor.iniciar();

      final token = await motor.accesoVigente();

      expect(token, 'acceso-2');
      expect(cliente.refrescos, 1);
      expect(motor.sesion!.tokenDeRefresco, 'refresco-2');
      expect((await almacen.leer())!.tokenDeAcceso, 'acceso-2',
          reason: 'la sesion refrescada queda guardada');
    });

    test('no refresca si al token todavia le queda de sobra', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.add(const Duration(minutes: 10))));
      final cliente = ClienteAuthFalso(alRefrescar: (_) => sesionDe());
      final motor = motorCon(cliente, almacen);
      await motor.iniciar();

      await motor.accesoVigente();

      expect(cliente.refrescos, 0);
    });

    test('varias llamadas a la vez comparten un solo refresco', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.add(const Duration(seconds: 5))));
      final cliente = ClienteAuthFalso(
          alRefrescar: (_) =>
              sesionDe(tokenDeAcceso: 'acceso-2', expiraEn: reloj.add(const Duration(minutes: 15))));
      final motor = motorCon(cliente, almacen);
      await motor.iniciar();

      await Future.wait([motor.accesoVigente(), motor.accesoVigente(), motor.accesoVigente()]);

      expect(cliente.refrescos, 1);
    });
  });

  group('criterio 3: si el refresco falla, se cierra la sesion', () {
    test('accesoVigente con refresco rechazado cierra y avisa', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.add(const Duration(seconds: 5))));
      final motor = motorCon(ClienteAuthFalso(alRefrescar: null), almacen);
      await motor.iniciar();

      var avisos = 0;
      motor.addListener(() => avisos++);

      await expectLater(motor.accesoVigente(), throwsA(isA<RefrescoRechazado>()));

      expect(motor.estado, isA<SinSesion>());
      expect((motor.estado as SinSesion).motivo, MotivoDeCierre.refrescoRechazado);
      expect((motor.estado as SinSesion).motivo!.mensaje, isNotEmpty);
      expect(await almacen.leer(), isNull, reason: 'no queda nada guardado');
      expect(avisos, greaterThanOrEqualTo(1), reason: 'el router se entera para ir al login');
    });

    test('cerrar() manual deja el motivo en cierreManual y no es un error', () async {
      final almacen = AlmacenEnMemoria()..guardar(sesionDe());
      final motor = motorCon(ClienteAuthFalso(), almacen);
      await motor.iniciar();

      await motor.cerrar();

      expect((motor.estado as SinSesion).motivo, MotivoDeCierre.cierreManual);
      expect(await almacen.leer(), isNull);
    });
  });

  group('criterio 4: al reabrir, sigo con sesion si el refresco sirve', () {
    test('iniciar con una sesion viva la deja activa sin tocar la red', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.add(const Duration(minutes: 10))));
      final cliente = ClienteAuthFalso(alRefrescar: (_) => sesionDe());
      final motor = motorCon(cliente, almacen);

      await motor.iniciar();

      expect(motor.estado, isA<ConSesion>());
      expect(cliente.refrescos, 0);
    });

    test('iniciar con el acceso vencido pero el refresco vivo renueva y sigue', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.subtract(const Duration(minutes: 1))));
      final cliente = ClienteAuthFalso(
          alRefrescar: (_) => sesionDe(
              tokenDeAcceso: 'acceso-nuevo', expiraEn: reloj.add(const Duration(minutes: 15))));
      final motor = motorCon(cliente, almacen);

      await motor.iniciar();

      expect(motor.estado, isA<ConSesion>());
      expect(motor.sesion!.tokenDeAcceso, 'acceso-nuevo');
      expect(cliente.refrescos, 1);
    });

    test('iniciar con el refresco ya muerto deja sin sesion', () async {
      final almacen = AlmacenEnMemoria()
        ..guardar(sesionDe(expiraEn: reloj.subtract(const Duration(minutes: 1))));
      final motor = motorCon(ClienteAuthFalso(alRefrescar: null), almacen);

      await motor.iniciar();

      expect(motor.estado, isA<SinSesion>());
      expect((motor.estado as SinSesion).motivo, MotivoDeCierre.refrescoRechazado);
    });

    test('iniciar sin nada guardado deja sin sesion, sin motivo de error', () async {
      final motor = motorCon(ClienteAuthFalso(), AlmacenEnMemoria());

      await motor.iniciar();

      expect(motor.estado, isA<SinSesion>());
      expect((motor.estado as SinSesion).motivo, MotivoDeCierre.sinSesionGuardada);
    });
  });

  group('entrar', () {
    test('entrar guarda la sesion y pasa a ConSesion', () async {
      final almacen = AlmacenEnMemoria();
      final cliente = ClienteAuthFalso(
          alEntrar: (_) => sesionDe(tokenDeAcceso: 'acceso-login'));
      final motor = motorCon(cliente, almacen);

      await motor.entrar(const Credenciales(email: 'a@b.co', password: 'x'));

      expect(motor.estado, isA<ConSesion>());
      expect((await almacen.leer())!.tokenDeAcceso, 'acceso-login');
    });
  });
}
