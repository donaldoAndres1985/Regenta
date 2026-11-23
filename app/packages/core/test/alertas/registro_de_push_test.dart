import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// El push del dispositivo, simulado: no hay Firebase en las pruebas.
class _PushSimulado implements ServicioDePush {
  _PushSimulado({
    this.inicial = PermisoDePush.sinPreguntar,
    this.respuestaAlPedir = PermisoDePush.concedido,
  });

  PermisoDePush inicial;
  final PermisoDePush respuestaAlPedir;
  String? tokenActual = 'token-1';
  int vecesQuePregunto = 0;
  final _renovados = StreamController<String>.broadcast();

  @override
  String get plataforma => 'ANDROID';

  @override
  Future<PermisoDePush> permiso() async => inicial;

  @override
  Future<PermisoDePush> pedirPermiso() async {
    vecesQuePregunto++;
    inicial = respuestaAlPedir;
    return respuestaAlPedir;
  }

  @override
  Future<String?> token() async => tokenActual;

  @override
  Stream<String> get tokensNuevos => _renovados.stream;

  void renovar(String token) => _renovados.add(token);
}

class _DispositivosFake implements RepositorioDeDispositivos {
  final List<Map<String, String?>> registrados = [];
  bool sinRed = false;

  @override
  Future<void> registrar({
    required String tokenFcm,
    required String plataforma,
    String? modelo,
    String? versionApp,
  }) async {
    if (sinRed) throw const ErrorDeRed();
    registrados.add({'token': tokenFcm, 'plataforma': plataforma, 'modelo': modelo, 'version': versionApp});
  }
}

void main() {
  group('El permiso se pide cuando tiene sentido, no al arrancar', () {
    test('al entrar sin haber preguntado nunca, no se pregunta ni se registra: se invita', () async {
      final push = _PushSimulado();
      final repo = _DispositivosFake();
      final registro = RegistroDePush(push, repo);

      await registro.alEntrar();

      expect(push.vecesQuePregunto, 0);
      expect(repo.registrados, isEmpty);
      expect(registro.invitarAActivar, isTrue);
    });

    test('C1: al activar se pregunta una vez y, concedido, el token queda registrado', () async {
      final push = _PushSimulado();
      final repo = _DispositivosFake();
      final registro = RegistroDePush(push, repo, modelo: 'Pixel 8', versionApp: '1.4.0');
      await registro.alEntrar();

      await registro.activar();

      expect(push.vecesQuePregunto, 1);
      expect(registro.registrado, isTrue);
      expect(registro.invitarAActivar, isFalse);
      expect(repo.registrados.single,
          {'token': 'token-1', 'plataforma': 'ANDROID', 'modelo': 'Pixel 8', 'version': '1.4.0'});
    });

    test('con el permiso ya concedido, al entrar se registra sin preguntar', () async {
      final push = _PushSimulado(inicial: PermisoDePush.concedido);
      final repo = _DispositivosFake();

      await RegistroDePush(push, repo).alEntrar();

      expect(push.vecesQuePregunto, 0);
      expect(repo.registrados.single['token'], 'token-1');
    });

    test('negado, no se registra y no se vuelve a invitar', () async {
      final push = _PushSimulado(respuestaAlPedir: PermisoDePush.denegado);
      final repo = _DispositivosFake();
      final registro = RegistroDePush(push, repo);
      await registro.alEntrar();

      await registro.activar();

      expect(repo.registrados, isEmpty);
      expect(registro.permiso, PermisoDePush.denegado);
      expect(registro.invitarAActivar, isFalse);
    });
  });

  test('cada token nuevo de FCM se vuelve a registrar', () async {
    final push = _PushSimulado(inicial: PermisoDePush.concedido);
    final repo = _DispositivosFake();
    await RegistroDePush(push, repo).alEntrar();

    push.renovar('token-2');
    await Future<void>.delayed(Duration.zero);

    expect(repo.registrados.map((r) => r['token']), ['token-1', 'token-2']);
  });

  test('C5: en Web no hay push: nada que preguntar ni registrar', () async {
    final repo = _DispositivosFake();
    final registro = RegistroDePush(const SinPush(), repo);

    await registro.alEntrar();
    await registro.activar();

    expect(registro.permiso, PermisoDePush.noDisponible);
    expect(registro.invitarAActivar, isFalse);
    expect(repo.registrados, isEmpty);
  });

  test('sin red no se pierde la alerta: queda el error y se reintenta al volver a entrar', () async {
    final push = _PushSimulado(inicial: PermisoDePush.concedido);
    final repo = _DispositivosFake()..sinRed = true;
    final registro = RegistroDePush(push, repo);

    await registro.alEntrar();
    expect(registro.registrado, isFalse);
    expect(registro.error, isNotNull);

    repo.sinRed = false;
    await registro.alEntrar();
    expect(registro.registrado, isTrue);
  });

  group('El aviso en el centro de alertas', () {
    Future<void> montar(WidgetTester tester, RegistroDePush registro) => tester.pumpWidget(
          MaterialApp(home: Scaffold(body: AvisoDePush(registro: registro))),
        );

    testWidgets('invita a activar y, al tocar, pregunta y registra', (tester) async {
      final push = _PushSimulado();
      final repo = _DispositivosFake();
      final registro = RegistroDePush(push, repo);
      await registro.alEntrar();
      await montar(tester, registro);

      expect(push.vecesQuePregunto, 0);
      expect(find.byKey(const Key('aviso-push')), findsOneWidget);

      await tester.tap(find.byKey(const Key('activar-push')));
      await tester.pumpAndSettle();

      expect(push.vecesQuePregunto, 1);
      expect(repo.registrados, hasLength(1));
      expect(find.byKey(const Key('aviso-push')), findsNothing);
    });

    testWidgets('en Web no aparece', (tester) async {
      final registro = RegistroDePush(const SinPush(), _DispositivosFake());
      await registro.alEntrar();
      await montar(tester, registro);

      expect(find.byKey(const Key('aviso-push')), findsNothing);
      expect(find.byKey(const Key('aviso-push-denegado')), findsNothing);
    });

    testWidgets('negado, recuerda dónde se cambia', (tester) async {
      final registro = RegistroDePush(_PushSimulado(inicial: PermisoDePush.denegado), _DispositivosFake());
      await registro.alEntrar();
      await montar(tester, registro);

      expect(find.byKey(const Key('aviso-push-denegado')), findsOneWidget);
    });
  });
}
