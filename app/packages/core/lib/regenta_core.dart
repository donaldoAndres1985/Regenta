/// Nucleo compartido de Regenta. Cada modulo depende de este paquete y de
/// ningun otro modulo.
library;

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

/// Version del contrato del nucleo. Sube cuando cambia algo que los modulos
/// consumen.
const String versionDelNucleo = '0.1.0';
