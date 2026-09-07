import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

class _Adaptador implements HttpClientAdapter {
  _Adaptador(this.responder);
  final ({int codigo, Object cuerpo}) Function(RequestOptions o) responder;
  RequestOptions? ultima;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    ultima = options;
    final r = responder(options);
    return ResponseBody.fromString(jsonEncode(r.cuerpo), r.codigo, headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  Dio dioCon(_Adaptador a) =>
      Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = a;

  final resumenJson = {
    'negocioId': '11111111-1111-1111-1111-111111111111',
    'nombreComercial': 'Ferreteria El Tornillo',
    'plan': 'PROFESIONAL',
    'patronOperativo': 'VENTA_DIRECTA',
    'estado': 'ACTIVO',
    'maxUsuarios': 10,
    'maxSucursales': 1,
    'usuariosActivos': 3,
    'modulos': [
      {'codigo': 'VENTAS', 'nombre': 'Ventas', 'tipo': 'PATRON', 'origen': 'PLAN', 'orden': 1},
      {'codigo': 'FACTURACION', 'nombre': 'Facturacion', 'tipo': 'CORE', 'origen': 'ADDON', 'orden': 5},
    ],
  };

  test('miNegocio() consulta /api/usuarios/mi-negocio y arma el resumen', () async {
    final adaptador = _Adaptador((_) => (codigo: 200, cuerpo: resumenJson));
    final cliente = ClienteDeNegocioHttp(dioCon(adaptador));

    final resumen = await cliente.miNegocio();

    expect(adaptador.ultima!.path, '/api/usuarios/mi-negocio');
    expect(resumen.nombreComercial, 'Ferreteria El Tornillo');
    expect(resumen.plan, 'PROFESIONAL');
    expect(resumen.maxUsuarios, 10);
    expect(resumen.usuariosActivos, 3);
    expect(resumen.modulos.map((m) => m.codigo), ['VENTAS', 'FACTURACION']);
    expect(resumen.modulos.firstWhere((m) => m.codigo == 'FACTURACION').origen, 'ADDON');
  });

  test('el resumen sabe decir si un modulo esta activo', () async {
    final adaptador = _Adaptador((_) => (codigo: 200, cuerpo: resumenJson));
    final resumen = await ClienteDeNegocioHttp(dioCon(adaptador)).miNegocio();

    expect(resumen.modulosActivos.tiene('FACTURACION'), isTrue);
    expect(resumen.modulosActivos.tiene('COMPRAS'), isFalse);
  });

  test('modulos() consulta /api/usuarios/modulos', () async {
    final adaptador = _Adaptador((_) => (codigo: 200, cuerpo: resumenJson['modulos']!));
    final cliente = ClienteDeNegocioHttp(dioCon(adaptador));

    final modulos = await cliente.modulos();

    expect(adaptador.ultima!.path, '/api/usuarios/modulos');
    expect(modulos.map((m) => m.codigo), ['VENTAS', 'FACTURACION']);
  });
}
