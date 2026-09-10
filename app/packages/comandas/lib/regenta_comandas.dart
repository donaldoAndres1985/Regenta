/// Modulo comandas. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/comanda/controlador_de_comanda.dart';
export 'src/comanda/estado_de_comanda.dart';
export 'src/comanda/proveedores.dart';
export 'src/datos/comanda_vista.dart';
export 'src/datos/repositorio_de_comandas.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_comanda.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'comandas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
