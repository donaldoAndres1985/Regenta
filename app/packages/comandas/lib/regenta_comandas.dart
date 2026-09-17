/// Modulo comandas. Punto de entrada unico del paquete.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/comanda/controlador_de_comanda.dart';
export 'src/comanda/estado_de_comanda.dart';
export 'src/comanda/proveedores.dart';
export 'src/cocina/controlador_de_cocina.dart';
export 'src/cocina/estado_de_cocina.dart';
export 'src/cocina/proveedores.dart';
export 'src/cuenta/controlador_de_division.dart';
export 'src/cuenta/estado_de_division.dart';
export 'src/cuenta/proveedores.dart';
export 'src/datos/comanda_vista.dart';
export 'src/datos/cuenta_vista.dart';
export 'src/datos/repositorio_de_comandas.dart';
export 'src/datos/repositorio_de_cocina.dart';
export 'src/datos/repositorio_de_cuentas.dart';
export 'src/datos/ticket_de_cocina.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_comanda.dart';
export 'src/ui/pantalla_dividir_cuenta.dart';
export 'src/ui/pantalla_kds.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'comandas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
