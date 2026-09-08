/// Modulo compras. Punto de entrada unico del paquete. Cuelga del nucleo, nunca
/// de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/orden_recibible.dart';
export 'src/datos/repositorio_de_recepciones.dart';
export 'src/escaner/escaner_de_codigos.dart';
export 'src/escaner/escaner_mobile_scanner.dart';
export 'src/recepcion/controlador_de_recepcion.dart';
export 'src/recepcion/entrada_de_linea.dart';
export 'src/recepcion/estado_de_recepcion.dart';
export 'src/recepcion/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_recepcion.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'compras';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
