/// Modulo facturacion. Punto de entrada unico del paquete. Cuelga del nucleo,
/// nunca de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/datos/factura_vista.dart';
export 'src/datos/repositorio_de_facturas.dart';
export 'src/factura/controlador_de_factura.dart';
export 'src/factura/estado_de_factura.dart';
export 'src/factura/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_factura.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'facturacion';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
