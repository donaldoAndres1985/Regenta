/// Nucleo compartido de Regenta. Cada modulo depende de este paquete y de
/// ningun otro modulo.
library;

// La navegacion de Regenta se construye sobre go_router; los modulos declaran
// sus GoRoute y usan context.go() a traves de este re-export.
export 'package:go_router/go_router.dart';

export 'src/adaptativa/area_de_toque.dart';
export 'src/alertas/alerta_vista.dart';
export 'src/alertas/controlador_de_alertas.dart';
export 'src/alertas/repositorio_de_alertas.dart';
export 'src/alertas/widgets_de_alertas.dart';
export 'src/adaptativa/composicion_adaptativa.dart';
export 'src/adaptativa/forma_de_pantalla.dart';
export 'src/http/cliente_http.dart';
export 'src/http/cola_de_salida.dart';
export 'src/http/errores_http.dart';
export 'src/local/base_local.dart';
export 'src/local/politica_de_purga.dart';
export 'src/navegacion/enlace_profundo.dart';
export 'src/navegacion/enrutador_regenta.dart';
export 'src/navegacion/guardia_de_rutas.dart';
export 'src/navegacion/ruta_protegida.dart';
export 'src/negocio/claims_de_sesion.dart';
export 'src/negocio/cliente_de_negocio_http.dart';
export 'src/negocio/modulos_activos.dart';
export 'src/negocio/perfil_de_sesion.dart';
export 'src/negocio/resumen_del_negocio.dart';
export 'src/sesion/almacen_de_sesion.dart';
export 'src/sesion/cliente_auth.dart';
export 'src/sesion/cliente_auth_http.dart';
export 'src/sesion/interceptor_de_refresco.dart';
export 'src/sesion/motor_de_sesion.dart';
export 'src/sesion/sesion.dart';
export 'src/sincronizacion/cola_de_salida_local.dart';
export 'src/sincronizacion/resumen_de_cola.dart';
export 'src/sincronizacion/sincronizacion_en_segundo_plano.dart';
export 'src/sincronizacion/trabajador_de_sincronizacion.dart';
export 'src/tema/patron_operativo.dart';
export 'src/tema/regenta_colors.dart';
export 'src/tema/regenta_spacing.dart';
export 'src/tema/regenta_theme.dart';
export 'src/tema/regenta_type.dart';

/// Version del contrato del nucleo. Sube cuando cambia algo que los modulos
/// consumen.
const String versionDelNucleo = '0.1.0';
