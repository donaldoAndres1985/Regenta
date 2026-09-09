/// Modulo mesas. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/mesa_en_plano.dart';
export 'src/datos/plano_del_salon.dart';
export 'src/datos/repositorio_de_mesas.dart';
export 'src/datos/sesion_de_mesa.dart';
export 'src/plano/controlador_del_plano.dart';
export 'src/plano/estado_del_plano.dart';
export 'src/plano/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_mesas.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'mesas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
