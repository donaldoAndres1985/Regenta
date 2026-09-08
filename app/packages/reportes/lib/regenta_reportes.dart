/// Modulo reportes. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/caja/controlador_de_reportes_de_caja.dart';
export 'src/caja/lista_de_sesiones_de_caja.dart';
export 'src/caja/repositorio_de_reportes_de_caja.dart';
export 'src/caja/sesion_de_caja_vista.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'reportes';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
