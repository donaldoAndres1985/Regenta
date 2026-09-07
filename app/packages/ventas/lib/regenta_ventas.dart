/// Modulo ventas. Punto de entrada unico del paquete. Cuelga del nucleo, nunca
/// de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/producto_buscado.dart';
export 'src/datos/repositorio_de_ventas.dart';
export 'src/escaner/escaner_de_codigos.dart';
export 'src/escaner/escaner_mobile_scanner.dart';
export 'src/pos/controlador_del_pos.dart';
export 'src/pos/estado_del_pos.dart';
export 'src/pos/linea_de_carrito.dart';
export 'src/pos/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_pos.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'ventas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
