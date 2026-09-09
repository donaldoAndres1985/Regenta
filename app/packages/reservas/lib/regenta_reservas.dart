/// Modulo reservas. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/calendario/calendario_de_ocupacion.dart';
export 'src/calendario/controlador_de_calendario.dart';
export 'src/calendario/ocupacion_vista.dart';
export 'src/calendario/pantalla_calendario.dart';
export 'src/calendario/repositorio_de_calendario.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'reservas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
