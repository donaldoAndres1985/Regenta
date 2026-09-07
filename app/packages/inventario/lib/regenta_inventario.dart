/// Modulo inventario. Punto de entrada unico del paquete. Cuelga del nucleo,
/// nunca de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/busqueda/controlador_de_busqueda.dart';
export 'src/busqueda/estado_de_busqueda.dart';
export 'src/busqueda/proveedores.dart';
export 'src/datos/codigo_resuelto.dart';
export 'src/datos/producto_encontrado.dart';
export 'src/datos/repositorio_de_inventario.dart';
export 'src/escaner/escaner_de_codigos.dart';
export 'src/escaner/escaner_mobile_scanner.dart';
export 'src/ui/captura_manual_de_codigo.dart';
export 'src/ui/fila_de_producto.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_inventario.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'inventario';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
