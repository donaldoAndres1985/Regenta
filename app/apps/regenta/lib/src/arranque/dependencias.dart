import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

import 'bodega_activa.dart';
import 'clientes_del_pos.dart';
import 'proveedores.dart';

/// La raíz de composición de la app (HU-119 criterio 5).
///
/// Es el único sitio donde se construye un `Dio`, se abre la base local y se
/// instancian los repositorios de todos los módulos. Los paquetes de módulo
/// declaran sus dependencias como providers que lanzan `UnimplementedError`
/// justamente para que sea aquí —y solo aquí— donde se resuelvan: un módulo no
/// sabe de dónde sale su `ClienteHttp`, y no tiene por qué.
class DependenciasDeLaApp {
  DependenciasDeLaApp._({
    required this.dio,
    required this.base,
    required this.motor,
    required this.perfil,
    required this.http,
    required this.auth,
  }) {
    // El puente que el núcleo no trae: `crearEnrutador` reacciona al perfil, y
    // el perfil solo cambia si alguien le pasa el token. Sin esto, entrar no
    // saca de la pantalla de login.
    motor.addListener(_seguirAlMotor);
    _seguirAlMotor();
  }

  /// La de verdad: se conecta al gateway y guarda la sesión en el almacén seguro.
  factory DependenciasDeLaApp.crear({
    required String urlDelGateway,
    String? dispositivoId,
    String? plataforma,
  }) {
    return DependenciasDeLaApp._armar(
      base: abrirBaseLocal(),
      almacen: AlmacenSeguroDeSesion(),
      urlDelGateway: urlDelGateway,
      dispositivoId: dispositivoId,
      plataforma: plataforma,
    );
  }

  /// Con la base en memoria y el almacén que le pasen. Misma cañería.
  factory DependenciasDeLaApp.paraPruebas({
    required BaseLocal base,
    required AlmacenDeSesion almacen,
    String urlDelGateway = 'https://gateway.local',
    HttpClientAdapter? adaptador,
  }) {
    return DependenciasDeLaApp._armar(
      base: base,
      almacen: almacen,
      urlDelGateway: urlDelGateway,
      adaptador: adaptador,
    );
  }

  static DependenciasDeLaApp _armar({
    required BaseLocal base,
    required AlmacenDeSesion almacen,
    required String urlDelGateway,
    String? dispositivoId,
    String? plataforma,
    HttpClientAdapter? adaptador,
  }) {
    final dio = Dio(BaseOptions(baseUrl: urlDelGateway));
    if (adaptador != null) dio.httpClientAdapter = adaptador;

    final motor = MotorDeSesion(
      // El login va por el mismo Dio: el interceptor se salta las rutas /auth/,
      // así que no hay riesgo de pedir un token para pedir un token.
      cliente: ClienteAuthHttp(dio),
      almacen: almacen,
      dispositivoId: dispositivoId,
      plataforma: plataforma,
    );
    dio.interceptors.add(InterceptorDeRefresco(motor, dio: dio));

    return DependenciasDeLaApp._(
      dio: dio,
      base: base,
      motor: motor,
      perfil: PerfilDeSesion(),
      http: ClienteHttp(dio, cola: ColaDeSalidaLocal(base)),
      auth: ClienteAuthHttp(dio),
    );
  }

  final Dio dio;
  final BaseLocal base;
  final MotorDeSesion motor;
  final PerfilDeSesion perfil;
  final ClienteHttp http;
  final ClienteAuth auth;

  /// El id real de la bodega activa (HU-029). Ver `proveedores.dart`,
  /// `bodegaActivaProvider`, para por qué existe esto en vez de leer el
  /// negocio del token.
  final BodegaActiva bodegaActiva = BodegaActiva();

  void _seguirAlMotor() {
    final sesion = motor.sesion;
    if (sesion == null) {
      perfil.limpiar();
      bodegaActiva.limpiar();
    } else {
      perfil.fijarToken(sesion.tokenDeAcceso);
      unawaited(cargarBodegaActivaSiHaySesion());
    }
  }

  Future<void>? _cargaDeBodegaEnCurso;

  /// Pide la bodega real al backend y la cachea. Se puede llamar las veces
  /// que sea -no repite la llamada si ya hay una bodega cargada o una carga en
  /// vuelo, y no hace nada sin sesión-. `main.dart` la espera una vez antes del
  /// primer frame (igual que ya se espera `motor.iniciar()`); `_seguirAlMotor`
  /// la dispara de nuevo, sin esperarla, cada vez que cambia la sesión (login,
  /// refresco de token) — ambos caminos comparten la misma carga en vuelo en
  /// vez de pedir la lista dos veces.
  Future<void> cargarBodegaActivaSiHaySesion() {
    if (motor.sesion == null || bodegaActiva.id != null) {
      return Future.value();
    }
    return _cargaDeBodegaEnCurso ??= _cargarBodegaActiva().whenComplete(() {
      _cargaDeBodegaEnCurso = null;
    });
  }

  Future<void> _cargarBodegaActiva() async {
    try {
      final lista = await RepositorioDeInventario(http).bodegas();
      if (lista.isNotEmpty) bodegaActiva.fijar(lista.first.id);
    } catch (_) {
      // Sin red, o la sesion ya no es valida: se sigue con el negocio como
      // placeholder (ver bodegaActivaProvider) hasta el proximo intento.
    }
  }

  /// Todo lo que los módulos piden, resuelto.
  ///
  /// Un provider que falte aquí no falla al arrancar: falla al abrir la
  /// pantalla que lo usa, que es el peor momento posible. Por eso hay un test
  /// que los lee todos.
  List<Override> overrides() => [
    dependenciasProvider.overrideWithValue(this),
    motorDeSesionProvider.overrideWithValue(motor),
    perfilDeSesionProvider.overrideWithValue(perfil),
    clienteHttpProvider.overrideWithValue(http),
    baseLocalProvider.overrideWithValue(base),

    // Ventas
    repositorioDeVentasProvider.overrideWithValue(RepositorioDeVentas(http)),
    // HU-158: las fracciones reales del catálogo para el POS.
    consultaDeFraccionesProvider.overrideWithValue(ConsultaDeFraccionesHttp(http)),
    // HU-163: detallar desde el POS cuando falta existencia.
    consultaDeDetalladoProvider.overrideWithValue(ConsultaDeDetalladoHttp(http)),
    repositorioDeCajaProvider.overrideWithValue(RepositorioDeCaja(http)),
    puedeDetallarEnPosProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_DETALLAR') ??
          false,
    ),
    // HU-164: importar productos solo con el permiso que trae el token.
    puedeImportarProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_IMPORTAR') ??
          false,
    ),
    // HU-161: detallar solo con el permiso que trae el token.
    puedeDetallarProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_DETALLAR') ??
          false,
    ),
    // HU-159: ajustar existencias solo con el permiso que trae el token.
    puedeAjustarExistenciasProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_AJUSTE_APLICAR') ??
          false,
    ),
    // HU-156: tablas de fracciones solo con el permiso que trae el token.
    puedeGestionarFraccionesProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_FRACCION_GESTIONAR') ??
          false,
    ),
    // HU-150: editar productos solo con el permiso que trae el token.
    puedeEditarProductosProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_PRODUCTO_EDITAR') ??
          false,
    ),
    // HU-179: cambiar el precio al recibir solo con el permiso que trae el token.
    puedeCambiarPreciosProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('INVENTARIO_PRODUCTO_EDITAR') ??
          false,
    ),
    // HU-141: anular movimientos de caja solo con el permiso que trae el token.
    puedeAnularMovimientosDeCajaProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos
              .contains('CAJA_MOVIMIENTO_ANULAR') ??
          false,
    ),
    repositorioDeClientesDeVentaProvider.overrideWithValue(
      ClientesDelPos(RepositorioDeClientesHttp(http, base)),
    ),
    bodegaDeVentaProvider.overrideWith(
      (ref) => ref.watch(bodegaActivaProvider),
    ),
    ventaEnCursoProvider.overrideWith((ref) => ref.watch(ventaDelPosProvider)),
    permisosDeLaSesionProvider.overrideWith(
      (ref) =>
          ref.watch(perfilComoNotifierProvider).claims?.permisos.toSet() ??
          const {},
    ),

    // Clientes
    repositorioDeClientesProvider.overrideWithValue(
      RepositorioDeClientes(http, base),
    ),

    // Inventario
    repositorioDeInventarioProvider.overrideWithValue(
      RepositorioDeInventario(http),
    ),

    // Mesas y comandas
    repositorioDeMesasProvider.overrideWithValue(RepositorioDeMesas(http)),
    repositorioDeComandasProvider.overrideWithValue(
      RepositorioDeComandas(http),
    ),
    repositorioDeCuentasProvider.overrideWithValue(RepositorioDeCuentas(http)),
    repositorioDeCocinaProvider.overrideWithValue(RepositorioDeCocina(http)),

    // Compras
    repositorioDeRecepcionesProvider.overrideWithValue(
      RepositorioDeRecepciones(http),
    ),
    repositorioDeProveedoresProvider.overrideWithValue(
      RepositorioDeProveedores(http),
    ),
    repositorioDeCompraDirectaProvider.overrideWithValue(
      RepositorioDeCompraDirecta(http),
    ),
    repositorioDeCuentasPorPagarProvider.overrideWithValue(
      RepositorioDeCuentasPorPagar(http),
    ),
    repositorioDeConfiguracionDeComprasProvider.overrideWithValue(
      RepositorioDeConfiguracionDeCompras(http),
    ),
    repositorioDeOrdenesProvider.overrideWithValue(
      RepositorioDeOrdenes(http),
    ),
    repositorioDeDevolucionesProvider.overrideWithValue(
      RepositorioDeDevoluciones(http),
    ),

    // Facturación
    repositorioDeFacturasProvider.overrideWithValue(
      RepositorioDeFacturas(http),
    ),

    // Reportes
    repositorioDeReportesDeCajaProvider.overrideWithValue(
      RepositorioDeReportesDeCaja(http),
    ),
    // HU-142: el listado de turnos reimprime la tirilla con el módulo de caja.
    reimprimirTirillaDeCajaProvider.overrideWith(
      (ref) => (sesionId) async {
        final tirilla = await ref.read(repositorioDeCajaProvider).tirillaDeCierre(sesionId);
        await ref.read(impresoraDeTirillaProvider)(tirilla, reimpresion: true);
      },
    ),

    // Usuarios / Configuración
    repositorioDeUsuariosProvider.overrideWithValue(
      RepositorioDeUsuarios(http),
    ),
    repositorioDeConfigDeCajaProvider.overrideWithValue(
      RepositorioDeConfigDeCaja(http),
    ),
  ];

  Future<void> cerrar() async {
    motor.removeListener(_seguirAlMotor);
    dio.close(force: true);
    await base.close();
  }
}
