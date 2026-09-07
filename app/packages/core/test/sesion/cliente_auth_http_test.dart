import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// Adaptador de dio de mentira: guarda la ultima peticion y devuelve lo que se
/// le diga.
class AdaptadorFalso implements HttpClientAdapter {
  AdaptadorFalso(this.responder);

  final ({int codigo, Map<String, dynamic> cuerpo}) Function(RequestOptions o) responder;
  RequestOptions? ultima;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    ultima = options;
    final r = responder(options);
    return ResponseBody.fromString(
      jsonEncode(r.cuerpo),
      r.codigo,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Map<String, dynamic> loginOk({
  String acceso = 'acc',
  String refresco = 'ref',
  int expira = 900,
}) => {
      'debeElegirNegocio': false,
      'negocios': <dynamic>[],
      'tokenDeAcceso': acceso,
      'tokenDeRefresco': refresco,
      'expiraEnSegundos': expira,
      'negocioId': '11111111-1111-1111-1111-111111111111',
      'usuarioId': '22222222-2222-2222-2222-222222222222',
      'plan': 'PROFESIONAL',
      'patron': 'VENTA_DIRECTA',
      'roles': ['ADMINISTRADOR'],
      'modulos': ['VENTAS'],
    };

void main() {
  Dio dioCon(AdaptadorFalso adaptador) {
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.regenta.test'));
    dio.httpClientAdapter = adaptador;
    return dio;
  }

  test('entrar postea a /api/usuarios/auth/login y arma la Sesion', () async {
    final adaptador = AdaptadorFalso((_) => (codigo: 200, cuerpo: loginOk(expira: 900)));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    final sesion = await cliente.entrar(const Credenciales(
        email: 'ana@tienda.co', password: 'clave-larga', dispositivoId: 'pixel-7', plataforma: 'android'));

    expect(adaptador.ultima!.path, '/api/usuarios/auth/login');
    expect(adaptador.ultima!.method, 'POST');
    final enviado = adaptador.ultima!.data as Map;
    expect(enviado['email'], 'ana@tienda.co');
    expect(enviado['dispositivoId'], 'pixel-7');
    expect(sesion.tokenDeAcceso, 'acc');
    expect(sesion.patron, 'VENTA_DIRECTA');
    expect(sesion.expiraEn.isAfter(DateTime.now().toUtc()), isTrue);
  });

  test('un login que pide elegir negocio lanza DebeElegirNegocio con la lista', () async {
    final adaptador = AdaptadorFalso((_) => (
          codigo: 200,
          cuerpo: {
            'debeElegirNegocio': true,
            'negocios': [
              {'negocioId': 'aaaaaaaa-0000-0000-0000-000000000001', 'nombreComercial': 'Tienda A'},
              {'negocioId': 'aaaaaaaa-0000-0000-0000-000000000002', 'nombreComercial': 'Tienda B'},
            ],
            'tokenDeAcceso': null,
            'tokenDeRefresco': null,
            'expiraEnSegundos': 0,
            'roles': <dynamic>[],
            'modulos': <dynamic>[],
          }
        ));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    await expectLater(
      cliente.entrar(const Credenciales(email: 'multi@x.co', password: 'clave-larga')),
      throwsA(isA<DebeElegirNegocio>()
          .having((e) => e.negocios.length, 'negocios', 2)
          .having((e) => e.negocios.first.nombreComercial, 'primero', 'Tienda A')),
    );
  });

  test('credenciales malas -> CredencialesInvalidas', () async {
    final adaptador = AdaptadorFalso((_) => (codigo: 401, cuerpo: {'mensaje': 'no'}));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    await expectLater(
      cliente.entrar(const Credenciales(email: 'a@b.co', password: 'mala')),
      throwsA(isA<CredencialesInvalidas>()),
    );
  });

  test('cuenta bloqueada (423) -> CuentaBloqueada', () async {
    final adaptador = AdaptadorFalso((_) => (codigo: 423, cuerpo: {'mensaje': 'bloqueada'}));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    await expectLater(
      cliente.entrar(const Credenciales(email: 'a@b.co', password: 'x')),
      throwsA(isA<CuentaBloqueada>()),
    );
  });

  test('refrescar postea el refresh token y devuelve la sesion nueva', () async {
    final adaptador = AdaptadorFalso((_) => (codigo: 200, cuerpo: loginOk(acceso: 'acc2', refresco: 'ref2')));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    final sesion = await cliente.refrescar(tokenDeRefresco: 'ref1', dispositivoId: 'pixel-7');

    expect(adaptador.ultima!.path, '/api/usuarios/auth/refrescar');
    expect((adaptador.ultima!.data as Map)['tokenDeRefresco'], 'ref1');
    expect(sesion.tokenDeAcceso, 'acc2');
    expect(sesion.tokenDeRefresco, 'ref2');
  });

  test('refrescar con 401 -> RefrescoRechazado', () async {
    final adaptador = AdaptadorFalso((_) => (codigo: 401, cuerpo: {'mensaje': 'usado'}));
    final cliente = ClienteAuthHttp(dioCon(adaptador));

    await expectLater(
      cliente.refrescar(tokenDeRefresco: 'ref-robado'),
      throwsA(isA<RefrescoRechazado>()),
    );
  });
}
