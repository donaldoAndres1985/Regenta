import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_clientes/regenta_clientes.dart';
import 'package:regenta_comandas/regenta_comandas.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';
import 'package:regenta_inventario/regenta_inventario.dart';
import 'package:regenta_mesas/regenta_mesas.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

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

  void _seguirAlMotor() {
    final sesion = motor.sesion;
    if (sesion == null) {
      perfil.limpiar();
    } else {
      perfil.fijarToken(sesion.tokenDeAcceso);
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
        repositorioDeClientesDeVentaProvider
            .overrideWithValue(ClientesDelPos(RepositorioDeClientesHttp(http, base))),
        bodegaDeVentaProvider.overrideWith((ref) => ref.watch(bodegaActivaProvider)),
        ventaEnCursoProvider.overrideWith((ref) => ref.watch(ventaDelPosProvider)),
        permisosDeLaSesionProvider.overrideWith(
            (ref) => ref.watch(perfilComoNotifierProvider).claims?.permisos.toSet() ?? const {}),

        // Clientes
        repositorioDeClientesProvider
            .overrideWithValue(RepositorioDeClientes(http, base)),

        // Inventario
        repositorioDeInventarioProvider
            .overrideWithValue(RepositorioDeInventario(http)),

        // Mesas y comandas
        repositorioDeMesasProvider.overrideWithValue(RepositorioDeMesas(http)),
        repositorioDeComandasProvider.overrideWithValue(RepositorioDeComandas(http)),
        repositorioDeCuentasProvider.overrideWithValue(RepositorioDeCuentas(http)),
        repositorioDeCocinaProvider.overrideWithValue(RepositorioDeCocina(http)),

        // Facturación
        repositorioDeFacturasProvider.overrideWithValue(RepositorioDeFacturas(http)),
      ];

  Future<void> cerrar() async {
    motor.removeListener(_seguirAlMotor);
    dio.close(force: true);
    await base.close();
  }
}
