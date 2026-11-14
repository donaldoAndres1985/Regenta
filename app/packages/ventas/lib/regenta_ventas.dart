/// Modulo ventas. Punto de entrada unico del paquete. Cuelga del nucleo, nunca
/// de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/caja/controlador_de_caja.dart';
export 'src/caja/datos/repositorio_de_caja.dart';
export 'src/caja/datos/sesion_caja_modelos.dart';
export 'src/caja/estado_de_caja.dart';
export 'src/caja/proveedores.dart';
export 'src/cliente/controlador_de_cliente.dart';
export 'src/cliente/estado_de_cliente.dart';
export 'src/cliente/proveedores.dart';
export 'src/cobro/controlador_de_cobro.dart';
export 'src/cobro/estado_del_cobro.dart';
export 'src/cobro/proveedores.dart';
export 'src/datos/cliente_de_la_venta.dart';
export 'src/datos/producto_buscado.dart';
export 'src/datos/repositorio_de_clientes_de_venta.dart';
export 'src/datos/repositorio_de_ventas.dart';
export 'src/escaner/escaner_de_codigos.dart';
export 'src/escaner/escaner_mobile_scanner.dart';
export 'src/pos/controlador_del_pos.dart';
export 'src/pos/estado_del_pos.dart';
export 'src/pos/linea_de_carrito.dart';
export 'src/pos/proveedores.dart';
export 'src/ui/dialogo_de_cierre_por_metodo.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_cliente_de_venta.dart';
export 'src/ui/pantalla_caja.dart';
export 'src/ui/pantalla_cobro.dart';
export 'src/ui/pantalla_buscar_venta.dart';
export 'src/ui/pantalla_devolucion.dart';
export 'src/ui/ticket_pos.dart';
export 'src/ui/pantalla_pos.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'ventas';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
