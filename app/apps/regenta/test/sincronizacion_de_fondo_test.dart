import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/src/arranque/dependencias.dart';
import 'package:regenta/src/arranque/sincronizacion.dart';
import 'package:regenta_core/regenta_core.dart';

/// El trabajo de fondo, sin el isolate.
///
/// Lo que el `callbackDispatcher` hace es armar estas mismas piezas desde cero
/// —porque un isolate de fondo no hereda nada— y correr una pasada. Aquí se
/// prueba esa pasada, que es donde están las decisiones.

class _AlmacenEnMemoria implements AlmacenDeSesion {
  _AlmacenEnMemoria([this._sesion]);

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

/// Un servidor de mentira que responde según la ruta y anota lo que le pidieron.
class _Servidor implements HttpClientAdapter {
  _Servidor({this.respuestaDeRefresco = 200, this.respuestaDeSubida = 200});

  int respuestaDeRefresco;
  int respuestaDeSubida;
  final List<String> recibidas = [];

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream,
      Future<void>? cancelFuture) async {
    recibidas.add('${options.method} ${options.path}');
    if (options.path.contains('/auth/refrescar')) {
      if (respuestaDeRefresco != 200) {
        return ResponseBody.fromString('{}', respuestaDeRefresco,
            headers: _json);
      }
      return ResponseBody.fromString(
        jsonEncode({
          'tokenDeAcceso': _token,
          'tokenDeRefresco': 'refresco-2',
          'expiraEnSegundos': 900,
          'negocioId': 'n-1',
          'usuarioId': 'u-1',
          'plan': 'PROFESIONAL',
          'patron': 'VENTA_DIRECTA',
          'roles': ['ADMINISTRADOR'],
          'modulos': ['VENTAS'],
        }),
        200,
        headers: _json,
      );
    }
    return ResponseBody.fromString('{}', respuestaDeSubida, headers: _json);
  }

  static const _json = {
    Headers.contentTypeHeader: [Headers.jsonContentType],
  };
}

const _token = 'eyJhbGciOiJIUzI1NiJ9.'
    'eyJuZWdvY2lvX2lkIjoibi0xIiwic3ViIjoidS0xIiwicGxhbiI6IlBST0ZFU0lPTkFMIiwi'
    'cGF0cm9uIjoiVkVOVEFfRElSRUNUQSIsIm1vZHVsb3MiOlsiVkVOVEFTIl0sInJvbGVzIjpb'
    'XSwicGVybWlzb3MiOltdfQ.firma';

Sesion _sesion({Duration vence = const Duration(minutes: 15)}) => Sesion(
      tokenDeAcceso: _token,
      tokenDeRefresco: 'refresco-1',
      expiraEn: DateTime.now().toUtc().add(vence),
      negocioId: 'n-1',
      usuarioId: 'u-1',
      plan: 'PROFESIONAL',
      patron: 'VENTA_DIRECTA',
      roles: const ['ADMINISTRADOR'],
      modulos: const ['VENTAS'],
    );

({DependenciasDeLaApp deps, BaseLocal base}) _armar(
    _AlmacenEnMemoria almacen, _Servidor servidor) {
  final base = BaseLocal(NativeDatabase.memory());
  return (
    deps: DependenciasDeLaApp.paraPruebas(
      base: base,
      almacen: almacen,
      adaptador: servidor,
    ),
    base: base,
  );
}

Future<void> _encolarUnaVenta(BaseLocal base) => ColaDeSalidaLocal(base).encolar(
      const OperacionEncolable(
        metodo: 'POST',
        ruta: '/api/ventas/offline',
        datos: {'numero': 'FV-1'},
      ),
    );

void main() {
  test('Criterio 1: con sesión y algo pendiente, la pasada lo sube y la cola queda vacía',
      () async {
    final servidor = _Servidor();
    final armado = _armar(_AlmacenEnMemoria(_sesion()), servidor);
    addTearDown(armado.deps.cerrar);
    await _encolarUnaVenta(armado.base);

    final resultado = await subirLoPendiente(armado.deps);

    expect(resultado, isTrue);
    expect(servidor.recibidas, contains('POST /api/ventas/offline'));
    final resumen = await TrabajadorDeSincronizacion(
            dio: armado.deps.dio, db: armado.base)
        .resumen();
    expect(resumen.pendientes, 0);
  });

  test('Criterio 2: el token vencido se refresca antes de subir', () async {
    final servidor = _Servidor();
    final almacen = _AlmacenEnMemoria(_sesion(vence: const Duration(minutes: -5)));
    final armado = _armar(almacen, servidor);
    addTearDown(armado.deps.cerrar);
    await _encolarUnaVenta(armado.base);

    await subirLoPendiente(armado.deps);

    expect(servidor.recibidas.first, contains('/auth/refrescar'));
    expect(servidor.recibidas, contains('POST /api/ventas/offline'));
    expect(armado.deps.motor.sesion?.tokenDeRefresco, 'refresco-2');
  });

  test('Criterio 5: sin sesión guardada no se sube nada', () async {
    final servidor = _Servidor();
    final armado = _armar(_AlmacenEnMemoria(), servidor);
    addTearDown(armado.deps.cerrar);
    await _encolarUnaVenta(armado.base);

    final resultado = await subirLoPendiente(armado.deps);

    expect(resultado, isTrue, reason: 'no hay nada que reintentar: no es un fallo');
    expect(servidor.recibidas, isEmpty);
    final resumen = await TrabajadorDeSincronizacion(
            dio: armado.deps.dio, db: armado.base)
        .resumen();
    expect(resumen.pendientes, 1, reason: 'lo pendiente se queda para cuando alguien entre');
  });

  test('Criterio 5: si el refresco es rechazado, no se insiste contra un token muerto',
      () async {
    final servidor = _Servidor(respuestaDeRefresco: 401);
    final almacen = _AlmacenEnMemoria(_sesion(vence: const Duration(minutes: -5)));
    final armado = _armar(almacen, servidor);
    addTearDown(armado.deps.cerrar);
    await _encolarUnaVenta(armado.base);

    final resultado = await subirLoPendiente(armado.deps);

    expect(resultado, isTrue);
    expect(servidor.recibidas.where((r) => r.contains('/api/ventas/')), isEmpty);
    expect(almacen.borrados, greaterThan(0), reason: 'la sesión muerta se borra');
  });

  test('Criterio 3: lo que choca al subir queda en la bandeja de conflictos', () async {
    final servidor = _Servidor(respuestaDeSubida: 409);
    final armado = _armar(_AlmacenEnMemoria(_sesion()), servidor);
    addTearDown(armado.deps.cerrar);
    await _encolarUnaVenta(armado.base);

    await subirLoPendiente(armado.deps);

    final resumen = await TrabajadorDeSincronizacion(
            dio: armado.deps.dio, db: armado.base)
        .resumen();
    expect(resumen.conflictos, 1);
    expect(resumen.pendientes, 0);
    expect(resumen.hayConflictos, isTrue);
  });

  test('Criterio 4: una pasada con la cola vacía no falla ni llama a nadie', () async {
    final servidor = _Servidor();
    final armado = _armar(_AlmacenEnMemoria(_sesion()), servidor);
    addTearDown(armado.deps.cerrar);

    final resultado = await subirLoPendiente(armado.deps);

    expect(resultado, isTrue);
    expect(servidor.recibidas, isEmpty);
  });

  test('El trabajo de fondo solo se registra donde existe: en Web no', () {
    expect(hayTrabajoEnSegundoPlano(esWeb: true, plataforma: 'android'), isFalse);
    expect(hayTrabajoEnSegundoPlano(esWeb: false, plataforma: 'android'), isTrue);
    expect(hayTrabajoEnSegundoPlano(esWeb: false, plataforma: 'ios'), isTrue);
    expect(hayTrabajoEnSegundoPlano(esWeb: false, plataforma: 'linux'), isFalse);
  });
}
