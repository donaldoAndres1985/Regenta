import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta/src/arranque/dependencias.dart';
import 'package:regenta_clientes/regenta_clientes.dart';
import 'package:regenta_comandas/regenta_comandas.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';
import 'package:regenta_reportes/regenta_reportes.dart';
import 'package:regenta_usuarios/regenta_usuarios.dart';
import 'package:regenta_inventario/regenta_inventario.dart';
import 'package:regenta_mesas/regenta_mesas.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

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

Sesion _sesionVigente() => Sesion(
  tokenDeAcceso: _token,
  tokenDeRefresco: 'refresco',
  expiraEn: DateTime.now().toUtc().add(const Duration(minutes: 15)),
  negocioId: 'n-1',
  usuarioId: 'u-1',
  plan: 'PROFESIONAL',
  patron: 'VENTA_DIRECTA',
  roles: const ['ADMINISTRADOR'],
  modulos: const ['VENTAS', 'INVENTARIO'],
);

/// Un JWT sin firmar con los claims que la app lee.
const _token =
    'eyJhbGciOiJIUzI1NiJ9.'
    'eyJuZWdvY2lvX2lkIjoibi0xIiwic3ViIjoidS0xIiwicGxhbiI6IlBST0ZFU0lPTkFMIiwi'
    'cGF0cm9uIjoiVkVOVEFfRElSRUNUQSIsIm1vZHVsb3MiOlsiVkVOVEFTIl0sInJvbGVzIjpb'
    'XSwicGVybWlzb3MiOlsiQ0xJRU5URVNfQ0xJRU5URV9DUkVBUiJdfQ.firma';

DependenciasDeLaApp _dependenciasCon(
  AlmacenDeSesion almacen, {
  HttpClientAdapter? adaptador,
}) => DependenciasDeLaApp.paraPruebas(
  base: BaseLocal(NativeDatabase.memory()),
  almacen: almacen,
  urlDelGateway: 'https://gateway.test',
  adaptador: adaptador,
);

/// HU-029: responde `GET /api/inventario/bodegas` con una lista fija, y
/// cuenta cuántas veces se pidió -para probar que la carga no se repite sola-.
class _ServidorDeBodegas implements HttpClientAdapter {
  _ServidorDeBodegas(this.bodegas);

  final List<Map<String, Object?>> bodegas;
  int pedidos = 0;

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    pedidos++;
    return ResponseBody.fromString(
      jsonEncode(bodegas),
      200,
      headers: const {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }
}

void main() {
  test(
    'Criterio 5: ninguna dependencia de módulo queda sin resolver',
    () async {
      final deps = _dependenciasCon(_AlmacenEnMemoria());
      addTearDown(deps.cerrar);
      final contenedor = ProviderContainer(overrides: deps.overrides());
      addTearDown(contenedor.dispose);

      // Si alguno siguiera en su `throw UnimplementedError`, esto revienta: es
      // exactamente el fallo que se ve hoy al abrir cualquier pantalla.
      expect(
        contenedor.read(repositorioDeVentasProvider),
        isA<RepositorioDeVentas>(),
      );
      expect(
        contenedor.read(repositorioDeClientesDeVentaProvider),
        isA<RepositorioDeClientesDeVenta>(),
      );
      expect(
        contenedor.read(repositorioDeClientesProvider),
        isA<RepositorioDeClientes>(),
      );
      expect(
        contenedor.read(repositorioDeInventarioProvider),
        isA<RepositorioDeInventario>(),
      );
      expect(
        contenedor.read(repositorioDeMesasProvider),
        isA<RepositorioDeMesas>(),
      );
      expect(
        contenedor.read(repositorioDeComandasProvider),
        isA<RepositorioDeComandas>(),
      );
      expect(
        contenedor.read(repositorioDeCuentasProvider),
        isA<RepositorioDeCuentas>(),
      );
      expect(
        contenedor.read(repositorioDeCocinaProvider),
        isA<RepositorioDeCocina>(),
      );
      expect(
        contenedor.read(repositorioDeRecepcionesProvider),
        isA<RepositorioDeRecepciones>(),
      );
      expect(
        contenedor.read(repositorioDeFacturasProvider),
        isA<RepositorioDeFacturas>(),
      );
      expect(
        contenedor.read(repositorioDeReportesDeCajaProvider),
        isA<RepositorioDeReportesDeCaja>(),
      );
      expect(
        contenedor.read(repositorioDeUsuariosProvider),
        isA<RepositorioDeUsuarios>(),
      );
      expect(
        contenedor.read(repositorioDeCajaProvider),
        isA<RepositorioDeCaja>(),
      );
      expect(contenedor.read(bodegaDeVentaProvider), isA<String>());
    },
  );

  test(
    'Criterio 5: los permisos de las pantallas salen del token, no de una lista vacía',
    () async {
      final deps = _dependenciasCon(_AlmacenEnMemoria(_sesionVigente()));
      addTearDown(deps.cerrar);
      await deps.motor.iniciar();
      final contenedor = ProviderContainer(overrides: deps.overrides());
      addTearDown(contenedor.dispose);

      expect(
        contenedor.read(permisosDeLaSesionProvider),
        contains('CLIENTES_CLIENTE_CREAR'),
      );
    },
  );

  test('Criterio 6: con una sesión guardada, la app abre dentro', () async {
    final deps = _dependenciasCon(_AlmacenEnMemoria(_sesionVigente()));
    addTearDown(deps.cerrar);

    await deps.motor.iniciar();

    expect(deps.motor.estado, isA<ConSesion>());
    expect(deps.perfil.claims, isNotNull);
    expect(deps.perfil.modulos.tiene('VENTAS'), isTrue);
  });

  test('Criterio 6: sin sesión guardada, la app abre en el login', () async {
    final deps = _dependenciasCon(_AlmacenEnMemoria());
    addTearDown(deps.cerrar);

    await deps.motor.iniciar();

    expect(deps.motor.estado, isA<SinSesion>());
    expect(deps.perfil.claims, isNull);
  });

  test('El perfil sigue al motor: entrar lo llena y cerrar lo vacía', () async {
    final almacen = _AlmacenEnMemoria(_sesionVigente());
    final deps = _dependenciasCon(almacen);
    addTearDown(deps.cerrar);

    await deps.motor.iniciar();
    expect(deps.perfil.claims, isNotNull);

    await deps.motor.cerrar();

    expect(
      deps.perfil.claims,
      isNull,
      reason: 'al cerrar sesión no puede quedar accesible nada del negocio',
    );
    expect(deps.perfil.modulos.todos, isEmpty);
  });

  group('HU-029: la bodega real de venta', () {
    test(
      'sin sesión, bodegaActivaProvider cae al negocio y no pide nada al backend',
      () async {
        final servidor = _ServidorDeBodegas([
          {'id': 'bod-1', 'codigo': 'BOD-PRAL', 'nombre': 'Principal', 'esDefault': true, 'activa': true},
        ]);
        final deps = _dependenciasCon(_AlmacenEnMemoria(), adaptador: servidor);
        addTearDown(deps.cerrar);
        final contenedor = ProviderContainer(overrides: deps.overrides());
        addTearDown(contenedor.dispose);

        await deps.motor.iniciar();

        expect(contenedor.read(bodegaDeVentaProvider), '');
        expect(servidor.pedidos, 0, reason: 'sin sesión no hay token con qué pedir nada');
      },
    );

    test(
      'con sesión, bodegaActivaProvider pasa del negocio al id real de la bodega',
      () async {
        final servidor = _ServidorDeBodegas([
          {'id': 'bod-real-1', 'codigo': 'BOD-PRAL', 'nombre': 'Principal', 'esDefault': true, 'activa': true},
        ]);
        final deps = _dependenciasCon(
          _AlmacenEnMemoria(_sesionVigente()),
          adaptador: servidor,
        );
        addTearDown(deps.cerrar);
        final contenedor = ProviderContainer(overrides: deps.overrides());
        addTearDown(contenedor.dispose);

        await deps.motor.iniciar();
        // _seguirAlMotor ya disparó la carga sin esperarla (ver dependencias.dart);
        // se espera explícito, como hace main.dart antes del primer frame.
        await deps.cargarBodegaActivaSiHaySesion();

        expect(contenedor.read(bodegaDeVentaProvider), 'bod-real-1');
      },
    );

    test(
      'cargarBodegaActivaSiHaySesion no repite el pedido si ya hay una bodega cargada',
      () async {
        final servidor = _ServidorDeBodegas([
          {'id': 'bod-real-1', 'codigo': 'BOD-PRAL', 'nombre': 'Principal', 'esDefault': true, 'activa': true},
        ]);
        final deps = _dependenciasCon(
          _AlmacenEnMemoria(_sesionVigente()),
          adaptador: servidor,
        );
        addTearDown(deps.cerrar);

        await deps.motor.iniciar();
        await deps.cargarBodegaActivaSiHaySesion();
        await deps.cargarBodegaActivaSiHaySesion();

        expect(servidor.pedidos, 1);
      },
    );

    test('al cerrar sesión, la bodega activa se limpia igual que el perfil', () async {
      final servidor = _ServidorDeBodegas([
        {'id': 'bod-real-1', 'codigo': 'BOD-PRAL', 'nombre': 'Principal', 'esDefault': true, 'activa': true},
      ]);
      final deps = _dependenciasCon(
        _AlmacenEnMemoria(_sesionVigente()),
        adaptador: servidor,
      );
      addTearDown(deps.cerrar);

      await deps.motor.iniciar();
      await deps.cargarBodegaActivaSiHaySesion();
      expect(deps.bodegaActiva.id, 'bod-real-1');

      await deps.motor.cerrar();

      expect(
        deps.bodegaActiva.id,
        isNull,
        reason: 'al cerrar sesión no puede quedar la bodega de otra sesión',
      );
    });
  });
}
