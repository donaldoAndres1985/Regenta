/// Modulo compras. Punto de entrada unico del paquete. Cuelga del nucleo, nunca
/// de otro modulo.
library;

import 'package:regenta_core/regenta_core.dart';

export 'src/compra_directa/controlador.dart';
export 'src/compra_directa/datos.dart';
export 'src/compra_directa/estado.dart';
export 'src/compra_directa/pantalla_compra_directa.dart';
export 'src/configuracion/configuracion_de_compras.dart';
export 'src/cuentas/controlador.dart';
export 'src/cuentas/datos.dart';
export 'src/cuentas/dialogo_de_pago.dart';
export 'src/cuentas/dialogo_de_pagos.dart';
export 'src/cuentas/reparto.dart';
export 'src/cuentas/pantalla_cuentas_por_pagar.dart';
export 'src/datos/orden_recibible.dart';
export 'src/datos/orden_resumen_vista.dart';
export 'src/datos/repositorio_de_recepciones.dart';
export 'src/escaner/escaner_de_codigos.dart';
export 'src/escaner/escaner_mobile_scanner.dart';
export 'src/proveedores/datos/producto_del_proveedor.dart';
export 'src/proveedores/datos/proveedor_del_negocio.dart';
export 'src/proveedores/datos/repositorio_de_proveedores.dart';
export 'src/proveedores/lista/controlador_de_proveedores.dart';
export 'src/proveedores/lista/estado_de_proveedores.dart';
export 'src/proveedores/lista/proveedores.dart';
export 'src/proveedores/ui/catalogo_del_proveedor.dart';
export 'src/proveedores/ui/formulario_de_proveedor.dart';
export 'src/proveedores/ui/pantalla_proveedores.dart';
export 'src/recepcion/controlador_de_recepcion.dart';
export 'src/recepcion/entrada_de_linea.dart';
export 'src/recepcion/estado_de_recepcion.dart';
export 'src/recepcion/proveedores.dart';
export 'src/ui/formato.dart';
export 'src/ui/pantalla_compras.dart';
export 'src/ui/pantalla_recepcion.dart';

/// Nombre del modulo, para el registro de paquetes activos por patron/plan.
const String nombreDelModulo = 'compras';

/// Deja constancia de que este modulo se construye sobre el nucleo.
const String nucleoRequerido = versionDelNucleo;
