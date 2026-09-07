/// Nucleo compartido de Regenta. Cada modulo depende de este paquete y de
/// ningun otro modulo.
library;

export 'src/adaptativa/area_de_toque.dart';
export 'src/adaptativa/composicion_adaptativa.dart';
export 'src/adaptativa/forma_de_pantalla.dart';
export 'src/http/cliente_http.dart';
export 'src/http/cola_de_salida.dart';
export 'src/http/errores_http.dart';
export 'src/sesion/almacen_de_sesion.dart';
export 'src/sesion/cliente_auth.dart';
export 'src/sesion/cliente_auth_http.dart';
export 'src/sesion/interceptor_de_refresco.dart';
export 'src/sesion/motor_de_sesion.dart';
export 'src/sesion/sesion.dart';
export 'src/tema/patron_operativo.dart';
export 'src/tema/regenta_colors.dart';
export 'src/tema/regenta_spacing.dart';
export 'src/tema/regenta_theme.dart';
export 'src/tema/regenta_type.dart';

/// Version del contrato del nucleo. Sube cuando cambia algo que los modulos
/// consumen.
const String versionDelNucleo = '0.1.0';
