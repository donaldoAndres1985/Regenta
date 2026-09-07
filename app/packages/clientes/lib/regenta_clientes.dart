/// Modulo clientes. Punto de entrada unico del paquete. Cuelga del nucleo,
/// nunca de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/cliente_en_lista.dart';
export 'src/datos/repositorio_de_clientes.dart';
export 'src/lista/controlador_de_clientes.dart';
export 'src/lista/estado_de_clientes.dart';
export 'src/lista/filtro_de_clientes.dart';
export 'src/lista/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_clientes.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'clientes';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
