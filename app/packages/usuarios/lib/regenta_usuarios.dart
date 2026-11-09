/// Modulo usuarios. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/configuracion_negocio_vista.dart';
export 'src/datos/repositorio_de_usuarios.dart';
export 'src/ui/pantalla_configuracion.dart';
export 'src/usuarios/proveedores.dart';


/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'usuarios';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
