# Funcionalidades de Sara POS — Inventario atómico por pantalla

> **Propósito de este documento:** especificación funcional exhaustiva, pantalla por pantalla, de la aplicación de escritorio Sara POS, extraída directamente del código fuente Java (no de la interfaz visual ni de suposiciones). Está pensado como insumo para **replicar estas funcionalidades en otra aplicación (Regenta)** sin necesidad de volver a leer el código original.
>
> **Cómo leer cada sección:** cada pantalla documenta su propósito, cómo se invoca, sus componentes de interfaz, una lista numerada y atómica de comportamientos (botón por botón, atajo por atajo, validación por validación, con mensajes literales cuando el código los tiene), las tablas de base de datos que lee o escribe, los reportes JasperReports que genera y las reglas de negocio no obvias. Cuando una pantalla tiene variantes casi idénticas en el código (p. ej. `Ventas` / `Ventas1` / `Ventas2` / `Ventas3`), se documentan las diferencias explícitamente en lugar de asumir que son iguales.
>
> **Hallazgos de código:** varias secciones incluyen notas de "bugs/peculiaridades" encontradas al leer el código (validaciones que nunca se activan, botones sin implementación, consultas SQL con columnas equivocadas, etc.). Se documentan tal cual existen hoy en Sara POS — **decidir caso por caso si se replican o se corrigen al construir Regenta.**
>
> Generado a partir de la lectura completa de los archivos `.java` en `src/`, el esquema `sarapos3_final_funcionando.sql` y las plantillas `.jrxml` de `REPORTES/`.

## Índice
- [Grupo: Autenticación, Menú Principal y Configuración](#grupo-autenticación-menú-principal-y-configuración)
  - [Loguin.java — Pantalla de inicio de sesión (teclado numérico de contraseña)](#loguinjava-pantalla-de-inicio-de-sesión-teclado-numérico-de-contraseña)
  - [principal.java — Selección de rol de usuario (pantalla intermedia tras conectar BD)](#principaljava-selección-de-rol-de-usuario-pantalla-intermedia-tras-conectar-bd)
  - [Menu.java — Panel de navegación por categorías (VERSIÓN LEGACY/INACTIVA)](#menujava-panel-de-navegación-por-categorías-versión-legacyinactiva)
  - [Menunew.java — Menú principal de navegación por módulos (activo)](#menunewjava-menú-principal-de-navegación-por-módulos-activo)
  - [MenuPrincipal.java — Ventana principal MDI (JFrame raíz, Main-Class del .jar)](#menuprincipaljava-ventana-principal-mdi-jframe-raíz-main-class-del-jar)
  - [Configuracion.java — Configuración del sistema (datos del negocio y parámetros de facturación)](#configuracionjava-configuración-del-sistema-datos-del-negocio-y-parámetros-de-facturación)
  - [BdConection.java — Configuración de conexión a la base de datos MySQL](#bdconectionjava-configuración-de-conexión-a-la-base-de-datos-mysql)
  - [AcercaDe.java — Acerca de / About](#acercadejava-acerca-de-about)
  - [fechas.java — Selector de rango de fechas (diálogo utilitario para informes)](#fechasjava-selector-de-rango-de-fechas-diálogo-utilitario-para-informes)
- [Grupo: Ventas / Punto de Venta](#grupo-ventas-punto-de-venta)
  - [Ventas.java — Pantalla principal de Venta / Punto de Venta (POS)](#ventasjava-pantalla-principal-de-venta-punto-de-venta-pos)
  - [Ventas1.java — Variante de la pantalla de Venta (caja/registradora concurrente #2)](#ventas1java-variante-de-la-pantalla-de-venta-cajaregistradora-concurrente-2)
  - [Ventas2.java — Variante de la pantalla de Venta (caja/registradora concurrente #3)](#ventas2java-variante-de-la-pantalla-de-venta-cajaregistradora-concurrente-3)
  - [Ventas3.java — Variante/prototipo de la pantalla de Venta (NO USADA — código muerto)](#ventas3java-varianteprototipo-de-la-pantalla-de-venta-no-usada-código-muerto)
  - [DetalleDeVenta.java — Diálogo de detalle de una venta (consulta)](#detalledeventajava-diálogo-de-detalle-de-una-venta-consulta)
  - [EditarVentas.java — Editar ventas (buscador de facturas — VERSIÓN ROTA/INCOMPLETA en producción)](#editarventasjava-editar-ventas-buscador-de-facturas-versión-rotaincompleta-en-producción)
  - [EditarVentas1.java — Editar ventas (VERSIÓN COMPLETA — código muerto, no enlazado a ningún menú)](#editarventas1java-editar-ventas-versión-completa-código-muerto-no-enlazado-a-ningún-menú)
  - [VentasDiarias.java — Detalle de Ventas / Cuentas por Cobrar (listado y filtro de ventas por fecha/cliente)](#ventasdiariasjava-detalle-de-ventas-cuentas-por-cobrar-listado-y-filtro-de-ventas-por-fechacliente)
  - [VentasGuardadas.java — Ventas Guardadas (carritos puestos en espera)](#ventasguardadasjava-ventas-guardadas-carritos-puestos-en-espera)
  - [VentasAnuladas.java — Ventas Anuladas (histórico de facturas anuladas)](#ventasanuladasjava-ventas-anuladas-histórico-de-facturas-anuladas)
  - [Cargarfactura.java — Diálogo "Cargar Factura" (buscar y cargar una factura para editar/anular)](#cargarfacturajava-diálogo-cargar-factura-buscar-y-cargar-una-factura-para-editaranular)
  - [CargarCotizacion.java — Diálogo "Cargar Cotización" (buscar y convertir una cotización en venta)](#cargarcotizacionjava-diálogo-cargar-cotización-buscar-y-convertir-una-cotización-en-venta)
  - [MovimientoCajaVentas.java — Diálogo "Movimiento de Caja" (entradas/salidas manuales de efectivo)](#movimientocajaventasjava-diálogo-movimiento-de-caja-entradassalidas-manuales-de-efectivo)
  - [cambio.java — Diálogo de Cambio/Vuelto](#cambiojava-diálogo-de-cambiovuelto)
  - [calculado.java — Diálogo "Calculadora de Denominaciones" (conteo de efectivo por billete/moneda)](#calculadojava-diálogo-calculadora-de-denominaciones-conteo-de-efectivo-por-billetemoneda)
  - [Descuento.java — Diálogo para ingresar el descuento de una línea de venta](#descuentojava-diálogo-para-ingresar-el-descuento-de-una-línea-de-venta)
  - [Pago.java — Diálogo de Forma de Pago (finalización de la venta / cobro)](#pagojava-diálogo-de-forma-de-pago-finalización-de-la-venta-cobro)
  - [Cantidad.java — Diálogo para ingresar la cantidad de una línea de venta](#cantidadjava-diálogo-para-ingresar-la-cantidad-de-una-línea-de-venta)
  - [Proceso.java — Diálogo de "Procesando..." (indicador de progreso)](#procesojava-diálogo-de-procesando-indicador-de-progreso)
  - [Resumen de dependencias cruzadas y hallazgos clave para la migración](#resumen-de-dependencias-cruzadas-y-hallazgos-clave-para-la-migración)
- [Grupo: Productos e Inventario](#grupo-productos-e-inventario)
  - [Productos.java — Gestión Principal de Productos](#productosjava-gestión-principal-de-productos)
  - [ListaProductos.java — Selector/Listado Emergente de Productos](#listaproductosjava-selectorlistado-emergente-de-productos)
  - [DetalleProducto.java — Detalle de Fracciones/Presentaciones de un Producto (Ventas)](#detalleproductojava-detalle-de-fraccionespresentaciones-de-un-producto-ventas)
  - [DetallarProductos.java — Detallar Productos (fraccionamiento producto‑a‑producto, pantalla completa)](#detallarproductosjava-detallar-productos-fraccionamiento-productoaproducto-pantalla-completa)
  - [DetallarProductosFacil.java — Detallar Productos (versión simplificada, un solo paso)](#detallarproductosfaciljava-detallar-productos-versión-simplificada-un-solo-paso)
  - [AgregarDetalleProducto.java — Alta/Edición de Presentaciones (detalle_producto)](#agregardetalleproductojava-altaedición-de-presentaciones-detalle_producto)
  - [AgregarExistecias.java — Agregar/Modificar Existencias y Datos de un Producto (sic "Existecias")](#agregarexisteciasjava-agregarmodificar-existencias-y-datos-de-un-producto-sic-existecias)
  - [CargarCompra.java — Selector de Factura de Compra para Cargar/Editar](#cargarcomprajava-selector-de-factura-de-compra-para-cargareditar)
  - [ImportarProductos.java — Importación Masiva de Productos desde Excel](#importarproductosjava-importación-masiva-de-productos-desde-excel)
  - [InventarioFacil.java — Inventario Fácil (alta/edición de producto con 3 niveles de precio + ajustes rápidos de stock)](#inventariofaciljava-inventario-fácil-altaedición-de-producto-con-3-niveles-de-precio-ajustes-rápidos-de-stock)
  - [InventarioGeneral.java — Inventario General (listado/valoración y activar‑desactivar productos)](#inventariogeneraljava-inventario-general-listadovaloración-y-activardesactivar-productos)
  - [GestionInventario.java — Gestión de Inventario (registro de compras / entradas y salidas)](#gestioninventariojava-gestión-de-inventario-registro-de-compras-entradas-y-salidas)
  - [MovimientoExistencias.java — Movimiento de Existencias (ajuste puntual de stock de un producto)](#movimientoexistenciasjava-movimiento-de-existencias-ajuste-puntual-de-stock-de-un-producto)
  - [Categorias.java — Mantenimiento de Categorías](#categoriasjava-mantenimiento-de-categorías)
  - [Unidades.java — Mantenimiento de Unidades de Medida](#unidadesjava-mantenimiento-de-unidades-de-medida)
  - [Iva.java — Aplicar IVA a Línea(s) del Carrito de Inventario](#ivajava-aplicar-iva-a-líneas-del-carrito-de-inventario)
  - [PrecioCompra.java — Editar Precio de Compra de una Línea (diálogo rápido)](#preciocomprajava-editar-precio-de-compra-de-una-línea-diálogo-rápido)
  - [PrecioVenta.java — Editar Precio de Venta (con calculadora opcional de margen)](#precioventajava-editar-precio-de-venta-con-calculadora-opcional-de-margen)
  - [CalcularPrecioVenta.java — Calculadora de Precio de Venta por Margen de Ganancia](#calcularprecioventajava-calculadora-de-precio-de-venta-por-margen-de-ganancia)
  - [ProductoRapidoNuevo.java — Alta/Edición Rápida de Producto (sin precios)](#productorapidonuevojava-altaedición-rápida-de-producto-sin-precios)
  - [VerificarCompras.java — Verificar Compras por Proveedor](#verificarcomprasjava-verificar-compras-por-proveedor)
  - [VerificarProductosCompras.java — Verificar Compras por Producto](#verificarproductoscomprasjava-verificar-compras-por-producto)
  - [detalleCompra.java — Detalle de Líneas de una Factura de Compra (visor)](#detallecomprajava-detalle-de-líneas-de-una-factura-de-compra-visor)
- [Grupo: Clientes, Proveedores y Cuentas](#grupo-clientes-proveedores-y-cuentas)
  - [Clientes.java — Gestión de Clientes](#clientesjava-gestión-de-clientes)
  - [DetalleCliente.java — Estado de Cuentas del Cliente](#detalleclientejava-estado-de-cuentas-del-cliente)
  - [buscarClientes.java — Selector de Cliente (diálogo de búsqueda)](#buscarclientesjava-selector-de-cliente-diálogo-de-búsqueda)
  - [Proveedor.java — Gestión de Proveedores](#proveedorjava-gestión-de-proveedores)
  - [buscarProveedor.java — Selector de Proveedor (diálogo de búsqueda)](#buscarproveedorjava-selector-de-proveedor-diálogo-de-búsqueda)
  - [CuentasPorCobrar.java — Cuentas por Cobrar a Clientes](#cuentasporcobrarjava-cuentas-por-cobrar-a-clientes)
  - [CuentasorporPagar.java — Cuentas por Pagar a Proveedores](#cuentasorporpagarjava-cuentas-por-pagar-a-proveedores)
  - [FiltrarCuentas.java — Filtro de Cuentas (proveedor/estado)](#filtrarcuentasjava-filtro-de-cuentas-proveedorestado)
  - [InformeCuentas.java — Informes de Cuentas por Cobrar](#informecuentasjava-informes-de-cuentas-por-cobrar)
  - [Abonos.java — Registro de Abonos de Clientes](#abonosjava-registro-de-abonos-de-clientes)
  - [AbonosCuentas.java — Registro de Abonos a Proveedores](#abonoscuentasjava-registro-de-abonos-a-proveedores)
  - [InformesClientes.java — Informe General / 360° del Cliente](#informesclientesjava-informe-general-360-del-cliente)
  - [InformesProductos.java — Informe de Productos (incluido por relación con Cuentas/Proveedores)](#informesproductosjava-informe-de-productos-incluido-por-relación-con-cuentasproveedores)
- [Grupo: Caja e Informes](#grupo-caja-e-informes)
  - [Convenciones y catálogos usados por todo el grupo](#convenciones-y-catálogos-usados-por-todo-el-grupo)
  - [MovimientoCaja.java — Movimientos de caja (Entrada / Salida de efectivo)](#movimientocajajava-movimientos-de-caja-entrada-salida-de-efectivo)
  - [CerrarCaja.java — Caja (arqueo y totales de la sesión abierta)](#cerrarcajajava-caja-arqueo-y-totales-de-la-sesión-abierta)
  - [cerrarCajaCalculado.java — Cierre de caja (arqueo, diferencias e impresión)](#cerrarcajacalculadojava-cierre-de-caja-arqueo-diferencias-e-impresión)
  - [buscarCierreCaja.java — Buscar cierre de caja](#buscarcierrecajajava-buscar-cierre-de-caja)
  - [DetalleMovimientoCaja.java — Detalle de un movimiento de caja individual](#detallemovimientocajajava-detalle-de-un-movimiento-de-caja-individual)
  - [detellaMovimientoCaja.java — Detalle de movimientos de un cierre de caja (listado completo)](#detellamovimientocajajava-detalle-de-movimientos-de-un-cierre-de-caja-listado-completo)
  - [InformesCaja.java — Informes de caja](#informescajajava-informes-de-caja)
  - [Notas sobre los renderers de moneda (`OTRASCLASES`)](#notas-sobre-los-renderers-de-moneda-otrasclases)

---

## Grupo: Autenticación, Menú Principal y Configuración

### Loguin.java — Pantalla de inicio de sesión (teclado numérico de contraseña)

**Propósito:** Diálogo modal para autenticar a un usuario dentro de un rol (privilegio) ya elegido en la pantalla `principal`. Verifica usuario/contraseña contra la tabla `usuarios` y, si es correcto, inicia sesión en la aplicación.

**Cómo se invoca / navegación:** Es un `javax.swing.JDialog` (NO JInternalFrame). Se instancia desde `principal.java` al pulsar uno de los botones de rol (Administrador, Gerente, Empleado, Vendedor):
```java
Loguin Loguin = new Loguin(this, true, MenuPrincipal.BD, "Administrador");
Loguin.setVisible(true);
```
Recibe 4 parámetros en el constructor: `parent` (instancia de `principal`), `modal` (boolean, siempre `true` en las llamadas actuales), `BD` (instancia estática `TestConnection` ya conectada, `MenuPrincipal.BD`) y `USUARIO_` (cadena con el rol textual: "Administrador", "Gerente", "Empleado" o "Vendedor"). El rol recibido se coloca como texto del label `TITULO` (que además actúa como el campo de comparación de privilegio en la consulta SQL). Al abrir, el campo de contraseña se vacía (`contrasena.setText("")`) y recibe el foco (`contrasena.grabFocus()`). No es redimensionable (`setResizable(false)`).

**Componentes de interfaz:**
- `TITULO` (JLabel): muestra el nombre del rol elegido (ícono de usuario + texto del rol). Su texto se usa también como criterio de búsqueda SQL del privilegio.
- `contrasena` (JPasswordField): campo donde se escribe la contraseña, oculta los caracteres.
- Teclado numérico en pantalla (panel `jPanel2`) formado por botones `UNO`…`NUEVE`, `CERO`, `PUNTO` (".") y `MENOS` ("-"): cada uno concatena su carácter al texto actual de `contrasena`.
- `LIMPIAR` (botón "CE"): vacía el campo de contraseña.
- `jButton3` ("Entrar", ícono ok.png): dispara la verificación de usuario y, si es válida, el login.
- `jButton2` ("Cancelar"): cierra el diálogo sin iniciar sesión.
- `jSeparator1`: separador visual decorativo.

**Funcionalidades atómicas (numeradas):**
1. Al construirse el diálogo, se fija el texto de `TITULO` al rol recibido por parámetro, se limpia `contrasena` y se le da el foco.
2. Al pulsar cualquiera de los botones numéricos (`UNO`…`NUEVE`, `CERO`), se concatena el dígito correspondiente al texto actual de `contrasena` (no hay máscara de longitud máxima ni bloqueo de caracteres).
3. Al pulsar `PUNTO`, se concatena un "." al texto de `contrasena`.
4. Al pulsar `MENOS`, el manejador existe pero está vacío (sin efecto, botón inactivo funcionalmente).
5. Al pulsar `LIMPIAR` ("CE"), se vacía por completo el campo `contrasena` (`setText("")`).
6. Al ganar el foco `contrasena` (clic o tab), su fondo cambia a un color gris-azulado `RGB(223,223,238)` y se selecciona todo el texto existente (`selectAll()`).
7. Al perder el foco `contrasena`, su fondo regresa a blanco `RGB(255,255,255)`.
8. Si dentro de `contrasena` se pulsa la tecla Enter (detectado en `keyTyped`, comparando `evt.getKeyChar()==KeyEvent.VK_ENTER`), se transfiere el foco al siguiente componente (`transferFocus()`) — en la práctica esto no dispara el login directamente, sólo mueve el foco.
9. Al pulsar `jButton3` ("Entrar"):
   - 9.1 Se ejecuta `VerificarUsuario()`, que lanza la consulta SQL: `SELECT upper(privilegios),CC,ID_USUARIO,upper(NOMBRES_APELLIDO) FROM usuarios WHERE usuarios.privilegios='<TITULO.getText()>' AND usuarios.contrasena='<contrasena.getText()>'` (concatenación directa de strings, sin parametrizar — vulnerable a inyección SQL).
   - 9.2 Si la consulta devuelve una fila (`resultado.next()` es true), se extraen: privilegio (en mayúsculas), `CC` (cédula), `ID_USUARIO` y `NOMBRES_APELLIDO` (en mayúsculas), y se devuelven como el string de privilegio.
   - 9.3 Si el privilegio devuelto tiene longitud > 0 (login correcto): se invoca `Obj1.loguin(CC, privilegio, ID_USUARIO, NOMBRE_USUARIO)` sobre la instancia de `principal` que abrió el diálogo, y el diálogo se cierra (`this.dispose()`).
   - 9.4 Si la consulta no retorna filas (usuario/contraseña incorrectos o no coincide con el rol elegido en `TITULO`): se muestra `JOptionPane.showMessageDialog(null, "Usuario y contraseña incorrecta.", "Inventario ESE", JOptionPane.WARNING_MESSAGE)`, se regresa el foco a `contrasena` y se selecciona todo su texto.
10. Al pulsar `jButton2` ("Cancelar"), el diálogo se cierra sin más acción (`this.dispose()`), no se llama a `loguin()`.
11. Métodos de utilidad `pasarFocusVerificarNumeros`, `ganarFoco`, `pierdeFoco` existen en la clase pero no están conectados a ningún evento real del formulario (código muerto/reutilizado de otra pantalla).

**Datos / tablas de BD leídas o escritas:**
- `usuarios` (SELECT únicamente): columnas `privilegios`, `CC`, `ID_USUARIO`, `NOMBRES_APELLIDO`, `contrasena`. Condición: `privilegios` debe coincidir EXACTAMENTE (en mayúsculas comparado contra el texto tal cual escrito en la BD, ya que el filtro WHERE usa `usuarios.privilegios` sin `upper()`) con el rol mostrado en `TITULO`, y `contrasena` debe coincidir con el texto ingresado (comparación de texto plano, sin hash).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- El login está *acoplado al rol elegido previamente* en la pantalla `principal`: un usuario con `PRIVILEGIOS='ADMINISTRADOR'` en la BD solo podrá iniciar sesión si entra por el botón "Administrador" de `principal`, porque el filtro WHERE exige que `privilegios` coincida con el texto puesto en `TITULO` (que es el botón pulsado en `principal`, con primera letra mayúscula, ej. "Administrador"). Dado que la comparación de la columna `privilegios` en la cláusula WHERE NO aplica `upper()` sobre ella (solo se compara igual contra el valor de `TITULO.getText()` tal cual, p. ej. "Administrador"), y en la tabla real los valores están guardados en mayúsculas (ej. `'ADMINISTRADOR'`), existe una inconsistencia de mayúsculas/minúsculas potencial entre lo que se guarda en BD (ver INSERT de ejemplo: `'ADMINISTRADOR'`) y lo que compara el WHERE (`'Administrador'`); en el SELECT sólo se aplica `upper()` a la columna que se LEE para comparar "privilegios" en el SELECT (el primer campo del SELECT), no en el WHERE. Esto puede causar fallos de login si los roles no coinciden textualmente (ver nota en "Notas/peculiaridades").
- No existe control de intentos fallidos, bloqueo de cuentas ni registro de auditoría de login en este archivo.
- La contraseña se almacena y compara en texto plano (columna `CONTRASENA varchar(255)`), sin cifrado/hash.

**Notas / peculiaridades:**
- Posible bug: el WHERE de la consulta (`usuarios.privilegios='"+TITULO.getText()+"'`) compara contra el texto EXACTO del botón pulsado en `principal` (p.ej. "Administrador" con mayúscula inicial solamente), mientras que los datos de ejemplo en la BD están en mayúsculas totales (`'ADMINISTRADOR'`). Si MySQL usa un collation case-insensitive (muy común, `utf8_general_ci`), el login funciona pese a la diferencia de mayúsculas; si el collation fuera case-sensitive, el login fallaría siempre. Al migrar a Regenta se recomienda normalizar ambos lados a mayúsculas explícitamente para evitar depender del collation de la BD.
- El teclado numérico en pantalla sugiere que esta ventana fue diseñada también para ser operable con pantalla táctil (sin teclado físico).
- El botón `MENOS` no tiene ningún efecto (código vacío) — posible resto de una pantalla genérica reutilizada de captura numérica.
- Existe acoplamiento bidireccional: `Loguin` conoce a `principal` (campo `Obj1`) y llama a `Obj1.loguin(...)`, que a su vez usa variables estáticas de `MenuPrincipal` (ver sección `principal.java`).


### principal.java — Selección de rol de usuario (pantalla intermedia tras conectar BD)

**Propósito:** Es la primera pantalla operativa que se muestra dentro del escritorio MDI tras una conexión exitosa a la base de datos; permite elegir con qué rol se va a iniciar sesión (Administrador, Gerente, Empleado o Vendedor) y, tras el login exitoso, instala el menú principal de la aplicación (`Menunew`).

**Cómo se invoca / navegación:** Es un `javax.swing.JInternalFrame` (ventana interna del escritorio `Escritorio` de `MenuPrincipal`). Se crea e inserta automáticamente dentro de `MenuPrincipal.TratarDeConectarBD()` cuando la conexión a la base de datos es exitosa:
```java
principal PRINCIPAL = new principal();
this.Escritorio.add(PRINCIPAL);
PRINCIPAL.setMaximum(true);
PRINCIPAL.show();
```
No recibe parámetros en el constructor. Al construirse, oculta la barra norte propia del JInternalFrame (`setNorthPane(null)`), de forma que no se ve la barra de título/controles estándar de la ventana interna.

**Componentes de interfaz:**
- Fila superior de 4 botones grandes con ícono (jButton14 "Vendedor", jButton15 "Empleado", jButton16 "Gerente", jButton17 "Administrador") — definidos en el panel `jPanel7`, pero NO tienen lógica real (sus `ActionPerformed` están vacíos). Parecen un remanente de diseño duplicado/no usado.
- Fila principal de 4 botones grandes con descripción (jButton10 "Administrador", jButton11 "Gerente", jButton12 "Empleado", jButton13 "Vendedor") en el panel `jPanel5` — éstos SÍ abren el diálogo de login (`Loguin`) con el rol correspondiente.
- `jButton2` ("Acerca de"): visual únicamente (resalta al pasar el mouse), sin acción al hacer clic (no abre ningún diálogo "Acerca de" realmente, su `ActionPerformed` no existe/está vacío — solo tiene manejadores mouseEntered/mouseExited).
- `jButton1` ("Cerrar"): cierra completamente la aplicación.
- `jLabel1`, `jLabel2`, `jLabel3`: textos informativos/branding (copyright, descripción del producto, logotipo).

**Funcionalidades atómicas (numeradas):**
1. Al pulsar `jButton10` ("Administrador" — descripción "responsable de gestionar y conservar los datos de su empresa"), se abre `new Loguin(this, true, MenuPrincipal.BD, "Administrador")` y se hace visible (modal).
2. Al pulsar `jButton11` ("Gerente"), se abre `new Loguin(this, true, MenuPrincipal.BD, "Gerente")`.
3. Al pulsar `jButton12` ("Empleado"), se abre `new Loguin(this, true, MenuPrincipal.BD, "Empleado")`.
4. Al pulsar `jButton13` ("Vendedor"), se abre `new Loguin(this, true, MenuPrincipal.BD, "Vendedor")`.
5. Al pasar el mouse sobre cualquiera de los 4 botones de `jPanel5` (10,11,12,13) se activa `setContentAreaFilled(true)` (efecto hover de fondo); al salir el mouse, vuelve a `false`.
6. Los botones 14-17 ("Vendedor/Empleado/Gerente/Administrador" de `jPanel7`) no ejecutan ninguna acción al pulsarlos (comentario `// TODO add your handling code here:` sin código real) — parecen vestigio de una versión anterior del diseño.
7. Al pulsar `jButton1` ("Cerrar"), se ejecuta `System.exit(0)`: cierre total e inmediato de la aplicación (sin pedir confirmación).
8. Método público `loguin(String CC, String privilegios, String ID_USUARIO, String NOMBRE_USUARIO_)`, invocado por `Loguin` tras una autenticación exitosa:
   - 8.1 Si `CC` no está vacío: guarda en el mapa estático `MenuPrincipal.DATOS_OBLIGATORIOS` las claves `"ID_USUARIO"`, `"CC"`, `"PRIVILEGIO"`, `"NOMBRE_USUARIO"` con los valores recibidos.
   - 8.2 Crea una instancia de `Menunew` pasando el privilegio (`MENU = new Menunew(privilegios)`), la agrega al escritorio `MenuPrincipal.Escritorio`, la maximiza (`setMaximum(true)`) y la muestra (`show()`). Esta ventana `Menunew` reemplaza visualmente a `principal` como panel de navegación principal (aunque `principal` no se cierra explícitamente aquí).
   - 8.3 Hace visible la etiqueta `MenuPrincipal.USUARIOS_` y le asigna como texto el privilegio del usuario (p. ej. "Administrador").
   - 8.4 Actualiza el texto de `MenuPrincipal.NOMBRE_USUARIO` con formato HTML: `"<html> <b>C.C : </b>" + CC + "</b> <br><b>NOMBRE :  </b>" + NOMBRE_USUARIO_`.

**Datos / tablas de BD leídas o escritas:** Ninguna consulta SQL directa en este archivo; delega toda verificación de credenciales a `Loguin`.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- La variable estática `principal.MENU` guarda la última instancia de `Menunew` creada tras cualquier login exitoso (comportamiento tipo singleton informal).
- No hay control para impedir múltiples inicios de sesión simultáneos (abrir varios `Loguin` o iniciar sesión más de una vez seguiría apilando ventanas `Menunew` en el escritorio).

**Notas / peculiaridades:**
- Existen dos conjuntos de botones de rol duplicados en la interfaz (jButton14-17 sin funcionalidad y jButton10-13 con funcionalidad real) — probablemente resto de una iteración de diseño no limpiada.
- El botón "Acerca de" (`jButton2`) de esta pantalla NO abre ningún diálogo pese al nombre — la funcionalidad real de "Acerca de" vive en `Menu.java`/`AcercaDe.java`, activada desde el menú de navegación posterior al login, no desde aquí.
- Las credenciales de ejemplo comentadas en el código (`// loguin("3838890", "ADMINISTRADOR", "1", "DONALDO GANDARA CORREA");`) exponen cédulas/roles de prueba en el código fuente — relevante solo como nota de higiene de código, no funcionalidad.


### Menu.java — Panel de navegación por categorías (VERSIÓN LEGACY/INACTIVA)

**Propósito:** Es un panel de navegación por íconos y enlaces (Ventas, Herramientas, Inventario, Cuentas, Clientes, Caja, Administración, Usuarios) pensado para abrir cada módulo del sistema. **Su funcionalidad real está casi completamente deshabilitada**: casi todos los manejadores de clic de los enlaces están comentados, dejando la clase como código muerto salvo por el enlace "Acerca de".

**Cómo se invoca / navegación:** Es un `javax.swing.JInternalFrame`. Tiene 15 constructores sobrecargados, uno por cada posible pantalla "padre" que podría instanciarlo (`Ventas`, `Productos`, `VentasGuardadas`, `Clientes`, `Categorias`, `MovimientoExistencias`, `GestionInventario`, `Proveedor`, `EditarVentas`, `DetallarProductos`, `CerrarCaja`, `ImportarProductos`, `MovimientoCaja`, `CuentasPorCobrar`, `CuentasorporPagar`, `InventarioFacil`, `InformesProductos`, `InventarioGeneral`, `principal`), todos reciben los mismos 8 parámetros: el padre, `modal` (boolean, sin uso real porque es JInternalFrame no JDialog), `BD` (TestConnection), `usuario_` (String), `Escritorio` (JDesktopPane), `DATOS_OBLIGATORIOS` (Map), `Conexion` (Connection) y `USUARIOS_` (JLabel). **No se encontró ningún punto activo del código (en los archivos leídos de este grupo) que instancie esta clase** — su reemplazo funcional es `Menunew.java`.

**Componentes de interfaz:** Organizado en 8 paneles temáticos con un título (JLabel con ícono) y enlaces subrayados en HTML (JLabel con cursor de mano), cada uno con mouse listeners de hover (subrayado on/off, actualmente comentados):
- `jPanel1` "Ventas": enlaces "Vender" (jLabel2), "Ventas Guardadas" (jLabel3), "Editar Ventas" (jLabel4), "Informes" (jLabel26).
- `jPanel2` "Herramientas": "Importar Productos" (jLabel14), "Inventario Facil" (jLabel25), "Informes" (jLabel27).
- `jPanel3` "Inventario": "Movimiento de Existencias" (jLabel10), "Gestión de Inventario" (jLabel19), "Detallar Producto" (jLabel11), "Informes" (jLabel28), "Inventario General" (jLabel29).
- `jPanel4` "Cuentas": "Cuentas por Cobrar" (jLabel6), "Cuentas por Pagar" (jLabel21), "Informes" (jLabel22).
- `jPanel5` "Clientes": "Clientes" (jLabel13), "Informes" (jLabel24).
- `jPanel6` "Caja": "Movimientos de Caja" (jLabel16), "Cerrar Caja" (jLabel17), "Informes" (jLabel18).
- `jPanel7` "Administración": "Productos" (jLabel8), "Categorias" (jLabel9), "Proveedores" (jLabel31), "Unidades" (jLabel32), "Iva" (jLabel33).
- `jPanel8` "Usuarios": "Usuarios" (jLabel35), "Acerca de" (jLabel36), "Cerrar Secion" (jLabel37).

**Funcionalidades atómicas (numeradas):**
1. Al pasar el mouse sobre cualquier enlace, el código para subrayar/desubrayar el texto está **comentado** (sin efecto visual real de hover en la mayoría).
2. Al hacer clic en "Vender", "Ventas Guardadas", "Productos", "Clientes", "Categorias", "Movimiento de Existencias", "Gestión de Inventario", "Editar Ventas", "Detallar Producto", "Cerrar Caja", "Importar Productos", "Movimientos de Caja", "Cuentas por Cobrar", "Cuentas por Pagar", "Inventario Facil", "Informes de Productos" e "Inventario General": el código que abriría la ventana correspondiente en el escritorio MDI está **comentado por completo**; al pulsar clic, NO ocurre nada.
3. Al hacer clic en "Acerca de" (jLabel36) — **única acción funcional de esta clase**: se crea y muestra un diálogo `new AcercaDe(this, true)` (modal).
4. Al hacer clic en "Cerrar Secion" (jLabel37): el código que reabriría `principal` y ocultaría la barra de usuario está **comentado**; no ocurre nada.
5. Método público `cargarVentaGuardada(int sudid, String cc, String NombreCliente)`: todo su contenido está comentado, no hace nada.

**Datos / tablas de BD leídas o escritas:** Ninguna (clase sin lógica de negocio activa).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna activa.

**Notas / peculiaridades:**
- Esta clase parece ser una **versión anterior/abandonada** del menú de navegación, sustituida por `Menunew.java` (que organiza la navegación por botones grandes con un `JList` lateral en vez de enlaces de texto por categoría). Para la migración a Regenta, **se debe tomar `Menunew.java` como referencia funcional real**, no `Menu.java`.
- Mantiene 15 constructores sobrecargados casi idénticos (solo cambia el tipo del parámetro "padre"), lo cual es un patrón de duplicación de código evidente — probablemente generado copiando/pegando para cada pantalla que en su momento debía abrir este menú.
- Dado que no se instancia en ninguno de los archivos de este grupo, es posible que sea instanciada desde otras pantallas no incluidas en este análisis (p. ej. `Ventas.java`, `Productos.java`, etc.), aunque su utilidad real sería mínima por el código comentado.


### Menunew.java — Menú principal de navegación por módulos (activo)

**Propósito:** Es el panel de navegación REAL y activo tras iniciar sesión: muestra botones grandes por módulo (Cuentas, Sistema, Clientes, Vender, Inventario, Caja, Productos, Herramientas, Usuarios, Ventas) y, según el botón pulsado, llena una lista lateral (`MenuPrincipal.MENU`) con las opciones de ese módulo; al seleccionar una opción en la lista se abre la ventana (JInternalFrame) correspondiente dentro del escritorio MDI de `MenuPrincipal`.

**Cómo se invoca / navegación:** Es un `javax.swing.JInternalFrame` que implementa `InternalFrameListener`. Se instancia únicamente desde `principal.loguin(...)` tras un login exitoso: `MENU = new Menunew(privilegios)`, se agrega a `MenuPrincipal.Escritorio`, se maximiza y se muestra. Recibe un único parámetro: `privilegios` (String, ej. "ADMINISTRADOR" o "VENDEDOR"). Al construirse oculta su barra norte (`setNorthPane(null)`), fija fondo blanco, se añade como listener de sus propios eventos de ventana interna, y habilita/deshabilita botones de módulo según el privilegio recibido (ver Reglas de negocio).

**Componentes de interfaz:**
- Botones de módulo (todos JButton con ícono, texto HTML en negrita + descripción, efecto hover que cambia el ícono a una versión más grande): `botonCuentas`, `botonSistema`, `botonClientes`, `botonVender`, `botonInventario`, `botonCaja`, `botonProductos`, `botonHerramientas` (visualmente presente pero deshabilitado/sin acción real), `botonUsuarios` (visualmente presente pero sin acción real), `botonVentas`.
- `jButton11` ("Cerrar sesion"): cierra la sesión actual y regresa a la pantalla `principal`.
- Elementos decorativos: iconos de redes sociales (`twiter`, `face`, `jLabel2` "play"), etiqueta "Nuestras redes sociales".
- `MODELO` (`DefaultListModel`): modelo de datos que alimenta el `JList` estático `MenuPrincipal.MENU` (definido en `MenuPrincipal`, no en esta clase) con las opciones de cada submenú.

**Funcionalidades atómicas (numeradas):**
1. Al construirse con `privilegios == "ADMINISTRADOR"`: se habilitan los botones `botonCaja`, `botonClientes`, `botonCuentas`, `botonInventario`, `botonProductos`, `botonVender`, `botonSistema`, `botonVentas`; se deshabilitan `botonHerramientas` y `botonUsuarios`.
2. Al construirse con `privilegios == "VENDEDOR"`: se habilitan solo `botonCaja`, `botonVender`, `botonVentas`; se deshabilitan `botonClientes`, `botonCuentas`, `botonHerramientas`, `botonInventario`, `botonProductos`, `botonSistema`, `botonUsuarios`.
3. Para cualquier otro valor de `privilegios` (p. ej. "GERENTE", "EMPLEADO") **no se aplica ningún bloque de habilitación/deshabilitación**: los botones quedan con su estado por defecto definido en el diseño (ver "Notas/peculiaridades" — posible hueco funcional).
4. Al pasar el mouse sobre cualquier botón de módulo habilitado, su ícono cambia a la versión de 68px (hover) y se activa `setContentAreaFilled(true)`; al salir, regresa al ícono de 64px y `setContentAreaFilled(false)`.
5. Al pulsar `botonProductos`: se llama `abrirMenuList(1)`.
6. Al pulsar `botonCaja`: se llama `abrirMenuList(2)`.
7. Al pulsar `botonClientes`: se llama `abrirMenuList(3)`.
8. Al pulsar `botonCuentas`: se llama `abrirMenuList(4)`.
9. Al pulsar `botonInventario`: se llama `abrirMenuList(6)`.
10. Al pulsar `botonSistema`: se llama `abrirMenuList(7)`.
11. Al pulsar `botonVentas`: se llama `abrirMenuList(9)`.
12. Al pulsar `botonVender`: se llama `abrirMenuList(10)` — este caso NO llena la lista lateral de opciones (código comentado para `boton==5`, no se usa aquí), en su lugar fija el título general a "Vender" y muestra directamente la ventana de ventas (`Ventas`), haciendo visible los toggle buttons `BV1`,`BV2`,`BV3` de `MenuPrincipal` (pestañas de ventas múltiples) y seleccionando `BV1`.
13. `botonHerramientas` y `botonUsuarios`: sus `ActionPerformed` existen pero no hacen nada funcional (`botonHerramientasActionPerformed` tiene código comentado; `botonUsuariosActionPerformed` está vacío con un TODO).
14. Método `abrirMenuList(int boton)` — núcleo de la navegación:
    - 14.1 Limpia siempre el modelo de lista (`MODELO.clear()`).
    - 14.2 Según el número de botón, llena `MODELO` con los textos de las opciones del submenú correspondiente (ver tabla abajo), fija el título (`MenuPrincipal.TITULO`) con texto e ícono propios del módulo, y hace visibles `MenuPrincipal.MENU_PANEL` y `MenuPrincipal.MENU` (el panel lateral con la lista).
      - boton=1 "Productos": Productos, Categorias, Proveedores, Importar productos, Unidades, Informe de productos.
      - boton=2 "Caja": si privilegio ADMINISTRADOR → Caja, Movimientos de caja, Informes de caja; si VENDEDOR → Caja, Movimientos de caja (sin Informes de caja).
      - boton=3 "Clientes": Administrar clientes, Abonos y Saldos, Informes clientes.
      - boton=4 "Cuentas": Cuentas por cobrar, Cuentas por pagar, Abonos a Proveedores, Informes de cuentas.
      - boton=5 "Herramientas": bloque completamente comentado, sin efecto.
      - boton=6 "Inventario": Inventario general, Gestion inventario, Inventario facil, Verificar Compras, Verificar Compras de Productos, Movimiento de existencias, Informes de inventario.
      - boton=7 "Sistema": Configuracion (única opción).
      - boton=9 "Ventas": Detalle de Ventas, Ventas Anuladas, Informes de ventas.
      - boton=10 "Vender": no llena lista (ver punto 12).
    - 14.3 Asigna el modelo a `MenuPrincipal.MENU.setModel(MODELO)` y selecciona el índice 0 (`setSelectedIndex(0)`).
    - 14.4 Además de llenar la lista, para los botones 1 (Productos), 2 (Caja→CerrarCaja), 3 (Clientes), 4 (Cuentas→CuentasPorCobrar), 6 (Inventario→InventarioGeneral), 7 (Sistema→Configuracion), 9 (Ventas→VentasDiarias) y 10 (Vender→Ventas): se abre DIRECTAMENTE la primera ventana relevante del módulo (no solo se llena la lista, también se lanza el JInternalFrame por defecto), verificando primero con `MenuPrincipal.VerificarInstanciaFrame("<TituloVentana>")` si ya existe una instancia abierta:
      - Si NO existe: se crea la ventana, se agrega a `MenuPrincipal.Escritorio`, se maximiza y se muestra; para boton=1 además se oculta esta ventana `Menunew` (`this.setVisible(false)`).
      - Si YA existe: se llama a `MenuPrincipal.ActivarFrame("<TituloVentana>", "Menunew", "", "", "", "")` para traerla al frente en lugar de duplicarla.
15. Método `eliminar()`: elimina el componente en el índice 1 del escritorio (`MenuPrincipal.Escritorio.remove(1)`) — utilitario no ligado a ningún evento visible en este archivo.
16. Al pulsar `jButton11` ("Cerrar sesion"): se llama `MenuPrincipal.ActivarFrame("Principal", "", "", "", "", "")` (reactiva/muestra la ventana `principal` ya existente en el escritorio en lugar de crear una nueva), se oculta `MenuPrincipal.USUARIOS_` y se vacía el texto de `MenuPrincipal.NOMBRE_USUARIO`. **No se destruye la sesión del `Menunew` actual ni se limpian los datos de `MenuPrincipal.DATOS_OBLIGATORIOS`** (ver Notas).
17. Implementación de `InternalFrameListener`: solo `internalFrameClosing` tiene efecto real (`this.dispose()`); el resto de eventos (`Opened`, `Closed`, `Iconified`, `Deiconified`, `Activated`, `Deactivated`) están vacíos.

**Datos / tablas de BD leídas o escritas:** Ninguna consulta SQL directa en esta clase; delega la carga de datos a las ventanas de cada módulo que invoca (`Productos`, `Clientes`, `CuentasPorCobrar`, `InventarioGeneral`, `Configuracion`, `VentasDiarias`, `Ventas`, `CerrarCaja`).

**Reportes generados:** Ninguno directamente (delega a las ventanas hijas, ej. "Informes de caja", "Informe de productos", etc., que no forman parte de este grupo de archivos).

**Reglas de negocio / validaciones no obvias:**
- El control de acceso por rol en esta pantalla es muy simple: solo reconoce literalmente los strings `"ADMINISTRADOR"` y `"VENDEDOR"`; cualquier otro valor de privilegio (p. ej. "GERENTE", "EMPLEADO", que sí son opciones de login en `principal.java`) **no activa ningún bloque de habilitación de botones**, por lo que su comportamiento depende del estado por defecto de los botones definido en el Form Builder (probablemente todos deshabilitados salvo los que ya vienen `enabled` de fábrica). Este es un hueco funcional relevante a resolver/decidir explícitamente al migrar a Regenta.
- El submenú de Caja cambia dinámicamente su contenido según el privilegio (ADMINISTRADOR ve "Informes de caja", VENDEDOR no).
- "Cerrar sesión" no es un cierre de sesión completo a nivel de aplicación: la ventana `Menunew` anterior sigue existiendo en el escritorio (solo se oculta/reactiva `principal`), por lo que el mapa `DATOS_OBLIGATORIOS` (que contiene CC, ID_USUARIO, PRIVILEGIO, NOMBRE_USUARIO del usuario autenticado) permanece en memoria con los datos del usuario anterior hasta el siguiente login exitoso, que lo sobrescribe.

**Notas / peculiaridades:**
- Existen variables estáticas públicas duplicadas con muy similar propósito a las de `Menu.java` (ej. `VENTANA_VENTAS`, `CUENTAS_POR_COBRAR`, `INVENTARIO_GENERAL`, etc.) — nuevamente, se trata de instancias "singleton informal" guardadas como estáticas públicas, accesibles desde cualquier otra clase.
- El comentario interno `//invoca : el frame que llama a activar un frame específico` (ver `MenuPrincipal.ActivarFrame`) documenta el patrón de navegación: en vez de cerrar y reabrir ventanas, el sistema reutiliza instancias de JInternalFrame ya creadas y solo las "activa"/reconfigura.
- El botón "Vender" (`abrirMenuList(10)`) es un caso especial que NO sigue el patrón de lista lateral + ventana, sino que abre directamente la ventana de ventas y activa un sistema de pestañas (`BV1`/`BV2`/`BV3`) para manejar hasta 3 ventas simultáneas (ver `MenuPrincipal.java`).


### MenuPrincipal.java — Ventana principal MDI (JFrame raíz, Main-Class del .jar)

**Propósito:** Es la ventana raíz de toda la aplicación (`extends javax.swing.JFrame`), contiene el escritorio MDI (`Escritorio`, un `JDesktopPane`) donde se insertan todas las demás pantallas como `JInternalFrame`, la barra de estado inferior (usuario conectado, nombre, info de conexión), el panel lateral de navegación (`MENU_PANEL`/`MENU`), las pestañas de ventas múltiples (`BV1`,`BV2`,`BV3`) y gestiona la conexión a la base de datos al arrancar. Contiene el método `main()` de la aplicación.

**Cómo se invoca / navegación:** Se instancia una única vez desde `main()`: `new MenuPrincipal().setVisible(true)`. En el constructor:
1. Llama a `initComponents()`.
2. Llama a `TratarDeConectarBD()` (intenta conectar a la BD y, según el resultado, abre `principal` o `BdConection`).
3. Maximiza la ventana (`setExtendedState(MAXIMIZED_BOTH)`).
4. Da foco al escritorio.
5. Oculta la barra de menú superior (`jMenuBar1.setVisible(false)`) — la barra de menú con ítems "Configuracion", "Administracion", "Reportes" (Volquetas/Maquinas), "Ayuda" está definida pero deliberadamente oculta y sin uso real (ver Notas).
6. Oculta la etiqueta de usuario (`USUARIOS_.setVisible(false)`).
7. Oculta el panel lateral de menú (`MENU_PANEL.setVisible(false)`).
8. Oculta los tres toggle buttons de pestañas de venta `BV1`, `BV2`, `BV3`, y preselecciona `BV1`.

**Componentes de interfaz:**
- `TITULO` (JLabel): título dinámico del módulo activo (ej. "Productos", "Caja", "Sistema"), actualizado desde `Menunew.abrirMenuList`.
- `jLabel2`: texto de marketing/branding ("Sara Punto de venta - Sistema POS...") — al hacer clic, solo imprime en consola la lista de títulos de ventanas internas abiertas (debug, sin efecto visible).
- `BV1`, `BV2`, `BV3` (JToggleButton "1","2","3"): pestañas para manejar hasta 3 ventanas de venta simultáneas (`Ventas`, `Ventas1`, `Ventas2`).
- `MENU_PANEL` → `jScrollPane1` → `MENU` (JList): lista lateral de opciones del submódulo activo, llenada por `Menunew`.
- `Escritorio` (JDesktopPane): el lienzo MDI central donde se insertan todas las ventanas internas del sistema.
- Panel inferior (`jPanel2`): `CONEXION` (JLabel, muestra usuario del sistema operativo + string de la conexión JDBC), `NOMBRE_USUARIO` (JLabel, nombre del usuario autenticado), `USUARIOS_` (JLabel, rol del usuario autenticado).
- `jMenuBar1`: barra de menú completa pero oculta, con menús "Configuracion" (Productos, Ventas Ctrl+V, Clientes, Administrar Usuarios Ctrl+D, Salir Ctrl+S), "Administracion" (ítems de un dominio ajeno: "Gestionar Viajes Volquetas" Ctrl+A, "Gestionar Horas de Maquinas" Ctrl+C, "Daños y Reparaciones", "Acpm"), "Reportes" (submenús "Volquetas" y "Maquinas", con ítems de viajes/horas/pagos/daños/ACPM/ganancia mensual), "Ayuda" (ítems "Acerca De" Ctrl+A y "Ayuda" Ctrl+Y).
- `jButton1`: botón de debug oculto/sin uso visual claro, imprime en consola los títulos de todos los JInternalFrame abiertos y cuál está seleccionado.

**Funcionalidades atómicas (numeradas):**
1. `TratarDeConectarBD()` (invocado al arrancar y también desde `BdConection` tras guardar una configuración nueva):
   - 1.1 Llama a `CargarInfoConexion()` (lee `Configuracion.properties` del directorio de trabajo: `HostName`, `baseDatos`, `puerto`, `usuario`, `clave`).
   - 1.2 Intenta `Conexion = BD.getConexion(HostName, baseDatos, puerto, usuario, clave)`.
   - 1.3 Si `Conexion` es null (fallo de conexión): abre modal `BdConection` (pantalla para editar los datos de conexión, ver más abajo).
   - 1.4 Si la conexión es exitosa: muestra en `CONEXION` el usuario del sistema operativo (`System.getProperty("user.name")`) y el string de la conexión JDBC; llama a `Configuracion()` (carga parámetros globales de negocio en el mapa `DATOS_OBLIGATORIOS`); crea e inserta la ventana `principal` maximizada en el escritorio.
   - 1.5 Siempre (haya o no conexión): calcula y guarda en `DATOS_OBLIGATORIOS` la clave `"DIRECTORIO_ACTUAL_USUARIO"` = `<directorio de trabajo>/REPORTES/`, y llama a `crearCarpetaTemporal(dir)`.
2. `crearCarpetaTemporal(String direccion)`: crea físicamente (si no existe) la carpeta `<direccion>//Temporal` en disco (`File.mkdirs()`) y guarda su ruta en `DATOS_OBLIGATORIOS["DIRECTORIO_EXPORTAR"]` — se usa como carpeta de trabajo temporal para exportaciones (Apache POI/iText) y archivos intermedios de reportes.
3. `Configuracion()`: ejecuta `SELECT IMPRESORA_TERMICA, IMPRESORA_TINTA, LISTA_PRECIOS, PUNTOS, REUTILIZAR_FACTURAS_ANULADAS, REFERENCIA_EXTENDIDA_NORMAL, RECIBO_CAJA_EXTENDIDO_LIMITADO, REGIMEN_COMUN_SIMPLIFICADO FROM configuracion` y guarda cada valor en `DATOS_OBLIGATORIOS` con las mismas claves (más una duplicada `"FACTURA_RENUENTE"` que, por error, se llena con el valor de `PUNTOS` — ver Notas); si falla, muestra `JOptionPane` de error "Error al Consultar"+e.
4. `DIAN()` (definido pero NO invocado desde el constructor actual — está comentado en el constructor): ejecuta `SELECT NAUTORIZACION, FECHA, INICIO, FIN FROM resolucion_factura WHERE ESTADO='ACTIVO'` y guarda los 4 valores en `DATOS_OBLIGATORIOS` (claves `resolucion`, `fecha`, `inicio`, `fin`) — datos de la resolución de facturación DIAN vigente.
5. Al pulsar `BV1` (pestaña "1"): selecciona visualmente `BV1` (deselecciona `BV2`/`BV3`); si no existe ya una ventana `Ventas` abierta, crea `new Ventas()`, la agrega al escritorio, la maximiza, limpia el buscador y aplica el filtro de tabla, la muestra y da foco al buscador; si ya existe, llama `ActivarFrame("Ventas", "", "", "", "", "")`.
6. Al pulsar `BV2` (pestaña "2"): análogo al anterior pero con la clase `Ventas1` (JInternalFrame título "Ventas1").
7. Al pulsar `BV3` (pestaña "3"): análogo pero con `Ventas2` (título "Ventas2").
8. Al hacer clic sobre `MENU` (lista lateral) — evento `mousePressed`: llama a `abrirFormSeleccion()` (ver detalle abajo). El evento `mouseClicked` del mismo componente está vacío (toda la lógica va en `mousePressed`).
9. `abrirFormSeleccion()`: lee el valor seleccionado de `MENU` y, según el texto exacto (comparación case-insensitive, con 3 espacios iniciales porque así se insertaron los textos en `Menunew`), abre la ventana correspondiente, siguiendo siempre el mismo patrón "si no existe instancia → crear, agregar al escritorio, maximizar, mostrar (con foco en el primer campo relevante cuando aplica); si ya existe → `ActivarFrame(...)`". Las correspondencias texto→ventana son:
   - "Productos" → `Productos` (y además llama `listarCategoriasImpuestos()` y `listarUnidades()` antes de mostrarla; oculta `MenuPrincipal` al mostrarla con `this.setVisible(false)` — ver Notas, posible bug).
   - "Categorias" → `Categorias`.
   - "Proveedores" → `Proveedor`.
   - "Unidades" → `Unidades`.
   - "Informe de productos" → `InformesProductos`.
   - "Movimientos de caja" → `MovimientoCaja` (da foco al campo `total`).
   - "Caja" → `CerrarCaja`.
   - "Informes de caja" → `InformesCaja`.
   - "Administrar clientes" → `Clientes` (título interno "Clientes", con foco en `BUSCAR`).
   - "Abonos y Saldos" → `Abonos` (título "Abonos").
   - "Informes clientes" → `InformesClientes`.
   - "Cuentas por cobrar" → `CuentasPorCobrar` (foco en `BUSCAR`).
   - "Cuentas por pagar" → `CuentasorporPagar` (título interno "Cuentas por pagar").
   - "Abonos a Proveedores" → `AbonosCuentas`.
   - "Informes de Cuentas" → `InformeCuentas`.
   - "Importar productos" → `ImportarProductos`.
   - "Inventario facil" → `InventarioFacil`.
   - "Inventario general" → `InventarioGeneral`.
   - "Gestion inventario" → `GestionInventario` (doble `BUSCAR.grabFocus()`).
   - "Verificar Compras" → `VerificarCompras`.
   - "Verificar Compras de Productos" → `VerificarProductosCompras`.
   - "Movimiento de existencias" → `MovimientoExistencias`.
   - "Detallar producto" → `DetallarProductos`.
   - "Informes de inventario" → bloque vacío, sin efecto (opción listada pero no implementada).
   - "Editar ventas" → `EditarVentas` (foco en `BUSCAR`).
   - "Ventas guardadas" → `VentasGuardadas` (foco en `BUSCAR`).
   - "Detalle de Ventas" → `VentasDiarias` (foco en `BUSCAR`).
   - "Ventas Anuladas" → `VentasAnuladas` (foco en `BUSCAR`).
   - "Informes de ventas" → bloque vacío, sin efecto.
10. `privilegios(String permiso)`: método de control de acceso basado en los valores `"Administrabor"` (sic, con error ortográfico) y `"Digitador"` que habilita/deshabilita ítems de la barra de menú oculta (`jMenuItem10`, `jMenuItem2`, `jMenuItem3`, `jMenuItem4`, `jMenu6`, `jMenu3`, `jMenu1`, `jMenuItem7`). **No se encontró ninguna llamada activa a este método** en el flujo actual (la barra de menú está oculta) — código muerto o vestigio de otra versión del login.
11. Ítem de menú "Salir" (`jMenuItem13`, Ctrl+S, dentro de la barra de menú oculta): si se invocara, mostraría `JOptionPane.showConfirmDialog(this,"¿ Desea Cerrar La Aplicacion ?","Confirmar",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)`; si la respuesta es "Sí" (valor 0), ejecuta `System.exit(0)`.
12. Ítem de menú "Ayuda" (`jMenuItem1`, Ctrl+Y) y el menú "Ayuda" (`jMenu9`) al hacer clic: ambos abren el archivo `<directorio de trabajo>/manual.pdf` con la aplicación predeterminada del sistema operativo (`Desktop.getDesktop().open(path)`).
13. Tecla `F10` presionada sobre el escritorio (`Escritorio` tiene foco, `keyPressed`): recorre todos los `JInternalFrame` abiertos y obtiene sus propiedades de estado (visible, cerrable, redimensionable, iconificable, icon, maximizable, seleccionado) — **solo con fines de depuración, sin efecto visible para el usuario** (no se imprime nada ni se usa el resultado).
14. `ActivarFrame(String TitleFrame, String invoca, String parametro1, String parametro2, String parametro3, String parametro4)` (método estático, núcleo del patrón de reutilización de ventanas):
    - 14.1 Recorre TODAS las ventanas internas abiertas del escritorio y a cada una la deselecciona y oculta (`setSelected(false)`, `setVisible(false)`), capturando cualquier excepción silenciosamente.
    - 14.2 Busca la ventana cuyo título coincide exactamente con `TitleFrame`; al encontrarla, la selecciona, la hace visible y la maximiza.
    - 14.3 Ejecuta lógica específica por tipo de ventana activada (gran bloque `if/else if` por nombre de título), por ejemplo:
      - Si `TitleFrame=="Menunew"`: oculta el panel lateral `MENU_PANEL`, limpia título e ícono, oculta `BV1/BV2/BV3` y preselecciona `BV1`.
      - Si es `"MovimientoCaja"`, `"Productos"`, `"CerrarCaja"`, `"InformesCaja"`, `"Editar Ventas"`, `"Ventas Guardadas"`, `"CuentasPorCobrar"`, `"Inventario facil"`: llama al método `limpiar()` de la ventana activa (reinicia sus campos).
      - Si es `"Clientes"`: según quién invoque (`invoca` = "Ventas"/"Ventas1"/"Ventas2"/"Ventas3"/"Menunew"/"Abonos"), muestra u oculta el botón `ESCOGER` de la ventana `Clientes` y fija su variable `aux`, para distinguir si el módulo Clientes se abrió para "elegir un cliente para una venta/abono" o para administración normal.
      - Si es `"Abonos"` e `invoca=="Clientes"`: llama `EscogerCliente(parametro1, parametro2, parametro3, parametro4, "")` con los datos del cliente elegido.
      - Si es `"Ventas"`/`"Ventas1"`/`"Ventas2"`/`"Ventas3"`: según quien invoque puede: `EscogerCliente(...)` (venir de Clientes), `limpiar()`+`cargarProductos()` (venir de Menunew), `cargarVentaGuardada(...)` (venir de VentasGuardadas), o re-cargar una venta existente para edición (`LISTARVENTA(parametro1)`, recalcular `TOTAL_PAGADO_VENTA_EDITAR`, fijar número de factura, `regresarPuntosAcumulados()`) cuando viene de "Editar Ventas"; siempre al final limpia el buscador y reaplica el filtro de la tabla con `RowFilter.regexFilter`.
      - Si es `"Gestion de invenario"`, `"Verificar Compras"`, `"Verificar Compras de Productos"`: si `invoca=="Menunew"` llama `limpiar(false)`.
      - Si es `"Cuentas por pagar"`: si `invoca=="Proveedor"` llama `EscogerProveedor(parametro2,parametro3)`; siempre llama `limpiar()` al final.
      - Si es `"Proveedor"`: según quién invoque (Gestion de invenario / Verificar Compras / Cuentas por pagar / Menunew / AbonosCuentas), muestra/oculta su botón `ESCOGER` y fija la variable `au` de esa ventana.
    - 14.4 Captura cualquier excepción del bloque y la imprime en consola; al coincidir el título procesado, sale del bucle (`break`).
15. `VerificarInstanciaFrame(String TitleFrame)` (método estático): recorre todas las ventanas internas abiertas del escritorio y devuelve `true` si alguna tiene ese título exacto, `false` en caso contrario — es la guarda usada en todo el sistema antes de crear una ventana nueva, para evitar duplicados.
16. `main(String[] args)`: intenta aplicar el Look&Feel del sistema operativo (`UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())`; hay otras opciones de skin "JTattoo" comentadas y descartadas) y luego crea y muestra `MenuPrincipal`.

**Datos / tablas de BD leídas o escritas:**
- `configuracion` (SELECT): columnas `IMPRESORA_TERMICA`, `IMPRESORA_TINTA`, `LISTA_PRECIOS`, `PUNTOS`, `REUTILIZAR_FACTURAS_ANULADAS`, `REFERENCIA_EXTENDIDA_NORMAL`, `RECIBO_CAJA_EXTENDIDO_LIMITADO`, `REGIMEN_COMUN_SIMPLIFICADO`. Se ejecuta siempre que la conexión a BD es exitosa.
- `resolucion_factura` (SELECT, en método `DIAN()` no invocado actualmente): columnas `NAUTORIZACION`, `FECHA`, `INICIO`, `FIN`, filtrando `WHERE ESTADO='ACTIVO'`.

**Reportes generados:** Ninguno directamente; solo abre `manual.pdf` (no es un reporte JasperReports, es documentación de usuario) mediante el visor PDF predeterminado del sistema operativo.

**Reglas de negocio / validaciones no obvias:**
- El patrón "ventana única por título" (`VerificarInstanciaFrame` + `ActivarFrame`) es la regla de negocio transversal más importante del sistema de navegación: nunca se deben crear dos `JInternalFrame` con el mismo título simultáneamente; en vez de eso se reactiva/reconfigura la existente. Al migrar a Regenta, cada "pantalla" debería comportarse de forma singleton por sesión de usuario.
- `ActivarFrame` propaga "contexto de invocación" (de qué pantalla se viene y hasta 4 parámetros de texto) a la ventana destino para que ésta decida cómo comportarse (p. ej. mostrar u ocultar un botón "Escoger", o pre-cargar un cliente/proveedor elegido) — patrón equivalente a pasar parámetros de navegación entre pantallas.
- Bug/inconsistencia potencial en `Configuracion()`: la clave `"FACTURA_RENUENTE"` se llena erróneamente con el valor de la columna `PUNTOS` (índice 4) en lugar de `REUTILIZAR_FACTURAS_ANULADAS` (índice 5), posiblemente por error de copia/pega al escribir el código (ver la misma duplicación en `Configuracion.java` y en el método idéntico `RecargarConfiguracion`).
- El bloqueo de edición de ventas abiertas vs. "Editar Ventas" depende de recalcular `VALOR_BRUTO + TOTALIVA` para `TOTAL_PAGADO_VENTA_EDITAR`, lo cual sugiere una regla de negocio de que al editar una venta histórica se recalcula el total ya pagado en función del valor bruto y el IVA guardados.

**Notas / peculiaridades:**
- La barra de menú (`jMenuBar1`) contiene un dominio de negocio completamente ajeno a un POS: "Gestionar Viajes Volquetas", "Gestionar Horas de Maquinas", "Ganancia Mensual por Volquetas/Maquinas", "Acpm", etc. Esto sugiere que este formulario (`MenuPrincipal`) se originó copiando el código base de otra aplicación distinta (gestión de volquetas/maquinaria) y fue reconvertido a un POS, dejando restos de menús sin limpiar; están ocultos (`jMenuBar1.setVisible(false)`) y sin acción real en casi todos sus ítems.
- Posible bug de UX: al abrir "Productos" desde `abrirFormSeleccion()` se oculta la ventana `MenuPrincipal` completa (`this.setVisible(false)`) en vez de solo ocultar el panel lateral — comportamiento inconsistente respecto a cómo se abren las demás ventanas del mismo método, que no ocultan `MenuPrincipal`.
- El método `privilegios(String permiso)` usa el valor `"Administrabor"` (error de tipeo, debería ser "Administrador") como comparación — dado que en el resto del sistema el rol se escribe "Administrador"/"ADMINISTRADOR", este método nunca se activaría correctamente aunque se llamara, reforzando que es código muerto.
- Fuerte acoplamiento mediante campos `public static`: `Escritorio`, `MENU`, `MENU_PANEL`, `TITULO`, `USUARIOS_`, `NOMBRE_USUARIO`, `BV1/BV2/BV3`, `DATOS_OBLIGATORIOS`, `BD`, `Conexion`, `VENTAS`, `VENTAS1`, `VENTAS2`, `VENTAS3` son accedidos directamente desde `Menunew`, `principal`, `Configuracion`, `BdConection` y, previsiblemente, desde todas las demás pantallas del sistema — es el patrón arquitectónico central de toda la aplicación (variables globales estáticas en lugar de inyección de dependencias), crítico de entender para la migración.


### Configuracion.java — Configuración del sistema (datos del negocio y parámetros de facturación)

**Propósito:** Pantalla para editar los datos generales del negocio (razón social, NIT, dirección, teléfono, email, logo) que aparecen en facturas/recibos, y para activar/desactivar un conjunto de parámetros de comportamiento de facturación (reutilizar numeración anulada, formato de referencia, formato de recibo de caja, régimen tributario).

**Cómo se invoca / navegación:** Es un `javax.swing.JInternalFrame`. Se instancia desde `Menunew.abrirMenuList(7)` (botón "Sistema" → opción "Configuracion" de la lista lateral), con patrón estándar de no-duplicado: `if(!MenuPrincipal.VerificarInstanciaFrame("Configuracion")) { VENTANA_CONFIGURACION = new Configuracion("Menunew"); ...}`. El constructor recibe un único parámetro `String aux` (el nombre de la pantalla que la invoca, usado solo al cerrar para decidir a dónde volver). Al construirse: oculta su barra norte, y llama automáticamente a `cargarConfiguracion()` y `cargarDatosNegocio()` para precargar todos los campos desde la base de datos.

**Componentes de interfaz:** Organizada en un `JTabbedPane` de 3 pestañas:
- Pestaña "TIKETES Y FACTURAS" (`jPanel1`):
  - `RAZON_SOCIAL`, `REPRESENTANTE_LEGAL`, `NIT`, `DIRECCION`, `TELEFONO`, `CEL`, `EMAIL` (JTextField): datos del negocio.
  - `foto_alumno` (JLabel con borde): previsualización del logo del negocio (nombre de variable heredado de otra plantilla — ver Notas).
  - `jButton1` ("Logo"): abre selector de archivo para elegir una imagen de logo (JPG/GIF/PNG).
  - `jButton2` ("Guardar"): valida y guarda los datos del negocio.
  - `factura_renuente` (JCheckBox: "Permitir reutilizar la numeración de facturas anuladas."): activa/desactiva reutilización de números de factura anulados.
- Pestaña "OTRAS CONFIGURACIONES" (`jPanel2`):
  - `REFERENCIA_EXTENDIDA_NORMAL` (JCheckBox, texto dinámico): alterna entre "Referencia Extendida 1-100800" y "Referencia Normal 100800".
  - `RECIBO_CAJA_EXTENDIDO_LIMITADO` (JCheckBox, texto dinámico): alterna entre "Recibo de caja extendido." y "Recibo de caja limitado.".
  - `REGIMEN_COMUN_SIMPLIFICADO` (JCheckBox, texto dinámico, inicialmente "jCheckBox2" sin configurar en diseño): alterna entre "Regimen simplificado." y "Regimen comun." (nótese inconsistencia de texto "Recibo comun." al desmarcar vs. "Regimen comun." usado en otro punto del código — ver Notas).
- Pestaña "RESOLUCIÓN DE FACTURACIÓN" (`jPanel4`): panel vacío, sin componentes reales (solo define tamaño) — funcionalidad no implementada en esta versión.
- `jLabel1` ("Configuración"): título de la pantalla.
- `jButton3` ("Cerrar"): cierra la pantalla y regresa a `Menunew` si fue invocada desde ahí.
- `jFileChooser1`: selector de archivos reutilizado para elegir el logo.

**Funcionalidades atómicas (numeradas):**
1. Al abrir la pantalla, se ejecuta `cargarConfiguracion()`: SELECT de `REUTILIZAR_FACTURAS_ANULADAS, REFERENCIA_EXTENDIDA_NORMAL, RECIBO_CAJA_EXTENDIDO_LIMITADO, REGIMEN_COMUN_SIMPLIFICADO` desde `CONFIGURACION`, y según cada valor sea `"1"` o no, marca/desmarca el checkbox correspondiente y ajusta su texto visible.
2. Al abrir la pantalla, se ejecuta `cargarDatosNegocio()`: SELECT de `upper(RAZON_SOCIAL), upper(REPRESENTANTE_LEGAL), NIT, upper(DIRECCION_NEGOCIO), TELEFONO, CEL, upper(EMAIL), URL_LOGO` desde `datos_negocio`, rellena los 7 campos de texto, guarda el nombre del archivo de logo en `name_foto`, carga y redimensiona (261x127 px) la imagen del logo desde `<DIRECTORIO_ACTUAL_USUARIO>/<name_foto>` y la muestra en `foto_alumno`, y da el foco a `RAZON_SOCIAL`.
3. Al pulsar `factura_renuente` (checkbox): si queda marcado, ejecuta `modificar_referencia("REUTILIZAR_FACTURAS_ANULADAS","1")`; si se desmarca, `modificar_referencia("REUTILIZAR_FACTURAS_ANULADAS","0")`.
4. Al pulsar `REFERENCIA_EXTENDIDA_NORMAL`: si queda marcado, llama `modificar_referencia("REFERENCIA_EXTENDIDA_NORMAL","1")` y cambia su texto a "Referencia Extendida 1-100800 "; si se desmarca, `modificar_referencia("REFERENCIA_EXTENDIDA_NORMAL","0")` y cambia el texto a "Referencia Normal 100800 "; en ambos casos vuelve a llamar `Configuracion()` para refrescar el caché de `DATOS_OBLIGATORIOS`.
5. Al pulsar `RECIBO_CAJA_EXTENDIDO_LIMITADO`: análogo, actualiza `RECIBO_CAJA_EXTENDIDO_LIMITADO` en BD a "1"/"0" y cambia el texto entre "Recibo de caja extendido."/"Recibo de caja limitado."; también refresca `Configuracion()`.
6. Al pulsar `REGIMEN_COMUN_SIMPLIFICADO`: análogo, actualiza `REGIMEN_COMUN_SIMPLIFICADO` a "1"/"0"; al marcar cambia el texto a "Regimen simplificado.", al desmarcar a "Regimen comun." (nótese: en la carga inicial `cargarConfiguracion()` el texto al desmarcar era "Recibo comun." en vez de "Regimen comun." — inconsistencia textual, ver Notas); también refresca `Configuracion()`.
7. `modificar_referencia(String campo, String valor)`: ejecuta `UPDATE configuracion SET <campo> = <valor>` (el valor se concatena SIN comillas — funciona porque `configuracion` define estas columnas como `varchar` pero el valor pasado es numérico sin comillas, lo cual MySQL tolera por coerción implícita) y luego llama `RecargarConfiguracion()` (en la clase `Configuracion`, re-lee `IMPRESORA_TERMICA, IMPRESORA_TINTA, LISTA_PRECIOS, PUNTOS, REUTILIZAR_FACTURAS_ANULADAS` y actualiza el mapa `MenuPrincipal.DATOS_OBLIGATORIOS`, reproduciendo el mismo bug de `"FACTURA_RENUENTE"` = valor de `PUNTOS` descrito en `MenuPrincipal.java`).
8. Al pulsar `jButton1` ("Logo"): limpia el estado de foto cargada, configura un filtro de extensión (`*.JPG,*.GIF,*.PNG`), abre un `JFileChooser` modal; si el usuario aprueba una selección, guarda la ruta del archivo elegido, redimensiona y muestra la previsualización (261x127 px) en `foto_alumno`, y marca `foto_cargada = true`; si ocurre una excepción, deja `foto_cargada = false`.
9. Al pulsar `jButton2` ("Guardar"), se valida en este orden estricto (deteniéndose en el primer campo vacío encontrado) y, si falla, muestra `JOptionPane.showMessageDialog(null, "<mensaje>", "Sara POS 1.0", JOptionPane.ERROR_MESSAGE)` y devuelve el foco al campo correspondiente:
   - 9.1 `RAZON_SOCIAL` vacío → "Falta razon social del negocio."
   - 9.2 `REPRESENTANTE_LEGAL` vacío → "Falta representante legal del negocio."
   - 9.3 `NIT` vacío → "Falta nit del negocio."
   - 9.4 `DIRECCION` vacío → "Falta direccion del negocio."
   - 9.5 `TELEFONO` vacío → "Falta telefono del negocio."
   - 9.6 `CEL` vacío → "Falta celular del negocio."
   - 9.7 `EMAIL` vacío → "Falta email del negocio."
   - 9.8 Si todos los campos tienen contenido, llama `actualizarDatosNegocio()`.
10. `actualizarDatosNegocio()`:
    - 10.1 Llama `guardarFoto()`: si `foto_cargada` es `true`, genera un nuevo nombre de archivo único `"LOGO"+<número aleatorio>+<extensión original>` (usa `OTRASCLASES.randoNumero().randoNumero()`), copia byte a byte el archivo origen elegido hacia `<DIRECTORIO_ACTUAL_USUARIO>/<nuevo_nombre>`, y actualiza `dir_imagen`/`name_foto` apuntando al nuevo archivo. Si no se cargó foto nueva, no hace nada (conserva el `name_foto` existente cargado al abrir la pantalla).
    - 10.2 Ejecuta `UPDATE datos_negocio SET RAZON_SOCIAL=..., REPRESENTANTE_LEGAL=..., NIT=..., DIRECCION_NEGOCIO=..., TELEFONO=..., CEL=..., EMAIL=..., URL_LOGO=<name_foto>` (concatenación directa de texto, sin cláusula WHERE — actualiza la única fila existente de la tabla `datos_negocio`, que se asume siempre tiene exactamente un registro).
    - 10.3 Muestra `JOptionPane.showMessageDialog(this,"Datos Modificados Correctamente.","Sara POS 1.0",JOptionPane.INFORMATION_MESSAGE)`.
11. Al pulsar `jButton3` ("Cerrar"): si `aux.equals("Menunew")` (la pantalla fue abierta desde el menú principal), llama `MenuPrincipal.ActivarFrame("Menunew","","","","","")` para regresar al menú principal; si `aux` tiene otro valor, no ocurre nada (no se cierra realmente la ventana en ese caso, posible bug).
12. Al pulsar Enter dentro de cualquiera de los 7 campos de texto (listeners `KeyPressed`/`KeyTyped` según el campo — inconsistencia menor de implementación entre campos, ver Notas): se transfiere el foco al siguiente componente (`transferFocus()`).
13. Al ganar el foco cualquiera de los 7 campos de texto, su fondo cambia a `RGB(223,223,238)` y se selecciona todo su texto; al perder el foco, el fondo vuelve a blanco `RGB(255,255,255)`.
14. `redimencionarImagen(String dir_img, int x, int y)`: función utilitaria que carga una imagen desde disco y la escala suavemente (`Image.SCALE_SMOOTH`) al tamaño indicado, devolviendo un `ImageIcon` listo para mostrar — usada tanto al cargar la configuración existente como al elegir una nueva foto.

**Datos / tablas de BD leídas o escritas:**
- `datos_negocio` (SELECT al abrir, UPDATE al guardar): columnas `RAZON_SOCIAL`, `REPRESENTANTE_LEGAL`, `NIT`, `DIRECCION_NEGOCIO`, `TELEFONO`, `CEL`, `EMAIL`, `URL_LOGO`. El UPDATE no tiene WHERE (afecta la única fila de la tabla).
- `CONFIGURACION` / `configuracion` (SELECT al abrir vía `cargarConfiguracion()`, SELECT adicional vía `Configuracion()`/`RecargarConfiguracion()`, UPDATE vía `modificar_referencia`): columnas relevantes en este archivo: `REUTILIZAR_FACTURAS_ANULADAS`, `REFERENCIA_EXTENDIDA_NORMAL`, `RECIBO_CAJA_EXTENDIDO_LIMITADO`, `REGIMEN_COMUN_SIMPLIFICADO`, además de `IMPRESORA_TERMICA`, `IMPRESORA_TINTA`, `LISTA_PRECIOS`, `PUNTOS` (leídas pero no editadas desde esta pantalla — probablemente editadas desde otra pantalla de configuración de impresoras no incluida en este grupo).

**Reportes generados:** Ninguno (esta pantalla no genera JasperReports; solo persiste configuración usada después por las pantallas de facturación/caja).

**Reglas de negocio / validaciones no obvias:**
- Todos los 7 campos de datos del negocio son obligatorios para guardar (validación secuencial, se detiene en el primer vacío).
- La tabla `datos_negocio` se asume de una sola fila (no hay selección por ID al leer ni al escribir) — el negocio modelado por Sara POS es de una sola sucursal/razón social.
- Los parámetros `REFERENCIA_EXTENDIDA_NORMAL`, `RECIBO_CAJA_EXTENDIDO_LIMITADO`, `REGIMEN_COMUN_SIMPLIFICADO` y `REUTILIZAR_FACTURAS_ANULADAS` se guardan como strings `"1"`/`"0"` en columnas `varchar`, no como booleanos nativos de MySQL.
- Al cambiar cualquiera de los checkboxes de "OTRAS CONFIGURACIONES", el cambio se persiste INMEDIATAMENTE en BD (no requiere pulsar "Guardar"), a diferencia de los datos del negocio de la primera pestaña que sí requieren el botón "Guardar" explícito.
- El logo se copia siempre a un nuevo nombre aleatorio al reemplazarlo (nunca sobrescribe el archivo anterior en disco), lo que puede generar archivos de logo huérfanos acumulados en `DIRECTORIO_ACTUAL_USUARIO` con el tiempo.

**Notas / peculiaridades:**
- Nombre de variable `foto_alumno` para el logo del negocio es un resto evidente de una plantilla de software educativo/de estudiantes reutilizada para este POS (no tiene relación con "alumnos" en un sistema de ventas).
- Inconsistencia de texto detectada entre `cargarConfiguracion()` (al desmarcar `REGIMEN_COMUN_SIMPLIFICADO` pone texto "Recibo comun.") y `REGIMEN_COMUN_SIMPLIFICADOActionPerformed` (al desmarcar pone texto "Regimen comun.") — mismo campo, dos textos ligeramente distintos según si se acaba de cargar la pantalla o si el usuario interactúa con el checkbox.
- El checkbox `REGIMEN_COMUN_SIMPLIFICADO` queda con el texto de diseño "jCheckBox2" visible brevemente si por algún error la consulta `cargarConfiguracion()` fallara (no se setea ningún texto por defecto en el diseño salvo ese placeholder).
- El botón "Cerrar" (`jButton3`) solo sabe volver a "Menunew"; si la pantalla fuera invocada alguna vez con otro valor de `aux`, el botón no haría nada — actualmente la única invocación real pasa `"Menunew"`.
- Reutiliza el mismo patrón de "refrescar caché en `DATOS_OBLIGATORIOS`" que `MenuPrincipal.Configuracion()`, incluyendo el mismo bug de la clave `"FACTURA_RENUENTE"` recibiendo el valor de `PUNTOS` en vez de `REUTILIZAR_FACTURAS_ANULADAS`.
- La pestaña "RESOLUCIÓN DE FACTURACIÓN" está vacía en el diseño (sin componentes), pese a que la tabla `resolucion_factura` (columnas `NAUTORIZACION`, `FECHA`, `INICIO`, `FIN`, `ESTADO`, `FECHA_VIGENCIA`) existe en la base de datos y se lee en `MenuPrincipal.DIAN()` — es una funcionalidad de administración de la resolución de facturación DIAN que parece no haberse completado en la UI de esta pantalla.


### BdConection.java — Configuración de conexión a la base de datos MySQL

**Propósito:** Diálogo modal que permite ingresar/corregir manualmente los parámetros de conexión a MySQL (host, base de datos, puerto, usuario, contraseña) cuando la aplicación no logra conectarse automáticamente al iniciar, y probar/guardar la nueva configuración.

**Cómo se invoca / navegación:** Es un `javax.swing.JDialog` modal, hijo de `MenuPrincipal`. Se instancia ÚNICAMENTE desde `MenuPrincipal.TratarDeConectarBD()` cuando `Conexion` resulta `null` tras el intento automático de conexión:
```java
BdConection BdConection_ = new BdConection(this, true);
BdConection_.setVisible(true);
```
Al construirse, llama automáticamente a `CargarInfoConexion()` para precargar los campos desde el archivo `Configuracion.properties` del directorio de trabajo.

**Componentes de interfaz:**
- `jLabel1` ("Error de Conexión", en rojo, fuente grande): título de alerta.
- `HostName` (JTextField): host/IP del servidor MySQL.
- `baseDatos` (JTextField): nombre de la base de datos (ej. `sarapos3`).
- `puerto` (JTextField): puerto TCP de MySQL.
- `usuario` (JTextField): usuario de MySQL.
- `clave` (JTextField, **no es JPasswordField** — la contraseña se muestra en texto plano en pantalla, ver Notas): contraseña de MySQL.
- `jButton3` ("Guardar"): prueba la conexión con los datos ingresados y, si es exitosa, la persiste.
- `jButton2` ("Cerrar"): cierra toda la aplicación.

**Funcionalidades atómicas (numeradas):**
1. Al construirse el diálogo, `CargarInfoConexion()` lee `<directorio de trabajo>/Configuracion.properties` y rellena los 5 campos (`HostName`, `baseDatos`, `puerto`, `usuario`, `clave`) con los valores actualmente guardados.
2. Al pulsar `jButton3` ("Guardar"):
   - 2.1 Intenta `Conexion_prueba = BD_prueba.getConexion(HostName.getText(), baseDatos.getText(), puerto.getText(), usuario.getText(), clave.getText())` (usa la instancia estática `TestConnection BD_prueba` propia de esta clase, NO la de `MenuPrincipal`).
   - 2.2 Si `Conexion_prueba` es `null` (conexión fallida): muestra `JOptionPane.showMessageDialog(this,"Conexion fallida, datos erroneos.","Sara POS 1.0",JOptionPane.ERROR_MESSAGE)` y el diálogo permanece abierto para reintentar.
   - 2.3 Si la conexión es exitosa: llama `GuardarInfoConexion()` (persiste SOLO el campo `baseDatos` en el archivo de propiedades — ver Notas, posible bug), cierra la conexión de prueba (`Conexion_prueba.close()`), llama `Obj.TratarDeConectarBD()` sobre la instancia de `MenuPrincipal` que abrió este diálogo (reintenta la conexión real de la aplicación con los nuevos datos ya guardados), y cierra el diálogo (`this.dispose()`).
3. Al pulsar `jButton2` ("Cerrar"): ejecuta `System.exit(0)` — cierra TODA la aplicación, no solo el diálogo (coherente con que, si no hay conexión a BD, la aplicación no puede operar).
4. Al cerrar la ventana con el botón "X" del sistema (evento `windowClosing`): también ejecuta `System.exit(0)`.
5. Evento `windowClosed`: existe el manejador pero está vacío (sin efecto adicional al ya cubierto por `windowClosing`).
6. `GuardarInfoConexion()`: carga el archivo de propiedades existente, sobreescribe SOLO la propiedad `baseDatos` con el valor actual del campo `baseDatos`, y guarda el archivo (`config.store(...)`) con el comentario de cabecera `"Sara POS 1.0 - Error de Conexión"`. El resto de líneas que actualizarían los demás campos (`HostName`, `baseDatos`, `puerto`, `usuario`, `clave`) de vuelta a los JTextField están comentadas tras el guardado (no se re-leen).

**Datos / tablas de BD leídas o escritas:** Ninguna tabla de negocio; solo intenta abrir una `Connection` JDBC de prueba contra el servidor/base indicados (no ejecuta ningún SELECT/INSERT/UPDATE de datos de aplicación).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- No hay validación de formato en ningún campo (puerto podría no ser numérico, host podría estar vacío, etc.); toda la "validación" es, en la práctica, el intento real de conexión JDBC.
- El archivo `Configuracion.properties` vive junto al ejecutable (`System.getProperty("user.dir")`), por lo que es un archivo de configuración local por instalación, no por usuario del sistema operativo.

**Notas / peculiaridades:**
- **Bug relevante:** `GuardarInfoConexion()` únicamente persiste la propiedad `baseDatos` en el archivo; `HostName`, `puerto`, `usuario` y `clave` ingresados por el usuario en el diálogo **nunca se guardan en el archivo de propiedades**, aunque sí se usaron para la conexión de prueba exitosa y aunque `MenuPrincipal.TratarDeConectarBD()` inmediatamente después vuelve a leer el archivo de propiedades (`CargarInfoConexion()`) para reconectar — esto significa que, si el usuario cambió host/puerto/usuario/clave (no solo la base de datos) para lograr conectar, la reconexión inmediata en `TratarDeConectarBD()` probablemente usará los valores VIEJOS de esas 4 propiedades (los del archivo), no los que el usuario acaba de validar como correctos, excepto `baseDatos` que sí se actualiza. Comportamiento a decidir explícitamente al migrar a Regenta (lo más razonable es persistir los 5 campos).
- El campo de contraseña `clave` es un `JTextField` normal (no `JPasswordField`), por lo que la contraseña de la base de datos se muestra en texto plano en la pantalla — inconsistente con `Loguin.java`, que sí usa `JPasswordField` para la contraseña de aplicación.
- Usa su propia instancia estática `TestConnection BD_prueba` y `Connection Conexion_prueba`, separada de `MenuPrincipal.BD`/`MenuPrincipal.Conexion` — es decir, la "conexión de prueba" y la "conexión real de la aplicación" son objetos distintos (aunque de la misma clase `TestConnection`, cuyos campos internos `sentencia`/`resultado`/`conexion` son `static`, por lo que en la práctica ambos objetos comparten el mismo estado estático subyacente de JDBC — ver `TestConnection.java`: todos sus campos relevantes son `static`).
- El título de la ventana (`setTitle("Sara POS  1.0 - Error de Conexión")`) y el comentario de cabecera guardado en el `.properties` reutilizan el mismo texto, indicando que esta pantalla fue pensada exclusivamente como pantalla de "recuperación de error de conexión al iniciar", no como una pantalla general de "Configurar conexión" accesible en cualquier momento desde un menú.


### AcercaDe.java — Acerca de / About

**Propósito:** Diálogo informativo simple ("About") que muestra el nombre del producto, su descripción, versión, autor del software y datos de contacto.

**Cómo se invoca / navegación:** Es un `javax.swing.JDialog` modal, hijo de `Menu` (NO de `Menunew`, ver Notas). Se instancia únicamente desde `Menu.jLabel36MouseClicked` (enlace "Acerca de" del panel de usuarios en la clase `Menu.java`, que es la pantalla de navegación legacy/inactiva descrita arriba):
```java
AcercaDe AcercaDe = new AcercaDe(this, true);
AcercaDe.setVisible(true);
```
No recibe más parámetros que el padre (`Menu`) y el flag modal.

**Componentes de interfaz:**
- `imageLabel`: logo/imagen del producto (`/Imagenes/Sin título-3.png`).
- `appTitleLabel`: " Sara Punto de Venta -Software POS " (negrita, fuente +4pt).
- `appDescLabel` (HTML): descripción larga: "Este programa lleva módulos completos de: facturación, compras, control de inventario, cuentas por pagar, cuentas por cobrar y reportes de gestión como: movimiento de caja, ventas por periodo, ventas por producto, etc."
- `versionLabel` / `appVersionLabel`: "Version del Producto :" / "1.0".
- `vendorLabel` / `appVendorLabel`: "Programadores:" / "!ng. Donaldo Gandara" (sic, con "!" en vez de "I").
- `homepageLabel` / `appHomepageLabel`: "Contacto:" / "3016088542 - donaldogandara@homail.com" (dominio de correo escrito "homail.com", probable error de tipeo por "hotmail.com").
- `jButton1` ("Cerrar"): oculta el diálogo.

**Funcionalidades atómicas (numeradas):**
1. Al pulsar `jButton1` ("Cerrar"), el diálogo se oculta (`setVisible(false)`) — **no se destruye** (`dispose()`), a diferencia de otros diálogos del sistema como `Loguin`.
2. No existe ninguna otra interacción; es una pantalla puramente informativa de solo lectura.

**Datos / tablas de BD leídas o escritas:** Ninguna; todos los textos están fijos (hardcodeados) en el código.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna.

**Notas / peculiaridades:**
- Esta pantalla depende de la clase `Menu` como único punto de entrada (`Menu Obj1`), que es la pantalla de navegación LEGACY/inactiva (ver sección `Menu.java` arriba). Dado que `Menu.java` prácticamente no se usa en el flujo activo de la aplicación (reemplazado por `Menunew.java`), es posible que **en la práctica el usuario final nunca pueda abrir este diálogo "Acerca de"** a través de la navegación real del sistema, salvo que exista otro punto de entrada no incluido en este grupo de archivos. Al migrar a Regenta, se recomienda verificar si existe otro acceso a "Acerca de" (p. ej. revisando si `botonUsuarios` de `Menunew`, actualmente sin acción, estaba pensado para esto) o si esta funcionalidad simplemente no es alcanzable en la versión actual del software.
- Todos los textos (versión, autor, contacto, descripción) están hardcodeados directamente en `initComponents()`, no provienen de ninguna tabla ni archivo de configuración — para Regenta, estos datos deberían ser configurables o al menos actualizados a la información real de contacto/versión vigente.
- Hay un método `main()` comentado que permite probar este diálogo de forma aislada (resto típico del NetBeans GUI Builder), sin efecto en producción.


### fechas.java — Selector de rango de fechas (diálogo utilitario para informes)

**Propósito:** Diálogo modal reutilizable para capturar un rango de fechas (fecha inicial y fecha final) y devolverlo a la pantalla de informes que lo invocó, para filtrar un reporte por periodo.

**Cómo se invoca / navegación:** Es un `javax.swing.JDialog` que implementa `KeyListener`. Tiene 3 constructores sobrecargados, uno por cada pantalla de informes que puede abrirlo como padre: `fechas(InformesCaja Obj, boolean modal)`, `fechas(InformeCuentas obj2, boolean modal)` y `fechas(VentasDiarias obj3, boolean modal)` (estas 3 clases de informes no forman parte de este grupo de archivos, pero se referencian aquí como consumidoras). Solo uno de los 3 campos padre (`Obj`, `obj2`, `obj3`) queda asignado según cuál constructor se use; los otros quedan en `null`. Al construirse, configura el formato de fecha de los dos selectores de fecha (`fechaInicial`, `fechaFinal`, componentes `datechooser.beans.DateChooserCombo` de una librería externa) al patrón `"yyyy-MM-dd"` (formato compatible con MySQL).

**Componentes de interfaz:**
- `JLFI` ("Fecha Inicial:", con ícono de calendario) y `fechaInicial` (DateChooserCombo): selector de fecha inicial del rango.
- `JLFI1` ("Fecha Final:", con ícono de calendario) y `fechaFinal` (DateChooserCombo): selector de fecha final del rango.
- `jButton6` ("Cargar"): confirma el rango elegido y lo envía a la pantalla de informes que abrió este diálogo.
- `mas` (botón con ícono "ok.png"): presente en el diseño pero su `ActionPerformed` está vacío (sin funcionalidad real).

**Funcionalidades atómicas (numeradas):**
1. Al pulsar `jButton6` ("Cargar"):
   - 1.1 Si el diálogo fue abierto desde `InformesCaja` (`Obj != null`): llama `Obj.informeFechas(fechaInicial.getText(), fechaFinal.getText())` y cierra el diálogo (`dispose()`).
   - 1.2 Si no, y fue abierto desde `InformeCuentas` (`obj2 != null`): llama `obj2.informeFechas(fechaInicial.getText(), fechaFinal.getText())` y cierra el diálogo.
   - 1.3 Si no, y fue abierto desde `VentasDiarias` (`obj3 != null`): llama `obj3.informeFechas(fechaInicial.getText(), fechaFinal.getText())` y cierra el diálogo.
   - 1.4 Las fechas se envían como texto plano ya formateado (`DateChooserCombo.getText()`), confiando en que el `DateFormat` fijado (`yyyy-MM-dd`) ya dejó el texto en el formato correcto para ser usado directamente en una cláusula SQL `WHERE fecha BETWEEN ... AND ...` por la pantalla de informes receptora.
2. Al presionar la tecla `ESC` en cualquier momento (manejador `keyPressed` de la implementación `KeyListener` de la clase): cierra el diálogo (`this.dispose()`).
3. Los eventos `fechaInicialOnSelectionChange`, `fechaInicialOnCommit`, `fechaFinalOnCommit` (propios del componente externo `DateChooserCombo`) existen como manejadores pero están vacíos — no se valida, por ejemplo, que la fecha final sea posterior a la inicial.
4. Métodos utilitarios `pasarFocusVerificarNumeros`, `ganarFoco`, `pierdeFoco`: presentes (idénticos a los de `Loguin.java`) pero no conectados a ningún evento de este formulario.

**Datos / tablas de BD leídas o escritas:** Ninguna consulta SQL directa en este archivo; solo transporta dos strings de fecha hacia el método `informeFechas(String, String)` de la pantalla de informes que lo invocó (esa pantalla sí ejecutará el SQL filtrando por el rango).

**Reportes generados:** No genera directamente un reporte JasperReports; es el paso previo de captura de parámetros que alimenta al método `informeFechas` de `InformesCaja`, `InformeCuentas` o `VentasDiarias`, quienes presumiblemente sí disparan el `.jrxml` correspondiente (esas clases no forman parte de este grupo de archivos).

**Reglas de negocio / validaciones no obvias:**
- No existe ninguna validación de que `fechaFinal` sea posterior o igual a `fechaInicial`, ni de que ambos campos tengan un valor seleccionado antes de pulsar "Cargar" — si el usuario pulsa "Cargar" sin elegir fechas, se enviarán los valores por defecto/vacíos del `DateChooserCombo` tal cual estén en ese momento.
- El formato fijo `yyyy-MM-dd` asegura compatibilidad directa con comparaciones de fecha en MySQL sin necesidad de conversión adicional en la pantalla receptora.

**Notas / peculiaridades:**
- El método `nulles()` (llamado al inicio de los 3 constructores) solo pone `Obj = null`; no limpia `obj2` ni `obj3` (irrelevante en la práctica porque Java ya los inicializa en `null` por defecto, pero el nombre sugiere la intención de "anular todas las referencias").
- Al ser una ventana totalmente genérica y reutilizada por 3 pantallas de informes distintas mediante sobrecarga de constructor, el patrón sería fácilmente reemplazable en Regenta por un único diálogo de "rango de fechas" parametrizado por un callback o por el nombre del reporte a generar, en lugar de 3 constructores con referencias directas a clases concretas.
- El botón `mas` (ícono "ok.png") parece ser un resto de copiar esta clase a partir de otro diálogo (posiblemente `Loguin.java`, que también usa el mismo ícono "ok.png" para su botón "Entrar"), sin que se haya completado o retirado del diseño en `fechas.java`.

---

## Grupo: Ventas / Punto de Venta

> Documentación de migración funcional generada a partir del código fuente real (`SISTEMA/src/*.java`), el esquema `sarapos3_final_funcionando.sql` y los reportes JasperReports en `REPORTES/CUENTAS/*.jrxml`. Todas las sentencias SQL citadas son las que ejecuta literalmente el código (concatenación de strings, sin PreparedStatement), lo que implica que el sistema es vulnerable a inyección SQL — relevante para la migración pero no se detalla aparte en cada sección salvo que aporte matiz de negocio.

---

### Ventas.java — Pantalla principal de Venta / Punto de Venta (POS)

**Propósito:** Pantalla central del sistema: permite construir el carrito de una venta (o edición de una venta ya realizada, o carga de una cotización/venta guardada), calcular subtotal/IVA/descuento/total, y finalizar la venta abriendo el diálogo de pago (`Pago`), o registrar una cotización, o guardar la venta para después.

**Cómo se invoca / navegación:**
- Es un `JInternalFrame` (ventana interna del `JDesktopPane` `MenuPrincipal.Escritorio`), no modal.
- Constructor `Ventas()`: venta nueva en blanco. Se instancia desde `Menunew.java` (`VENTAS = new Ventas();`) y desde `MenuPrincipal.java` (`VENTAS = new Ventas();`) al pulsar el botón/opción "Ventas" del menú principal o la pestaña "BV1".
- Constructor `Ventas(String numero_factura)`: abre la pantalla ya cargada en **modo edición** de una factura existente, llamando internamente a `LISTARVENTA(numero_factura)`. Se usa desde `EditarVentas1.java` (`new Ventas(sudid)`) — ver nota de código muerto en la sección de `EditarVentas1`.
- También se puede re-activar una instancia ya abierta vía `MenuPrincipal.ActivarFrame("Ventas", invoca, sudid, cc, nombre, listaPrecio)` en lugar de crear una nueva (patrón usado en todo el sistema para no duplicar ventanas).
- Internamente puede navegar a: `Cargarfactura` (cargar una factura ya facturada para anular/consultar), `CargarCotizacion` (cargar una cotización guardada para convertirla en venta), `VentasGuardadas` (no se abre directamente desde aquí, es al revés: `VentasGuardadas` abre `Ventas`), `MovimientoCajaVentas`, `Clientes`, `ListaProductos`, `Cantidad`, `Descuento`, `PrecioVenta`, `AgregarExistecias`, `DetalleCliente`, `DetalleProducto`, `Pago`, `cambio`.

**Componentes de interfaz:**
- `BUSCAR` (JTextField): búsqueda/escáner de producto (por referencia, código de barras, nombre, unidades, categoría, existencias, precio compra/venta, impuesto — columnas 1 a 9 de `PRODUCTOS`).
- `PRODUCTOS` (JTable, catálogo filtrable con `TableRowSorter trsfiltro`): columnas **ID, REFERENCIA, CODIGO DE BARRAS, PRODUCTO, UNIDADES, CATEGORIA, EXISTENCIAS, PRECIO COMPRA, PRECIO VENTA, IMPUESTOS, ESTANTE**. Menú contextual (clic derecho): *Agregar Existencias*, *Agregar Producto a la Venta*, *Detallar Producto* (estos dos últimos solo visibles si `PRIVILEGIO == "ADMINISTRADOR"`).
- `VENTAS` (JTable, carrito de la venta en curso): columnas visibles **PRODUCTO, CANTIDAD, VALOR UNITARIO, IVA, DESCUENTO, TOTAL**; columnas ocultas (ancho 0) **id, editar, nuevo, existencias, iva_producto, valor_producto**. Menú contextual: *Eliminar Producto*, *Modificar Cantidad / Precio Venta / Descuento*, *Detallar*.
- `VENTA_EDITAR` (JTable oculta, no visible en pantalla salvo dentro de un `jScrollPane4` que no se añade al layout visible): guarda una copia "antes de editar" de las líneas de una factura que se está editando, usada para calcular qué debe regresar a inventario.
- `TCLIENTE` / `CC_CLIENTE` (JTextField, no editables): nombre y cédula/NIT del cliente seleccionado; se pintan en azul y con cursor de mano cuando hay cliente real (no "OCASIONAL").
- `ListaPrecios` (JComboBox): PVP MINORISTA / MAYORISTA / VIP según configuración `DATOS_OBLIGATORIOS["LISTA_PRECIOS"]` (1, 2 o 3 niveles).
- `n_factura` (JLabel): número de factura/remisión/cotización actual.
- `FECHA` (JLabel): reloj en vivo (hilo `run()`/`calcularHora()`, se actualiza cada segundo) o fecha de la factura cargada (en modo edición, en azul).
- `fecha` (`DateChooserCombo`, oculto, `setVisible(false)`): fecha "de sistema" usada en los INSERT a BD (no es la que ve el usuario).
- Etiquetas de totales: `VALOR_BRUTO_`, `DESCUENTO_`, `TOTALIVA_`, `TOTAL_` (JLabel grandes, formato moneda).
- `RF` (JLabel "Factura Venta:"): al hacer clic alterna el modo Factura/Remisión (ver reglas de negocio).
- Botones barra horizontal (`jToolBar1`): `jButton1` Buscar (Ctrl+F), `jButton13` Editar/Agregar existencias (Ctrl+X), `jButton4` Cantidad (Ctrl+C), `jButton5` Valor/Precio (Ctrl+V), `jButton6` Descuento (Ctrl+B), `jButton7` Eliminar (Ctrl+E), `Bvender` Vender (Ctrl+Enter), `BventaRapida` Venta Rápida (Ctrl+P), `Bcotizar` Cotizar (Ctrl+S), `BventaNueva` Limpiar/Nueva venta (Ctrl+L). Todos excepto Buscar/Editar solo visibles si el carrito tiene filas.
- Botones barra vertical (`jToolBar2`): `jButton14` Cliente (Alt+C), `BcargarCotizacion` Cargar Cotización, `BcargarFactura` Cargar Factura, `BanularFactura` Anular Factura (solo visible tras cargar/editar una factura), `BanularFactura1` "Movi. Caja" (abre `MovimientoCajaVentas`), `jButton9` Abrir Monedero (Ctrl+M, imprime "CUENTAS/AbrirCajon"), `jButton2` Cerrar (Esc).

**Funcionalidades atómicas (numeradas):**
1. Al escribir en `BUSCAR`, se filtra en vivo la tabla `PRODUCTOS` con expresión regular (cada palabra separada por espacio se convierte en `.*texto.*`, búsqueda difusa multi-palabra) sobre las columnas 1-9.
2. Si el texto de `BUSCAR` es un solo carácter y es la tecla "." (`VK_DECIMAL`), el campo se auto-completa a `"1-"` (atajo para referencias que empiezan por "1-").
3. Al pulsar **Enter** en `BUSCAR`: si el filtro deja exactamente **1** fila visible, esa fila se agrega automáticamente al carrito y se abre el diálogo `Cantidad` para esa línea recién agregada; el buscador se limpia y recupera el foco.
4. Al pulsar **Enter** en `BUSCAR` con **más de 1** coincidencia y habiendo escrito letras/dígitos (bandera `ESCRIBIR`): se abre `ListaProductos` (buscador ampliado) precargado con las filas filtradas y el mismo texto de búsqueda.
5. Al pulsar **Enter** con **0** coincidencias y habiendo escrito: también se abre `ListaProductos` (con todas las filas, para seguir buscando allí).
6. Doble clic sobre una fila de `PRODUCTOS` no hace nada en esta clase base (el handler está vacío) — el agregado real ocurre por Enter en `BUSCAR` o por el menú contextual "Agregar Producto a la Venta".
7. **Ctrl+F** o botón Buscar: abre `ListaProductos` con el catálogo completo precargado y el texto actual de `BUSCAR`.
8. **Ctrl+C** o botón Cantidad, o doble clic en la celda "CANTIDAD" de una fila del carrito, o el ítem de menú "Modificar Cantidad...": abre el diálogo `Cantidad` para la fila seleccionada (requiere fila seleccionada).
9. **Ctrl+V** o botón Valor, o doble clic en la celda "VALOR UNITARIO": limpia el portapapeles del sistema (`borrarClipboard()`) y abre el diálogo `PrecioVenta`.
10. **Ctrl+B** o botón Descuento, o doble clic en la celda "DESCUENTO": abre el diálogo `Descuento`.
11. **Ctrl+X** o botón Editar, o doble clic en la celda "PRODUCTO", o el menú "Agregar Existencias": abre `AgregarExistecias` para el producto de la fila seleccionada (edición rápida de stock).
12. **Ctrl+E** o botón Eliminar, o el ítem de menú "Eliminar Producto": elimina la fila seleccionada del carrito y recalcula totales; selecciona la última fila restante.
13. **AltGr (Alt Graph)** con una fila del carrito seleccionada: decrementa en 1 la cantidad de esa fila; si llega a 0 o menos, elimina la fila; recalcula totales. (Atajo rápido "quitar una unidad".)
14. **Ctrl+D**: abre `DetalleProducto` para la fila seleccionada (detalle de fracciones, p.ej. venta de galones/medidas parciales).
15. **Ctrl+2** / **Ctrl+Numpad2**: si no existe ya una instancia de "Ventas1", la crea, la maximiza y la muestra (segunda caja/registradora concurrente); si ya existe, la activa (`MenuPrincipal.ActivarFrame`). Marca el botón de menú `BV2` como seleccionado.
16. **Ctrl+3** / **Ctrl+Numpad3**: igual que el punto anterior pero para "Ventas2" (tercera caja), marca `BV3`.
17. **Ctrl+P** o botón "Venta Rápida": llama `VentaRapida()` → factura inmediatamente la venta como "Efectivo" con `ENTREGADO = total exacto` (sin cambio), **sin abrir el diálogo de Pago**, imprime el tiquete y limpia la pantalla.
18. **Ctrl+G**: `facturaRenuente()` — flujo de "factura terca/pendiente": si no hay ya una factura terca abierta para el usuario, reserva un número de factura (`CARGAR_NUMERO_FACTURA_RENUENTE(true)`), inserta las líneas actuales directamente en la tabla `ventas` (sin pasar por el diálogo de pago ni descontar completamente el flujo normal) y registra en `FACTURARENUENTE` y en `caja` un movimiento "FACTURA ACUMULADA" en estado `ABIERTO`; además llama al procedimiento almacenado `CALL totalrenuente('<factura>')`. Pensado para seguir acumulando líneas a una misma factura en varias sesiones (ej. cuando el cliente va añadiendo productos a lo largo del día).
19. **Ctrl+Alt+J**: `RemisionRenuente()` — mismo mecanismo que el punto 18 pero fuerza modo Remisión (pone `FACTURA_REMISION=false`, texto `RF` en negro) y usa la numeración de `REMISION`/`REMISIONRENUENTE`.
20. **Ctrl+L** o botón Limpiar: `AbrirVentaNueva()` — si el carrito tiene líneas y no se trata de una venta cargada/guardada ni en edición, pregunta "¿Desea empezar una nueva venta?" (Sí/No) antes de limpiar; si es una venta guardada o en edición, limpia directamente (y si estaba en edición, antes llama `cancelarPuntos()` para revertir puntos redimidos); si el carrito está vacío, limpia sin preguntar.
21. **Ctrl+S** o botón Cotizar: `Cotizar()` — si el carrito tiene líneas, confirma "¿Desea realizar cotización?"; si Sí: asigna número de cotización (`CT<id>` desde contador `COTIZACION` en `configuracion`), guarda las líneas en la tabla `cotizacion` (sin tocar inventario), imprime el reporte "CUENTAS/Cotizacion" y limpia la pantalla. Si el carrito está vacío, muestra error.
22. **Alt+C** o botón Cliente: abre (o activa) la pantalla `Clientes` en modo "Ventas" para elegir/crear cliente.
23. **Ctrl+Enter** o botón Vender: `Vender()` (ver regla de negocio detallada más abajo) — abre el diálogo `Pago` con el monto a cobrar ya calculado, salvo el caso especial de nota crédito que cubre el 100% del total, en cuyo caso factura directo sin diálogo de pago.
24. **Escape** (en cualquier componente con el `KeyListener` de la ventana, o botón Cerrar): `cerrar()` — si el carrito tiene líneas y la venta no es una "guardada" ni una "en edición", pregunta Sí/No/Cancelar para guardar la venta como pendiente antes de salir (Sí exige cliente asignado, llama a `guardarVentas()`); si No, descarta el carrito y regresa al menú; si está vacío/en edición/guardada, si estaba en edición revierte puntos redimidos (`cancelarPuntos()`) y regresa al menú.
25. Botón "Cargar Factura" (`BcargarFactura`): solo habilitado si el carrito está vacío; abre el diálogo `Cargarfactura` para buscar y cargar una factura ya emitida en modo edición (si el carrito no está vacío muestra error "Para cargar una factura debe terminar la venta que esta realizando.").
26. Botón "Cargar Cotización" (`BcargarCotizacion`): igual que el anterior pero para abrir `CargarCotizacion` y transformar una cotización en venta (requiere carrito vacío).
27. Botón "Anular Factura" (`BanularFactura`, solo visible tras cargar una factura): pide confirmación; si la factura tiene una deuda a débito activa (`ventas_debito.ESTADO='DEBE'`) se anula directo; de lo contrario pregunta si se desea generar nota crédito (requiere cliente no "OCASIONAL"); en cualquier caso al final se restituye todo el inventario de esa factura.
28. Botón "Movi. Caja" (`BanularFactura1`): abre `MovimientoCajaVentas` para registrar entradas/salidas manuales de efectivo en caja.
29. Botón "Abrir Monedero" (Ctrl+M): imprime directamente el reporte "CUENTAS/AbrirCajon" (comando ESC/POS para abrir el cajón de dinero vía la impresora térmica configurada).
30. Clic en la etiqueta `RF` ("Factura Venta:"): alterna `FACTURA_REMISION` entre verdadero/falso y cambia el color del texto (ver regla de negocio Factura vs Remisión).
31. Clic en `TCLIENTE` (si el cliente no es "OCASIONAL"): abre `DetalleCliente` con el historial del cliente.
32. Cambiar `ListaPrecios`: recarga `PRODUCTOS` con la columna de precio correspondiente (`PRECIO_VENTA`, `PRECIO_VENTA2` o `PRECIO_VENTA3`).
33. Al perder el foco `BUSCAR` cambia su color de fondo (feedback visual blanco/lavanda) sin alterar el texto.

**Datos / tablas de BD leídas o escritas:**
- `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades` — **SELECT** para poblar `PRODUCTOS` (unión de productos con stock en LUGAR='1' y productos activos sin fila de stock, mostrados con existencia 0).
- `ventas` — **INSERT** de cada línea al facturar (`guardarVentas`), **DELETE** al anular o al reeditar (`EliminarVenta`, `editarVentaGuardada`), **SELECT** al cargar una factura para editar (`LISTARVENTA`) o para `regresarPuntosAcumulados`.
- `ventas_debito` — **INSERT** si el tipo de pago es "Debito" (id 5) (`guardarVentasDebito`), **DELETE** al anular (`EliminarVentaDebito`), **SELECT** para saber si una factura tiene deuda activa (`ventaDebicontado`).
- `ventasguardadas` — **INSERT** al "Guardar venta" (`guardarVentas()` sin args), **DELETE** al facturar una venta que venía de guardada, **SELECT** al cargar (`cargarVentaGuardada`).
- `cotizacion` — **INSERT** al cotizar (`guardarCotizacion`), **SELECT** al cargar una cotización (`ListarCotizacion`).
- `caja` — **INSERT** por cada venta/movimiento (`guardarMovimientoCaja`, `CARGAR_NUMERO_FACTURA_RENUENTE`), **UPDATE** al editar una venta ya facturada (`editarMovimientoCaja`), **DELETE** al anular (`EliminarMovimientoCaja`).
- `movimiento_inventario` — **INSERT** en cada salida ("(Salida) Venta") y en cada devolución ("(Entrada) Devolucion") de stock.
- `stockproductos` — **UPDATE** (resta/suma cantidad) en `descontarInventario_` / `DevolucionAInventario_`.
- `facturaanulada` — **INSERT** al anular si `REUTILIZAR_FACTURAS_ANULADAS=1`; **SELECT/DELETE** al reutilizar ese número (`VerificarFacturaAnulada`).
- `FACTURAS_ANULADAS` (tabla de archivo con las líneas completas) — **INSERT** al anular si NO se reutiliza numeración.
- `TIKETE` — **SELECT/UPDATE** columna `ESTADO` como contador autoincremental manual del número de factura.
- `configuracion` — **SELECT/UPDATE** columnas `REMISION`, `COTIZACION`, `CENTRADASALIDA` (contadores), y `PRIVILEGIO`/`LISTA_PRECIOS`/`REUTILIZAR_FACTURAS_ANULADAS` leídas desde `MenuPrincipal.DATOS_OBLIGATORIOS` (cacheadas en memoria, no re-consultadas cada vez).
- `FACTURARENUENTE` / `REMISIONRENUENTE` — **INSERT/SELECT** para el flujo de factura/remisión "terca".
- `notascredito` — **INSERT** (`guardarNotaCredito`), **UPDATE** (`inactivarNotasCredito`, `inactivarNotasCredito2`), **SELECT** (`VerificarNotasCreditod`).
- `puntos` — **UPDATE** suma/resta de puntos de fidelidad al editar una venta (`regresarPuntosAcumulados`, `cancelarPuntos`).
- `cliente` — **SELECT** `IDCLIENTES` por cédula/NIT (`id_cliente`).

**Reportes generados:**
- `CUENTAS/Factura_venta.jrxml` — factura de venta normal o remisión (según `tituloFactura()`), impresa en impresora térmica configurada (`IMPRESORA_TERMICA`).
- `CUENTAS/Cotizacion.jrxml` — impresión de cotización.
- `CUENTAS/AbrirCajon.jrxml` — comando de apertura del cajón de dinero.
- El tipo de pago "Debito" (id 5) fuerza el título de reporte `"REMISION"` incluso si en realidad es una factura (línea `datosExportarImprimir(..., "REMISION", ...)`), aparente inconsistencia/atajo de implementación a replicar con cuidado.

**Reglas de negocio / validaciones no obvias:**
- **Factura vs. Remisión:** el campo `FACTURA_REMISION` (booleano) decide si `CARGAR_NUMERO_FACTURA()` consume el contador de `TIKETE.ESTADO` (factura, prefijo implícito "FV" en otros flujos) o el contador `configuracion.REMISION` (prefijo "RM"). Se alterna haciendo clic en la etiqueta `RF`. Por defecto al limpiar la pantalla siempre vuelve a `true` (modo Factura).
- **Reutilización de facturas anuladas:** si `DATOS_OBLIGATORIOS["REUTILIZAR_FACTURAS_ANULADAS"] == "1"`, el próximo número de factura reutiliza el de la última anulada (`VerificarFacturaAnulada`) en vez de tomar uno nuevo del contador.
- **IVA:** cada producto tiene una categoría de impuesto (`CATEGORIA_IMPUESTO.VALOR`, porcentaje). El precio mostrado en el catálogo YA incluye IVA; al agregarlo al carrito se "desarma" (`PRECIO_VENTA / (1+iva%)`) para guardar precio base e IVA por separado, y el total de la línea se reconstruye sumando ambos menos el descuento.
- **Descuento por línea:** se valida que el descuento no supere el precio unitario del producto (ver `Descuento.java`).
- **Control de stock:** `calcularTotales` marca cada línea como "HAY"/"NOHAY" comparando cantidad contra `verificrExistenciaProductos`, pero la venta **no bloquea** la facturación aunque una línea esté en "NOHAY" en esta clase base (el método `verificarProductosCeros()` existe pero no se usa en el flujo de `Vender()`; sí se usa — comentado — en otras variantes).
- **Puntos de fidelidad:** al editar una venta ya facturada que había redimido puntos, esos puntos se devuelven al cliente (`regresarPuntosAcumulados`) y, si el usuario cancela la edición sin volver a vender, se le vuelven a restar (`cancelarPuntos`) para no duplicar el beneficio.
- **Notas crédito:** si el cliente seleccionado tiene saldo activo en `notascredito`, al vender: (a) si el saldo no cubre el total, se abre `Pago` solo por la diferencia y, al imprimir el tiquete, se inactivan TODAS las notas crédito activas del cliente (se consumen aunque no cubrieran el 100%); (b) si el saldo cubre el total completo, la venta se descuenta directamente contra una o varias notas crédito (recorriendo las activas de mayor a menor hasta agotar el total) y se factura sin abrir el diálogo de pago, con tipo de pago "Gratis".
- **Edición de venta (debe vs. pagada):** al editar una factura, si ya tenía una deuda a débito activa (`ventaDebicontado`), el nuevo pago se calcula sobre el saldo pendiente; si la venta editada queda por debajo o por encima de lo ya pagado, se actualiza (no se duplica) el movimiento de caja según si éste ya estaba `CERRADO` (cierre de caja ya hecho, no se toca) o sigue `ABIERTO` (se actualiza in-place).
- **Devolución a inventario al editar/anular:** siempre que se reemplaza o anula una venta, las cantidades de las líneas antiguas (capturadas en `VENTA_EDITAR`) regresan a `stockproductos` antes de insertar las líneas nuevas.
- **Privilegios:** las opciones de menú contextual "Detallar Producto" y "Agregar Existencias" del catálogo solo se muestran si el usuario autenticado tiene `PRIVILEGIO == "ADMINISTRADOR"`.

**Notas / peculiaridades:**
- Implementa `KeyListener` directamente sobre el frame además de usar `KeyAdapter`s por componente; el método `keyPressed(KeyEvent e)` acumula caracteres en `codigoBarras_` pero el código que lo procesaba (`ListarProductoCodigoBarras1`) está comentado — soporte de lectura de código de barras directo al foco de la ventana (sin pasar por `BUSCAR`) está deshabilitado en el código actual.
- Hay bastante código muerto comentado (atajos antiguos, validaciones de "no hay stock" al vender, doble-Enter para vender) que sugiere el flujo de venta fue simplificado/cambiado con el tiempo sin limpiar los comentarios.
- `Bvender.setToolTipText("Realizar Venta (Alt+Enter)")` pero el atajo realmente implementado es **Ctrl+Enter**; el tooltip quedó desactualizado.

---

### Ventas1.java — Variante de la pantalla de Venta (caja/registradora concurrente #2)

**Propósito:** Segunda instancia idéntica de la pantalla de ventas, para permitir una segunda venta en curso en paralelo (multi-caja) sin perder el carrito de la primera.

**Diferencias explícitas con `Ventas.java`:** **Ninguna funcional.** El `diff` entre ambos archivos es de solo 3 bloques triviales:
1. Nombre de la clase: `Ventas` → `Ventas1`.
2. Nombres de constructores (`Ventas()`/`Ventas(String)` → `Ventas1()`/`Ventas1(String)`).
3. `setTitle("Ventas")` → `setTitle("Ventas1")`.

Todo el resto del archivo (4328 líneas) es carácter-por-carácter idéntico: mismos componentes, mismos atajos de teclado, mismas consultas SQL, mismas reglas de negocio. Se invoca desde `Ventas.java`/`Ventas2.java`/`Ventas3.java` vía **Ctrl+2**, y desde `MenuPrincipal`/`Menunew` al pulsar la pestaña/botón correspondiente a la segunda caja (`MenuPrincipal.BV2`). Remitirse íntegramente a la sección de `Ventas.java` para propósito, componentes, atajos, datos, reportes y reglas de negocio.

**Notas / peculiaridades:** Código 100% duplicado (copy-paste) en vez de una sola clase parametrizada — para la migración a Regenta, basta con implementar **una** pantalla de ventas que soporte múltiples pestañas/instancias concurrentes en vez de 3 clases clonadas.

---

### Ventas2.java — Variante de la pantalla de Venta (caja/registradora concurrente #3)

**Propósito:** Tercera instancia idéntica de la pantalla de ventas (misma función que `Ventas1.java`).

**Diferencias explícitas con `Ventas.java`:** Igual que `Ventas1.java`: solo difiere el nombre de clase/constructores (`Ventas2`) y el título de ventana (`"Ventas2"`). Resto del código idéntico. Se invoca vía **Ctrl+3** desde cualquiera de las tres pantallas de venta, o desde el botón `MenuPrincipal.BV3`.

**Notas / peculiaridades:** Misma observación que `Ventas1.java` sobre duplicación de código.

---

### Ventas3.java — Variante/prototipo de la pantalla de Venta (NO USADA — código muerto)

**Propósito aparente:** Versión anterior/alternativa de la pantalla de venta con una interfaz más simple (sin pestaña de cotizaciones independiente, con filtro por categoría, sin manejo de notas crédito/remisión/anulación de factura). A diferencia de `Ventas1`/`Ventas2`, **esta clase nunca se instancia** en ningún punto vivo del sistema: `MenuPrincipal.java` declara el campo estático `public static Ventas3 VENTAS3 = null;` pero jamás ejecuta `new Ventas3()`; ningún menú, botón ni atajo de las otras pantallas abre esta clase. Varios diálogos (`Cantidad`, `Descuento`, `ListaProductos`, `PrecioVenta`, `AgregarExistecias`) conservan un constructor sobrecargado que acepta `Ventas3` como padre, pero `Pago.java` **no** tiene constructor para `Ventas3`, y dentro de `Ventas3.java` mismo todas las llamadas a `new Pago(...)` están comentadas.

**Cómo se invoca / navegación:** No alcanzable desde la interfaz de usuario en el estado actual del código. Es un `JInternalFrame` igual que las anteriores, con constructores `Ventas3()` y `Ventas3(String numero_factura)`.

**Componentes de interfaz (diferencias respecto a `Ventas.java`):**
- Botones renombrados/distintos en vez de la barra `jToolBar1`/`jToolBar2` de `Ventas.java`: `b_cliente`, `b_abrirmonedero`, `b_vender`, `b_atras` (cerrar), `b_ventasnueva`, `b_imprimirtikets` (cotizar/imprimir), `b_ventasguardadas`. No existen botones individuales de Cantidad/Valor/Descuento/Eliminar en la barra (esas acciones solo están disponibles por doble clic en la tabla o por el menú contextual).
- `categorias` (JComboBox nuevo): filtro de catálogo por categoría de producto (`"*.*"` = todas), alimentado por `listarCategorias()`; no existe en `Ventas.java`.
- `cotizar` (JToggleButton, no existe en `Ventas.java`): maneja la cotización de forma independiente al "Vender".
- `TCLIENTE`/`CC_CLIENTE` son **JLabel** (no editables ni interactivos como en `Ventas.java`, que usa `JTextField`).
- No existen `BanularFactura`, `BanularFactura1`, `BcargarCotizacion` (sí existe `BcargarFactura`), ni el toggle visual de Remisión (`RF`); no hay campo `FACTURA_REMISION`.
- `VENTAS` tiene una columna menos que en `Ventas.java` (no incluye la columna oculta "valor_producto" nº 11; el `ModificarZiseColomnasTablas` solo oculta hasta la columna 10).
- Tabla `PRODUCTOS` tiene un `ModificarZiseColomnasTablas1()` adicional que oculta más columnas y amplía el ancho de "PRODUCTO" (700px) — pensado para una vista más compacta, probablemente orientada a pantalla táctil.

**Funcionalidades atómicas — diferencias de comportamiento relevantes:**
1. `configuraciones()` llama a `cargarProductos("-1")` (con parámetro de categoría) y a `listarCategorias()`; el método `cargarProductos(String id_categoria)` tiene firma distinta a la de `Ventas.java` (`cargarProductos()` sin args).
2. Seleccionar una categoría en `categorias` recarga el catálogo filtrado por `ID_CATEGORIA` (o sin filtro si se elige "*.*").
3. El botón **"Vender" (`b_vender`) no hace nada**: todo el cuerpo de `b_venderActionPerformed` que debería abrir `Pago` está comentado; solo queda la validación "No se ha agregado ningún producto para la venta." si el carrito está vacío. Es decir, en esta variante la venta **no se puede completar por el botón principal**.
4. El atajo **Ctrl+P** (dentro de `BUSCARKeyReleased`) sí funciona: llama `imprimirTikest(...)` directamente con tipo de pago "Efectivo" y el total exacto como entregado (venta rápida sin diálogo de pago), igual que `VentaRapida()` de `Ventas.java` pero inline.
5. El doble-Enter (`DOBLE_ENTER==2`) que en versiones más viejas abría `Pago` también está comentado — no finaliza ninguna venta.
6. El botón **"Imprimir tiquete/Cotizar" (`b_imprimirtikets`)** y el toggle **`cotizar`** implementan la cotización de forma distinta a `Ventas.java`: llaman a `guardarVentas("4",0,0,false)` (que inserta las líneas en la tabla **`ventas`**, no en `cotizacion`) usando el mismo contador de número de factura (`CARGAR_NUMERO_FACTURA`/`FORMATO_NUMERO_FACTURA` con prefijo "FV"), imprimen el reporte **"Cotizacion"** (sin la ruta "CUENTAS/"), y después **eliminan** esa fila recién insertada de `ventas` (`EliminarRegistroCotizacion()`). Es decir: la "cotización" en esta variante consume temporalmente un número de factura real y usa la tabla de ventas como almacenamiento transitorio, en vez del mecanismo limpio de `Ventas.java` (contador `COTIZACION` propio + tabla `cotizacion` dedicada).
7. El botón **"Abrir Monedero" (`b_abrirmonedero`)** está completamente comentado (código que escribiría a un archivo `"USBPOS0"` para abrir el cajón) — no hace nada.
8. El botón **"Ventas Guardadas" (`b_ventasguardadas`)** está completamente comentado — no abre nada.
9. No existen los atajos Ctrl+G/Ctrl+Alt+J (factura/remisión "terca"), ni Ctrl+B/Ctrl+V tampoco en `BUSCARKeyReleased` (sí sigue existiendo Ctrl+2/Ctrl+3 para cambiar de caja, y Ctrl+D para detalle).
10. No existe `CARGAR_NUMERO_COTIZACION`, ni los métodos de notas crédito (`guardarNotaCredito`, `inactivarNotasCredito*`, `VerificarNotasCreditod`), ni `anularFactura`, `EliminarVenta`, `EliminarVentaDebito`, `EliminarMovimientoCaja`, `facturaAnulada`, `ventaDebicontado`, ni `ListarCotizacion` — todo ese bloque de funcionalidad de `Ventas.java` fue removido de esta variante.
11. `LISTARVENTA(sudid)` (constructor de edición) ya no restaura el color azul/cursor del cliente, ni vuelve a mostrar `BanularFactura` (no existe), ni valida "la factura no existe" si no hay filas.

**Datos / tablas de BD:** Las mismas `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades`, `ventas`, `ventas_debito`, `movimiento_inventario`, `caja`, con las diferencias anotadas arriba (cotización usando `ventas` en vez de `cotizacion`; sin acceso a `notascredito`, `facturaanulada`, `FACTURARENUENTE`).

**Reportes generados:** `Cotizacion` (ruta relativa, sin prefijo `CUENTAS/`, posible bug de ruta si se llegara a usar) y los mismos de factura de venta que en `Ventas.java` vía `imprimirTikest`.

**Reglas de negocio / validaciones no obvias:** Ninguna reutilizable con garantías — como el flujo de venta normal está roto (punto 3), esta pantalla **no es apta para producción** en su estado actual.

**Notas / peculiaridades:** Confirmado código muerto/abandonado — no se instancia desde ningún menú ni atajo vivo, y su acción principal ("Vender") no está implementada. Se documenta por completitud y porque los diálogos compartidos (`Cantidad`, `Descuento`, etc.) siguen manteniendo compatibilidad con ella. **No debe migrarse tal cual**: para Regenta, tómese `Ventas.java` como referencia de comportamiento correcto y complete; `Ventas3.java` solo aporta la idea de un combo de filtro por categoría, que puede valorarse como mejora de UX si se desea incorporar al rediseño.

---

### DetalleDeVenta.java — Diálogo de detalle de una venta (consulta)

**Propósito:** Diálogo modal de solo lectura que muestra el detalle de líneas de una factura (o de una factura anulada) junto con subtotal, IVA y total — usado como "ver detalle" desde varias pantallas de consulta/cobro.

**Cómo se invoca / navegación:** `JDialog` modal, con **seis constructores sobrecargados** según la pantalla que lo invoca, cada uno fija cuál es la ventana padre y una bandera interna `aux`:
`DetalleDeVenta(CuentasPorCobrar, boolean, String id)`, `DetalleDeVenta(CerrarCaja, boolean, String id)`, `DetalleDeVenta(DetalleCliente, boolean, String id)`, `DetalleDeVenta(detellaMovimientoCaja, boolean, String id)`, `DetalleDeVenta(InformesClientes, boolean, String id)`, `DetalleDeVenta(VentasDiarias, boolean, String id)` — todos estos llaman a `LISTARVENTA(id)` (consulta sobre `ventas`). El sexto, `DetalleDeVenta(VentasAnuladas, boolean, String id)`, llama en cambio a `LISTARVENTAANULADAS(id)` (consulta sobre `facturas_anuladas`). Dentro del grupo de Ventas se invoca desde `VentasDiarias.java` (doble clic o menú "Detallar Venta") y desde `VentasAnuladas.java` (doble clic o menú "Detallar Venta").

**Componentes de interfaz:**
- `VENTAS` (JTable): columnas **PRODUCTO, CANTIDAD, VALOR UNITARIO, IVA, DESCUENTO, TOTAL, id** (la última oculta).
- `TCLIENTE` (JLabel): nombre del cliente.
- `n_factura` (JLabel): número de factura consultada.
- `subtotal_`, `totaliva_`, `total_` (JLabel con borde tipo "caja"): totales de la factura, tal como quedaron grabados en la BD (no recalculados en pantalla).
- `jButton4` "Cerrar": vacío (sin acción explícita; se cierra con Escape o X de la ventana, `DISPOSE_ON_CLOSE`).

**Funcionalidades atómicas:**
1. Al abrir, limpia la tabla y consulta la factura/anulación por `id`, poblando una fila por cada línea de producto.
2. Cada fila muestra producto, cantidad, precio unitario, categoría de IVA, descuento y total de línea.
3. Los campos `subtotal_`, `totaliva_`, `total_` y `TCLIENTE` se sobrescriben en cada fila leída (quedan con el valor de la última fila procesada, ya que la consulta repite esos valores en cada registro — no hay bug visible porque son iguales en todas las filas de una misma factura).
4. **Escape** cierra el diálogo (`dispose()`).
5. No permite editar, eliminar ni reimprimir nada — es puramente informativo.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** sobre `productos, ventas, CATEGORIA_IMPUESTO, cliente` (constructor estándar) o `productos, facturas_anuladas, CATEGORIA_IMPUESTO, cliente` (constructor de `VentasAnuladas`), filtrando por `ventas.ID = <id>` / `facturas_anuladas.ID = <id>`.
- No realiza ninguna escritura.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna — es un visor puro.

**Notas / peculiaridades:** Nombre de clase y de campos escrito con errores tipográficos originales del proveedor (`detellaMovimientoCaja`); se preserva tal cual aparece en el código fuente para trazabilidad.

---

### EditarVentas.java — Editar ventas (buscador de facturas — VERSIÓN ROTA/INCOMPLETA en producción)

**Propósito:** Pantalla para buscar facturas históricas por número o por rango de fechas, pensada para luego reabrirlas en modo edición. **Es la versión actualmente enlazada al menú**, pero su función de "ver detalle y editar" está incompleta/deshabilitada — ver puntos 3 y 4 de funcionalidades.

**Cómo se invoca / navegación:** `JInternalFrame`. Se abre desde `MenuPrincipal.java`, opción de menú **"Editar ventas"**: `new EditarVentas("Menunew")`. No es modal (es una ventana interna del escritorio). El parámetro de texto (`au`) solo se usa para saber a qué pantalla volver al cerrar ("Menunew").

**Componentes de interfaz:**
- `EDITAR_VENTAS` (JTable, filtrable con `TableRowSorter trsfiltro`): columnas **NUMERO FACTURA, FECHA, TOTAL FACTURA, CLIENTE, VENDEDOR**.
- `BUSCAR` (JTextField): filtra `EDITAR_VENTAS` por columnas 0-3.
- `fecha_i` / `fecha_i1` (`DateChooserCombo`): etiquetas "Fecha Inicial:"/"Fecha Final:" — presentes visualmente pero **sin efecto** (ver punto 2).
- `fecha` (DateChooserCombo oculto): fecha de sistema usada para inserciones (no llega a usarse realmente porque esta clase no escribe en BD).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, `CargarVentas()` lista solo las facturas cuya fecha coincide con la **fecha máxima registrada** en toda la tabla `ventas` (`fecha = (SELECT MAX(FECHA) from ventas)`) — es decir, únicamente las ventas del último día en que se vendió algo en el sistema, no todo el histórico.
2. Los selectores `fecha_i`/`fecha_i1` ("Fecha Inicial"/"Fecha Final") están en la interfaz pero sus manejadores `fecha_iOnCommit`/`fecha_i1OnCommit` están completamente comentados: **cambiar las fechas no filtra ni recarga nada**.
3. Escribir en `BUSCAR` filtra en vivo las filas visibles de `EDITAR_VENTAS` (columnas 0-3) por expresión regular.
4. Al presionar **Enter** en `BUSCAR` con texto no vacío: `verificarVenta(texto)` comprueba que exista esa factura en `ventas`; si existe, llama a `LISTARVENTA(id)`; si no, simplemente re-selecciona el texto escrito (sin mensaje de error). **`LISTARVENTA` no hace nada visible**: ejecuta la consulta a BD pero todas las líneas que deberían llenar una tabla de detalle, los totales y el nombre de cliente están comentadas (`//`). No existe ninguna tabla de detalle visible en esta clase para mostrarlas de todas formas.
5. Doble clic sobre una fila de `EDITAR_VENTAS`: obtiene el número de factura de la fila y llama a `LISTARVENTA(sudid)` — mismo resultado nulo que el punto 4.
6. **F5**: refoca y limpia visualmente `BUSCAR`.
7. **Escape**, o cierre normal: `cerrar()` — si `au.equals("Menunew")`, vuelve al menú principal.
8. **No existe ningún botón ni mecanismo para realmente reabrir la factura en la pantalla de Ventas para editarla.** La funcionalidad de "editar" que da nombre a la pantalla no está implementada en esta versión.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `ventas, usuarios` (listado inicial, restringido a la fecha máxima).
- **SELECT** `productos, ventas, CATEGORIA_IMPUESTO, cliente` (en `LISTARVENTA`, resultado descartado por el código comentado).
- **SELECT** `VENTAS` (`verificarVenta`, solo existencia).
- No se realiza ninguna escritura desde esta pantalla (los métodos `EliminarVenta`, `EliminarMovimientoCaja`, `facturaAnulada`, `guardarMovimientoInventario`, `DevolucionAInventario_` están **definidos pero nunca invocados** desde ningún botón o evento — código muerto dentro de la clase).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna activa (la pantalla es, en la práctica, un visor incompleto de facturas del último día).

**Notas / peculiaridades:**
- **Bug/funcionalidad incompleta confirmada**: esta es la clase realmente enlazada en el menú "Editar ventas" de `MenuPrincipal`, pero no permite ver el detalle de una factura ni reabrirla para editar — el método que debería hacerlo existe, pero su cuerpo fue comentado y nunca se restauró.
- La implementación correcta y completa de esta misma pantalla existe en el archivo hermano **`EditarVentas1.java`**, que sin embargo **no está enlazado a ningún menú** (ver sección siguiente). Para la migración a Regenta se recomienda tomar como referencia de comportamiento **`EditarVentas1.java`**, no `EditarVentas.java`.
- Contiene métodos muertos sin ninguna llamada (`verificarVenta` sí se usa; pero `facturaAnulada`, `EliminarMovimientoCaja`, `EliminarVenta`, `DevolucionAInventario_`, `guardarMovimientoInventario` no se usan en absoluto dentro de este archivo).

---

### EditarVentas1.java — Editar ventas (VERSIÓN COMPLETA — código muerto, no enlazado a ningún menú)

**Propósito:** Misma intención que `EditarVentas.java` (buscar facturas y reabrirlas para editarlas), pero con la funcionalidad de detalle y de reapertura **sí implementada**. Representa, muy probablemente, la versión que el sistema debería usar.

**Cómo se invoca / navegación:** Idéntica forma de constructor (`EditarVentas1(String au)`), pero **nunca se instancia** en ningún punto del código (`grep` de `new EditarVentas1(` no devuelve resultados fuera del propio archivo) — no hay ninguna opción de menú, botón ni atajo que abra esta clase. Se documenta como la especificación de comportamiento correcto a replicar.

**Diferencias explícitas con `EditarVentas.java`:**
1. La tabla de facturas (`EDITAR_VENTAS`) pierde la columna "CLIENTE" (queda **NUMERO FACTURA, FECHA, TOTAL FACTURA, VENDEDOR**).
2. `CargarVentas()` **ya no restringe por fecha máxima**: lista **todas** las ventas (`SELECT ventas.ID,FECHA,TOTAL_PAGAR,upper(usuarios.NOMBREs_apellido) FROM ventas,usuarios WHERE ventas.ID_USUARIO = usuarios.ID_USUARIO GROUP BY ventas.ID,FECHA ORDER BY FECHA`, orden ascendente, sin el filtro `fecha = MAX(FECHA)` que tenía `EditarVentas.java`).
3. Se agrega un panel "Detalle de la venta" con una **segunda tabla `VENTAS`** (PRODUCTO, CANTIDAD, VALOR UNITARIO, IVA, DESCUENTO, TOTAL) y las etiquetas `subtotal_`, `totaliva_`, `total_`, `TCLIENTE`, `n_factura` — todas estas sí se llenan de verdad, porque en esta versión el cuerpo de `LISTARVENTA` **no está comentado**: agrega cada línea a la tabla de detalle y actualiza subtotal/IVA/total/cliente/número de factura reales.
4. Se eliminan los selectores de rango de fechas (`fecha_i`/`fecha_i1`, "Fecha Inicial"/"Fecha Final") — esa función fue removida por completo en lugar de dejarla rota.
5. Se agregan tres botones: **"Cerrar"** (`jButton4`, cierra la pantalla), **"Limpiar"** (`jButton5`, llama a `limpiar()`), **"Editar Venta"** (`jButton6`, acción nueva clave):
   - Si no hay ninguna factura seleccionada (`sudid` vacío): muestra el error *"Debe seleccionar una venta para ser editada."*.
   - Si hay una seleccionada: si **no** existe ya una instancia de la pantalla "Ventas" abierta, crea una nueva con `new Ventas(sudid)` (constructor de edición), la añade al escritorio maximizada y la muestra, ocultando el panel de menú lateral (`MenuPrincipal.MENU_PANEL.setVisible(false)`); si **ya** existe una instancia de "Ventas" abierta, en vez de crear otra, llama a `MenuPrincipal.ActivarFrame("Ventas","EditarVentas",""+sudid,"","","")` para reactivarla y (según el despachador central de `MenuPrincipal`) cargarle esa factura.
6. `BUSCARKeyTyped`: al presionar Enter, en esta versión **solo transfiere el foco** al siguiente componente (ya no intenta cargar la factura tecleada directamente como hacía `EditarVentas.java` con `verificarVenta`/`LISTARVENTA`); la búsqueda por número ahora depende de seleccionar la fila en la tabla y pulsar "Editar Venta".
7. Se eliminan del código los métodos que en `EditarVentas.java` quedaban sin uso: `verificarVenta`, `facturaAnulada`, `EliminarMovimientoCaja`, `EliminarVenta`, `DevolucionAInventario_`, `guardarMovimientoInventario` — limpieza de código muerto.
8. El campo `BUSCAR` pasa de `public` a `private`.

**Componentes de interfaz adicionales (ver diferencias):** `jPanel1` ("Detalle de la venta"), `VENTAS` (tabla de detalle), `total_`/`totaliva_`/`subtotal_` (JLabel), `jButton4`/`jButton5`/`jButton6`, `jPanel2` con `TCLIENTE`/`n_factura`.

**Funcionalidades atómicas (las que SÍ cambian respecto a `EditarVentas.java`):**
1. Doble clic en una fila de `EDITAR_VENTAS`, o seleccionarla y teclear Enter en `BUSCAR` (tras escribir), carga de verdad el detalle completo de esa factura en la tabla `VENTAS` inferior y actualiza cliente/número/subtotal/IVA/total.
2. Botón "Editar Venta": abre (o reutiliza) la pantalla `Ventas` ya posicionada sobre esa factura, lista para modificarla (ver punto 5 de las diferencias).
3. Botón "Limpiar": recarga el listado completo de facturas y limpia la tabla de detalle.
4. Botón "Cerrar": cierra la pantalla y regresa al menú.

**Datos / tablas de BD leídas o escritas:** Las mismas consultas de lectura que `EditarVentas.java` (`ventas`, `usuarios`, `productos`, `CATEGORIA_IMPUESTO`, `cliente`), ahora sin la restricción de fecha máxima y con los resultados realmente usados. No escribe en BD (la escritura ocurre en la pantalla `Ventas` a la que delega).

**Reportes generados:** Ninguno (delega en `Ventas.java` tras pulsar "Editar Venta").

**Reglas de negocio / validaciones no obvias:** Exige seleccionar una factura antes de permitir editar.

**Notas / peculiaridades:**
- **Código muerto pero funcionalmente correcto**: para la migración a Regenta, el comportamiento de referencia de "Editar Ventas" debe tomarse de este archivo (listar TODO el histórico, mostrar detalle, botón "Editar Venta" que reabre `Ventas(sudid)`), no del `EditarVentas.java` realmente enlazado hoy al menú, que está incompleto.
- Queda abierta la duda funcional (a resolver con el negocio) de si el comportamiento deseado es permitir editar factura de *cualquier fecha* (como aquí) o solo las del día actual/último (como en la versión en producción) — es una decisión de producto, no solo técnica.

---

### VentasDiarias.java — Detalle de Ventas / Cuentas por Cobrar (listado y filtro de ventas por fecha/cliente)

**Propósito:** Pantalla de consulta que lista las ventas registradas (facturas de contado y a débito), permite filtrar por cliente o por rango de fechas, ver el total acumulado, consultar el detalle de cada una y reimprimirlas/exportarlas en distintos formatos.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `VentasDiarias(String aux)` (el parámetro no se usa para lógica, solo existe en la firma). Se usa como ventana de "Cuentas por Cobrar"/"Detalle de Ventas" invocada desde otras pantallas de clientes/informes (no se abre desde las pantallas de `Ventas`, sino al revés: `DetalleDeVenta` es invocado desde aquí).

**Componentes de interfaz:**
- `CLIENTES` (JTable, columnas **NUMERO FACTURA, CC / NIT, NOMBRES / RAZON SOCIAL, FECHA, TOTAL, ESTADO**; selección múltiple por rango). Menú contextual (`jPopupMenu1`): *Detallar Venta*, *Imprimir factura de venta (Tiquete)*, *Imprimir factura de venta (Carta)*, *PDF factura de venta (Tiquete)*, *PDF factura de venta (Carta)*.
- `BUSCAR` (JTextField, sin filtro en vivo implementado — su `KeyReleased` está vacío).
- `TOTAL_` (JLabel): suma de la columna TOTAL de las filas cargadas.
- Botones: `jButton4` "Filtrar por Clientes" (abre `buscarClientes`), `jButton5` "Filtrar Entre Fechas" (abre `fechas`), `jButton2` "Limpiar", `jButton3` "Cerrar".
- `jTabbedPane2` con una única pestaña "LISTA DE VENTAS".

**Funcionalidades atómicas (numeradas):**
1. Al abrir/limpiar, calcula automáticamente la **fecha máxima registrada** en `ventas` (`MaxFechaCaja()`) y carga únicamente las ventas de ese día (`cargarCuentas(true)`), uniendo dos subconsultas: ventas sin registro en `ventas_debito` para esa fecha (columna ESTADO = "PAGO") y ventas que sí están en `ventas_debito` (columna ESTADO = el estado real, p.ej. "DEBE").
2. `cargarCliente(idCliente)`: filtra solo por cliente (`cargarCuentas(false)`), combinando también pagadas y a débito para ese cliente.
3. `informeFechas(fechaInicial, fechaFinal)`: filtra por rango de fechas (con o sin cliente adicional, 4 combinaciones de consulta SQL distintas según haya o no cliente y haya o no fecha final).
4. Doble clic en una fila de `CLIENTES`, o menú "Detallar Venta": abre `DetalleDeVenta` (modal) con el número de factura de la fila seleccionada.
5. Menú "Imprimir factura de venta (Tiquete)": para cada fila seleccionada (soporta selección múltiple), imprime el reporte **"CUENTAS/Factura_venta"** en la impresora térmica configurada.
6. Menú "Imprimir factura de venta (Carta)": igual pero con el reporte **"CUENTAS/Factura_venta_Carta"** en la impresora de tinta normal (`IMPRESORA_TINTA`).
7. Menú "PDF factura de venta (Tiquete)": exporta a PDF el reporte "CUENTAS/Factura_venta" con nombre de archivo aleatorio (`randoNumero`) en la carpeta `DIRECTORIO_EXPORTAR`.
8. Menú "PDF factura de venta (Carta)": igual con "CUENTAS/Factura_venta_Carta".
9. Botón "Filtrar por Clientes": abre el buscador de clientes modal `buscarClientes`.
10. Botón "Filtrar Entre Fechas": abre el selector modal `fechas`.
11. Botón "Limpiar": recarga a la fecha máxima y limpia filtros de cliente/fecha.
12. Botón "Cerrar" / **Escape**: `cerrar()` → `MenuPrincipal.ActivarFrame("Menunew","CuentasPorCobrar","","","","")`.
13. **F5**: refoca y limpia visualmente `BUSCAR` (sin recargar datos).

**Datos / tablas de BD leídas o escritas:**
- **SELECT** sobre `ventas`, `cliente`, `ventas_debito` en múltiples variantes (todas-por-fecha-máxima / por-cliente / por-rango-fechas / por-cliente-y-rango), siempre con `UNION` entre "ventas sin deuda activa" (ESTADO literal `'PAGO'`) y "ventas con fila en `ventas_debito`" (ESTADO real de esa tabla).
- **SELECT** `MAX(fecha) FROM ventas` para la fecha por defecto.
- No realiza ninguna escritura.

**Reportes generados:** `CUENTAS/Factura_venta` (impresión térmica y PDF) y `CUENTAS/Factura_venta_Carta` (impresión en tinta y PDF).

**Reglas de negocio / validaciones no obvias:**
- Una venta se considera "PAGO" (contado) si **no** tiene fila asociada en `ventas_debito`; si la tiene, su estado visible es el que diga esa tabla (típicamente "DEBE").
- Por defecto solo muestra el día de la última venta registrada en todo el sistema, no el día actual del reloj — si no se ha vendido nada "hoy" pero sí "ayer", al abrir la pantalla mostrará las de "ayer".

**Notas / peculiaridades:**
- Contiene un bloque grande de código comentado (`cargarCliente` con firma distinta, ids de reporte alternativos `fce`/`ice`/`fcec`/`ifcec`) que sugiere una versión anterior más genérica de impresión de cuentas por cobrar, reemplazada por el menú contextual actual.
- El campo `BUSCAR` existe en la interfaz pero no filtra nada (su `keyReleased` está vacío) — posible funcionalidad planeada y no terminada.

---

### VentasGuardadas.java — Ventas Guardadas (carritos puestos en espera)

**Propósito:** Pantalla para listar las ventas que el cajero guardó para después (ver `Ventas.guardarVentas()` sin argumentos), ver su detalle y cargarlas de nuevo en la pantalla de Ventas para continuar y facturar.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `VentasGuardadas(String au)`. Se referencia como comentario/código muerto desde `Ventas3.java` (`b_ventasguardadasActionPerformed`, deshabilitado) — en el flujo vivo actual de `Ventas.java`/`Ventas1`/`Ventas2` **no hay ningún botón que abra esta pantalla** (solo queda alcanzable, presumiblemente, desde el menú principal `MenuPrincipal`, fuera del alcance de este grupo de archivos). Desde aquí sí se abre `Ventas` (nueva instancia o reactivada) para continuar la venta.

**Componentes de interfaz:**
- `CLIENTES` (JTable): columnas **REFERENCIA (SUDID), CC / NIT, NOMBRES, FECHA** — lista de ventas guardadas agrupadas por `SUDID`.
- `VENTAS` (JTable, panel "Detalles de la venta"): columnas **PRODUCTO, CANTIDAD, VALOR UNITARIO, IVA, DESCUENTO, TOTAL** — detalle de la venta guardada seleccionada.
- `TCLIENTE` (JLabel), `n_factura` (JLabel, siempre "0" — no aplica a una venta guardada), `subtotal_`/`totaliva_`/`total_` (JLabel).
- `BUSCAR` (JTextField): filtra `CLIENTES` por columnas 1-4.
- Botones: `jButton4` "Cerrar", `jButton5` "Limpiar", `jButton6` "Cargar".

**Funcionalidades atómicas (numeradas):**
1. Al abrir/limpiar, `CargarVentas()` lista todas las ventas guardadas agrupadas por `SUDID`, con cliente y fecha.
2. Escribir en `BUSCAR` filtra en vivo la lista por referencia/cédula/nombre/fecha.
3. Doble clic sobre una fila de `CLIENTES`: carga el detalle completo de esa venta guardada en la tabla `VENTAS` inferior (`LISTARVENTA`), recalcula subtotal/IVA/total, y guarda en memoria `sudid`, `cc_nit_cliente`, `name_nit_cliente` para el botón "Cargar".
4. `calcularTotales()` (al cargar detalle): por cada línea, calcula precio vigente del producto (`precioVenta`, **no** el precio guardado al momento de guardar la venta — es decir, si el precio del producto cambió después de guardarse, se usa el precio **actual**), aplica IVA de la categoría y descuento, y suma al subtotal/IVA/total. El bloque que limitaría la cantidad a la existencia real está comentado (no se valida contra stock al recalcular).
5. Botón "Cargar" (o doble clic + Cargar): si no se seleccionó ninguna venta guardada (`sudid==0`), muestra error *"Debe seleccionar una venta guardada."*; si hay seleccionada: si no existe ya una instancia de "Ventas" abierta, crea `new Ventas()`, la agrega maximizada, llama a `VENTAS.cargarVentaGuardada(sudid, cc_nit_cliente, name_nit_cliente)` para poblar el carrito, y la muestra, ocultando el panel de menú; si ya existe una instancia abierta, en vez de crear otra, reactiva vía `MenuPrincipal.ActivarFrame("Ventas","VentasGuardadas",""+sudid,cc_nit_cliente,name_nit_cliente,"")`.
6. Botón "Limpiar": recarga el listado completo y limpia la selección/detalle.
7. Botón "Cerrar" / **Escape**: regresa al menú principal (`MenuPrincipal.ActivarFrame("Menunew",...)`) si `au.equals("Menunew")`.
8. **F5**: refoca `BUSCAR`.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `ventasguardadas` + `cliente` (listado agrupado por `SUDID`).
- **SELECT** `productos`, `ventasguardadas`, `CATEGORIA_IMPUESTO` (detalle de una venta guardada).
- **SELECT** `productos` (precio vigente, `precioVenta`), `stockproductos`+`productos` (existencia, aunque no se usa para bloquear).
- No escribe nada esta pantalla directamente; la escritura real (inserción en `ventas`, borrado de `VENTASGUARDADAS`, etc.) ocurre dentro de `Ventas.cargarVentaGuardada` + el flujo normal de venta una vez cargada.

**Reportes generados:** Ninguno desde esta pantalla.

**Reglas de negocio / validaciones no obvias:**
- El total mostrado al recalcular una venta guardada usa el **precio actual** del producto, no el que tenía cuando se guardó — una venta guardada hace tiempo puede mostrar un total distinto al que tenía originalmente si los precios cambiaron entre tanto.
- No bloquea cargar una venta guardada cuyos productos ya no tengan existencias suficientes (la validación correspondiente está comentada).

**Notas / peculiaridades:** Mismo patrón "crear si no existe, si no activar" para evitar duplicar ventanas de `Ventas`, consistente con el resto del sistema.

---

### VentasAnuladas.java — Ventas Anuladas (histórico de facturas anuladas)

**Propósito:** Pantalla de solo consulta que lista las facturas anuladas (archivadas en `facturas_anuladas` al momento de anular, ver `Ventas.anularFactura`/`GuardarFacturasAnuladas`) y permite ver su detalle.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `VentasAnuladas(String aux)` (parámetro no usado en lógica). Es el padre que instancia `DetalleDeVenta` con la variante "anuladas" (`LISTARVENTAANULADAS`).

**Componentes de interfaz:**
- `CLIENTES` (JTable): columnas **NUMERO FACTURA, CC / NIT, NOMBRES / RAZON SOCIAL, FECHA, TOTAL**. Menú contextual: *Detallar Venta*.
- `BUSCAR` (JTextField, sin filtro en vivo implementado, igual que en `VentasDiarias.java`).
- `TOTAL_` (JLabel, declarado pero no se calcula/usa en ningún método — queda siempre en 0, a diferencia de `VentasDiarias.java` que sí lo calcula).
- `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. Al abrir/limpiar, `cargarCuentas()` lista **todas** las facturas anuladas (`facturas_anuladas` + `cliente`), agrupadas por ID, ordenadas por fecha — sin restricción de fecha ni de cliente (a diferencia de `VentasDiarias.java`, aquí no hay filtros).
2. Doble clic en una fila, o menú "Detallar Venta": abre `DetalleDeVenta` (constructor `VentasAnuladas`) con el número de factura anulada, que internamente consulta `facturas_anuladas` en vez de `ventas`.
3. Botón "Cerrar" / **Escape**: regresa a `MenuPrincipal.ActivarFrame("Menunew","CuentasPorCobrar","","","","")`.
4. **F5**: refoca `BUSCAR` (sin recargar nada).

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `facturas_anuladas` + `cliente`, agrupado por `facturas_anuladas.ID`.
- No realiza ninguna escritura.

**Reportes generados:** Ninguno (la impresión que existía en `VentasDiarias.java` no está presente aquí).

**Reglas de negocio / validaciones no obvias:** Ninguna — listado histórico simple, sin filtros de fecha/cliente ni reimpresión.

**Notas / peculiaridades:**
- `TOTAL_`, `jLabel7` ("TOTAL CUENTAS POR COBRAR"), `jTabbedPane2`, `fecha` (DateChooserCombo) y varios campos quedaron declarados/inicializados por el generador de formularios pero sin usar realmente (vestigios de copiar `VentasDiarias.java` como base y simplificarlo, sin terminar la limpieza).
- Al archivarse una factura anulada en `facturas_anuladas`, esta pantalla es la única vía de consulta de ese histórico dentro del grupo de Ventas.

---

### Cargarfactura.java — Diálogo "Cargar Factura" (buscar y cargar una factura para editar/anular)

**Propósito:** Diálogo modal invocado desde el botón "Cargar Factura" de la pantalla de Ventas; permite buscar una factura ya emitida (por número, o por rango de fechas) y, al seleccionarla, la carga en la pantalla de Ventas que lo invocó (modo edición/anulación).

**Cómo se invoca / navegación:** `JDialog` modal con **tres constructores sobrecargados**: `Cargarfactura(Ventas parent, boolean modal)`, `Cargarfactura(Ventas1 parent, boolean modal)`, `Cargarfactura(Ventas2 parent, boolean modal)` — guarda la referencia al padre correspondiente (`Obj`, `Obj1` u `Obj2`) y al cerrar invoca el método `LISTARVENTA` de ese padre específico. **No existe constructor para `Ventas3`** (coherente con que `Ventas3` es código muerto). Se invoca únicamente cuando el carrito de la pantalla de Ventas está vacío (`BcargarFacturaActionPerformed`).

**Componentes de interfaz:**
- `VENTAS` (JTable, con `TableRowSorter`): columnas **NUMERO FACTURA, FECHA, TOTAL FACTURA, CLIENTE**.
- `BUSCAR` (JTextField): búsqueda directa por número de factura o filtro de texto sobre las columnas 0-4.
- `fecha_i` / `fecha_i1` (`DateChooserCombo`, no editables manualmente — `setFocusable(false)`, solo mediante el selector): "Fecha Inicial"/"Fecha Final".
- `mas` (JButton oculto en el layout visible, sin acción implementada).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga por defecto **todas** las facturas de la fecha máxima registrada en `ventas` (`fecha = (SELECT MAX(FECHA) from ventas)`), ordenadas por fecha descendente.
2. Seleccionar `fecha_i` y/o `fecha_i1`: recarga la lista filtrando por rango de fechas (`fecha_i <= FECHA <= fecha_i1`) sobre todas las facturas (ya no solo la fecha máxima).
3. Escribir en `BUSCAR` filtra en vivo la lista visible por número/fecha/total/cliente.
4. Doble clic sobre una fila: llama a `LISTARVENTA(numero)` en la pantalla de Ventas padre correspondiente (`Obj`/`Obj1`/`Obj2`) y cierra el diálogo.
5. Presionar **Enter** en `BUSCAR` con texto no vacío: `VerificarFactura(texto)` comprueba que exista en `ventas`; si existe, carga esa factura en el padre y cierra el diálogo; si no existe, muestra el error *"La factura que intenta cargar no existe."* y mantiene el foco en `BUSCAR`.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `ventas` + `cliente` (listado por fecha máxima o por rango).
- **SELECT** `VENTAS` (verificación de existencia por ID, en `VerificarFactura`).
- No escribe nada; la escritura (si corresponde) ocurre después, dentro de `Ventas.LISTARVENTA`, que esta clase invoca sobre el padre.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna adicional a la validación de existencia de la factura.

**Notas / peculiaridades:** El patrón de "tres constructores con tres campos `Obj`/`Obj1`/`Obj2` y luego `if(Obj!=null) ... else if(Obj1!=null) ...`" se repite en todos los diálogos compartidos entre las tres cajas (`CargarCotizacion`, `Cantidad`, `Descuento`, `Pago`, etc.) — es el mecanismo elegido por el proveedor en vez de usar una interfaz/clase común para las tres pantallas de venta.

---

### CargarCotizacion.java — Diálogo "Cargar Cotización" (buscar y convertir una cotización en venta)

**Propósito:** Diálogo modal, análogo a `Cargarfactura.java` pero para la tabla `cotizacion`: busca una cotización guardada y, al seleccionarla, la carga como carrito en la pantalla de Ventas (vía `Ventas.ListarCotizacion`).

**Cómo se invoca / navegación:** `JDialog` modal, mismos tres constructores por pantalla (`Ventas`, `Ventas1`, `Ventas2`), invocado desde el botón "Cargar Cotización" (`BcargarCotizacionActionPerformed`), solo habilitado cuando el carrito está vacío.

**Componentes de interfaz:** Idénticos a `Cargarfactura.java` salvo que la tabla se titula **NUMERO COTIZACION, FECHA, TOTAL COTIZACION, CLIENTE** y el límite de carga es de **100** registros más recientes (`LIMIT 100`).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga las últimas 100 cotizaciones (sin restricción de fecha, a diferencia de `Cargarfactura.java` que por defecto solo muestra la fecha máxima).
2. Seleccionar rango de fechas (`fecha_i`/`fecha_i1`) recarga filtrando por ese rango (manteniendo el límite de 100).
3. Filtro en vivo por texto en `BUSCAR`.
4. Doble clic sobre una fila: llama a `ListarCotizacion(numero)` sobre la pantalla de Ventas padre y cierra el diálogo.
5. **Enter** en `BUSCAR`: valida existencia en `cotizacion` (`VerificarFactura`, mismo nombre de método que en `Cargarfactura.java` pero apuntando a la tabla `cotizacion`); si existe, carga y cierra; si no, muestra *"La cotizacion que intenta cargar no existe."*.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `cotizacion` + `cliente` (listado, por defecto o por rango de fechas, límite 100).
- **SELECT** `cotizacion` (verificación de existencia por ID).
- No escribe nada.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** El límite de 100 resultados puede ocultar cotizaciones antiguas si no se usa el filtro de fechas — a tener en cuenta al migrar (¿paginar en Regenta en vez de truncar?).

**Notas / peculiaridades:** Misma estructura de tres constructores que `Cargarfactura.java`; igual ausencia de soporte para `Ventas3`.

---

### MovimientoCajaVentas.java — Diálogo "Movimiento de Caja" (entradas/salidas manuales de efectivo)

**Propósito:** Diálogo modal para registrar manualmente un movimiento de caja (entrada o salida de efectivo) no asociado a una venta — por ejemplo, retiros, aportes de caja menor, correcciones.

**Cómo se invoca / navegación:** `JDialog` modal, constructores para `Ventas`, `Ventas1`, `Ventas2` (de nuevo, sin `Ventas3`). Se invoca desde el botón "Movi. Caja" (`BanularFactura1`) de la pantalla de Ventas.

**Componentes de interfaz:**
- `Razon` (JComboBox, oculto por el diseño final pero presente): "(Entrada) Efecivo" / "(Salida) Efectivo".
- `Rentrada` / `Rsalida` (JRadioButton, agrupados): seleccionan el tipo de movimiento; por defecto `Rentrada` seleccionado.
- `total` (JTextField): monto del movimiento, formateado como moneda al perder el foco.
- `descripcion` (JTextArea): motivo/descripción obligatoria.
- `jButton5` "Guardar".
- `fecha` (DateChooserCombo, oculto, fecha de sistema para el registro).

**Funcionalidades atómicas (numeradas):**
1. Clic en `Rentrada`/`Rsalida`: pasa el foco a `total` y selecciona todo su texto.
2. Al ganar foco `total`: muestra el valor como número editable (sin formato de moneda); al perderlo, lo formatea como moneda (o "0" si está vacío).
3. Solo se permiten dígitos en `total` (otros caracteres son consumidos/ignorados), salvo Enter que transfiere el foco.
4. Botón "Guardar": valida en orden — si `total` está vacío → error *"Falta total del movimiento de caja."* y foco a `total`; si `descripcion` está vacía → error *"Falta descripción del movimiento de caja."* y foco a `descripcion`; si el valor parseado es `<=0` → mismo error de total. Si todo es válido, pide confirmación *"¿Desea agregar un movimiento de '(Entrada/Salida) Efectivo' a la caja?"*; al confirmar, si es "Salida" invierte el signo del monto (`total_ * -1`) y hace **INSERT** en `caja` con `IDMOVIMIENTO` = "5" (Entrada) o "6" (Salida) según catálogo `movimiento`, `ID_FATURA` = `"MC"+contador`, estado `ABIERTO`, `SUM='SI'`, `FACTURA_REMISION='true'`, `ID_TIPO_PAGO='0'`; muestra mensaje de éxito, limpia el formulario y cierra el diálogo.
5. El número de referencia del movimiento (`"MC"+id`) se obtiene de un contador propio `configuracion.CENTRADASALIDA`, que se autoincrementa en cada guardado (`maxEntradaSalida()`), distinto del contador de facturas/remisiones/cotizaciones.
6. `internalFrameClosing`: si la ventana se cierra como `JInternalFrame` (interfaz implementada pero el diálogo real es un `JDialog`, método presente por si se reutiliza como interna), se descarta (`dispose()`).

**Datos / tablas de BD leídas o escritas:**
- **INSERT** en `caja` (movimiento manual).
- **SELECT** `MAX(ID) FROM CAJA WHERE ESTADO='CERRADO'` (siguiente id de movimiento, `maxIdMovimiento`).
- **SELECT/UPDATE** `configuracion.CENTRADASALIDA` (contador de referencia "MC").

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Las salidas se registran con el monto en negativo dentro de `caja.TOTAL_NETO`, de forma que el cálculo de totales de caja (en otras pantallas de cierre de caja) pueda sumarlas directamente sin lógica especial de signo.

**Notas / peculiaridades:** Aunque implementa `InternalFrameListener`, la clase extiende `JDialog`, no `JInternalFrame` — esa interfaz parece vestigial/copiada de otra clase.

---

### cambio.java — Diálogo de Cambio/Vuelto

**Propósito:** Diálogo modal simple e informativo que muestra, al finalizar una venta, el total a pagar, lo entregado por el cliente y el cambio/vuelto a devolver.

**Cómo se invoca / navegación:** `JDialog` modal, constructor único `cambio(boolean modal, double total_pagar, double entregado)`. Se invoca desde `Ventas.imprimirTikest(...)` justo después de imprimir el tiquete, con los montos ya ajustados por descuento por puntos si aplica (ver `Ventas.java`, puntos de "Reglas de negocio").

**Componentes de interfaz:**
- `total` (JLabel): total a pagar.
- `entregado` (JLabel): monto entregado por el cliente.
- `cambio` (JLabel): vuelto a entregar.
- `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. Al construirse, calcula `cambio_ = entregado - total_pagar`; si el entregado es menor al total (venta a débito/parcial), muestra cambio **0** en vez de un valor negativo.
2. El foco inicial se posiciona en el botón "Cerrar" (pensado para cerrarse con solo presionar Enter/Espacio tras ver el vuelto).
3. Botón "Cerrar": `dispose()`.

**Datos / tablas de BD leídas o escritas:** Ninguna — es puramente de presentación con los valores que recibe por parámetro.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** El cambio nunca se muestra negativo, aunque matemáticamente lo fuera (se trunca a 0 en pantalla cuando `entregado < total_pagar`).

**Notas / peculiaridades:** Clase deliberadamente mínima; toda la lógica de cálculo de cuánto mostrar ya viene resuelta desde `Ventas.imprimirTikest` antes de instanciar este diálogo.

---

### calculado.java — Diálogo "Calculadora de Denominaciones" (conteo de efectivo por billete/moneda)

**Propósito:** Diálogo modal que permite contar el efectivo físico de la caja introduciendo la **cantidad de billetes/monedas por denominación** (no el total a mano), calculando automáticamente el valor de cada denominación y el total general. **No pertenece al flujo de venta (`Ventas`) sino al de cierre de caja** (`cerrarCajaCalculado`); se documenta aquí porque el enunciado lo incluyó en este grupo de archivos, pero funcionalmente es una dependencia cruzada con el módulo de Caja.

**Cómo se invoca / navegación:** `JDialog` modal, constructor único `calculado(cerrarCajaCalculado obj, boolean modal)` — el único padre soportado es `cerrarCajaCalculado` (pantalla de cierre de caja), **no** se instancia desde ninguna pantalla de `Ventas`.

**Componentes de interfaz:**
- Once pares de campos `C1..C11` (cantidad, JTextField editable) / `TF1..TF11` (subtotal de esa denominación, JTextField no editable), correspondientes a las denominaciones colombianas: **$100.000, $50.000, $20.000, $10.000, $5.000, $2.000, $1.000, $500, $200, $100, $50**.
- `total_` (JTextField no editable): suma de todas las denominaciones.
- `jButton1` "Guardar", `jButton2` "Ceros" (reinicia todos los campos a 0).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, todos los campos de cantidad y subtotal se inicializan en "0" (formato moneda) y el foco se posiciona en `C1`.
2. Al ganar foco cualquier `Cx`, se muestra el número editable (sin separadores) y se selecciona todo el texto.
3. Al perder foco cualquier `Cx`: se formatea como moneda (o "0" si queda vacío) y se recalcula el total (`calcularTotal()`).
4. Solo se aceptan dígitos en cada `Cx` (y la coma como separador decimal, carácter ASCII 44).
5. **Tab/Enter** avanza de `C1` a `C11` en orden (campo `setNextFocusableComponent` encadenado) y finalmente a "Guardar".
6. `calcularTotal()`: multiplica la cantidad de cada denominación por su valor nominal (100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50 respectivamente), escribe el subtotal en el `TFx` correspondiente y acumula el total general en `total_`.
7. Botón "Ceros": reinicia todos los campos a 0 (equivalente a volver a abrir el diálogo).
8. Botón "Guardar": llama a `obj.calculado(total)` (pasa el total calculado al padre `cerrarCajaCalculado`), devuelve el foco al campo `TC1` del padre y cierra el diálogo.
9. Al cerrar la ventana (botón X o `windowClosing`/`windowClosed`): también devuelve el foco a `obj.TC1`, aunque no se haya pulsado "Guardar" (es decir, cerrar sin guardar igualmente reenfoca el campo del padre, pero sin transferirle ningún total).

**Datos / tablas de BD leídas o escritas:** Ninguna directamente — el total calculado se entrega al padre (`cerrarCajaCalculado`), que es quien decide qué hacer con él (fuera del alcance de este grupo de archivos).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna de negocio de ventas; es una utilidad de aritmética de denominaciones.

**Notas / peculiaridades:** Confirmado como **dependencia cruzada con el módulo de Caja**, no con Ventas — para la migración a Regenta puede implementarse como un componente reutilizable de "conteo de efectivo" e integrarse donde corresponda (cierre de caja), no necesariamente dentro del módulo de Ventas.

---

### Descuento.java — Diálogo para ingresar el descuento de una línea de venta

**Propósito:** Diálogo modal simple para capturar el valor de descuento (en pesos, por unidad) a aplicar a la línea de producto seleccionada en el carrito.

**Cómo se invoca / navegación:** `JDialog` modal con **cinco constructores sobrecargados**: `Descuento(Ventas, boolean, String nombre_producto)`, `Descuento(Ventas1, ...)`, `Descuento(Ventas2, ...)`, `Descuento(Ventas3, ...)` (sí tiene soporte para `Ventas3`, a diferencia de `Pago`/`Cargarfactura`/`CargarCotizacion`/`MovimientoCajaVentas`), y `Descuento(GestionInventario, ...)` (reutilizado también desde el módulo de inventario, fuera de este grupo). Se abre vía **Ctrl+B**, botón "Descuento", doble clic en la celda "DESCUENTO" del carrito, o el ítem de menú contextual "Modificar Cantidad / Precio Venta / Descuento" cuando la columna seleccionada es la de descuento.

**Componentes de interfaz:**
- `NP` (JLabel): nombre del producto (también como tooltip).
- `pvpiva` (JTextField): valor del descuento a ingresar (solo dígitos y coma decimal).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, muestra el nombre del producto y posiciona el foco en `pvpiva` (vía el `KeyListener` agregado en el constructor).
2. Al ganar foco `pvpiva`: resalta el fondo y selecciona todo el texto.
3. Solo acepta dígitos y coma (`,`) como separador decimal; cualquier otro carácter es descartado.
4. **Enter** con texto no vacío: parsea el valor (`#,###.00`) y llama a `Obj/Obj1/Obj2/Obj3/ObjGestionInventario.Descuento(valor)` sobre la ventana padre correspondiente, devuelve el foco a `BUSCAR` de esa pantalla y cierra el diálogo.
5. **Escape**: cierra el diálogo sin aplicar ningún cambio.

**Datos / tablas de BD leídas o escritas:** Ninguna directamente — delega el cálculo y la validación en el método `Descuento(Double)` de la pantalla padre.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias (ejecutadas en el padre, p.ej. `Ventas.Descuento(Double)`):** Si el descuento ingresado es **mayor** que el precio unitario de la línea, se rechaza con el mensaje *"El descuento no puede ser mayor al precio del producto."* y no se aplica ningún cambio; si es válido, se aplica, se recalculan los totales, y si la fila editada no es la última del carrito, se mueve automáticamente la selección a la siguiente fila y se abre de nuevo este mismo diálogo para ella (permite capturar descuentos fila por fila en cadena sin tener que reabrir manualmente el diálogo cada vez).

**Notas / peculiaridades:** Es uno de los pocos diálogos que sí soporta explícitamente `Ventas3` como padre, aunque como `Ventas3` nunca se instancia, ese soporte es también código muerto en la práctica.

---

### Pago.java — Diálogo de Forma de Pago (finalización de la venta / cobro)

**Propósito:** El diálogo más grande y crítico del flujo de venta: captura la forma de pago, el monto entregado por el cliente, gestiona la redención de puntos de fidelidad, decide si imprime el tiquete y con qué tipo de impresora, y finalmente dispara la facturación real (`imprimirTikest`) sobre la pantalla de Ventas que lo invocó — o, si se abrió desde Cuentas por Cobrar/Abonos, dispara `PagarCuentas` sobre esa pantalla en su lugar.

**Cómo se invoca / navegación:** `JDialog` modal, con **seis constructores sobrecargados** según el origen: `Pago(Ventas, boolean, double total_pagar, String cc_cliente, String nombre_cliente, TestConnection BD, boolean debicontado)`, `Pago(Ventas1, ...)` (misma firma), `Pago(Ventas2, ...)` (misma firma) — **no existe constructor para `Ventas3`**, confirmando que esa variante no puede completar una venta por esta vía —, `Pago(CuentasPorCobrar, boolean, double, String, String, TestConnection)`, `Pago(Abonos, ...)`, `Pago(AbonosCuentas, ...)` (estos tres últimos, de otro módulo, sin el parámetro `debicontado`). Dentro del grupo de Ventas se abre desde `Ventas.Vender()` con el total ya calculado (y descontando lo ya pagado si es edición de una venta existente).

**Componentes de interfaz:**
- Siete botones de forma de pago (`JToggleButton`, grupo exclusivo `buttonGroup1`): **B1 "Efectivo" (F2)**, **B2 "Cheque" (F3)**, **B3 "Vale" (F4)**, **B4 "Tarjeta" (F5)**, **B5 "Gratis" (F6)**, **B6 "Debito" (F7)**, **B7 "Banco" (F8)**.
- Teclado numérico en pantalla (`JButton`): `SIETE, OCHO, NUEVE, CUATRO, CINCO, SEIS, UNO, DOS, TRES, CERO, PUNTO, MENOS, LIMPIAR("CE")` — permite digitar el monto entregado sin teclado físico (pensado para pantallas táctiles).
- `total` (JTextField no editable): total a pagar (puede variar si se redimen puntos).
- `entregado` (JTextField): monto que entrega el cliente.
- `cambio` (JTextField no editable): vuelto calculado.
- `cc` / `nombre` (JTextField no editables) y `deuda` (JTextField no editable) + sus etiquetas: visibles solo en modo "Debito".
- `tarjeta` (JLabel "Haga el pago con la tarjeta y pulse aceptar."): visible solo en modo "Tarjeta".
- `gratis` (JLabel "Sin cargo."): visible solo en modo "Gratis".
- `debito` (JLabel en rojo "Debe asignar un cliente para realizar el debito."): visible cuando se intenta pagar a débito sin cliente real asignado.
- `jpuntos`/`total_puntos`, `redimir` (JCheckBox "Redimir puntos"), `puntos_redimidos`, `jpuntos2`/`puntos_esta_venta`: bloque de fidelización, visible solo si la configuración de puntos (`PUNTOS`) está activa para el negocio.
- `imprimirTikete` (JToggleButton): decide si se imprime o no el comprobante.
- `TipoImpresora` (JToggleButton): alterna entre impresora térmica de tirilla e impresora normal de tinta.
- `debiContado` (JCheckBox "Debito - Contado"): habilita pago mixto (parte en efectivo, resto a débito) cuando B6 está seleccionado.
- `aceptar` (JButton) y `jButton3` (JButton "Cerrar"/cancelar).

**Funcionalidades atómicas (numeradas):**
1. Al abrir: si viene con `debicontado=true` (reedición de una venta que ya tenía deuda a débito activa), se preconfigura todo en modo "Debito" con el saldo pendiente, deshabilita el teclado numérico y los demás botones de pago, y si el cliente es "OCASIONAL" bloquea "Aceptar" mostrando el aviso de asignar cliente; si no, carga la deuda a la fecha (`cargarDeudaFecha`).
2. Si no viene en modo débito y el total a pagar es `>0`: habilita el teclado numérico y los 7 botones de forma de pago, selecciona "Efectivo" (B1) por defecto, y pone el foco en `entregado`.
3. Si el total a pagar es `<=0` (ej. la venta ya quedó completamente cubierta por nota crédito antes de llegar aquí): habilita directamente "Aceptar", deshabilita el resto, y muestra el aviso *"Debe hacerle una devolucion de dinero al cliente, de $0, y despues clic en aceptar."*.
4. Seleccionar **B1 Efectivo** (clic, o **F2**): habilita teclado numérico, campo `entregado` editable, limpia el monto entregado y deja "Aceptar" deshabilitado hasta que se digite un valor.
5. Seleccionar **B2 Cheque** (clic, o **F3**): fija `entregado = total`, deshabilita el teclado numérico (no aplica cambio), habilita "Aceptar" directamente.
6. Seleccionar **B3 Vale** (clic, o **F4**): igual que Cheque.
7. Seleccionar **B4 Tarjeta** (clic, o **F5**): oculta los campos de entregado/cambio, muestra el aviso de "pase la tarjeta", fija `entregado = total`, habilita "Aceptar".
8. Seleccionar **B5 Gratis** (clic, o **F6**): oculta todos los importes y el bloque de puntos, muestra "Sin cargo.", fija `entregado = total` (cambio 0), habilita "Aceptar" directamente — **particularidad**: en el manejador de teclado (Enter) este modo se trata como un `else` separado que **no** verifica si "Aceptar" está habilitado (siempre factura), mientras que el botón "Aceptar" por clic sí comparte la misma rama con los demás modos.
9. Seleccionar **B6 Debito** (clic, o **F7**): muestra los campos `cc`/`nombre`/`deuda` (prellenados con los del cliente), fija `entregado = total`, y si el cliente es "OCASIONAL" bloquea "Aceptar" y muestra el aviso en rojo; si no, lo habilita y consulta la deuda acumulada a la fecha (`cargarDeudaFecha`, suma de `ventas.TOTAL_PAGAR` de todas las ventas con `ventas_debito.ESTADO='DEBE'` de ese cliente).
10. Seleccionar **B7 Banco** (clic, o **F8**): similar a Cheque/Vale, fija `entregado=total`, habilita "Aceptar" directamente, sin mostrar campos de cliente.
11. Botones numéricos del teclado en pantalla: concatenan el dígito al valor actual de `entregado` (ignorando cualquier formato de moneda ya aplicado) y recalculan el cambio en vivo (`CALCULAR_CAMBIO`).
12. Escribir directamente en `entregado` (teclado físico): solo acepta dígitos/coma; en cada tecla soltada recalcula `cambio_ = entregado - total` (ajustado por puntos redimidos si aplica) y habilita "Aceptar" solo si el cambio resultante es `>=0` (salvo en modo Débito, donde además exige que `debiContado` esté marcado y el monto sea `>0` para habilitar, y revisa si el cliente es "Ocasional").
13. Tecla **"+" (VK_ADD)** mientras se edita `entregado`: alterna el estado de `imprimirTikete` (atajo rápido para no imprimir comprobante).
14. Checkbox `redimir` ("Redimir puntos"): si se marca, calcula cuántos puntos se pueden descontar (`total_puntos_redimidos` vs. `total_pagar_` — si los puntos cubren más que el total, solo se descuenta hasta la mitad del total `(int)total_pagar_/2`; si no, se descuentan todos los puntos disponibles) y **reduce el campo `total`** por ese valor; si se desmarca, restaura el total original. En ambos casos limpia lo digitado en `entregado` y le devuelve el foco.
15. Checkbox `debiContado` ("Debito - Contado", solo visible con B6 seleccionado): al marcarlo, habilita `entregado` para digitar un pago parcial inmediato (resto queda a débito) y deshabilita "Aceptar" hasta digitar algo; al desmarcarlo, vuelve a fijar `entregado = total` y revalida cliente/deuda como en el punto 9.
16. Toggle `imprimirTikete`: alterna entre "Imprimir Tiquete o Factura Venta" (negro) y "No Imprimir Tiquete o Factura Venta" (rojo), y persiste la preferencia en `configuracion.IMPRIMIR_TIKETE` (TRUE/FALSE) para que la próxima venta recuerde la elección; habilita/deshabilita el toggle `TipoImpresora` en consecuencia.
17. Toggle `TipoImpresora`: alterna el ícono/texto entre "Impresora Termica de Tirilla" e "Impresora Normal de Tinta" (solo cambia la preferencia visual/del objeto; quien realmente decide el formato de impresión al facturar es el booleano pasado a `imprimirTikest`/`PagarCuentas`).
18. Botón **"Aceptar"** (o **Enter**, excepto en modo "Gratis" que se maneja aparte como en el punto 8): si está habilitado, según cuál referencia de padre no sea nula (`Obj`/`Obj1`/`Obj2`/`objeto`/`objeto_abonos`/`objeto_AbonosCuentas`): si se redimieron puntos, primero descuenta los puntos del cliente (`descontarPuntos`, resta `PUNTOS` en la tabla `puntos`) y luego llama a `imprimirTikest(entregado, FORMAPAGO, descuento_puntos_$, puntos_de_esta_venta, puntos_canjeados, imprimirTikete?, PUNTOS_habilitado?, debiContado?)`; si no se redimieron, llama igual pero con descuento 0 y puntos redimidos 0; cierra el diálogo y devuelve el foco al buscador de la pantalla padre; si el cliente no es "Ocasional" (cc≠"1000") y la forma de pago no es "Debito", además acredita los puntos ganados en esta venta (`agregarPuntos`, suma en la tabla `puntos`).
19. **Escape**: cierra el diálogo sin facturar (y, si el padre es `Ventas`, regresa el foco/selección al catálogo de productos) — ojo: el código de este atajo asume que `Obj` (la referencia a `Ventas`) nunca es null, por lo que si el diálogo se abrió desde `Ventas1`/`Ventas2`/Cuentas por Cobrar y se presiona Escape, lanzaría `NullPointerException` (bug potencial, ver nota).
20. Botón "Cerrar" (`jButton3`): igual que Escape pero sin ese riesgo — primero comprueba `if(Obj != null)` antes de tocarlo.
21. `cargarPuntos()` (al construir el diálogo): lee de la BD los puntos acumulados del cliente, si el sistema tiene habilitados los puntos (`configuracion.PUNTOS`), el factor puntos-por-mil (`PUNTOSXMIL`) y el valor en pesos de cada punto (`VALORPESOSPUNTOS`), además de la preferencia guardada de impresión de tiquete; calcula cuánto valen en pesos los puntos acumulados (`puntosPlata`) y cuántos puntos generaría esta venta (`plataPuntos`, `total_pagar * PUNTOSXMIL / 1000`); muestra/oculta todo el bloque de puntos según corresponda.

**Datos / tablas de BD leídas o escritas:**
- **SELECT** `puntos` + `configuracion` (puntos acumulados, si el sistema usa puntos, factores de cálculo, preferencia de impresión) — `cargarPuntos`.
- **SELECT** `ventas` + `ventas_debito` + `cliente` (deuda acumulada a la fecha del cliente, solo filas con `ESTADO='DEBE'`) — `cargarDeudaFecha`.
- **SELECT** `CLIENTE` (`id_cliente` por cédula/NIT).
- **UPDATE** `configuracion.IMPRIMIR_TIKETE` (preferencia de impresión).
- **UPDATE** `PUNTOS` — resta (`descontarPuntos`, al redimir) o suma (`agregarPuntos`, al generar puntos nuevos de la venta) sobre `puntos.PUNTOS`.
- No inserta directamente en `ventas`/`caja`/etc.: delega esa escritura en `imprimirTikest` (de la pantalla `Ventas` padre) o en `PagarCuentas` (de `CuentasPorCobrar`/`Abonos`/`AbonosCuentas`, fuera de este grupo).

**Reportes generados:** Ninguno directamente — decide (vía los parámetros `imprimirTikete.isSelected()` y `TipoImpresora.isSelected()`) si y cómo se imprime el comprobante, pero la impresión real ocurre dentro del método `imprimirTikest` del padre.

**Reglas de negocio / validaciones no obvias:**
- El pago nunca se habilita (`aceptar.setEnabled`) si el "cambio" calculado sería negativo, excepto en los modos que no usan efectivo (Cheque/Vale/Tarjeta/Gratis/Banco), que siempre fijan `entregado = total` y habilitan "Aceptar" de inmediato sin pedir monto.
- El modo "Debito" exige obligatoriamente un cliente distinto de "OCASIONAL"/"Ocasional" (comparación hecha de forma inconsistente: en algunos puntos compara contra la cadena `"OCASIONAL"` en mayúsculas y en otros contra `"Ocasional"` con mayúscula inicial — posible bug de comparación sensible a mayúsculas que puede dejar pasar o bloquear el débito según cómo esté capitalizado el nombre real en BD).
- La redención de puntos limita el descuento máximo a la mitad del total de la venta cuando los puntos disponibles alcanzarían para cubrir más que el 100% del total (`descontar_puntos = (int)total_pagar_/2`), evitando que una venta quede en "total negativo" por exceso de puntos.
- Los puntos ganados por una venta **no** se acreditan si la forma de pago es "Debito" (se acreditan solo al momento de cancelar la deuda, presumiblemente desde el módulo de Cuentas por Cobrar, fuera de este grupo) ni si el cliente es "OCASIONAL" (cc = "1000").

**Notas / peculiaridades:**
- Fuerte duplicación de código entre las ramas `Obj`/`Obj1`/`Obj2` en `aceptarActionPerformed` y en `keyReleased` (Enter) — el mismo bloque de ~15 líneas se repite 3 veces letra por letra cambiando solo el nombre de la referencia.
- Bug potencial señalado en el punto 19 de funcionalidades (Escape asume `Obj` no nulo).
- No existe constructor para `Ventas3`, coherente con que esa pantalla no puede completar ventas.
- El algoritmo de cálculo de cambio con puntos redimidos se repite casi idéntico en tres lugares distintos del archivo (`entregadoKeyReleased`, `CALCULAR_CAMBIO`, y de forma implícita en `aceptarActionPerformed`) — candidato claro a refactorizar como una sola función en la reimplementación.

---

### Cantidad.java — Diálogo para ingresar la cantidad de una línea de venta

**Propósito:** Diálogo modal simple para capturar/modificar la cantidad de unidades de la línea de producto seleccionada en el carrito (admite decimales, para ventas por fracción de unidad, p.ej. medio galón).

**Cómo se invoca / navegación:** `JDialog` modal con **cinco constructores sobrecargados**: `Cantidad(Ventas, boolean, String nombre_producto)`, `Cantidad(Ventas1, ...)`, `Cantidad(Ventas2, ...)`, `Cantidad(Ventas3, ...)` (también soporta `Ventas3`, igual que `Descuento.java`), y `Cantidad(GestionInventario, boolean, boolean extender, String nombre_producto)` (reutilizado desde el módulo de inventario). Se abre: automáticamente al agregar un producto nuevo mediante búsqueda por Enter cuando hay una sola coincidencia (ver `Ventas.BUSCARKeyReleased`), vía **Ctrl+C**, botón "Cantidad", doble clic en la celda "CANTIDAD" del carrito, o el menú contextual "Modificar Cantidad / Precio Venta / Descuento" cuando la columna activa es la de cantidad.

**Componentes de interfaz:**
- `NP` (JLabel): nombre del producto.
- `pvpiva` (JTextField): cantidad a ingresar.
- `mas` (JButton, ícono "ok"): confirma el valor (equivalente a Enter).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, muestra el nombre del producto y da foco a `pvpiva`.
2. Al ganar foco: resalta fondo y selecciona todo el texto.
3. Solo acepta dígitos y coma decimal.
4. **Enter**, o clic en el botón `mas`: con texto no vacío, parsea el valor y llama a `cantidades(valor)` sobre la ventana padre correspondiente (`Obj`/`Obj1`/`Obj2`/`Obj3`) — o `cantidades(new BigDecimal(valor), extender)` si el padre es `GestionInventario` —; para el caso `Obj` (pantalla `Ventas` base) además limpia y refiltra explícitamente el buscador de esa pantalla antes de devolverle el foco; cierra el diálogo.
5. **Escape**: cierra el diálogo sin aplicar cambios.

**Datos / tablas de BD leídas o escritas:** Ninguna directamente — delega en `cantidades(Double)` de la pantalla padre, que sí recalcula totales y, en `Ventas.java`, si la fila editada no es la última, selecciona la siguiente fila y vuelve a abrir este mismo diálogo encadenado (igual patrón que `Descuento.java`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna propia; toda la validación (ej. existencia de stock) queda a cargo de la pantalla padre, que en `Ventas.java` no bloquea explícitamente por falta de stock al cambiar la cantidad (solo marca la fila como "NOHAY" visualmente vía `calcularTotales`).

**Notas / peculiaridades:** Estructura casi idéntica a `Descuento.java` (mismo patrón de 4-5 constructores, mismo manejo de foco/teclas); candidatos a unificarse en un solo componente parametrizable en la reimplementación.

---

### Proceso.java — Diálogo de "Procesando..." (indicador de progreso)

**Propósito:** Diálogo modal mínimo que muestra un GIF animado de "cargando" mientras se ejecuta una operación larga. **No pertenece al flujo de Ventas**: su único constructor acepta como padre `InformeCuentas` (módulo de informes/cuentas), no `Ventas`/`Ventas1`/`Ventas2`/`Ventas3`. Se documenta aquí por completitud ya que el enunciado lo incluyó en este grupo, pero es, igual que `calculado.java`, una dependencia cruzada con otro módulo.

**Cómo se invoca / navegación:** `JDialog` modal, constructor único `Proceso(InformeCuentas Obj, boolean modal)`.

**Componentes de interfaz:**
- `jLabel1` (JLabel): ícono `load.gif` (animación de carga).
- `jLabel2` (JLabel): texto de estado (queda con el valor por defecto "jLabel2" en el formulario — nunca se le asigna un mensaje real en el código visto).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, simplemente muestra el GIF animado; no tiene ningún manejador de evento propio (ni botones, ni teclas, ni lógica) — es un indicador visual puro que el padre (`InformeCuentas`) abre y cierra manualmente mientras dura su propio procesamiento.

**Datos / tablas de BD leídas o escritas:** Ninguna.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna.

**Notas / peculiaridades:** El `jLabel2` nunca recibe un texto distinto al autogenerado "jLabel2" en ninguna parte del código revisado — probablemente una funcionalidad planeada (mensaje de estado dinámico) que nunca se terminó de conectar. Para la migración, **no es necesario** incluirlo dentro del módulo de Ventas; corresponde a Informes/Cuentas.

---

### Resumen de dependencias cruzadas y hallazgos clave para la migración

1. **`Ventas.java` ≡ `Ventas1.java` ≡ `Ventas2.java`** (salvo nombre de clase/título): representan tres instancias concurrentes de la misma pantalla de POS, activables entre sí con Ctrl+2/Ctrl+3. En Regenta debería bastar con **una** implementación que soporte múltiples pestañas/ventanas abiertas simultáneamente, no tres clases clonadas.
2. **`Ventas3.java` es código muerto** (nunca instanciado) con un flujo de venta roto (el botón "Vender" no factura nada); no debe tomarse como referencia de comportamiento, salvo por la idea de un combo de filtro por categoría de producto.
3. **`EditarVentas.java` (en producción) está incompleta/rota**: no muestra detalle ni permite reabrir una factura para editar, aunque es la clase enlazada al menú "Editar ventas". **`EditarVentas1.java` (código muerto, nunca enlazada) sí implementa correctamente** esa función, incluyendo el botón "Editar Venta" que reabre `Ventas(sudid)`. Se recomienda tomar `EditarVentas1.java` como especificación de comportamiento para Regenta, validando con el negocio si el alcance debe ser "todo el histórico" (como en `EditarVentas1`) o "solo el último día" (como en `EditarVentas` actual).
4. **`calculado.java` y `Proceso.java`** pertenecen funcionalmente al módulo de Caja/Informes, no al de Ventas, aunque se listaron en este grupo; se documentaron igual por completitud, señalando su padre real (`cerrarCajaCalculado`, `InformeCuentas`).
5. **Patrón repetido de "tres/cuatro referencias nulas" (`Obj/Obj1/Obj2/Obj3`)** en todos los diálogos compartidos (`Cargarfactura`, `CargarCotizacion`, `MovimientoCajaVentas`, `Cantidad`, `Descuento`, `Pago`) en vez de polimorfismo — oportunidad clara de simplificación en el rediseño.
6. **Todas las consultas SQL se construyen por concatenación de strings** (sin `PreparedStatement`), incluyendo datos introducidos por el usuario (números de factura, nombres de cliente, montos) — riesgo de inyección SQL a mitigar en Regenta.
7. **Reglas de dinero no triviales a preservar**: desarmado de IVA al agregar un producto (`precio_con_iva / (1+tasa)`), consumo de notas crédito (parcial vs. total), tope de redención de puntos al 50% del total, truncamiento del cambio a 0 cuando el pago es parcial/débito, reutilización opcional de números de factura anulados, numeración independiente para Factura/Remisión/Cotización/Movimiento de caja manual.

---

## Grupo: Productos e Inventario

### Productos.java — Gestión Principal de Productos
**Propósito:** Pantalla CRUD completa de productos: crear, editar y listar productos, con cálculo de precio de venta/IVA/margen/beneficio y asignación de existencias iniciales.

**Cómo se invoca / navegación:** `JInternalFrame` (`extends javax.swing.JInternalFrame`), se instancia con `new Productos(aux)` donde `aux` es un string de origen (p. ej. `"Menunew"`) usado para saber a qué pantalla volver al cerrar. No es modal; vive dentro del escritorio MDI de `MenuPrincipal`. Al cerrar (`Escape` o botón "Cerrar") llama `MenuPrincipal.ActivarFrame("Menunew","","","","","")` si `aux.equals("Menunew")`.

**Componentes de interfaz:**
- `BUSCAR` (JTextField): caja de búsqueda con placeholder "Tecla (F5) Buscar" en cursiva gris; al enfocar se limpia y pone fuente normal.
- `referencia`, `codigoBarras`, `nombre` (JTextField).
- `categoria`, `impuestos`, `unidades` (JComboBox).
- `min`, `max`, `Cantidad`, `ubicacion` (JTextField) — stock mínimo, máximo, cantidad inicial y estante/ubicación.
- `pvp` (precio venta sin IVA mostrado/calculado), `pvpiva` (precio venta con IVA), `precioCompra1` (JTextField editable).
- `margen`, `beneficio` (JTextField, solo lectura, fondo gris).
- `tablaProductos` (JTable) columnas: `Refecencia`, `Nombre`, `Categoria`, `Title 4` (ID, oculta).
- Botones: `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar" (icono delete), `jButton4` (botón de depuración sin texto visible útil, acción casi vacía).
- `NP` (JLabel icono lupa, "Buscar:").

**Funcionalidades atómicas (numeradas):**
1. Al abrir, se cargan combos `categoria` (`listarCategorias()`), `impuestos` (`listarCategoriasImpuestos()`) y `unidades` (`listarUnidades()`), luego `limpiar()`.
2. `listarCategoriasImpuestos()`: si `MenuPrincipal.DATOS_OBLIGATORIOS.get("REGIMEN_COMUN_SIMPLIFICADO")` es `"1"` (régimen simplificado) se listan todas las categorías de impuesto y no se preselecciona ninguna (`setSelectedIndex(-1)`); si es `"0"` (régimen común) se excluye la categoría cuyo nombre sea exactamente `"Iva 0%"` y se preselecciona el índice 1, recalculando `porcentaje_impuesto`.
3. Tecla F5 en cualquier parte de la pantalla (vía `KeyListener` global) pone foco en `BUSCAR`, limpia el placeholder y cambia estilo.
4. Tecla `Escape` cierra la pantalla (`cerrar()`).
5. `BUSCAR` filtra `tablaProductos` con `RowFilter.regexFilter` sobre columnas 1,2,3, reemplazando espacios por `.*.` (búsqueda tipo "contiene cada palabra en cualquier orden").
6. Doble clic en una fila de `tablaProductos` llama `ListarProducto("productos.ID", id)` usando la columna oculta 3 como ID.
7. `codigoBarras` + Enter: si el campo no está vacío y `!updateSave`, llama `ListarProducto("CODIGO_BARRAS", codigoBarras.getText())`.
8. `referencia` + Enter: si `codigoBarras` está vacío, copia el valor de `referencia` dentro de `codigoBarras`.
9. `ListarProducto(columna, valor)` hace SELECT de productos + categorías + categoria_impuesto + unidades + stockproductos y rellena todos los campos; calcula `pvpiva_ = pvp_ + (pvp_*porcentaje_impuesto/100)`; `margen_ = (pvp_-costo)/costo*100`; `beneficio_ = (pvp_-costo)/pvp_*100`; marca `updateSave = true` (modo edición).
10. `pvp` KeyReleased: si `impuestos.getSelectedIndex()==0` el PVP+IVA es igual al PVP (sin impuesto); si no, `pvpiva = pvp + pvp*porcentaje_impuesto/100`.
11. `pvpiva` KeyReleased: recalcula `porcentaje_impuesto` del combo actual y `pvp = pvpiva / (1 + porcentaje_impuesto/100)`.
12. `precioCompra1` KeyReleased: si hay valor, calcula `ganancia = pvp - costo`; `margen = ganancia/costo*100`; `beneficio = ganancia/pvp*100`, formateados como porcentaje con 2 decimales; si no hay valor, limpia `margen`/`beneficio`.
13. `impuestos` ActionPerformed: si `!AUX` (no está en medio de una carga programática) y `pvp_ > 0`, recalcula `pvpiva_` con el nuevo porcentaje.
14. Botón **Guardar** (`jButton1`), validaciones en este orden (cada una muestra `JOptionPane.ERROR_MESSAGE` con el texto literal y pone foco en el campo):
    - Si `referencia` vacía: se autogenera `referencia = cargarReferencia()+1` (NO muestra mensaje, código de aviso está comentado).
    - `nombre.getText().equals("")` → **"Falta el nombre del producto."**
    - `categoria.getSelectedIndex()==-1` → **"Seleccione la categoria del producto."** (abre el combo con `showPopup()`).
    - `unidades.getSelectedIndex()==-1` → **"Seleccione unidades del producto."**
    - `min.getText().equals("")` → **"Falta Stock Min del producto."**
    - `max.getText().equals("")` → **"Falta Stock Max del producto."**
    - `Cantidad.getText().equals("")` → **"Falta Cantidad del producto."**
    - `pvp.getText().length()<=0` → **"Falta precio de venta del producto."**
    - `precioCompra1.getText().length()<=0` → **"Falta precio de compra del producto."**
15. Si pasa validaciones y `updateSave==true` (modo edición): verifica duplicados excluyendo el propio ID (`verificarReferencia2`, `verificarCodigoBarras2`, `verificarNombre2`); si ninguno es verdadero hace `UPDATE productos` (referencia, código, nombre, precio_compra, precio_venta [recalculado sin IVA: `precio_venta_iva_ / (porcentaje/100+1)`], id_categoria, id_categoria_impuesto, id_unidades, stock_min, stock_max, activo='TRUE') y `UPDATE stockproductos SET CANTIDAD=.., ESTANTE=..` por ID; muestra **"Producto Modificado Correctamente."** (INFORMATION) y llama `limpiar()`. Si algún duplicado existe muestra respectivamente **"La referencia que intenta guardar ya existe, en otro producto diferente."**, **"El codigo de barras que intenta guardar ya existe, en otro producto diferente."** o **"El producto que intenta guardar ya existe."**.
16. Si `updateSave==false` (modo creación): verifica duplicados globales (`verificarReferencia`, `verificarCodigoBarras`, `verificarNombre`); si no hay duplicado hace `INSERT INTO productos (...)` con `ACTIVO='TRUE'`, `PRECIO_VENTA2='0'`, `PRECIO_VENTA3='0'`, luego `INSERT INTO stockproductos (ID,LUGAR,CANTIDAD,ESTANTE)` con `LUGAR='1'`; si `referencia_nueva` es true, llama `modificar_referencia(referencia)` que hace `UPDATE configuracion SET referencia = <nueva>` (actualiza el contador global de referencia correlativa); muestra **"Producto Agregado Correctamente."**; si hay duplicados, mismos 3 mensajes de arriba (sin "en otro producto diferente").
17. Botón **Limpiar** (`jButton2`): limpia todos los campos, vuelve a cargar combos y tabla, resetea variables de estado (`updateSave=false`, `id=""`, montos a 0), restaura placeholder de `BUSCAR`, y autogenera la siguiente referencia disponible con `cargarReferencia()+1`.
18. Botón **Cerrar** (`jButton3`): llama `cerrar()`.
19. `jButton4` (acción casi vacía, código de depuración sin efecto visible salvo cálculo interno descartado) — vestigio no funcional.

**Datos / tablas de BD leídas o escritas:**
- `productos` (SELECT/INSERT/UPDATE: REFERENCIA, CODIGO_BARRAS, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, ID_CATEGORIA, ID_CATEGORIA_IMPUESTO, ID_UNIDADES, STOCK_MIN, STOCK_MAX, ACTIVO, PRECIO_VENTA2, PRECIO_VENTA3).
- `categorias` (SELECT para combo y para resolver ID_CATEGORIA por nombre).
- `categoria_impuesto` (SELECT CATEGORIA, VALOR; resolución de ID_CATEGORIA_IMPUESTO).
- `unidades` (SELECT UNIDADES; resolución de ID_UNIDADES).
- `stockproductos` (INSERT/UPDATE: ID, LUGAR, CANTIDAD, ESTANTE).
- `configuracion` (SELECT/UPDATE columna `referencia`, contador correlativo).

**Reportes generados:** Ninguno invocado directamente desde esta pantalla.

**Reglas de negocio / validaciones no obvias:**
- El PVP mostrado al usuario (`pvp`) y el PVP+IVA (`pvpiva`) son dos representaciones del mismo precio; lo que se guarda en `productos.PRECIO_VENTA` siempre es el valor **sin** IVA, recalculado a partir de `pvpiva` en el momento de guardar (no del campo `pvp` directamente), usando el porcentaje del combo `impuestos` vigente en ese instante.
- Las validaciones de duplicados distinguen explícitamente alta (sin excluir ningún ID) de edición (excluyendo el ID propio con `ID<>'id'`).
- El código de barras no es obligatorio (solo valida duplicado si no está vacío); la referencia sí se autogenera si falta.
- `ACTIVO` siempre se fuerza a `"TRUE"` tanto en alta como en edición desde esta pantalla (no hay forma de desactivar un producto aquí; eso se hace desde `InventarioGeneral`).

**Notas / peculiaridades:**
- Existe un segundo constructor `Productos()` sin parámetros, completamente comentado (código muerto duplicado del constructor activo).
- Gran cantidad de código comentado duplicado dentro de `ListarProducto` (una versión antigua `ListarProductoCodigoBarras` queda comentada completa).
- El filtro de `BUSCAR` usa columnas 1,2,3 de `tablaProductos`, pero el modelo de esa tabla solo tiene 4 columnas (0..3), por lo que la columna 3 (ID oculto) también entra en el filtro de texto — comportamiento no documentado pero real.
- Depende de `MenuPrincipal.BD` (conexión/sentencia JDBC estáticos) y de `MenuPrincipal.DATOS_OBLIGATORIOS` (mapa de configuración global: `REGIMEN_COMUN_SIMPLIFICADO`, etc.).

---

### ListaProductos.java — Selector/Listado Emergente de Productos
**Propósito:** Diálogo reutilizable de búsqueda y selección de un producto, usado como "buscador rápido" desde varias pantallas de venta e inventario.

**Cómo se invoca / navegación:** `JDialog` modal. Tiene **6 constructores sobrecargados**, uno por cada posible pantalla padre: `Ventas`, `Ventas1`, `Ventas2`, `Ventas3`, `GestionInventario`, `VerificarProductosCompras`. Internamente guarda la referencia del padre en el campo correspondiente (`Obj`, `Obj1`, `Obj2`, `Obj3`, `Obj4`, `Obj5`) dejando los demás en `null`; toda la lógica de eventos pregunta `if(Obj!=null){...}else if(Obj1!=null){...}` etc. En `GestionInventario` se instancia una sola vez como atributo persistente (`ListaProductos ListaProductos = new ListaProductos(this,true)`) y se reutiliza, sincronizando su tabla con `GestionInventario.cargarProductos()`.

**Componentes de interfaz:**
- `TABLA` (JTable, `public static`) columnas: ID, REFERENCIA, CODIGO DE BARRAS, PRODUCTO, UNIDADES, CATEGORIA, EXISTENCIAS, PRECIO COMPRA, PRECIO VENTA, IMPUESTOS, ESTANTE. Columnas 0,1,2,4,5,7,9 se ocultan (ancho 0); quedan visibles PRODUCTO (ancho 700), EXISTENCIAS(120), ESTANTE(150).
- `BUSCAR` (JTextField, `public`).
- `jLabel9` "Buscar:" con icono lupa.

**Funcionalidades atómicas (numeradas):**
1. Al construirse, configura renderer de moneda en todas las columnas, crea `TableRowSorter`, limpia `BUSCAR`, aplica filtro vacío sobre columnas 1-9, selecciona fila 0 y mapea la tecla Enter de la tabla a la acción `select-columnCell`.
2. `BUSCAR` KeyReleased: filtra `TABLA` por regex sobre columnas 1-9 (cada palabra separada por espacio se convierte en `.*.`); si se pulsa Enter y hay filas visibles, mueve el foco/selección a la fila 0.
3. `BUSCAR` KeyReleased con Escape: limpia el filtro del padre correspondiente y cierra el diálogo (`dispose()`).
4. Doble clic o Enter sobre una fila de `TABLA`: según cuál `Obj*` esté activo, llama al método del padre con firma distinta:
   - Para `Obj`/`Obj1`/`Obj2`: `padre.ListarProductoCodigoBarras(id, nombre, Double precioVenta, impuestoTexto)` usando columnas 0,3,8,9; luego si `padre.VENTAS` tiene fila seleccionada abre `Cantidad` para esa venta.
   - Para `Obj3`: misma llamada pero usando columna 7 como precio (en vez de 8) — **inconsistencia de índice respecto a Obj/Obj1/Obj2** (ver Notas).
   - Para `Obj4` (GestionInventario): `padre.ListarProductoCodigoBarras(id, nombre, BigDecimal precioCompra(col7), BigDecimal precioVenta(col8))`, limpia selección, restaura `BUSCAR` del padre y abre diálogo `Cantidad` sobre la fila seleccionada en `GESTIONAR_INVENTARIO`.
   - Para `Obj5` (VerificarProductosCompras): misma firma que `Obj4` pero solo oculta el diálogo (`setVisible(false)`) en vez de cerrarlo con Enter en la tabla, y con `dispose()` en el doble clic.
5. Enter con Escape dentro de `TABLA`: limpia filtro del padre activo (`Obj`/`Obj1`/`Obj2`/`Obj3`).

**Datos / tablas de BD leídas o escritas:** No consulta la base directamente; solo pinta filas que cada pantalla padre le inyecta en su propio `TABLA` (p. ej. `GestionInventario.cargarProductos()` copia fila por fila desde su tabla `PRODUCTOS` hacia `ListaProductos.TABLA`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- No valida nada por sí mismo: toda regla de negocio (existencia, categoría, etc.) vive en la pantalla padre que la invoca.

**Notas / peculiaridades:**
- Fuerte acoplamiento con 6 clases distintas vía `instanceof`-like checks manuales (`if (Obj!=null)`), patrón repetido y frágil: una pantalla nueva que quisiera reutilizar este buscador necesitaría un séptimo constructor y repetir toda la cascada de `if/else if`.
- Inconsistencia real detectada: el manejador de teclado (`TABLAKeyReleased`) usa columna **8** como precio para `Obj3`, mientras el de mouse (`TABLAMouseClicked`) usa columna **7** para `Obj3` — comportamiento distinto según se seleccione con teclado o mouse.

---

### DetalleProducto.java — Detalle de Fracciones/Presentaciones de un Producto (Ventas)
**Propósito:** Diálogo invocado desde las pantallas de venta para mostrar y elegir una presentación fraccionada de un producto a granel (por ejemplo, vender 1/4 de galón o medio kilo de un producto que se vende por unidad completa).

**Cómo se invoca / navegación:** `JDialog`, constructores para `Ventas`, `Ventas1`, `Ventas2` (recibe `id`, `nombre`). No modal obligatoriamente (depende del flag `modal` recibido). Al elegir una fila llama `padre.detallarProducto(valor, precio, valorCalculado)` y cierra.

**Componentes de interfaz:**
- `PRODUCTO` (JTextField, solo lectura) — nombre del producto.
- `DETALLE` (JTable) columnas: MEDIDA, Valor (oculta), PRECIO.
- Menú contextual con ítem "Agregar Detalle" que abre `AgregarDetalleProducto`.
- `fecha` (DateChooserCombo, presente pero no visible/usado funcionalmente aquí).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, `DetalleProducto(id)` intenta 3 estrategias en cascada, deteniéndose en la primera que devuelva filas:
   - a) `SELECT MEDIDA,VALOR,PRECIO FROM detalle_producto WHERE ID_producto=id ORDER BY PRECIO DESC` (detalle específico del producto).
   - b) Si no hay filas: `SELECT ... FROM detalle_producto WHERE UNIDADES=<unidad del producto> AND ID_CATEGORIA=<categoría del producto> ORDER BY PRECIO DESC` (detalle genérico por unidad+categoría).
   - c) Si tampoco hay filas: genera fracciones **hardcodeadas** según la unidad de medida y el valor de la unidad del producto:
     - Si unidad = `GLN` y valor unitario = 55: agrega filas 50,45,40,35,30,25,20,15,10,5,1,1/2,1/4,1/8,1/16 GLN con sus fracciones decimales y precio = fracción × `PRECIO_VENTA`.
     - Si unidad = `GLN` y valor = 5: agrega 1, 1/2, 1/4, 1/8, 1/16 GLN.
     - Si unidad = `GLN` y valor = 1: agrega 1/2, 1/4, 1/8, 1/16 GLN.
     - Si unidad = `KG` y valor = 50: agrega 45,40,35,30,25,20,15,10,5,1,1/2,1/4,1/8 KG.
     - Si unidad = `KG` y valor = 40: agrega 35,30,25,20,15,10,5,1,1/2,1/4,1/8 KG.
     - Si unidad = `KG` y valor = 25: agrega 20,15,10,5,1,1/2,1/4,1/8 KG.
     - Unidad `MTS`: rama vacía (no genera nada).
2. Doble clic o Enter sobre una fila de `DETALLE`: toma columnas 1 (valor) y 2 (precio) de la fila, llama `padre.detallarProducto(valor, precio, valorCalculado)`, limpia y restaura el `BUSCAR` del padre, y cierra el diálogo.
3. Escape cierra sin seleccionar nada.
4. Menú contextual "Agregar Detalle" abre `AgregarDetalleProducto(this, true, id, nombre)` para mantener la tabla `detalle_producto`.

**Datos / tablas de BD leídas o escritas:**
- `productos` (SELECT ID_CATEGORIA por ID).
- `productos` + `unidades` (SELECT unidad por ID de producto).
- `detalle_producto` (SELECT MEDIDA, VALOR, PRECIO por ID_producto o por UNIDADES+ID_CATEGORIA).
- `productos` + `unidades` (SELECT UNIDADES.UNIDADES, UNIDADES.VALOR, productos.PRECIO_VENTA para la rama de generación automática).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- `valorCalculado` se marca `true` solo cuando la fuente de los datos fue la tabla `detalle_producto` (estrategias a o b); si se usó la generación automática hardcodeada, permanece en `false` — esto se propaga como parámetro a `padre.detallarProducto(...)`, por lo que el comportamiento de venta puede variar según el origen del dato.
- El texto de unidad `UNIDADES.UNIDADES` se recorta tomando la subcadena después del primer espacio (`substring(indexOf(" ")+1)`), asumiendo un formato tipo `"1 GLN"`.

**Notas / peculiaridades:**
- Las fracciones y sus precios de galones/kilos son constantes de negocio embebidas directamente en el código (no configurables desde la base de datos ni desde pantalla alguna); cualquier cambio en política de fraccionamiento requiere modificar el código fuente.
- Comparte estructura casi idéntica con `DetallarProductosFacil.java` pero con un modelo de datos y flujo distintos (ver comparación en esa sección).

---

### DetallarProductos.java — Detallar Productos (fraccionamiento producto‑a‑producto, pantalla completa)
**Propósito:** Pantalla de inventario para convertir unidades de un producto "grande" (producto a detallar) en unidades de un producto "pequeño" ya existente (producto detallado), validando que ambos compartan categoría y unidad de medida base, y trasladando existencias de uno al otro.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `DetallarProductos(String aux)`. Vive en el escritorio MDI; cierre con `cerrar()` similar a `Productos`.

**Componentes de interfaz:**
- `tablaProductos` (JTable) columnas: ID(oculta), NOMBRE, CATEGORIA, UNIDADES, EXISTENCIAS, VALOR_UNIDADES(oculta).
- Radios `uno` ("1" = seleccionando el producto a **detallar**/origen) y `dos` ("2" = seleccionando el producto **detallado**/destino), agrupados en `buttonGroup1`.
- Panel "Producto Detallado" (en la UI, en realidad es el producto **origen**, etiquetas `nombre`, `categoria`, `unidades`, `cantidad_detallado`).
- Panel "Producto Detallado" (segundo, en realidad producto **destino**): `nombre_d`, `categoria_d`, `unidades_d`, `cantidad_detallar`, `existencias_d`.
- Botones: `bdetallar` "Detallar" (icono check), `jButton2` "Limpiar", `jButton3` "Cerrar".
- `BUSCAR` con placeholder "Tecla (F5) Buscar".

**Funcionalidades atómicas (numeradas):**
1. `cargarProductos()`: `SELECT productos.ID, NOMBRE, CATEGORIA, UNIDADES, stockproductos.CANTIDAD, unidades.VALOR` con UNION de productos sin fila en `stockproductos` (existencia 0); llena `tablaProductos`.
2. `limpiar()`: selecciona radio `uno`, limpia todos los campos de ambos paneles, deshabilita `bdetallar`, recarga la tabla, selecciona fila 0, restaura placeholder de `BUSCAR`, aplica filtro vacío sobre columnas 1,2,3.
3. Doble clic en una fila de `tablaProductos`:
   - Si `uno` está seleccionado (elige producto **origen/a detallar**): si la unidad de esa fila **no** es `"UNIDAD"`, carga `id_producto_detallar`, `nombre_d`, `categoria_d`, `unidades_d`, pone `cantidad_detallar="1"`, `existencias_d`, guarda `valor_producto_detallar`, cambia el radio a `dos` y pone foco en `cantidad_detallar`. Si la unidad es `"UNIDAD"` muestra **"El producto que intena detallar, no tienen una unidad valida."** (ERROR).
   - Si `dos` está seleccionado (elige producto **destino/detallado**): si el nombre de la fila es distinto al ya elegido como origen y su unidad no es `"UNIDAD"`, carga `id_producto_detallado`, `nombre`, `categoria`, `unidades`, guarda `valor_producto_detallado`; si `categoria_d` (origen) coincide con `categoria` (destino), compara el sufijo de unidad después del valor numérico (p. ej. "GLN" vs "GLN") entre ambos; si coinciden calcula `cantidad_detallado = (valor_producto_detallar * cantidad_detallar) / valor_producto_detallado` y habilita `bdetallar`; si no coinciden muestra **"Los productos que intenta detallar, tienen diferente unidad de medida."** y limpia `nombre/categoria/unidades` destino. Si las categorías no coinciden muestra **"Los productos que intenta detallar, son de diferente categoria."**. Si el nombre es igual al origen o la unidad es `"UNIDAD"`, muestra **"Debe escoger un producto diferente al que intenta detallar."** o **"El producto que intena detallar, no tienen una unidad valida."** respectivamente, y deshabilita `bdetallar`.
4. `cantidad_detallar` KeyReleased (al escribir manualmente la cantidad a detallar): repite la validación de existencia suficiente — si `cantidad_detallar > existencias_d` muestra **"Los cantidad que intenta detallar, es mayor a la existencia del producto ."** y resetea a "1"; si es válida y las categorías/unidades coinciden, recalcula `cantidad_detallado` igual que el punto 3 y habilita `bdetallar`; si no, limpia y deshabilita.
5. Botón **Detallar** (`bdetallar`): si hay cantidades en ambos campos:
   - Si el producto destino ya tiene fila en `stockproductos`: `UPDATE stockproductos SET CANTIDAD = CANTIDAD + cantidad_detallado WHERE ID=destino`.
   - Si no: `INSERT INTO stockproductos (ID,LUGAR,CANTIDAD) VALUES (destino,'',cantidad_detallado)`.
   - Luego `descontarInventario(origen, cantidad_detallar)` → `UPDATE stockproductos SET CANTIDAD = CANTIDAD - cantidad_detallar WHERE ID=origen`.
   - Muestra **"Se detallo con exito el producto seleccionado."** (INFORMATION) y llama `limpiar()`.
6. `BUSCAR` filtra por regex sobre columnas 1,2,3 de `tablaProductos`.
7. Tecla espacio sobre `tablaProductos` manda el carácter escrito a `BUSCAR` y le da foco (patrón "escribir para buscar" típico de estas pantallas, aunque mal cableado — ver Notas).

**Datos / tablas de BD leídas o escritas:**
- `productos`, `categorias`, `unidades`, `stockproductos` (SELECT combinados para listar productos y existencias).
- `stockproductos` (UPDATE/INSERT para sumar en destino y restar en origen).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- "Misma unidad de medida" se verifica comparando el **sufijo textual** después del valor numérico de la unidad (p. ej. "GLN"), no un ID de unidad — frágil ante formatos inconsistentes.
- Permite existencias negativas en el producto origen después de restar (no hay control de que `cantidad_detallar <= existencia` dentro de `bdetallarActionPerformed`; ese control solo ocurre en el KeyReleased de `cantidad_detallar`, por lo que si el usuario nunca dispara ese evento —p. ej. llega con valor precargado— podría enviarse una cantidad mayor a la existencia).
- Un producto con unidad `"UNIDAD"` nunca puede participar como origen ni como destino del detallado.

**Notas / peculiaridades:**
- Los nombres visuales de los paneles ("Producto Detallado" para ambos) son confusos/erróneos: el primer panel (radio `uno`) en realidad corresponde al producto **origen**, mientras que el título dice "Detallado" para ambos — posible copy-paste sin corregir el rótulo.
- Las variables `id_producto_detallar`/`valor_producto_detallar` representan el producto **origen** (el que se consume) y `id_producto_detallado`/`valor_producto_detallado` el producto **destino** (el que recibe existencias) — nomenclatura invertida respecto a la intuición ("detallar" suena a la acción que falta, "detallado" al resultado), hay que leer el código con cuidado para no confundir el sentido real del flujo al reimplementar.

---

### DetallarProductosFacil.java — Detallar Productos (versión simplificada, un solo paso)
**Propósito:** Versión rápida del fraccionamiento de productos, pensada para usarse directamente desde la pantalla de Ventas cuando un producto no tiene existencia suficiente: permite dividir manualmente una unidad base de un producto origen (ya conocido) en N partes iguales y asignarlas a un producto destino elegido de una lista filtrable.

**Cómo se invoca / navegación:** `JDialog` modal, único constructor: `DetallarProductosFacil(Ventas parent, boolean modal, String id, String producto_, double exisencia, String buscar_)`. Se invoca típicamente desde `Ventas` cuando el usuario intenta vender un producto sin existencias suficientes del producto base, para "romper" una unidad mayor en unidades menores ya catalogadas.

**Componentes de interfaz:**
- `producto` (JTextField, solo lectura) — producto **origen** (a detallar), ya fijado al abrir.
- `PRODUCTOS` (JTable) columnas ID(oculta), PRODUCTO, EXISTENCIA — lista de productos candidatos a **destino**, filtrable con `BUSCAR`.
- `producto2` (JTextField, solo lectura) — producto **destino** elegido con doble clic en `PRODUCTOS`.
- `cantidad_detallar` (JTextField) — cuántas unidades del producto origen se van a consumir.
- `dividir` (JTextField) — en cuántas partes se divide cada unidad origen (factor multiplicador).
- `cantidad_detallado` (JTextField, solo lectura) — resultado `cantidad_detallar × dividir`.
- Botones `bdetallar` "Detallar", `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. Al abrir: configura renderer/ancho de columnas, `producto.setText(producto_)`; si `exisencia>0` pone `cantidad_detallar="1"`; carga `PRODUCTOS` vía `cargarProductos("-1")` (todas las categorías); aplica el texto de búsqueda recibido (`buscar_`) como filtro inicial; pone foco en `dividir`.
2. `cargarProductos(id_categoria)`: SELECT de productos activos con su existencia real (`stockproductos.CANTIDAD`) o 0 si no tiene fila en `stockproductos`, uniendo `categoria_impuesto`, `categorias`, `unidades`; si `id_categoria` es `"-1"` trae todas las categorías, si no filtra por `ID_CATEGORIA`.
3. `BUSCAR` filtra `PRODUCTOS` por regex sobre columnas 1,2,3.
4. `cantidad_detallar` KeyReleased: si el valor tecleado supera `exisencia` (la existencia del producto origen recibida al abrir), lo recorta automáticamente a `exisencia`.
5. `dividir` KeyReleased: calcula `cantidad_detallado = cantidad_detallar × dividir` (ambos como enteros).
6. Doble clic en una fila de `PRODUCTOS`: fija `producto2` (nombre) e `id2` (ID) como producto destino.
7. Botón **Detallar**: si `producto2` está vacío muestra **"Seleccione el producto donde se va a detallar las cantidades."** (ERROR). Si hay destino y ambas cantidades tienen valor:
   - Si el destino ya existe en `stockproductos`: `UPDATE stockproductos SET CANTIDAD = CANTIDAD + cantidad_detallado WHERE ID=id2`.
   - Si no: `INSERT INTO stockproductos (ID,LUGAR,CANTIDAD) VALUES (id2,'',cantidad_detallado)`.
   - Descuenta del origen: `UPDATE stockproductos SET CANTIDAD = CANTIDAD - cantidad_detallar WHERE ID=id` (id del producto origen recibido al abrir).
   - Muestra **"Se detallo con exito el producto seleccionado."**, llama `obj.cargarProductos()` (refresca la pantalla de Ventas) y cierra el diálogo.
8. Botón **Cerrar**: cierra sin hacer cambios.

**Datos / tablas de BD leídas o escritas:**
- `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades` (SELECT para listar candidatos destino).
- `stockproductos` (INSERT/UPDATE para sumar en destino y restar en origen, columna CANTIDAD).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- A diferencia de `DetallarProductos.java` (pantalla completa), aquí **no se valida** que el producto destino tenga la misma categoría ni la misma unidad de medida que el origen — la relación "cuántas unidades destino equivalen a una unidad origen" la decide el usuario manualmente con el campo `dividir`, sin ningún chequeo automático de coherencia.
- La única protección de existencia es que `cantidad_detallar` no puede teclearse por encima de la `exisencia` recibida al abrir (pero si esa existencia cambió entre que se abrió la pantalla de Ventas y se abrió este diálogo, no se revalida contra la base de datos).

**Notas / peculiaridades:**
- Es funcionalmente más simple y menos segura que `DetallarProductos.java`: no verifica categoría/unidad compartida, delegando esa responsabilidad enteramente al criterio del usuario.
- Nombre de clase y de pantalla coinciden casi exactamente con `DetallarProductos.java`, lo que genera alta probabilidad de confusión; ambas escriben a la misma tabla `stockproductos` con lógica equivalente pero controles distintos.
- Variable de instancia `exisencia` (sic, con errata ortográfica "exisencia" en vez de "existencia") replicada también en otros varios archivos del sistema (p. ej. mensajes también usan "exisencia"/"intena").

---

### AgregarDetalleProducto.java — Alta/Edición de Presentaciones (detalle_producto)
**Propósito:** Mantenimiento CRUD de la tabla `detalle_producto` (las fracciones/presentaciones usadas por `DetalleProducto.java`), permitiendo definir medida+valor+precio ya sea a nivel de una combinación Unidad+Categoría (aplica a todos los productos de esa categoría/unidad) o a nivel de un producto específico, con opción de copiar en bloque filas ya cargadas hacia otra categoría/producto.

**Cómo se invoca / navegación:** `JDialog` modal, invocado únicamente desde el menú contextual "Agregar Detalle" de `DetalleProducto.java`: `new AgregarDetalleProducto(DetalleProducto parent, boolean modal, String id, String nombre)`.

**Componentes de interfaz:**
- `CUnidades`, `CCategoria` (JComboBox) — listas de unidades y categorías existentes.
- `jRadioButton1` "Listar Uniidades y Categorias" (sic) / `jRadioButton2` "Listar por Productos" (agrupados en `buttonGroup1`).
- `producto` (JTextField) — muestra el nombre del producto recibido.
- `medida`, `valor`, `precio` (JTextField) — datos de la nueva fila a insertar/editar.
- `DETALLE` (JTable) columnas MEDIDA, VALOR, PRECIO, Title 4 (ID, **editable=true** aunque no se usa para edición manual real).
- `jCheckBox1` "Utilizar los elementos filtrados, para agregarlos a otros."
- Botones `jButton1` "Buscar", `jButton2` "Guardar Valores a Categoria", `jButton3` "Guardar Valores a Producto", `jButton4` "Limpiar".

**Funcionalidades atómicas (numeradas):**
1. Al abrir: carga combos `CUnidades`/`CCategoria`, selecciona `jRadioButton1` y pone el nombre de producto recibido en `producto`.
2. Botón **Buscar**: si `jRadioButton1` está seleccionado, llama `DetalleProducto(unidad, ID_CATEGORIA(categoria))` (consulta por unidad+categoría); si `jRadioButton2`, llama `DetalleProducto(id)` (consulta por producto específico). Ambas: `SELECT MEDIDA,VALOR,PRECIO,ID FROM detalle_producto WHERE <condición> ORDER BY PRECIO DESC` y llenan `DETALLE`.
3. Doble clic en una fila de `DETALLE`: carga `medida`/`valor`/`precio` en los campos de edición y guarda `idParaActualizar`; marca `updateSave=true` (modo edición de esa fila).
4. Botón **Guardar Valores a Categoria** (`jButton2`):
   - Si `jCheckBox1` **no** está marcado: valida `medida`, `valor`, `precio` no vacíos (mensajes **"Falta medida."**, **"Falta factor de la unidad."** × 2 — mismo texto reutilizado para valor y precio). Si `updateSave`: `UPDATE detalle_producto SET medida=.., valor=.., precio=.. WHERE ID=idParaActualizar`, mensaje **"Unidades Modificada Correctamente"**. Si no: `INSERT INTO detalle_producto (Unidades, id_producto='0', id_categoria, medida, valor, precio)`, mensaje **"Unidades Agregada Correctamente"**. Tras cualquiera de las dos, recarga la tabla por unidad+categoría actuales.
   - Si `jCheckBox1` **sí** está marcado: pregunta confirmación **"Desea agregar los elementos de la tabla a la categoria seleccionada?"** (YES_NO); si acepta, hace un `INSERT INTO detalle_producto` por **cada fila actualmente visible en `DETALLE`** (usando sus columnas 0,1,2 como medida/valor/precio) hacia la unidad/categoría elegidas en los combos, mensaje final **"Unidades agregadas correctamente."**; si la tabla está vacía, muestra **"No hay unidades en la tabla para agregar."**.
5. Botón **Guardar Valores a Producto** (`jButton3`): misma lógica que el punto 4 pero destinada al producto específico (`id_producto=id`, `id_categoria='0'`, `Unidades='0'`), y en el modo copia masiva usa el mismo `id` de producto para todas las filas en vez del combo de categoría.
6. Botón **Limpiar** (`jButton4`): selecciona `jRadioButton1`, resetea combos al índice 0, limpia `medida/valor/precio`, borra la tabla `DETALLE`, desmarca `jCheckBox1`, resetea `updateSave=false`.
7. Escape cierra el diálogo.

**Datos / tablas de BD leídas o escritas:**
- `unidades`, `categorias` (SELECT para combos).
- `detalle_producto` (SELECT MEDIDA,VALOR,PRECIO,ID; INSERT/UPDATE por categoría+unidad o por producto).
- `categorias` (SELECT ID por nombre, para resolver `ID_CATEGORIA`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- Guardar "a Categoría" usa `id_producto='0'` como marcador de "no aplica a un producto específico"; guardar "a Producto" usa `id_categoria='0'` y `Unidades='0'` como marcador equivalente — el esquema de `detalle_producto` usa el valor `0` como "comodín"/NULL implícito en ambos sentidos.
- En modo "copia masiva" (`jCheckBox1` marcado), no se valida duplicado alguno: puede insertar filas repetidas si se ejecuta más de una vez.

**Notas / peculiaridades:**
- El mensaje de error para "Falta factor de la unidad." se reutiliza textualmente tanto para el campo `valor` como para el campo `precio` (copy-paste, mensaje poco preciso para el segundo caso).
- La columna oculta "Title 4" del modelo de `DETALLE` es, en la práctica, el ID de la fila en `detalle_producto`, pero el modelo generado por el builder la marca como editable (`canEdit[3]=true`) sin que exista ningún editor de celda cableado — vestigio del generador de formularios NetBeans.

---

### AgregarExistecias.java — Agregar/Modificar Existencias y Datos de un Producto (sic "Existecias")
**Propósito:** Diálogo para editar referencia/código/nombre/PVP/costo de un producto existente y, opcionalmente, sumarle una cantidad de existencia inicial, usado desde varias pantallas de ventas y desde `GestionInventario` para alta rápida.

**Cómo se invoca / navegación:** `JDialog`, con **9 constructores sobrecargados**: por cada pantalla `Ventas`, `Ventas1`, `Ventas2`, `Ventas3` hay una variante con datos ya conocidos (`referencia_, codigo_barras_, nombre_producto_, precio_venta, precio_compra`) y otra que solo recibe el `id` y los autocarga (`CargarProductoVerifiarExistecia`); y una variante adicional para `GestionInventario` que abre el formulario vacío con los campos de precio deshabilitados (solo para productos "rápidos" sin precio aún).

**Componentes de interfaz:**
- `referencia`, `codigoBarras`, `nombre` (JTextField).
- `pvp`, `precioCompra1` (JTextField, formateados como moneda al perder foco).
- `Cantidad` (JTextField) — existencia a sumar.
- `categorias` (JComboBox, presente en el layout pero no se usa funcionalmente en el flujo descrito).
- `GANANCIA` (JLabel, rojo) — margen de ganancia calculado como `(pvp-costo)/costo*100`, formateado como porcentaje.
- Botón `jButton1` "R" — genera nueva referencia correlativa con prefijo `1-`.
- Botones `jButton3` "Cerrar", `jButton5` "Guardar Producto".

**Funcionalidades atómicas (numeradas):**
1. Al abrir con datos conocidos: rellena todos los campos, formatea `pvp`/`precioCompra1` como moneda, calcula y muestra `GANANCIA`.
2. Al abrir solo con `id` (variante `CargarProductoVerifiarExistecia`): hace `SELECT REFERENCIA, CODIGO_BARRAS, NOMBRE, PRECIO_VENTA, PRECIO_COMPRA FROM productos WHERE ID=id`, rellena los campos y calcula `GANANCIA` igual que el punto 1; pone foco en `Cantidad`.
3. Al abrir desde `GestionInventario` (producto "rápido" nuevo): deja todos los campos de texto vacíos y **deshabilita** `pvp`, `precioCompra1` y `Cantidad` (edición de precios no permitida en este flujo).
4. `referencia` + Enter: si `codigoBarras` está vacío, lo copia desde `referencia`.
5. Botón **"R"** (`jButton1`): `referencia.setText("1-" + ((long) cargarReferencia()))`, marca `nuevaReferencia=true` (referencia con prefijo de sucursal/sede `1-`).
6. Botón **Guardar Producto** (`jButton5`): valida `nombre` no vacío (**"Falta el nombre del producto."**), `pvp` ≥ 0 (**"Falta precio de venta del producto."**), `precioCompra1` ≥ 0 (**"Falta precio de compra del producto."**). Si pasa: `UPDATE productos SET REFERENCIA=.., CODIGO_BARRAS=.., NOMBRE=.., PRECIO_COMPRA=.., PRECIO_VENTA=.., ID_CATEGORIA_IMPUESTO='1', ID_UNIDADES='1', STOCK_MIN='0', STOCK_MAX='0', ACTIVO='TRUE' WHERE ID=id`; si `Cantidad` tiene texto, llama `movimiento_existencias(id, cantidad)`; muestra **"Producto Modificado Correctamente."**; refresca la tabla de la pantalla padre que corresponda (`Obj.cargarProductos()`, etc.) y pone foco en su `BUSCAR`; si `nuevaReferencia` era true, llama `modificar_referencia(referencia)` (recorta el prefijo `"1-"` y actualiza `configuracion.referencia` con el número puro).
7. `movimiento_existencias(id, cantidad)`: si el producto no tiene fila en `stockproductos`, `INSERT INTO stockproductos (ID,LUGAR,CANTIDAD,ESTANTE) VALUES (id, ID_LUGAR("Principal"), cantidad, '')`; si ya existe, `UPDATE stockproductos SET CANTIDAD = CANTIDAD + cantidad WHERE ID=id`; en ambos casos registra `guardarMovimientoInventario(id, cantidad, "(Entrada) Compra IF", precioCompra1)` → `INSERT INTO movimiento_inventario (ID, CANTIDAD, FECHA, MOVIMIENTO, PRECIO, ID_USUARIO)`.
8. Botón **Cerrar** (`jButton3`) / tecla Escape: cierra el diálogo y devuelve el foco a `BUSCAR` de la pantalla padre correspondiente, sin guardar.

**Datos / tablas de BD leídas o escritas:**
- `productos` (SELECT por ID; UPDATE de REFERENCIA, CODIGO_BARRAS, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, ID_CATEGORIA_IMPUESTO, ID_UNIDADES, STOCK_MIN, STOCK_MAX, ACTIVO).
- `stockproductos` (INSERT/UPDATE CANTIDAD).
- `movimiento_inventario` (INSERT con movimiento `"(Entrada) Compra IF"`).
- `ubicacion` (SELECT ID por nombre `"Principal"`).
- `configuracion` (UPDATE columna `referencia`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- Esta pantalla **fuerza** `ID_CATEGORIA_IMPUESTO='1'` y `ID_UNIDADES='1'` en cada guardado, sin permitir elegir otra categoría de impuesto o unidad — asume que esos IDs son valores por defecto válidos para cualquier producto editado desde aquí (riesgo si esos IDs no corresponden siempre al mismo significado en la base de datos destino).
- También fuerza `STOCK_MIN='0'` y `STOCK_MAX='0'` en cada guardado, sobrescribiendo cualquier valor de stock mínimo/máximo que el producto tuviera previamente.

**Notas / peculiaridades:**
- Nombre de clase con errata ("Existecias" en vez de "Existencias"), consistente con otras erratas del proyecto ("exisencia", "intena").
- El combo `categorias` existe en el formulario pero no tiene ningún manejador de evento relevante; parece un remanente de una versión anterior del diálogo.
- Fuerte duplicación de código entre las variantes de constructor (prácticamente el mismo cuerpo repetido 8 veces, una por cada combinación de pantalla padre × con/sin datos).

---

### CargarCompra.java — Selector de Factura de Compra para Cargar/Editar
**Propósito:** Diálogo de búsqueda que permite elegir una factura de compra ya registrada (por número, por rango de fechas, o escribiendo directamente el número) para cargarla en `GestionInventario` y poder modificarla.

**Cómo se invoca / navegación:** `JDialog` modal, único constructor `CargarCompra(GestionInventario parent, boolean modal)`, invocado desde el botón `jButton5` ("...") de `GestionInventario`.

**Componentes de interfaz:**
- `BUSCAR` (JTextField) — filtro de texto libre o número de factura exacto.
- `fecha_i`, `fecha_i1` (DateChooserCombo, no editables por teclado) — fecha inicial/final (presentes en el layout; los manejadores `onCommit` están comentados, por lo que **no filtran realmente** la tabla).
- `VENTAS` (JTable, nombre heredado de un formulario de ventas reutilizado) columnas: NUMERO FACTURA, FECHA, TOTAL FACTURA, PROVEEDOR.

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga automáticamente las **últimas 50 facturas** de compra (`ORDER BY FECHA DESC LIMIT 50`), agrupadas por `NUMERO_FACTURA, FECHA`, con el nombre de proveedor en mayúsculas.
2. `BUSCAR` KeyReleased: filtra `VENTAS` por regex sobre columnas 0-4 (cada palabra separada por espacio se convierte en comodín).
3. Doble clic en una fila de `VENTAS`: llama `ObjGestionInventario.CarcarCompras(numeroFactura, proveedor)` (nótese "Carcar" sin la "g", typo consistente en varias clases) y cierra el diálogo.
4. `BUSCAR` + Enter (`pasarFocusVerificarNumeros`): si el texto no está vacío, verifica con `VerificarFactura(texto)` si existe esa factura exacta en `compras`; si existe, llama igualmente a `CarcarCompras` con el texto tecleado y el proveedor de la fila actualmente seleccionada, y cierra; si no existe, muestra **"La factura que intenta cargar no existe."** y mantiene el foco en `BUSCAR`.

**Datos / tablas de BD leídas o escritas:**
- `compras` + `proveedor` (SELECT NUMERO_FACTURA, FECHA, TOTAL_FACTURA, RAZON_SOCIAL agrupado, límite 50).
- `compras` (SELECT numero_factura para verificar existencia).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- El listado inicial está limitado a 50 facturas más recientes; no hay paginación, por lo que facturas más antiguas solo son accesibles tecleando su número exacto en `BUSCAR` (gracias a la validación por Enter), no por scroll.
- El filtro por rango de fechas (`fecha_i`/`fecha_i1`) está presente visualmente pero inoperante (código comentado) — es una funcionalidad incompleta/deshabilitada, no un error silencioso de SQL.

**Notas / peculiaridades:**
- Reutiliza clases de renderer (`CurrencyTableCellRenderer_EDITAR_VENTAS`) y nombres de componentes (`VENTAS`) propios de una pantalla de ventas, evidenciando que este diálogo fue clonado desde un selector de ventas y reutilizado para compras sin renombrar completamente sus partes internas.
- El título visible del diálogo es **"Seleccionar Venta"**, aunque funcionalmente selecciona una **compra** — texto no corregido tras la reutilización.

---

### ImportarProductos.java — Importación Masiva de Productos desde Excel
**Propósito:** Pantalla para importar productos en lote desde un archivo Excel (.xlsx), crear automáticamente categorías nuevas detectadas en el archivo, y dar de alta cada producto con su existencia inicial.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `ImportarProductos(String au)`. Usa Apache POI (`org.apache.poi.xssf.usermodel.*`) para leer el archivo.

**Componentes de interfaz:**
- `PRODUCTOS_IMPORTADOS` (JTable) columnas: `#`, REFERENCIA, CODIGO BARRA, NOMBRE PRODUCTO, EXISTENCIAS, PRECIO COMPRA, PRECIO DE VENTA, CATEGORIA, guardado(oculta).
- `BUSCAR` (JTextField), `categorias` (JComboBox, filtro de categorías ya existentes en BD — para una vista alternativa de productos ya cargados, no de los importados).
- Botones: `jButton4` "... Abrir Excel", `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar".
- Menú contextual sobre la tabla: "Eliminar Producto", "Modificar Cantidad / Precio Compra / Precio Venta" (sin implementación real), "Agregar Producto" (sin implementación real).
- `open` (JFileChooser).

**Funcionalidades atómicas (numeradas):**
1. Botón **"... Abrir Excel"**: abre `JFileChooser` (modo guardar, `showSaveDialog`, usado atípicamente como selector de archivo a abrir); si se aprueba, llama `readXLSXFile(ruta)`.
2. `readXLSXFile(file)`: abre el libro con `XSSFWorkbook`, toma la **primera hoja** (`getSheetAt(0)`), recorre filas **saltando la fila 0** (encabezados); por cada fila, recorre celdas y según su tipo (`CELL_TYPE_STRING` o `CELL_TYPE_NUMERIC`) agrega el valor a un vector (los números se truncan a `long`); si la celda en posición 1 del vector no es vacía, agrega una columna `true` (bandera "guardado") y añade la fila a `PRODUCTOS_IMPORTADOS`; si la tabla queda con filas, habilita el botón "Guardar".
3. Tecla espacio sobre `PRODUCTOS_IMPORTADOS`: decrementa en 1 la columna 2 (existencia) de la fila seleccionada; si llega a 0 o menos, elimina la fila completa de la tabla (mecanismo de ajuste manual rápido antes de guardar).
4. Botón **Guardar** (`jButton1`): 
   - Recolecta todas las categorías distintas (columna 7) presentes en la tabla; para cada una que no exista ya en `categorias` (`verificarClategoria`), hace `INSERT INTO CATEGORIAS (CATEGORIA)`.
   - Para cada fila de la tabla, determina referencia/código de barras con la regla: si ambos (columnas 1 y 2) son `"0"`, autogenera referencia correlativa empezando en `1000001` (incrementando por cada producto) y deja código de barras vacío; si solo referencia es `"0"`, usa el código de barras como referencia también; si solo código es `"0"`, usa la referencia y deja código `"0"`; si ninguno es `"0"`, usa ambos tal cual.
   - Verifica duplicados (`verificarReferencia`, `verificarCodigoBarras`, `verificarNombre`); si no hay duplicado, `INSERT INTO productos (REFERENCIA, CODIGO_BARRAS, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, ID_CATEGORIA, ID_CATEGORIA_IMPUESTO='1', ID_UNIDADES='1', STOCK_MIN='0', STOCK_MAX='0')` y llama `movimiento_existencias(idNuevoProducto, existenciaColumna4)`; si hay duplicado, marca la columna "guardado" en `"false"` (deja constancia visual de que esa fila no se guardó).
   - Al final muestra **"Se importaron con exito .N producto"** (el número de filas efectivamente insertadas, con el texto literal incluyendo el punto pegado a "exito").
5. `movimiento_existencias`: igual patrón que en otras pantallas — INSERT o UPDATE sobre `stockproductos` según exista o no fila previa para ese producto.
6. `categorias` (combo, filtro superior) ActionPerformed: si se elige `"*.*"` carga todos los productos ya existentes en BD (no los importados) vía `cargarProductos("-1")`; si se elige una categoría concreta, filtra por `ID_CATEGORIA`. Esta vista es informativa/paralela, no afecta el proceso de importación.
7. Botón **Limpiar** (`jButton2`): resetea banderas de estado (`updateSave=false`, `referencia_nueva=false`) — **no vacía la tabla de importados** (la línea `limpiarTabla()` del método `limpiar()` está comentada), por lo que "Limpiar" en esta pantalla deja intactas las filas ya cargadas del Excel.
8. Menú contextual "Eliminar Producto": elimina la fila seleccionada de la tabla (`PRODUCTOS_IMPORTADOS`), sin tocar la base de datos (porque aún no se ha guardado).

**Datos / tablas de BD leídas o escritas:**
- `categorias` (SELECT/INSERT nuevas categorías detectadas en el Excel).
- `productos` (INSERT masivo; SELECT para verificar duplicados por REFERENCIA/CODIGO_BARRAS/NOMBRE).
- `stockproductos` (INSERT/UPDATE CANTIDAD por producto nuevo).
- `ubicacion` (no se usa `ID_LUGAR` en esta variante de `movimiento_existencias`; el INSERT a `stockproductos` deja `LUGAR` vacío).
- `configuracion` (no se toca aquí, a diferencia de otras pantallas — no hay manejo de referencia correlativa global vía `configuracion.referencia`, la numeración 1000001+ es local a esta pantalla y no se coordina con el contador de `Productos.java`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- La numeración automática de referencias (`1000001`, `1000002`, ...) es un contador **local al proceso de importación actual**, reiniciado cada vez que se abre la pantalla; no consulta ni actualiza `configuracion.referencia`, por lo que puede colisionar con referencias ya asignadas manualmente desde `Productos.java` si ese rango se superpone.
- Solo se lee la **primera hoja** del Excel; hojas adicionales se ignoran silenciosamente.
- El tipo de celda `CELL_TYPE_BLANK` está explícitamente ignorado (código comentado), por lo que celdas vacías en medio de una fila simplemente no agregan ningún elemento al vector — si eso desalinea las columnas esperadas, el resto de la fila puede leerse corrido de posición y producir datos erróneos sin ningún aviso.

**Notas / peculiaridades:**
- El `JFileChooser` se abre con `showSaveDialog` (diálogo de "Guardar") para en realidad **elegir un archivo a abrir**, lo cual puede mostrar al usuario el botón "Guardar" en vez de "Abrir" según el Look & Feel — comportamiento inconsistente con la intención real del botón "... Abrir Excel".
- El combo `categorias` superior no tiene relación con el archivo Excel importado: es un filtro de los productos *ya en base de datos*; coexiste de forma algo confusa con la tabla de importados en la misma pantalla.
- Hay una instancia `XSSFWorkbook test = new XSSFWorkbook();` creada y nunca usada dentro de `readXLSXFile` (código muerto).

---

### InventarioFacil.java — Inventario Fácil (alta/edición de producto con 3 niveles de precio + ajustes rápidos de stock)
**Propósito:** Pantalla "todo en uno" para crear o editar un producto con hasta tres precios de venta (minorista/mayorista/VIP, según configuración), importar productos masivamente desde Excel, y sumar/restar existencias directamente, incluyendo utilidades administrativas de limpieza de existencias.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `InventarioFacil(String aux)`.

**Componentes de interfaz:**
- `referencia` (+ botón `buton_referencia` para autogenerar), `codigoBarras`, `nombre`, `categoria` (combo).
- `impuestos` (combo de categorías de impuesto), `pvp` (precio con IVA), `precioCompra1`.
- `pvp1` ("PVP MAYORISTA"), `pvp2` ("PVP VIP") — visibles/habilitados u ocultos según `MenuPrincipal.DATOS_OBLIGATORIOS.get("LISTA_PRECIOS")`: `"1"` oculta ambos y columnas correspondientes de la tabla; `"2"` oculta solo VIP.
- `Cantidad` (a sumar), `existencias` (solo lectura, existencia actual).
- `PRODUCTOS_IMPORTADOS` (JTable) columnas: `#`, REFERENCIA, CODIGO BARRA, NOMBRE PRODUCTO, EXISTENCIAS, PRECIO COMPRA, PVP MINORISTA, PVP MAYORISTA, PVP VIP, CATEGORIA, guardado(oculta).
- `categorias` (JComboBox, oculto con `setVisible(false)` tras configurar — vestigio no usado visualmente aunque sigue cargado).
- Botones: `jButton4` "Abrir Excel", `jButton1` "Guardar" (import masivo), `jButton2` "Limpiar", `jButton3` "Cerrar", `jButton5` "Guardar Producto" (alta/edición individual), `SUMAR` "Sumar Existencias", `RESTAR` "Restar Existencias".
- Menú contextual sobre la tabla: "Eliminar Existencias Producto Seleccionado", "Eliminar Existencias de Productos Filtrados" (rojo), "Eliminar Existencias Negativas y Colocarlas en Cero", separador, "Eliminar Producto de la Base de Datos" (rojo).

**Funcionalidades atómicas (numeradas):**
1. Igual mecánica de importación Excel que `ImportarProductos.java` (misma lógica de `readXLSXFile`), pero con columnas PVP1/PVP2 adicionales.
2. `ListarProducto(columna, valor)` (por código de barras vía Enter en `codigoBarras`): carga producto completo con sus 3 precios y categoría de impuesto; calcula `pvpiva_ = pvp_ + pvp_*%iva/100` para mostrar en `pvp`; habilita `SUMAR`/`RESTAR`; llama `cargarCantidadExistencia()`.
3. Botón **"..."** junto a referencia (`buton_referencia`): autogenera referencia con o sin prefijo `"1-"` según `DATOS_OBLIGATORIOS.get("REFERENCIA_EXTENDIDA_NORMAL")` (`"1"`→con prefijo, `"0"`→sin prefijo); marca `referencia_nueva=true`; el botón mismo se oculta/muestra según ese mismo parámetro al llamar `limpiar()`.
4. Botón **Guardar Producto** (`jButton5`): resuelve `ID_CATEGORIA_IMPUESTO` según régimen (comun o simplificado, mismo criterio que otras pantallas) tomando siempre el valor del combo `impuestos` elegido por el usuario; valida nombre no vacío/no placeholder (**"Falta el nombre del producto."**), categoría elegida (**"Seleccione la categoria del producto."**), `pvp_ < 0` (**"Falta precio de venta del producto."**), `preciocompra_ < 0` (**"Falta precio de compra del producto."**).
   - Si `updateSave` (edición): verifica duplicados excluyendo el ID propio; si ninguno, `UPDATE productos` con `PRECIO_VENTA` recalculado sin IVA (`pvpConIva/(1+%/100)`), `PRECIO_VENTA2=pvp1`, `PRECIO_VENTA3=pvp2` (estos dos **sin** recálculo de IVA, se guardan tal cual se muestran); si `Cantidad` tiene texto llama `movimiento_existencias`; si `referencia_nueva`, actualiza `configuracion.referencia`; mensaje **"Producto Modificado Correctamente."**; si `trsfiltro.getViewRowCount()<=1` recarga la lista completa (`limpiar()`), si no solo limpia el formulario de producto (`limpiar_producto_nuevo()`).
   - Si no (`alta`): verifica duplicados globales; si ninguno, `INSERT INTO productos (... PRECIO_VENTA2, PRECIO_VENTA3 ...)` con `ACTIVO='TRUE'`; si `Cantidad` tiene texto llama `movimiento_existencias`; actualiza referencia correlativa si aplica; mensaje **"Producto Agregado Correctamente."**; limpia todo y recarga.
5. Botón **Sumar Existencias** (`SUMAR`): valida `nombre` y `Cantidad` no vacíos (**"Falta el nombre del producto."**, **"Falta la cantidad producto."**); llama `movimiento_existencias(ID_PRODUCTO(nombre), cantidad)`; mensaje **"Existencias modificada correctamente."**; limpia solo el formulario de producto (no recarga la tabla completa).
6. Botón **Restar Existencias** (`RESTAR`): misma validación; llama `movimiento_existencias_menos` (`UPDATE stockproductos SET CANTIDAD = CANTIDAD - cantidad`); mismo mensaje de éxito. **No valida que la existencia resultante sea ≥ 0** (puede dejar existencia negativa sin aviso).
7. Menú contextual "Eliminar Existencias Producto Seleccionado": confirma **"Desea eliminar existencias del producto deleccionado?"** (sic "deleccionado"); si acepta, `eliminarExistenciasProductos(id)` pone `CANTIDAD='0'` (o inserta fila en 0 si no existía) y registra movimiento `"(Salida) Elimino Existencias"`.
8. Menú contextual "Eliminar Existencias de Productos Filtrados": **doble confirmación** (**"Desea eliminar las existencias de los productos filtrados, se perderan las existencias?"** y luego **"En realidad desea eliminar existencias, se perderan todos los datos?"**); si ambas se aceptan, aplica `eliminarExistenciasProductos` a **todas las filas actualmente visibles según el filtro** (no solo la seleccionada).
9. Menú contextual "Eliminar Existencias Negativas y Colocarlas en Cero": confirma **"Desea eliminar las existencias negativas y colocarlas en cero?"**; si acepta, `UPDATE stockproductos SET CANTIDAD='0' WHERE CANTIDAD < 0` (operación masiva sobre toda la tabla, sin filtro de categoría/búsqueda).
10. Menú contextual "Eliminar Producto de la Base de Datos": confirma **"Desea eliminar por completo de la base de datos el producto seleccionado?"**; si acepta, `eliminarProductoMenuPrincipal(id)` hace `DELETE FROM stockproductos WHERE ID=id` y luego `DELETE FROM productos WHERE ID=id` (en ese orden, cada uno en su propio try/catch independiente); mensaje **"Producto eliminado correctamente."**.
11. `BUSCAR`/`nombre` (el campo `nombre` también actúa como buscador al escribir, vía `nombreKeyReleased`) filtran `PRODUCTOS_IMPORTADOS` por regex sobre columnas 0-9.
12. Tecla F5 global pone foco en `nombre` (no en `BUSCAR`, a diferencia de otras pantallas) y limpia su placeholder.

**Datos / tablas de BD leídas o escritas:**
- `productos` (SELECT/INSERT/UPDATE completo incluyendo PRECIO_VENTA2/PRECIO_VENTA3; DELETE).
- `categorias`, `categoria_impuesto`, `unidades` (SELECT para combos y resolución de IDs).
- `stockproductos` (INSERT/UPDATE/DELETE CANTIDAD, también operación masiva de corrección de negativos).
- `movimiento_inventario` (INSERT por cada entrada/salida, con motivos `"(Entrada) Compra IF"`, `"(Salida) Venta IF"`, `"(Salida) Elimino Existencias"`).
- `ubicacion` (SELECT ID por `"Principal"`).
- `configuracion` (UPDATE referencia correlativa).

**Reportes generados:** Ninguno invocado directamente.

**Reglas de negocio / validaciones no obvias:**
- Los tres niveles de precio (minorista con IVA recalculado, mayorista y VIP tal cual digitados) tienen tratamiento **asimétrico**: solo el precio principal se "limpia" de IVA antes de guardar; los otros dos se graban exactamente como el usuario los escribió.
- La visibilidad de columnas/mayorista/VIP depende de un parámetro global de configuración (`LISTA_PRECIOS`), por lo que el comportamiento de la pantalla cambia completamente según la instalación.
- `RESTAR` no impide existencia negativa; `Eliminar Existencias Negativas...` es la única herramienta administrativa para corregirlas, y actúa sobre **toda la tabla de stock**, no solo sobre el producto en edición.

**Notas / peculiaridades:**
- Prácticamente duplica la lógica de `ImportarProductos.java` para la parte de importación Excel (mismo algoritmo de lectura de celdas), con columnas extra para los 3 precios.
- El combo `categorias` se carga pero se oculta inmediatamente (`categorias.setVisible(false)`), quedando sin uso visible — posible vestigio de una versión previa del formulario.
- Mezcla responsabilidades muy distintas en una sola pantalla (alta/edición de producto + importación Excel + ajuste de stock + borrado masivo administrativo), lo que la hace considerablemente más compleja y riesgosa que sus pantallas "hermanas" (`Productos.java`, `ImportarProductos.java`, `MovimientoExistencias.java`) combinadas.

---

### InventarioGeneral.java — Inventario General (listado/valoración y activar‑desactivar productos)
**Propósito:** Reporte/listado maestro de todos los productos (activos e inactivos) con su valoración de inventario (total a precio de compra y a precio de venta), con filtros por categoría/estado/texto y la posibilidad de activar o desactivar un producto directamente desde la grilla.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `InventarioGeneral(String aux)`.

**Componentes de interfaz:**
- `PRODUCTOS_IMPORTADOS` (JTable, nombre reutilizado de otras pantallas pero aquí es de solo listado) columnas: ESTADO(checkbox boolean), ID, REFERENCIA, CODIGO BARRA, CATEGORIA, NOMBRE PRODUCTO, IVA, EXISTENCIAS, PRECIO DE COMPRA, PRECIO DE VENTA, TOTAL PRECIO COMPRA, TOTAL PRECIO VENTA.
- `TPRODUCTOS` (total de productos en BD), `TPRODUCTOSFILTRADOS` (total tras filtro), `TPCOMPRA`, `TPVENTA` (sumatorias, todos JTextField de solo lectura).
- `BUSCAR` (texto libre), `categorias` (combo con `"*.*"` + lista), `estado` (combo fijo `{"*.*","ACTIVO","INACTIVO"}`).
- Botones `jButton2` "Limpiar", `jButton3` "Cerrar".
- Menú contextual: "Activar / Desactivar Producto".

**Funcionalidades atómicas (numeradas):**
1. Al abrir: carga combo de categorías (con `"*.*"` al inicio), calcula `TotalProductos()` (`SELECT COUNT(ID) FROM productos`) y llama `limpiar()`.
2. `cargarProductos(llamda)` construye dinámicamente una de 4 consultas SQL según la combinación de `categorias`/`estado` seleccionados (ambos en "*.*", solo categoría filtrada, categoría+estado filtrados, o solo estado filtrado), siempre con UNION para incluir productos sin fila en `stockproductos` (existencia 0, totales 0); cada fila trae `ACTIVO`, `ID`, `REFERENCIA`, `CODIGO_BARRAS`, `CATEGORIA`, `NOMBRE`, `categoria_impuesto.CATEGORIA` (IVA), `CANTIDAD`, `PRECIO_COMPRA`, `PRECIO_VENTA con IVA`, `CANTIDAD*PRECIO_COMPRA` y `CANTIDAD*PRECIO_VENTAconIVA`.
3. Doble clic o tecla espacio sobre una fila: `activarDesactivar()` invierte el booleano de la columna ESTADO en memoria y hace `UPDATE productos SET ACTIVO = 'true'/'false' WHERE ID=producto` inmediatamente (sin confirmación, sin mensaje de éxito/alerta — cambio silencioso e inmediato).
4. `BUSCAR` filtra por regex sobre columnas 0-7; al escribir recalcula totales filtrados (`calcularTotalesBIG()`), que suma columnas 10 y 11 (totales compra/venta) de **todas las filas visibles según el filtro actual** y actualiza `TPRODUCTOSFILTRADOS`, `TPCOMPRA`, `TPVENTA`.
5. Cambiar `categorias` o `estado` dispara recarga completa (`cargarProductos("CATEGORIAS")`) con la combinación de filtros SQL correspondiente (ver punto 2) y reselecciona la primera fila.
6. Botón **Limpiar**: resetea `categorias`/`estado` a `"*.*"`, restaura placeholder de `BUSCAR`, recarga con `cargarProductos("EMPESO")` (sic, parámetro de depuración sin efecto real en la lógica — solo se usa en un `System.out.println`).
7. Botón **Cerrar**: cierra la pantalla (vuelve a `Menunew` si `aux` corresponde).

**Datos / tablas de BD leídas o escritas:**
- `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades` (SELECT combinados para la grilla y totales).
- `productos` (UPDATE columna `ACTIVO` al activar/desactivar).

**Reportes generados:** Ninguno invocado directamente (es en sí mismo un "reporte" en pantalla, no genera un .jrxml).

**Reglas de negocio / validaciones no obvias:**
- Activar/desactivar un producto es una acción **instantánea sin confirmación** (a diferencia de casi todas las demás acciones destructivas del sistema, que sí piden `JOptionPane.showConfirmDialog`).
- "Total Precio Venta" ya incluye el IVA calculado (`PRECIO_VENTA + PRECIO_VENTA*%IVA/100`), no es el precio neto de venta.
- El total general de productos (`TPRODUCTOS`) cuenta **todos** los productos (activos e inactivos, sin filtrar), mientras que `TPRODUCTOSFILTRADOS` sí respeta el filtro de texto/categoría/estado vigente — son dos magnitudes distintas mostradas simultáneamente.

**Notas / peculiaridades:**
- Hay un bloque completo de código comentado (`CargarTotales66`) que calculaba totales con `double` en vez de `BigDecimal`, reemplazado por `CargarTotalesBIG` (vestigio de una refactorización para evitar errores de precisión en dinero) — mantenido en otras pantallas hermanas (`GestionInventario`, `VerificarCompras`) con el mismo patrón de nombres.
- El código de permisos por rol (mostrar/ocultar ítems de menú según `SUPERADMINISTRADOR`/`ADMINISTRADOR`) existe comentado en `limpiar()` pero está deshabilitado; actualmente **todos los usuarios ven el mismo menú contextual completo**, independientemente de su privilegio.

---

### GestionInventario.java — Gestión de Inventario (registro de compras / entradas y salidas)
**Propósito:** Pantalla central para registrar compras a proveedores (o cualquier otro movimiento de entrada/salida de inventario), armando un "carrito" de productos con cantidad, precio unitario, descuento e IVA por línea, calculando totales, y permitiendo además editar precios de venta/compra del catálogo al vuelo durante la captura.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `GestionInventario(String aux)`. Internamente instancia y mantiene viva una `ListaProductos` propia (`this.ListaProductos = new ListaProductos(this,true)`) como buscador auxiliar.

**Componentes de interfaz:**
- `Razon` (combo): `"(Entrada) Compra", "(Entrada) Devolucion", "(Entrada) Traspaso", "(Salida) Compra", "(Salida) Devolucion", "(Salida) Rotura", "(Salida) Traspaso"`.
- `fecha` (DateChooserCombo), `PROVEEDOR` (solo lectura, se llena vía diálogo `buscarProveedor`), botón `jButton4` "...".
- `FACTURA` (JTextField), `FORMA_PAGO` (combo, cargado desde `tipo_pago`).
- `BUSCAR` (JTextField, buscador de productos por código/nombre), `PRODUCTOS` (JTable catálogo lateral, columnas ID,REFERENCIA,CODIGO DE BARRAS,PRODUCTO,UNIDADES,CATEGORIA,EXISTENCIAS,PRECIO COMPRA,PRECIO VENTA,IMPUESTOS,ESTANTE — usado como fuente para `ListaProductos`).
- `GESTIONAR_INVENTARIO` (JTable, el "carrito") columnas: ID(oculta), PRODUCTO, CANTIDAD, VALOR UNITARIO, DESCUENTO, IVA, VALOR TOTAL, PRECIO VENTA, UPDATE(oculta, bandera "este renglón fue modificado manualmente").
- `GESTIONAR_INVENTARIO2` (JTable oculta de la UI visible, usada como "snapshot" del estado original al cargar una factura existente, para poder calcular diferencias al actualizar).
- `Total_compras`, `TotalVentas` (solo lectura).
- Botones: `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar", `jButton5` "..." (cargar factura existente), `jButton6` "Agregar Producto Rapido".
- Menú contextual sobre `GESTIONAR_INVENTARIO`: "Eliminar Producto", "Modificar Cantidad / Precio Compra / Precio Venta" (sin implementación real), separador, "Calcular Precio Venta", separador, "Modificar Producto Rapido", "Eliminar Existencias".
- Menú contextual sobre `PRODUCTOS`: "Agregar Producto", "Recargar Lista Productos" (ambos sin implementación real en el código visible).

**Funcionalidades atómicas (numeradas):**
1. Al abrir: carga `FORMA_PAGO` desde `tipo_pago`, llama `limpiar(false)` (carga catálogo completo en `PRODUCTOS` y lo copia a `ListaProductos.TABLA`).
2. `cargarProductos()`: SELECT de catálogo activo (`ACTIVO='true'`) con su existencia en `LUGAR='1'`, uniendo categoría, unidad e impuesto; UNION con productos activos sin existencia. Precio mostrado = `lista_precio_` (por defecto `"PRECIO_VENTA"`) + su IVA.
3. Doble clic en una fila de `GESTIONAR_INVENTARIO` abre, según la **columna** donde se hizo clic, un diálogo específico: columna 2→`Cantidad`, columna 3→`PrecioCompra`, columna 4→`Descuento`, columna 5→`Iva`, columna 7→`PrecioVenta`; cada uno al confirmar escribe el valor en esa celda, marca la columna "UPDATE" en `"true"` y recalcula totales (`CargarTotalesBIG`).
4. `BUSCAR` + Enter: si el filtro deja **exactamente 1** fila visible en `PRODUCTOS`, agrega ese producto directamente al carrito (`ListarProductoCodigoBarras`) y abre el diálogo `Cantidad` para esa línea; si quedan **más de 1** o **0** filas, abre el buscador emergente `ListaProductos` con el mismo texto de búsqueda precargado.
5. `ListarProductoCodigoBarras(ID, nombre, precioCompra, precioVenta)`: si el producto no está ya en el carrito, agrega una fila con cantidad inicial `1`, descuento `0`, IVA `0`, total = precioCompra (sin descuento/IVA aún), precio de venta recibido, y bandera `"false"`; selecciona esa fila; recalcula totales. Si el producto **ya estaba** en el carrito, no lo duplica: solo reselecciona la fila existente (`SeleccionarProdutoAgregadoTabla` / `indexNombreProductoTableVentas`, comparación por **nombre**, no por ID).
6. `CargarTotalesBIG()`: por cada fila del carrito, calcula `ValorUnitarioConDescuento = ValorUnitario - (ValorUnitario*descuento)`, `PrecioCompra = ValorUnitarioConDescuento + (ValorUnitarioConDescuento*iva)`, escribe ese `PrecioCompra` recalculado de vuelta en la columna 6 de la fila; acumula `totalPrecioCompra += PrecioCompra*cantidad` y `totalPrecioVenta += PrecioVenta(col7)*cantidad`; actualiza `Total_compras`/`TotalVentas` con formato moneda.
7. Menú contextual "Eliminar Producto": quita la fila seleccionada del carrito (excepto si su nombre es `"TOTALES"`, salvaguarda de un patrón antiguo de fila de totales ya no usado activamente) y recalcula totales.
8. Menú contextual "Calcular Precio Venta": si la fila seleccionada no es `"TOTALES"`, toma el "VALOR UNITARIO" (columna 3, que en este contexto representa el costo) y si es > 0 abre `CalcularPrecioVenta` (calculadora de margen, ver su propia sección — nótese que en esa pantalla el botón de aplicar no está realmente cableado); si es 0, muestra **"Falta precio de compra."**.
9. Menú contextual "Modificar Producto Rapido": abre `ProductoRapidoNuevo(this, true, idProductoSeleccionado)` para renombrar/recategorizar sin tocar precios.
10. Menú contextual "Eliminar Existencias": confirma **"Desea eliminar existencias del producto deleccionado?"** (sic); si acepta, pone `CANTIDAD='0'` en `stockproductos` para ese producto, registra movimiento `"(Salida) Elimino Existencias"` con precio `0`, muestra **"Existencias modificadas"** (sin punto final, a diferencia de otros mensajes del sistema) y refresca `cargarProductos()`.
11. Botón **"Agregar Producto Rapido"** (`jButton6`): abre `ProductoRapidoNuevo(this, true)` en modo alta (sin ID) para crear un producto nuevo con solo referencia/código/nombre/categoría (precio queda en 0, a completar luego).
12. Botón **"..."** junto a FACTURA (`jButton5`): abre `CargarCompra(this, true)` para elegir una factura existente a editar.
13. Botón **"..."** junto a PROVEEDOR (`jButton4`): abre `buscarProveedor(this, true)` para elegir el proveedor de la compra.
14. Botón **Guardar** (`jButton1`): valida que el carrito no esté vacío (**"Debe escojer un producto al cual agregar existencias."**), que `PROVEEDOR` no esté vacío (**"Falta Información Proveedor."**) y que `FACTURA` no esté vacía (**"Falta Numero de Factura de la Compra."**). Según `GuardarActualizar`:
    - `false` (factura nueva): si `VerificarFactura(numeroFactura)` ya existe en BD, muestra **"El numero de factura que intenta agregar ya existe."**; si no, llama `guardar()`.
    - `true` (se cargó una factura existente vía `CargarCompra`/`CarcarCompras`): llama `ActualizarCompras()`.
15. `guardar()`: por cada línea del carrito, `guardarCompras(...)` → `INSERT INTO compras (FECHA, ID_PROVEEDOR, NUMERO_FACTURA, TOTAL_FACTURA, ID_TIPO_PAGO, FECHA_VENCE_COMPRA_DEBITO, ID_PRODUCTO, CANTIDAD, PRECIO_COMPRA, DESCUENTO, IVA, VALOR_TOTAL, TOTAL, ID_USUARIO, ESTADO)` donde `ESTADO` es `"DEBE"` si la forma de pago es `"DEBITO"`, si no `"PAGO"`, y `FECHA_VENCE_COMPRA_DEBITO` se calcula sumando los días de crédito del proveedor a la fecha actual; luego ajusta `stockproductos` **sumando** si `Razon` empieza con `"(Entrada)"` o **restando** en caso contrario, y registra `movimiento_inventario` con la `Razon` tal cual como texto de movimiento; si la columna "UPDATE" de esa línea es `"true"` (el usuario tocó precio/descuento/IVA manualmente), además actualiza `productos.PRECIO_COMPRA`/`PRECIO_VENTA` — con recálculo de IVA (`CalcularPrecioVenta`, forzando `ID_CATEGORIA_IMPUESTO='3'` al 19% si el producto no tenía impuesto asignado) si el régimen es "comun", o sin recálculo si es "simplificado". Al final: **"Existencias de productos Modificada Correctamente."** y `limpiar(false)`.
16. `ActualizarCompras()`: para cada línea marcada como "UPDATE=true", llama `ActualizarCompras(...)` (UPDATE de la fila en `compras` por `ID_PRODUCTO`+`NUMERO_FACTURA`) y ajusta `stockproductos` por la **diferencia** entre la cantidad actual y la cantidad original guardada en el snapshot `GESTIONAR_INVENTARIO2` (si subió, suma la diferencia con movimiento `"(Entrada) Compra"`; si bajó, resta la diferencia con movimiento `"(Salida) Devolucion"`); para cada línea presente en el snapshot pero ya eliminada del carrito actual, al final llama `EliminarProductosEliminadorGestionInventario2()` que hace `DELETE FROM compras WHERE ID_PRODUCTO=.. AND NUMERO_FACTURA=..` y revierte la existencia correspondiente restándola con movimiento `"(Salida) Devolucion"`. También actualiza `compras.TOTAL_FACTURA` para **todas** las filas de esa factura (no solo las modificadas), para mantener consistencia del total. Mensaje final idéntico al de `guardar()`.
17. Botón **Limpiar** (`jButton2`): si hay filas en el carrito, pregunta **"¿Desea perder el trabajo realizado?."** antes de vaciar; si está vacío, limpia directo.
18. Botón **Cerrar** (`jButton3`): si hay filas en el carrito, pregunta **"Procede a salir de gestion de inventario, ¿Desea perder el trabajo realizado?."** antes de cerrar; si está vacío, cierra directo. Mismo comportamiento al presionar **Escape**.
19. `limpiar(aux)`: vacía ambas tablas del carrito, resetea proveedor/factura/forma de pago/razón, y si `aux==false` recarga el catálogo (`cargarProductos()`); pone los totales en 0.

**Datos / tablas de BD leídas o escritas:**
- `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades` (SELECT catálogo).
- `tipo_pago` (SELECT para combo; resolución de `ID_TIPO_PAGO`).
- `compras` (INSERT por línea nueva; UPDATE por línea modificada de una factura existente; DELETE de líneas quitadas; UPDATE masivo de `TOTAL_FACTURA`; SELECT para verificar existencia de número de factura y para recargar una factura completa con `CarcarCompras`).
- `stockproductos` (UPDATE/INSERT según entradas/salidas; ajuste por diferencia al actualizar).
- `movimiento_inventario` (INSERT por cada movimiento de stock, con el texto de `Razon` como motivo, salvo en "Eliminar Existencias" que usa un texto fijo).
- `proveedor` (SELECT `DIAS_CREDITO` para calcular vencimiento de crédito).
- `ubicacion` (SELECT ID por `"Principal"`).

**Reportes generados:** Ninguno invocado directamente.

**Reglas de negocio / validaciones no obvias:**
- Detectar un producto "ya agregado" al carrito se hace comparando **nombre** (no ID) en mayúsculas — dos productos homónimos de distinta categoría serían tratados como el mismo renglón si ambos aparecen en el mismo carrito.
- Solo las líneas con bandera "UPDATE=true" (el usuario tocó manualmente cantidad/precio/descuento/IVA/precio de venta) propagan cambios al catálogo (`productos.PRECIO_COMPRA/PRECIO_VENTA`); las líneas agregadas sin tocar ningún valor nunca modifican el precio maestro del producto, solo el stock.
- El cálculo de precio de compra con descuento e IVA (`CargarTotalesBIG`) se recalcula y **sobrescribe** la columna "VALOR TOTAL" del carrito en cada refresco, usando siempre los valores crudos de descuento/IVA almacenados como fracción (ej. `0.19` para 19%), no como porcentaje entero.
- Al actualizar una factura existente, la comparación de cantidades usa un snapshot tomado en el momento de cargar (`GESTIONAR_INVENTARIO2`), por lo que si la existencia real en base de datos cambió por otra vía entre la carga y el guardado (p. ej. una venta concurrente), el ajuste por diferencia puede no reflejar el estado real del stock.

**Notas / peculiaridades:**
- `CalcularPrecioVenta(precioVentaConIva, idProducto)` (método interno, no confundir con la clase `CalcularPrecioVenta.java`) tiene el efecto secundario de **modificar permanentemente** `productos.ID_CATEGORIA_IMPUESTO` a `'3'` (fijo, asumido como 19%) si detecta que el producto no tenía impuesto asignado — esto ocurre de forma silenciosa dentro del flujo de guardado, sin avisar al usuario que se le asignó impuesto a un producto que no lo tenía.
- La variable de instancia `lista_precio_` permite en teoría listar el catálogo a partir de un precio distinto a `PRECIO_VENTA`, pero en el código disponible nunca se reasigna a otro valor — funcionalidad preparada pero no activada desde la UI visible.
- Reutiliza extensamente los mismos cálculos de `VerificarCompras.java`/`detalleCompra.java` (`DescuentoValorUnitario`, `ValorTotal`, `CargarTotalesBIG`) con firmas `static` idénticas copiadas en cada clase en lugar de un util compartido.

---

### MovimientoExistencias.java — Movimiento de Existencias (ajuste puntual de stock de un producto)
**Propósito:** Pantalla simple y acotada para sumar o restar existencias de **un solo producto** con una razón de movimiento explícita, y de paso fijar sus niveles de stock mínimo/máximo.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `MovimientoExistencias(String aux)`.

**Componentes de interfaz:**
- `referencia`, `codigoBarras`, `nombre` (solo lectura) — identificación del producto.
- `fecha` (DateChooserCombo), `Razon` (combo, mismas 7 opciones que en `GestionInventario`).
- `pvp`, `pvpiva`, `precioCompra1`, `margen`, `beneficio` (todos solo lectura, informativos).
- `Existencia` (solo lectura, existencia actual), `min`, `max` (editables, stock mínimo/máximo), `ubicacion` (editable).
- `Cantidad` (JTextField) — cantidad del movimiento a aplicar.
- `tablaProductos` (JTable) columnas Refecencia, Nombre, Categoria, Existencias — buscador de producto.
- Botones `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. `cargarProductos()`: lista REFERENCIA, NOMBRE, CATEGORIA y existencia (con UNION para productos sin fila en `stockproductos`, mostrando 0).
2. Doble clic en una fila de `tablaProductos`: `ListarProducto("REFERENCIA", referenciaDeLaFila)`.
3. `codigoBarras` + Enter: `ListarProducto("CODIGO_BARRAS", valor)`.
4. `ListarProducto(columna, valor)`: carga datos del producto, calcula `pvpiva_ = pvp_ + pvp_*%iva/100`, `margen_`/`beneficio_` igual que otras pantallas, llama `cargarCantidadExistencia()` (que también carga `min`/`max` desde `productos.STOCK_MIN/STOCK_MAX` — nota: el método real consultado en el código solo trae `CANTIDAD` y `LUGAR`, el relleno de min/max ocurre dentro de `ListarProducto` mismo vía las columnas 11 y 12 del SELECT principal), y pone foco en `Cantidad`.
5. Botón **Guardar** (`jButton1`): valida `nombre` no vacío (**"Debe escojer un producto al cual agregar existencias."**), `min` no vacío (**"Debe escribir la cantidad de Stock min."**), `max` no vacío (**"Debe escribir la cantidad de Stock max."**). La validación de `Cantidad` está **comentada** (no exige que `Cantidad` tenga valor ni que sea ≥ 0 — ver Notas).
   - Si `updateSave` (producto ya cargado, caso normal): si `Razon` empieza con `"(Entrada)"`, `UPDATE stockproductos SET CANTIDAD = CANTIDAD + cantidad, LUGAR = ubicacion WHERE ID=id`, registra movimiento, llama `guardarStoc_min_max()`, mensaje **"Existencia del producto Agregada Correctamente."**; si es salida, primero compara `Cantidad > Existencia` → si es mayor, muestra **"Existencia del producto no alcanza para dar la salida."** y **no hace nada más**; si no, resta y mensaje **"Existencia del producto Modificada Correctamente."**.
   - Si no `updateSave` (caso raro, producto sin fila previa de stock y no se marcó como cargado): si la razón es de entrada, `INSERT INTO stockproductos`; si es de salida, muestra **"El producto actual no tiene existencias, para darle salida."** sin insertar nada.
6. `guardarStoc_min_max()`: `UPDATE productos SET STOCK_MIN=.., STOCK_MAX=.. WHERE ID=id` (persiste los campos editables de stock mínimo/máximo cada vez que se guarda un movimiento).
7. Botón **Limpiar**: resetea todos los campos, recarga tabla de productos, restaura placeholder de `BUSCAR`.
8. Botón **Cerrar** / Escape: cierra (vuelve a `Menunew` si corresponde).

**Datos / tablas de BD leídas o escritas:**
- `productos`, `categorias`, `categoria_impuesto`, `unidades` (SELECT para cargar el producto).
- `stockproductos` (SELECT CANTIDAD+LUGAR; UPDATE/INSERT CANTIDAD/LUGAR).
- `productos` (UPDATE STOCK_MIN/STOCK_MAX).
- `movimiento_inventario` (INSERT con el texto de `Razon` como motivo y precio = `pvpiva` actual del producto).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- La validación de salida insuficiente (`Cantidad > Existencia`) es la **única** protección contra existencia negativa de todo este grupo de pantallas que la implementa de forma bloqueante (corta el flujo sin guardar nada), a diferencia de `InventarioFacil.RESTAR` o de `DetallarProductos`/`DetallarProductosFacil`, que permiten quedar en negativo.
- Guardar siempre persiste `STOCK_MIN`/`STOCK_MAX` aunque el usuario solo haya querido ajustar la cantidad — no hay forma de modificar solo la cantidad sin reafirmar (o involuntariamente sobrescribir) los límites de stock.

**Notas / peculiaridades:**
- La validación de que `Cantidad` tenga contenido está comentada en el código (`//        else if(Cantidad.getText().equals("") ...`), por lo que técnicamente el botón Guardar puede ejecutarse con `Cantidad` vacía: `Double.parseDouble("")` lanzaría una excepción no capturada específicamente para ese caso, cayendo en el `catch (Exception e)` genérico del bloque, que solo muestra el mensaje de error con el texto de la excepción concatenado (p. ej. `"Error al insertar el nuevo producto.java.lang.NumberFormatException: ..."`) — mensaje de error poco amigable para el usuario final si esto ocurre.

---

### Categorias.java — Mantenimiento de Categorías
**Propósito:** CRUD simple de categorías de producto (una sola columna `CATEGORIA`).

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `Categorias(String aux)`.

**Componentes de interfaz:**
- `NOMBRES` (JTextField) — nombre de la categoría.
- `CATEGORIAS` (JTable, una sola columna "NOMBRE CATEGORIA").
- `BUSCAR` (JTextField).
- Botones `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. `CargarCategorias()`: `SELECT CATEGORIA FROM CATEGORIAS ORDER BY CATEGORIA` (mayúsculas).
2. Doble clic en una fila: `ListarClCategoria(nombre)` → `SELECT ID, CATEGORIA FROM CATEGORIAS WHERE CATEGORIA=nombre`; si existe, carga `NOMBRES`, selecciona todo el texto, marca `updateSave=true`; si no existe (caso de carrera improbable), llama `limpiar()`.
3. Botón **Guardar**: valida `NOMBRES` no vacío (**"Falta nombre de la categoria."**).
   - Si `updateSave`: `UPDATE CATEGORIAS SET CATEGORIA=.. WHERE ID=id`; mensaje **"Categoria Modificada Correctamente"** (sin punto final).
   - Si no: verifica duplicado con `verificarClategoria()` y, si no existe, `INSERT INTO CATEGORIAS (CATEGORIA)`; mensaje **"Categoria Agregada Correctamente"** (sin punto final); si ya existe, **"La Categoria que intenta agregar ya existe."**.
4. `BUSCAR` filtra `CATEGORIAS` por regex sobre la columna 0.
5. Tecla espacio sobre la tabla manda el carácter tecleado a `BUSCAR` y le da foco.
6. Botón **Limpiar** / Escape: ver comportamiento estándar de limpieza/cierre.

**Datos / tablas de BD leídas o escritas:**
- `categorias`/`CATEGORIAS` (SELECT/INSERT/UPDATE de la columna `CATEGORIA`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- Ninguna regla de negocio compleja: solo nombre obligatorio y no duplicado (al crear).

**Notas / peculiaridades:**
- **Bug real detectado:** `verificarClategoria()` consulta `SELECT ID FROM categoria WHERE categoria=...` (tabla **`categoria`**, en singular — nombre de tabla inexistente; la tabla real usada en el resto del sistema es `categorias`/`CATEGORIAS` en plural). Esta consulta fallará siempre con una excepción SQL que se captura silenciosamente (`catch (SQLException ex){System.out.println(...)}`), haciendo que `id` nunca pase de `false` — en la práctica, **la validación de categoría duplicada nunca se activa realmente**: siempre permite insertar, incluso si el nombre ya existe (puede crear categorías duplicadas sin aviso).

---

### Unidades.java — Mantenimiento de Unidades de Medida
**Propósito:** CRUD de unidades de medida (`UNIDADES`) junto con su factor de conversión (`VALOR`), usado como base para el fraccionamiento de productos y para los combos de unidad en `Productos.java`/`InventarioFacil.java`.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `Unidades(String aux)`. Estructura casi idéntica a `Categorias.java` (mismo autor/plantilla).

**Componentes de interfaz:**
- `NOMBRES` (nombre de la unidad), `valor` (factor numérico).
- `CATEGORIAS` (JTable, nombre de variable reutilizado de la plantilla de `Categorias.java`) columnas "Unidades", "Factor" (Factor marcado `canEdit=true` pero sin editor cableado para persistir cambios inline).
- `BUSCAR`.
- Botones `jButton1` "Guardar", `jButton2` "Limpiar", `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. `CargarUnidades()`: `SELECT unidades, valor FROM unidades ORDER BY unidades`.
2. Doble clic en una fila: `ListarClCategoria(nombre)` → `SELECT ID_unidades, unidades, valor FROM unidades WHERE unidades=nombre`; carga `NOMBRES` y `valor`, marca `updateSave=true`.
3. Botón **Guardar**: valida `NOMBRES` (**"Falta nombre de la unidad."**) y `valor` (**"Falta factor de la unidad."**) no vacíos.
   - Si `updateSave`: `UPDATE unidades SET unidades=.., valor=.. WHERE ID_unidades=id`; mensaje **"Unidades Modificada Correctamente"**.
   - Si no: verifica duplicado con `verificarClategoria()`; si no existe, `INSERT INTO Unidades (Unidades, valor)`; mensaje **"Unidades Agregada Correctamente"**; si existe, **"La Unidades que intenta agregar ya existe."** (sic, concordancia de género/número incorrecta, texto literal).
4. `BUSCAR` filtra por columna 0.
5. Limpiar/Cerrar: comportamiento estándar.

**Datos / tablas de BD leídas o escritas:**
- `unidades` (SELECT/INSERT/UPDATE de `UNIDADES` y `VALOR`, clave `ID_UNIDADES`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- `VALOR` es el "factor" que usan otras pantallas (`DetalleProducto.java`) para decidir, por ejemplo, qué tabla de fracciones de galón/kilo aplicar (comparando contra 55, 5, 1, 50, 40, 25, etc.) — este campo es la pieza de configuración central de ese comportamiento, aunque aquí se edita como un simple número sin ninguna documentación en pantalla de su significado.

**Notas / peculiaridades:**
- **Mismo bug que `Categorias.java`:** `verificarClategoria()` también consulta la tabla inexistente `categoria` (singular) en vez de `unidades`, por lo que la validación de unidad duplicada tampoco se activa nunca en la práctica — se puede crear la misma unidad más de una vez sin aviso.
- La columna "Factor" de la tabla está definida como editable en el modelo, pero no existe ningún manejador de edición de celda que persista ese cambio a la base de datos; es, en la práctica, de solo lectura funcional pese a `canEdit[1]=true`.

---

### Iva.java — Aplicar IVA a Línea(s) del Carrito de Inventario
**Propósito:** Diálogo pequeño y específico para asignar un porcentaje de IVA (categoría de impuesto) a la línea seleccionada del carrito de `GestionInventario`, o a todas las líneas del carrito a la vez.

**Cómo se invoca / navegación:** `JDialog` modal, único constructor `Iva(GestionInventario ObjGestionInventario, boolean modal, String nombre_producto)`, invocado desde el menú contextual de `GestionInventario` al hacer doble clic en la columna "IVA" de una fila del carrito.

**Componentes de interfaz:**
- `NP` (JLabel) — muestra el nombre del producto de la fila origen.
- `iva` (JComboBox) — categorías de `categoria_impuesto`.
- `productoActual` / `todosLosProductos` (JRadioButton, agrupados; `productoActual` preseleccionado por defecto).

**Funcionalidades atómicas (numeradas):**
1. Al abrir: carga el combo `iva` desde `categoria_impuesto` (sin preselección, `setSelectedIndex(-1)`), muestra el nombre del producto recibido, selecciona `productoActual`.
2. Enter en cualquiera de los tres controles (`iva`, `productoActual`, `todosLosProductos` — los tres tienen el mismo manejador duplicado): si no se eligió ninguna categoría de IVA (`iva.getSelectedIndex()<=0`), muestra **"Especifique el Iva."** (ERROR) y cierra el diálogo igualmente (el `dispose()` ocurre fuera del `if`, aun en caso de error — ver Notas); si sí se eligió, llama `ObjGestionInventario.Iva(valorPorcentualDeLaCategoria, productoActual.isSelected())` y pone foco en `BUSCAR` del padre, luego cierra.
3. `GestionInventario.Iva(valor, soloProductoActual)`: si `soloProductoActual` es true, pone `valor/100` solo en la fila actualmente seleccionada del carrito; si es false, pone `valor/100` en **todas** las filas del carrito; en cualquier caso marca la fila seleccionada como "UPDATE=true" y recalcula totales.
4. Escape (vía `keyPressed`, no `keyReleased` como en el resto del sistema) cierra el diálogo sin aplicar cambios.

**Datos / tablas de BD leídas o escritas:**
- `categoria_impuesto` (SELECT CATEGORIA para el combo; SELECT VALOR por categoría elegida).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- Cuando se elige "Todos los Productos", el IVA se aplica a **todas** las filas del carrito sin excepción (no respeta si alguna línea ya tenía un IVA distinto asignado intencionalmente), y solo marca como "UPDATE=true" la fila que estaba seleccionada en el momento, no todas las que efectivamente cambiaron — esto significa que, de las demás filas afectadas, el cambio de IVA **no se propagará** al catálogo maestro (`productos.PRECIO_COMPRA/PRECIO_VENTA`) al guardar la compra, porque esa propagación depende exclusivamente de la bandera "UPDATE".
5. (continuación de atómicas) Debido al punto anterior, aplicar IVA a "Todos los Productos" tiene un efecto visual inmediato en la tabla pero un efecto de persistencia **parcial e inconsistente** sobre el catálogo.

**Notas / peculiaridades:**
- El mensaje de error "Especifique el Iva." se muestra pero el diálogo se cierra de todos modos (`this.dispose()` está fuera del `if/else`), por lo que el usuario no tiene oportunidad de corregir sin reabrir el diálogo desde cero.
- El manejador de `Escape` está en `keyPressed` mientras casi todas las demás pantallas del sistema lo manejan en `keyReleased` — inconsistencia menor de qué evento de teclado dispara el cierre.

---

### PrecioCompra.java — Editar Precio de Compra de una Línea (diálogo rápido)
**Propósito:** Micro‑diálogo para teclear un nuevo precio de compra y aplicarlo a la línea seleccionada del carrito de `GestionInventario`.

**Cómo se invoca / navegación:** `JDialog` modal, único constructor `PrecioCompra(GestionInventario parent, boolean modal, String nombre_producto)`, invocado desde el doble clic en la columna "VALOR UNITARIO" del carrito.

**Componentes de interfaz:**
- `NP` (JLabel, nombre del producto).
- `pvpiva` (JTextField, pese al nombre de variable, aquí representa el **precio de compra** a aplicar — nombre heredado de una plantilla genérica de diálogo numérico).
- `mas` (JButton, icono "ok", sin acción propia cableada — el guardado real ocurre por Enter, no por clic en este botón).

**Funcionalidades atómicas (numeradas):**
1. Solo acepta dígitos y la coma (carácter 44) en el campo (`pasarFocusVerificarNumeros`).
2. Enter en el campo: si tiene contenido, llama `Obj.PrecioCompra(new BigDecimal(valor))` (método de `GestionInventario` que escribe en la columna 3 del carrito, marca "UPDATE=true" y recalcula totales) y cierra el diálogo. Si está vacío, simplemente cierra sin aplicar nada.
3. Escape cierra sin aplicar.

**Datos / tablas de BD leídas o escritas:** Ninguna consulta directa; solo escribe en memoria de la tabla del padre.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna validación de rango o formato más allá de "solo dígitos y coma".

**Notas / peculiaridades:**
- El botón visual `mas` no tiene ningún `ActionListener` con efecto real (`masActionPerformed` está vacío); toda la interacción funcional depende de presionar Enter dentro del campo de texto — el botón es puramente decorativo en el estado actual del código.

---

### PrecioVenta.java — Editar Precio de Venta (con calculadora opcional de margen)
**Propósito:** Diálogo para fijar el precio de venta de una línea, con dos modos: modo simple (desde pantallas de Ventas, solo precio) y modo con calculadora de margen porcentual (desde `GestionInventario`).

**Cómo se invoca / navegación:** `JDialog` modal, **5 constructores**: uno para `GestionInventario` (recibe además `BigDecimal ValorTotal`, el costo base, y muestra el panel de porcentaje) y cuatro equivalentes para `Ventas`/`Ventas1`/`Ventas2`/`Ventas3` (sin panel de porcentaje, tamaño de ventana más pequeño).

**Componentes de interfaz:**
- `NP` (no usado en la variante GestionInventario como etiqueta visible en el flujo descrito, pero sí recibe el nombre del producto).
- `pvpiva` (JTextField) — precio base o resultado final según el modo.
- `jPanel1` (visible solo en el modo GestionInventario): `porcentaje` (combo `"Neto","10 %",...,"70 %"`), `totalp` (solo lectura, resultado del cálculo).

**Funcionalidades atómicas (numeradas):**
1. Modo GestionInventario: al abrir, deshabilita `porcentaje`, pone `totalp` en 0, carga `pvpiva` con el `ValorTotal` recibido, habilita `porcentaje`, hace visible `jPanel1` y ajusta el tamaño de ventana a 408×158.
2. Modo Ventas (cualquiera de las 4 variantes): oculta `jPanel1`, tamaño de ventana 408×106.
3. `pvpiva` KeyReleased: habilita/deshabilita `porcentaje` según si el campo tiene texto.
4. Cambiar `porcentaje` (solo relevante en modo GestionInventario): si el índice es ≥1 (no "Neto"), calcula `total = pvpiva*(1+%/100)` con % fijo por cada opción (10,15,20,...,70) y lo muestra en `totalp`; si es "Neto" (índice 0) o ninguno, `total=0`.
5. Botón **"mas"** o Enter en `pvpiva`: aplica el precio según el padre activo:
   - `Ventas`/`Ventas1`/`Ventas2`/`Ventas3`: `padre.cantidades3(valorDePvpiva)` directamente (ignora el panel de porcentaje, que ni existe en este modo).
   - `GestionInventario` (`Obj5`): si se eligió algún porcentaje (`porcentaje.getSelectedIndex()>=1`), aplica `Obj5.PrecioVenta(new BigDecimal(totalp.getText() formateado))`; si no, aplica `Obj5.PrecioVenta(new BigDecimal(pvpiva.getText()))` (el valor base sin markup).
6. `GestionInventario.PrecioVenta(valor)`: escribe en columna 7 ("PRECIO VENTA") de la fila seleccionada del carrito, marca "UPDATE=true", recalcula totales.
7. Escape cierra sin aplicar.

**Datos / tablas de BD leídas o escritas:** Ninguna consulta directa.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- La tabla de porcentajes de markup (10% a 70%, en pasos de 5) está **hardcodeada** como una cadena de `if/else if` idéntica repetida dos veces en el archivo (una en `porcentajeActionPerformed` y otra, comentada/no usada, en el flujo de Enter) en vez de una fórmula paramétrica — cualquier cambio de los pasos de porcentaje requiere editar manualmente cada rama.
- Solo accesible/relevante la calculadora de porcentaje desde `GestionInventario`; en los flujos de `Ventas*` el precio tecleado se aplica literal, sin markup.

**Notas / peculiaridades:**
- Buena parte del manejador `pasarFocusVerificarNumeros` (la rama que aplica el precio en Enter) duplica exactamente la lógica de `masActionPerformed`, con pequeñas variaciones no siempre coherentes entre sí (p. ej. hay una línea comentada `Obj5.cantidades3(...)` que sugiere un intento previo de unificar comportamiento con las pantallas de Ventas que finalmente no se usó para `GestionInventario`).

---

### CalcularPrecioVenta.java — Calculadora de Precio de Venta por Margen de Ganancia
**Propósito:** Pequeña calculadora para, dado un precio de compra conocido, teclear un "% Ganancia" deseado y ver en vivo el precio de venta resultante.

**Cómo se invoca / navegación:** `JDialog` modal, constructor `CalcularPrecioVenta(GestionInventario parent, boolean modal, double precio_compra_)`, invocado desde el menú contextual "Calcular Precio Venta" de `GestionInventario`.

**Componentes de interfaz:**
- `pvpiva` (JTextField, solo lectura) — precio de venta calculado (etiqueta "Precio Venta").
- `porcentaje` (JTextField editable) — porcentaje de ganancia deseado (etiqueta "% Ganancia").
- `mas` (JButton icono "ok").

**Funcionalidades atómicas (numeradas):**
1. Al abrir, limpia `porcentaje` y `pvpiva`.
2. `porcentaje` KeyReleased: calcula `pvpiva = precio_compra_ * (porcentaje/100) + precio_compra_` y lo muestra en vivo (sin formato de moneda, número crudo con todos los decimales de `double`).
3. Botón **"mas"** / Enter en `pvpiva`: el código que debería aplicar el resultado al padre está **completamente comentado** (`// Obj1.PrecioVenta(...)`); el único efecto real de estas dos acciones es **cerrar el diálogo** (`this.dispose()`), sin transferir ningún valor de vuelta a `GestionInventario`.
4. Escape cierra el diálogo.

**Datos / tablas de BD leídas o escritas:** Ninguna.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** No aplica — ningún valor calculado llega a persistirse ni a aplicarse al carrito.

**Notas / peculiaridades:**
- **Funcionalidad incompleta/no operativa confirmada en el código:** esta pantalla, tal como está en el código fuente leído, calcula y muestra el precio de venta sugerido pero **no tiene forma de aplicarlo** al carrito de `GestionInventario`; el usuario puede ver el número pero debe copiarlo manualmente y pegarlo en otro diálogo (`PrecioVenta`) si quiere usarlo. Es un candidato explícito a "bug/feature incompleta" que el equipo de Regenta debería decidir si replica igual (por fidelidad) o completa (por utilidad real).

---

### ProductoRapidoNuevo.java — Alta/Edición Rápida de Producto (sin precios)
**Propósito:** Diálogo minimalista para crear o renombrar un producto desde `GestionInventario` sin necesidad de pasar por el formulario completo de `Productos.java`, útil cuando se está registrando una compra y aparece un producto que aún no existe en el catálogo.

**Cómo se invoca / navegación:** `JDialog` modal, dos constructores: `ProductoRapidoNuevo(GestionInventario obj, boolean modal)` (alta) y `ProductoRapidoNuevo(GestionInventario obj, boolean modal, String idProducto)` (edición, precarga el producto).

**Componentes de interfaz:**
- `referencia`, `codigoBarras`, `nombre` (JTextField).
- `categoria` (JComboBox).
- Botones `jButton3` "Cerrar", `jButton5` "Guardar Producto".

**Funcionalidades atómicas (numeradas):**
1. En modo edición, `ListarProducto("productos.ID", idProducto)` precarga referencia/código/nombre/categoría y marca `updateSave=true`.
2. `referencia` + Enter: si `codigoBarras` vacío, lo copia de `referencia`.
3. `codigoBarras` + Enter: si no está vacío y `!updateSave`, busca producto existente por ese código (`ListarProducto("CODIGO_BARRAS", valor)`).
4. Botón **Guardar Producto**: si `referencia` vacía, autogenera con `cargarReferencia()+1` (sin mensaje); valida `nombre` no vacío/no placeholder (**"Falta el nombre del producto."**) y `categoria` elegida (**"Seleccione la categoria del producto."**).
   - Si `updateSave`: verifica duplicados excluyendo el ID propio; si ninguno, `UPDATE productos SET REFERENCIA=.., CODIGO_BARRAS=.., NOMBRE=.., PRECIO_COMPRA='0', PRECIO_VENTA='0', ID_CATEGORIA=.., ID_CATEGORIA_IMPUESTO='1', ID_UNIDADES='1', STOCK_MIN='0', STOCK_MAX='0', ACTIVO='TRUE', PRECIO_VENTA2='0', PRECIO_VENTA3='0' WHERE ID=id` (**fuerza todos los precios a 0**, incluso si el producto ya tenía precios cargados previamente — ver Notas); mensaje **"Producto Modificado Correctamente."**; llama `obj.modificarNombre(nombre)` (actualiza solo el nombre visible en la fila ya agregada al carrito de `GestionInventario`, sin tocar precios del carrito) y cierra.
   - Si no: verifica duplicados globales; si ninguno, `INSERT INTO productos (...)` con todos los precios en 0 y `ACTIVO='TRUE'`; mensaje **"Producto Agregado Correctamente."**; llama `obj.cargarProductos()` (refresca catálogo lateral de `GestionInventario`) y cierra.
5. Botón **Cerrar**: cierra sin guardar.

**Datos / tablas de BD leídas o escritas:**
- `productos`, `categorias` (SELECT para precargar y resolver categoría).
- `productos` (INSERT/UPDATE con precios forzados a 0, impuesto y unidad fijos en "1").
- `configuracion` (UPDATE referencia correlativa si se autogeneró).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- A diferencia de `AgregarExistecias.java` (que sí preserva el precio existente al editar), **este diálogo en modo edición sobrescribe `PRECIO_COMPRA`, `PRECIO_VENTA`, `PRECIO_VENTA2` y `PRECIO_VENTA3` a 0 incondicionalmente**, sin leer ni preservar los valores previos del producto — si se usa "Modificar Producto Rapido" sobre un producto que ya tenía precios configurados, esos precios se **pierden** (quedan en 0) aunque el usuario solo quisiera corregir el nombre o la categoría.
- También fuerza `ID_CATEGORIA_IMPUESTO='1'` y `ID_UNIDADES='1'` siempre, igual que `AgregarExistecias.java`.

**Notas / peculiaridades:**
- Este es el riesgo de datos más serio identificado en todo el grupo "Productos e Inventario": usar la opción de menú "Modificar Producto Rapido" sobre un producto existente con precios ya cargados **borra sus precios silenciosamente** (no hay ninguna advertencia al usuario de esta consecuencia). Cualquier reimplementación en Regenta debería decidir explícitamente si replica este comportamiento (fidelidad) o lo corrige (debería ser tratado como bug a resolver, documentándolo como decisión consciente).

---

### VerificarCompras.java — Verificar Compras por Proveedor
**Propósito:** Reporte en pantalla, por proveedor, de todas sus facturas de compra con su total, permitiendo entrar al detalle de línea de cualquiera de ellas.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `VerificarCompras(String aux)`.

**Componentes de interfaz:**
- `PROVEEDOR`, `TFNit`, `TFDireccion`, `TFTelefono` (todos solo lectura) — datos del proveedor elegido vía botón `jButton4` "..." (`buscarProveedor`).
- `COMPRAS_PROVEEDOR` (JTable) columnas PROVEEDOR, NUMERO DE FACTURA, FECHA, TOTAL FACTURA.
- `TotalCompras` (solo lectura, suma de todas las facturas listadas).
- Botones `jButton1` "Guardar" (sin implementación real visible — el cuerpo está comentado/vacío), `jButton2` "Limpiar", `jButton3` "Cerrar".
- Menú contextual (heredado del patrón de `GestionInventario` pero mayormente sin uso real aquí: "Eliminar Producto", "Modificar Cantidad/Precio...", "Calcular Precio Venta", "Agregar Producto", "Recargar Lista Productos").

**Funcionalidades atómicas (numeradas):**
1. `cargarProveedor(nit, razon_social, direccion, celular, email, id)` (invocado por `buscarProveedor` al elegir un proveedor): rellena los campos de proveedor y llama `CargarCompras(id)`.
2. `CargarCompras(ID)`: `SELECT NUMERO_FACTURA, FECHA, TOTAL_FACTURA, RAZON_SOCIAL FROM compras, proveedor WHERE ... AND proveedor.ID_PROVEEDOR=ID GROUP BY NUMERO_FACTURA, FECHA ORDER BY FECHA DESC LIMIT 50`; llena la tabla y llama `CargarTotalesBIG()` (suma la columna 3 de todas las filas a `TotalCompras`, con formato moneda).
3. Doble clic en una fila de `COMPRAS_PROVEEDOR`: abre `detalleCompra(this, true, PROVEEDOR.getText(), id_proveedor, numeroFacturaDeLaFila)` como diálogo modal de solo lectura.
4. Botón **Limpiar**/**Cerrar**: comportamiento estándar (con confirmación **"¿Desea perder el trabajo realizado?."** / **"Procede a salir de gestion de inventario, ¿Desea perder el trabajo realizado?."** si la tabla tiene filas — mensajes heredados literalmente de `GestionInventario`, poco apropiados para una pantalla de solo consulta).

**Datos / tablas de BD leídas o escritas:**
- `compras` + `proveedor` (SELECT agrupado, límite 50 por proveedor).

**Reportes generados:** Ninguno invocado directamente (es un reporte en pantalla, no un .jrxml).

**Reglas de negocio / validaciones no obvias:**
- Igual que `CargarCompra.java`, el listado está limitado a las 50 facturas más recientes del proveedor elegido.

**Notas / peculiaridades:**
- Aunque visualmente comparte estructura casi idéntica con `GestionInventario` (mismos nombres de método `CargarTotalesBIG`, `DescuentoValorUnitario`, `ValorTotal`, mismo menú contextual completo), en la práctica es una pantalla de **solo consulta**: los métodos que modificarían datos (`cantidades`, `PrecioCompra`, `Descuento`) están presentes pero con su cuerpo comentado/vacío, y el botón "Guardar" no hace nada funcional — vestigio de haber sido clonada desde `GestionInventario` y despojada de su capacidad de escritura sin eliminar el código muerto correspondiente.
- Los mensajes de confirmación al cerrar/limpiar mencionan literalmente "gestion de inventario" y "perder el trabajo realizado", textos que no tienen sentido real en una pantalla donde no se puede editar nada — confirma el origen por copia de `GestionInventario.java`.

---

### VerificarProductosCompras.java — Verificar Compras por Producto
**Propósito:** Igual que `VerificarCompras.java` pero con la consulta invertida: se elige un **producto** y se listan todas las facturas (de cualquier proveedor) en las que ese producto fue comprado.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `VerificarProductosCompras(String aux)`. Al abrir, carga de inmediato el catálogo completo en una tabla interna `PRODUCTOS` (no visible directamente en el layout principal, se usa como fuente para el buscador `ListaProductos`).

**Componentes de interfaz:**
- `PRODUCTO` (solo lectura) — nombre del producto elegido.
- `IDProducto` (solo lectura) — ID interno del producto elegido.
- Botón `jButton4` "..." — abre `ListaProductos(this, true)` (copiando filas desde `PRODUCTOS` hacia el buscador) para elegir el producto.
- `COMPRAS_PROVEEDOR` (JTable) columnas PROVEEDOR, NUMERO DE FACTURA, FECHA, TOTAL FACTURA (mismo nombre de tabla que en `VerificarCompras`, aquí lista facturas de distintos proveedores para un mismo producto).
- Botones `jButton2` "Limpiar", `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. `ListarProductoCodigoBarras(ID, nombre, precioCompra, precioVenta)` (invocado por `ListaProductos` al elegir un producto): fija `PRODUCTO`/`IDProducto` y llama `CargarCompras(ID)`.
2. `CargarCompras(ID)`: `SELECT NUMERO_FACTURA, FECHA, TOTAL_FACTURA, RAZON_SOCIAL FROM compras, proveedor WHERE ... AND compras.ID_PRODUCTO=ID GROUP BY NUMERO_FACTURA, FECHA ORDER BY FECHA DESC` (sin límite de 50 en esta variante, a diferencia de `VerificarCompras`/`CargarCompra`).
3. Doble clic en una fila de `COMPRAS_PROVEEDOR`: resuelve `ID_proveedor(nombreProveedorDeLaFila)` (SELECT a `proveedor` por `RAZON_SOCIAL`) y abre `detalleCompra(this, true, proveedor, idProveedor, numeroFactura, IDProducto.getText())` — variante de `detalleCompra` con el parámetro extra `idProducto`.
4. Botón **Limpiar**: resetea `PRODUCTO`/`IDProducto`.

**Datos / tablas de BD leídas o escritas:**
- `productos`, `stockproductos`, `categoria_impuesto`, `categorias`, `unidades` (SELECT para poblar el catálogo interno `PRODUCTOS` usado por el buscador).
- `compras` + `proveedor` (SELECT agrupado por producto, sin límite de filas).
- `proveedor` (SELECT ID_PROVEEDOR por RAZON_SOCIAL, para resolver el proveedor de la fila elegida antes de abrir el detalle).

**Reportes generados:** Ninguno invocado directamente.

**Reglas de negocio / validaciones no obvias:**
- A diferencia de las otras dos pantallas de "verificar compras" (`VerificarCompras`, `CargarCompra`), esta **no limita** el resultado a 50 filas — puede traer el historial completo de compras de un producto muy comprado sin paginación.
- Resolver el proveedor por `RAZON_SOCIAL` (nombre) en vez de por ID almacenado en la fila es potencialmente ambiguo si dos proveedores compartieran el mismo nombre social (caso de datos sucios), aunque en la práctica `RAZON_SOCIAL` suele ser único por diseño de negocio.

**Notas / peculiaridades:**
- También hereda el patrón de `GestionInventario` (métodos `cantidades`, `PrecioCompra`, `Descuento` con cuerpo comentado/vacío, menú contextual con ítems inoperantes) — es, igual que `VerificarCompras.java`, una pantalla de solo consulta construida por copia de la pantalla de edición, sin limpiar el código muerto resultante.

---

### detalleCompra.java — Detalle de Líneas de una Factura de Compra (visor)
**Propósito:** Diálogo de solo lectura que muestra las líneas (productos) de una factura de compra específica, con sus totales, reutilizado tanto desde `VerificarCompras.java` (filtrando por proveedor+factura) como desde `VerificarProductosCompras.java` (filtrando además por un producto concreto).

**Cómo se invoca / navegación:** `JDialog` modal, dos constructores: `detalleCompra(VerificarCompras Obj4, boolean modal, String proveedor, String id_proveedor, String factura)` y `detalleCompra(VerificarProductosCompras Obj5, boolean modal, String proveedor, String id_proveedor, String factura, String idProducto)` (este último recibe un `idProducto` adicional que, según el código revisado, no se usa para filtrar la consulta — ver Notas).

**Componentes de interfaz:**
- `PROVEEDOR` (`public static`, solo lectura), `FACTURA` (solo lectura).
- `GESTIONAR_INVENTARIO` (JTable, mismo esquema visual que el carrito de `GestionInventario`: ID,PRODUCTO,CANTIDAD,VALOR UNITARIO,DESCUENTO,IVA,VALOR TOTAL,PRECIO VENTA,UPDATE) pero aquí usada solo para mostrar, sin edición real.
- `Total_compras`, `TotalVentas` (solo lectura).
- `BUSCAR` (`public static`) — filtro de texto sobre las líneas mostradas.
- Botón `jButton3` "Cerrar".

**Funcionalidades atómicas (numeradas):**
1. `CarcarCompras(NUMERO_FACTURA, ID_PROVEEDOR)`: `SELECT ... FROM compras, proveedor, tipo_pago, productos WHERE ... compras.NUMERO_FACTURA=.. AND proveedor.ID_PROVEEDOR=.. GROUP BY compras.ID_PRODUCTO` — trae una fila por producto distinto de esa factura/proveedor, con cantidad, precio de compra, descuento, IVA, valor total y precio de venta del producto; llena `GESTIONAR_INVENTARIO` y llama `CargarTotalesBIG()`.
2. `CargarTotalesBIG()`: idéntico algoritmo que en `GestionInventario`/`VerificarCompras` (recalcula precio de compra con descuento+IVA por línea, acumula totales de compra y venta multiplicando por cantidad).
3. `BUSCAR` filtra la tabla por regex sobre columnas 0-4.
4. Botón **Cerrar**: cierra el diálogo.
5. Doble clic sobre una fila de la tabla: sin efecto (el manejador `GESTIONAR_INVENTARIOMouseClicked` está vacío).

**Datos / tablas de BD leídas o escritas:**
- `compras`, `proveedor`, `tipo_pago`, `productos` (SELECT agrupado por producto de una factura+proveedor concretos).

**Reportes generados:** Ninguno invocado directamente.

**Reglas de negocio / validaciones no obvias:**
- Si la misma factura tuviera productos duplicados en distintas filas originales de `compras` (p. ej. por haberse agregado el mismo producto dos veces), el `GROUP BY compras.ID_PRODUCTO` los **consolida en una sola fila visual**; los valores mostrados (cantidad, totales) corresponden solo a la **primera fila agrupada** que MySQL devuelva para ese grupo (no hay funciones de agregación `SUM()` sobre cantidad/valor en el SELECT), por lo que si existieran líneas duplicadas reales en `compras` para el mismo producto+factura, el detalle mostrado podría **no representar el total real comprado** de ese producto en esa factura.

**Notas / peculiaridades:**
- El parámetro `idProducto` recibido en el constructor desde `VerificarProductosCompras` **no se utiliza en ninguna consulta SQL** de `CarcarCompras` (la consulta sigue traicionando únicamente por `NUMERO_FACTURA` + `ID_PROVEEDOR`, mostrando **todos** los productos de esa factura, no solo el producto específico que originó la navegación) — esto significa que, al entrar al detalle desde "Verificar Compras por Producto", el usuario ve la factura completa (todos los productos comprados en ella), no únicamente el producto que buscaba, lo cual puede ser una funcionalidad incompleta o un comportamiento intencional de "ver el contexto completo de la factura" — debe decidirse explícitamente al reimplementar si se desea filtrar también por producto.
- Es, junto con `VerificarCompras.java` y `VerificarProductosCompras.java`, la tercera pantalla que reutiliza visualmente la estructura de "carrito" de `GestionInventario` para un propósito puramente de consulta, confirmando un patrón de desarrollo recurrente en todo el sistema (clonar la pantalla de edición y despojarla de capacidad de escritura sin limpiar el código resultante).

---

## Grupo: Clientes, Proveedores y Cuentas

---

### Clientes.java — Gestión de Clientes

**Propósito:** Pantalla CRUD de clientes (crear, modificar, buscar, listar) que además sirve como selector de cliente cuando se invoca desde Ventas o Abonos.

**Cómo se invoca / navegación:** Es un `JInternalFrame` (no modal, vive dentro del escritorio `MenuPrincipal.Escritorio`). Se instancia con `new Clientes(String aux)` donde `aux` indica el contexto de origen: `"Menunew"`, `"Ventas"`, `"Ventas1"`, `"Ventas2"`, `"Ventas3"`, `"Abonos"`, `"CuentasPorCobrar"`. El botón **Agregar** (`ESCOGER`) sólo es visible cuando `aux` es `"Ventas"`, `"Ventas1"`, `"Ventas2"`, `"Ventas3"` o `"Abonos"` (permite "escoger" el cliente activo y volver a la pantalla llamadora). El cierre (`ESC` o botón **Cerrar**) invoca `MenuPrincipal.ActivarFrame(...)` devolviendo el control a la pantalla indicada por `aux`.

**Componentes de interfaz:**
- Campos de texto: `CC` (cédula/NIT), `NOMBRES`, `DIRECCION`, `PAIS`, `DEPARTAMENTO`, `MUNICIPIO`, `TELEFONO`, `CELULAR`, `FAX`, `EMAIL`, `COMENTARIO`.
- Combo `ListaPrecios`: ítems dinámicos según `MenuPrincipal.DATOS_OBLIGATORIOS.get("LISTA_PRECIOS")` → `"1"`=solo PVP MINORISTA (deshabilitado), `"2"`=MINORISTA+MAYORISTA, `"3"`=MINORISTA+MAYORISTA+VIP.
- Campo `BUSCAR` (placeholder "Tecla (F5) Buscar" en itálica gris) para filtrar la tabla.
- Tabla `CLIENTES` — columnas: `CC/NIT`, `NOMBRES`, `TELEFONO`, `PUNTOS`, `DESCUENTO` (equivalente a puntos×valor), `ID`(oculta), `Title7`(oculta), `Title8`(oculta). Columnas PUNTOS/DESCUENTO se ocultan si `DATOS_OBLIGATORIOS.get("PUNTOS") != "true"`.
- Botones: `jButton1`="Guardar", `jButton2`="Limpiar", `jButton3`="Cerrar", `ESCOGER`="Agregar".
- Menú contextual sobre el título: "Remediar" (jMenuItem1). Menú contextual sobre la tabla: "Estado de Cuentas" (jMenuItem2).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga la grilla con `CargarClientes()`: `SELECT cliente.CC, UPPER(NOMBRES), CELULAR, puntos.PUNTOS, puntos.PUNTOS*configuracion.VALORPESOSPUNTOS, cliente.IDCLIENTES, UPPER(DIRECCION), UPPER(LISTAPRECIOS) FROM cliente,puntos,configuracion WHERE cliente.IDCLIENTES <> 1 AND cliente.IDCLIENTES = puntos.IDCLIENTES GROUP BY cliente.CC ORDER BY UPPER(NOMBRES)` (excluye el cliente genérico id=1).
2. Escribir en `CC` y soltar tecla (`CCKeyReleased`): si `CC` no vacío y no está en modo edición (`!updateSave`), ejecuta `ListarCliene("CC", CC.getText())` que busca el cliente exacto y, si existe, carga todos los campos y pone `updateSave=true` (modo edición).
3. Doble clic en una fila de la tabla: `ListarCliene("IDCLIENTES", <valor columna 5>)` carga ese cliente en el formulario para edición.
4. Escribir en `BUSCAR` (`BUSCARKeyReleased`): aplica `RowFilter.regexFilter` sobre columnas 0-3 de la tabla (CC, NOMBRES, TELEFONO, PUNTOS), insensible a mayúsculas, con comodín por espacios (`" ".*."`).
5. Tecla Enter dentro de `BUSCAR` con exactamente 1 fila visible: carga automáticamente ese único cliente (`ListarCliene`) y limpia el campo de búsqueda.
6. Botón **Guardar** (`jButton1`): valida en orden — si `CC` vacío → mensaje "Falta CC/NIT del cliente." y foco a `CC`; si `NOMBRES` vacío → "Falta nombre del cliente."; si `CELULAR` vacío → "Falta numero celular del cliente.". Si pasa validación:
   - Si `updateSave` (edición): ejecuta `CALL ACTUALIZAR_CLIENTE('CC','NOMBRES','DIRECCION','PAIS','DEPARTAMENTO','MUNICIPIO','TELEFONO','CELULAR','FAX','EMAIL','COMENTARIO','LISTAPRECIOS','ID')`. Éxito → "Cliente Modificado Correctamente."; error → "Error al modificar el Cliente."+excepción.
   - Si no (alta nueva): primero `verificarCliente()` (ver más abajo); si ya existe → "La CC/NIT que intenta agregar ya existe." y foco a `CC`. Si no existe: `CALL INSERTAR_CLIENTE('CC','NOMBRES','DIRECCION','PAIS','DEPARTAMENTO','MUNICIPIO','TELEFONO','CELULAR','FAX','EMAIL','COMENTARIO','LISTAPRECIOS')`. Éxito → "Cliente Agregado Correctamente"; error → "Error al insertar el nuevo cliente"+excepción.
7. Botón **Limpiar** (`jButton2`): restablece el formulario — `PAIS` por defecto `"COLOMBIA"`, `DEPARTAMENTO` por defecto `"SUCRE"`, resto en blanco, recarga la tabla, limpia filtro, reinicia `updateSave=false`.
8. Botón **Cerrar** (`jButton3`) / tecla `ESC`: cierra y regresa a la pantalla de origen según `aux` (Menunew, Ventas, Ventas1, Ventas2, CuentasPorCobrar, Abonos) vía `MenuPrincipal.ActivarFrame`.
9. Botón **Agregar** (`ESCOGER`): si no hay cliente cargado (`!updateSave`) → "Debe seleccionar un cliente"; si hay cliente, según `aux` invoca `MenuPrincipal.ActivarFrame(<pantalla>,"Clientes","",CC,NOMBRES,ListaPrecios o DIRECCION+TELEFONO)` — para Ventas/Ventas1/2/3 pasa la lista de precios seleccionada; para "Abonos" pasa `DIRECCION` y `TELEFONO` en lugar de la lista de precios.
10. Menú contextual "Estado de Cuentas" (clic derecho sobre fila de tabla): abre `DetalleCliente` modal con la `CC` de la fila seleccionada.
11. Menú contextual "Remediar" (clic derecho sobre el título): rutina de migración de datos — lee todos los `IDCLIENTES,CC` de `cliente` y, para cada par, ejecuta `UPDATE PUNTOS/VENTAS/VENTAS_DEBITO/VENTASGUARDADAS SET IDCLIENTES = <idcliente> WHERE IDCLIENTES = <cc>` (corrige referencias que quedaron apuntando al CC en vez del ID interno). Al terminar muestra "listerine." (texto literal, aparentemente un mensaje de depuración/placeholder olvidado).
12. Tecla `F5`: pone foco y estilo activo en `BUSCAR`.

**Datos / tablas de BD leídas o escritas:**
- `cliente` (SELECT para grilla y para `ListarCliene`/`verificarCliente`/`id_cliente`; INSERT/UPDATE vía procedimientos almacenados `INSERTAR_CLIENTE`/`ACTUALIZAR_CLIENTE`).
- `puntos`, `configuracion` (SELECT, para calcular puntos y descuento en la grilla).
- `PUNTOS`, `VENTAS`, `VENTAS_DEBITO`, `VENTASGUARDADAS` (UPDATE masivo en la función "Remediar").

**Reportes generados:** Ninguno directo desde esta pantalla.

**Reglas de negocio / validaciones no obvias:**
- `verificarCliente()` comprueba duplicados con `SELECT CC FROM cliente WHERE IDCLIENTES = '<valor de CC.getText()>'` — es decir, compara el texto ingresado en el campo CC contra la columna `IDCLIENTES`, no contra la columna `CC`. Esto es sospechoso/posible bug: funciona solo si por convención `IDCLIENTES` y `CC` comparten valores, o si en la práctica nunca hay colisión real.
- El cliente con `IDCLIENTES = 1` se excluye siempre de la grilla (probablemente el "cliente genérico" usado en ventas de mostrador).
- La visibilidad de columnas de puntos depende de la bandera global `PUNTOS` en `DATOS_OBLIGATORIOS`.

**Notas / peculiaridades:**
- Gran cantidad de código comentado (versiones previas de constructores con `Menunew`/`MenuPrincipal` como parámetro, lógica de "directo" para autoseleccionar cliente en doble clic).
- El mensaje "listerine." en la función "Remediar" es casi con certeza un texto de depuración dejado sin corregir.
- Comparte estructura casi idéntica a `Proveedor.java` (misma plantilla visual/funcional adaptada a proveedores).

---

### DetalleCliente.java — Estado de Cuentas del Cliente

**Propósito:** Ventana de solo lectura que muestra el historial completo de movimientos de un cliente: compras, compras a crédito pendientes, abonos y notas crédito.

**Cómo se invoca / navegación:** `JDialog` modal, constructor `DetalleCliente(boolean modal, String cc)`. Se abre desde `Clientes.java` (menú contextual "Estado de Cuentas") y desde `CuentasPorCobrar.java` / `InformeCuentas.java` (menú contextual "Estado de Cuentas" sobre la tabla de cuentas). Tamaño fijo 866×435, no redimensionable.

**Componentes de interfaz:**
- Pestaña "DATOS CLIENTE": campos de solo lectura `CC`, `NOMBRES`, `DIRECCION`, `TELEFONO`, `FAX`, `EMAIL`.
- Pestaña "COMPRAS": tabla `COMPRAS` — columnas `NUMERO FACTURA`, `FECHA`, `TOTAL`, `Title4` (estado: "PAGO"/"DEBE", oculta).
- Pestaña "COMPRAS DEBITO": tabla `ComprasDebito` — columnas `NUMERO FACTURA`, `FECHA`, `DIAS MORA`, `TOTAL`.
- Pestaña "ABONOS": tabla `Abonos` — columnas `FECHA EMISION`, `ABONO`, `SALDO ABONO`.
- Pestaña "NOTAS CREDITO": tabla `NOTAS` — columnas `FECHA`, `VALOR NOTA CREDITO`, `SALDO NOTA CREDITO`.
- Botón `cerrar`.

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga datos del cliente (`SELECT CC,NOMBRES,DIRECCION,TELEFONO,FAX,EMAIL,IDCLIENTES FROM CLIENTE WHERE cc=?`) y, de inmediato, las 4 tablas: `CargarComprasDebito`, `CargarSaldosClinte`, `CargarComprasClinte`, `CargarNotasCredito`.
2. Cambiar de pestaña (clic en `jTabbedPane1`) recarga la tabla correspondiente: índice 1 → Compras Débito, índice 2 → Abonos, índice 3 → Compras. (índice 0, Datos Cliente, no hace nada).
3. Cada tabla agrega al final una fila "TOTALES" con la suma de la columna relevante (`CargarTotales`, `CargarTotalesAbonos`, `CargarTotalesCompras`, `CargarTotalesNotasCredito`).
4. Doble clic en una fila de `ComprasDebito` o `COMPRAS`: abre `DetalleDeVenta` (modal) con el número de factura, mostrando el detalle de ítems de esa venta.
5. Tecla `ESC`: cierra el diálogo (`dispose()`).
6. Botón `cerrar`: cierra el diálogo.

**Datos / tablas de BD leídas o escritas (solo SELECT):**
- `cliente` — datos generales.
- `ventas`, `ventas_debito` — compras pagadas (`UNION` de facturas sin registro DEBE en `ventas_debito` = "PAGO" y facturas con `ESTADO='DEBE'` = "DEBE") en `CargarComprasClinte`; compras a crédito pendientes (`ESTADO='DEBE'`) con días de mora `DATEDIFF(CURDATE(),FECHA_DEBITO)` en `CargarComprasDebito`.
- `abonos` — abonos activos (`ESTADO='ACTIVO'`) del cliente.
- `notascredito` — notas crédito activas (`ESTADO='ACTIVO'`) del cliente.

**Reportes generados:** Ninguno (pantalla de consulta, no exporta ni imprime).

**Reglas de negocio / validaciones no obvias:**
- Solo se muestran abonos y notas crédito en estado `ACTIVO` (no se listan los inactivos/consumidos).
- `CargarComprasClinte` distingue "PAGO" vs "DEBE" por exclusión en `ventas_debito`, no por un campo propio de `ventas`.

**Notas / peculiaridades:**
- Tiene dos constructores diseñados (uno recibiendo `Ventas` como padre) pero el segundo está completamente comentado/deshabilitado; solo el constructor con `(boolean,String)` está activo.
- Mucho código de cierre/MDI comentado (antiguo manejo de `Menunew`/`CuentasPorCobrar` como padre `JInternalFrame`), reemplazado por el simple `dispose()`.

---

### buscarClientes.java — Selector de Cliente (diálogo de búsqueda)

**Propósito:** Diálogo reutilizable de búsqueda/selección de cliente, usado como "popup picker" desde varias pantallas (no tiene lógica propia de negocio, solo delega al llamador).

**Cómo se invoca / navegación:** `JDialog` modal con **5 constructores sobrecargados**, uno por cada pantalla llamadora posible: `buscarClientes(InformesClientes, boolean)`, `buscarClientes(Abonos, boolean)`, `buscarClientes(CuentasPorCobrar, boolean)`, `buscarClientes(InformeCuentas, boolean)`, `buscarClientes(VentasDiarias, boolean)`. Cada constructor guarda la referencia al objeto llamador (los demás quedan `null`) y, al seleccionar un cliente, se invoca el método de callback correspondiente de esa clase (`cargarCliente(...)` o `EscogerCliente(...)`), luego `dispose()`.

**Componentes de interfaz:**
- Tabla `clientes` — columnas `CC/NIT`, `NOMBRES`, `DIRECCION`, `CELULAR`, `EMAIL`, `ID` (oculta).
- Campo `BUSCAR` con icono de lupa.
- Botón `mas` (icono "ok", sin acción real: `masActionPerformed` vacío).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga todos los clientes: `SELECT CC,UPPER(NOMBRES),UPPER(DIRECCION),CELULAR,UPPER(EMAIL),IDCLIENTES FROM cliente ORDER BY UPPER(NOMBRES)`.
2. Escribir en `BUSCAR`: filtra la tabla (`RowFilter.regexFilter`) sobre columnas 0-4, insensible a mayúsculas, comodín de espacios.
3. Doble clic en una fila: invoca el método de callback del llamador activo (según cuál de los 5 objetos no sea `null`) pasando CC, NOMBRES, DIRECCION, CELULAR/TELEFONO, EMAIL, ID (el callback de `VentasDiarias` solo recibe el `ID`). Luego cierra el diálogo.
4. Tecla Enter en `BUSCAR` con exactamente 1 fila visible tras el filtro: selecciona automáticamente esa fila (mismo efecto que el doble clic) y cierra.

**Datos / tablas de BD leídas o escritas:** Solo SELECT sobre `cliente`.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna validación propia; toda la lógica de negocio vive en el callback de la pantalla que lo invocó.

**Notas / peculiaridades:**
- Los 5 campos `Obj`, `ObjAbonos`, `objCuentasPorCobrar`, `objInformeCuentas`, `objVentasDiarias` se comprueban en cadena de `if/else if` tanto en el doble clic como en el Enter — duplicación de la misma lógica de despacho en dos sitios.
- Mucho código muerto comentado relacionado con una variante antigua para buscar facturas de venta (`VerificarFactura`).

---

### Proveedor.java — Gestión de Proveedores

**Propósito:** Pantalla CRUD de proveedores (crear, modificar, buscar, listar), análoga a `Clientes.java`, incluyendo datos de crédito del proveedor (días de crédito ofrecidos).

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `Proveedor(String au)` con `au` ∈ {`"Gestion de invenario"`, `"Cuentas por pagar"`, `"Abonos a Proveedores"`, y por defecto cualquier otro valor deja oculto el botón Agregar}. El botón **Agregar** (`ESCOGER`) es visible solo para esos 3 valores de `au`.

**Componentes de interfaz:**
- Campos de texto: `NIT`, `NOMBRES` (razón social), `DIRECCION`, `TELEFONO`, `EMAIL`, `DESCRIPCION` (`JTextArea`), `DIAS_CREDITO`.
- Campo `BUSCAR` (placeholder "Tecla (F5) Buscar").
- Tabla `PROVEEDOR` — columnas `CC/NIT`, `NOMBRE/RAZON SOCIAL`, `TELEFONO`.
- Botones: `jButton1`="Guardar", `jButton2`="Limpiar", `jButton3`="Cerrar", `ESCOGER`="Agregar".
- Panel "Datos Credito" con etiqueta explicativa y campo `DIAS_CREDITO`.

**Funcionalidades atómicas (numeradas):**
1. Carga inicial: `CargarProveedores()` → `SELECT NIT,UPPER(RAZON_SOCIAL),TELEFONO FROM proveedor ORDER BY UPPER(RAZON_SOCIAL)`.
2. Doble clic en fila de la tabla: `ListarCProveedor(NIT)` carga el proveedor completo en el formulario (incluye `DIAS_CREDITO`) y activa modo edición (`updateSave=true`).
3. Escribir en `BUSCAR`: filtra tabla por columnas 1-3 vía regex.
4. Tecla `F5`: activa/resetea el campo `BUSCAR` con estilo normal y foco.
5. Botón **Guardar**: valida en orden — `NIT` vacío → "Falta CC/NIT del Proveedor."; `NOMBRES` vacío → "Falta Nombre / Razon Social del Proveedor."; `DIRECCION` vacío → "Falta direccion del Proveedor."; `TELEFONO` vacío → "Falta telefono del Proveedor.". Si pasa:
   - Si `updateSave` (edición): `UPDATE PROVEEDOR SET RAZON_SOCIAL=,NIT=,DIRECCION=,TELEFONO=,EMAIL=,DESCRIPCION=,DIAS_CREDITO= WHERE ID_PROVEEDOR = <id>` (SQL directo, no procedimiento almacenado). Éxito → "Proveedor Modificado Correctamente."; error → "Error al modificar el Proveedor."+e.
   - Si no (alta): `verificarProveedor()`; si ya existe (mismo NIT) → "La CC/NIT que intenta agregar ya existe."; si no, `INSERT INTO PROVEEDOR (RAZON_SOCIAL,NIT,DIRECCION,TELEFONO,EMAIL,DESCRIPCION,DIAS_CREDITO) VALUES (...)`. Éxito → "Proveedor Agregado Correctamente."; error → "Error al insertar el nuevo Proveedor."+e.
6. Botón **Limpiar**: borra todos los campos, pone `DIAS_CREDITO="0"`, recarga la tabla y el filtro.
7. Botón **Cerrar** / `ESC`: cierra y regresa según `au` (`"Gestion de invenario"`, `"Cuentas por pagar"`, `"Abonos a Proveedores"`) vía `MenuPrincipal.ActivarFrame`.
8. Botón **Agregar** (`ESCOGER`): requiere `updateSave=true` (proveedor cargado), si no → "Debe seleccionar un Proveedor."; si sí, según `au`:
   - `"Gestion de invenario"` → `ActivarFrame("Gestion de invenario","Proveedor","", ID_proveedor(), NOMBRES.getText(),"")`.
   - `"Cuentas por pagar"` → `ActivarFrame("Cuentas por pagar","Proveedor","", ID_proveedor(), NOMBRES.getText(),"")`.
   - `"Abonos a Proveedores"` → `ActivarFrame("Abonos a Proveedores","Proveedor", NIT.getText(), NOMBRES.getText(), DIRECCION.getText(), TELEFONO.getText())`.

**Datos / tablas de BD leídas o escritas:**
- `proveedor` — SELECT (listado, búsqueda por NIT, `ID_proveedor()` busca por nombre de razón social), INSERT, UPDATE directos (sin procedimiento almacenado, a diferencia de `Clientes.java`).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:**
- `verificarProveedor()` compara correctamente `NIT` contra `NIT` (`SELECT RAZON_SOCIAL FROM proveedor WHERE NIT = <nit ingresado>`) — a diferencia del posible bug análogo en `Clientes.java`.
- `DIAS_CREDITO` es el único dato "de crédito" capturado; no hay límite de cupo (monto máximo), solo plazo en días.
- Las tres rutas de "Agregar" para `"Gestion de invenario"` y `"Cuentas por pagar"` **no envían el NIT** como tercer parámetro (lo dejan vacío `""`) y en su lugar colocan `ID_proveedor()` en el cuarto parámetro — inconsistente con la ruta de `"Abonos a Proveedores"` que sí envía `NIT.getText()` en el tercer parámetro. Cada pantalla destino debe tener en cuenta esta diferencia de orden de argumentos.

**Notas / peculiaridades:**
- Bastante código muerto: dos constructores adicionales completamente comentados que recibían `GestionInventario` o `CuentasorporPagar` como padre directo.
- Comparte casi toda la estructura visual y de flujo con `Clientes.java` (ambas plantillas fueron generadas a partir de una base común).

---

### buscarProveedor.java — Selector de Proveedor (diálogo de búsqueda)

**Propósito:** Diálogo reutilizable de búsqueda/selección de proveedor, análogo a `buscarClientes.java`.

**Cómo se invoca / navegación:** `JDialog` modal con **4 constructores sobrecargados**: `buscarProveedor(CuentasorporPagar, boolean)`, `buscarProveedor(AbonosCuentas, boolean)`, `buscarProveedor(GestionInventario, boolean)`, `buscarProveedor(VerificarCompras, boolean)`. Cada uno guarda solo su referencia activa (resto `null`).

**Componentes de interfaz:**
- Tabla `PROVEEDOR` — columnas `CC/NIT`, `RAZON SOCIAL`, `DIRECCION`, `CELULAR`, `EMAIL`, `ID` (oculta).
- Campo `BUSCAR`.
- Botón `mas` (sin acción real).

**Funcionalidades atómicas (numeradas):**
1. Carga inicial: `SELECT NIT,UPPER(RAZON_SOCIAL),UPPER(DIRECCION),TELEFONO,UPPER(EMAIL),ID_PROVEEDOR FROM proveedor ORDER BY UPPER(RAZON_SOCIAL)`.
2. Filtro en `BUSCAR` sobre columnas 0-4 vía regex.
3. Doble clic en fila: invoca el callback del llamador activo — `Obj.cargarProveedor(...)` (CuentasorporPagar), `Obj2.cargarProveedor(...)` (AbonosCuentas) y `Obj4.cargarProveedor(...)` (VerificarCompras) reciben los 6 campos (NIT, razón social, dirección, celular, email, ID); `Obj3.cargarProveedor(...)` (GestionInventario) solo recibe NIT, razón social e ID (3 parámetros). Cierra el diálogo tras la selección.
4. Enter con exactamente 1 fila filtrada: mismo efecto que doble clic.

**Datos / tablas de BD leídas o escritas:** Solo SELECT sobre `proveedor`.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna; delega toda lógica al callback.

**Notas / peculiaridades:** Estructura casi idéntica a `buscarClientes.java` (mismo patrón de 4-5 constructores + despacho condicional). `VerificarFactura` también aparece aquí como código muerto heredado de la plantilla.

---

### CuentasPorCobrar.java — Cuentas por Cobrar a Clientes

**Propósito:** Pantalla principal de gestión de cuentas por cobrar: selecciona un cliente, muestra sus facturas pendientes (estado `DEBE`), permite armar una "canasta de pago" combinando facturas y abonos disponibles, y liquidar la deuda registrando el movimiento de caja correspondiente.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor sin parámetros `CuentasPorCobrar()`. Se activa desde el menú principal. Tiene 3 pestañas: "CUENTAS POR COBRAR", "CUENTAS A PAGAR" (nombre de la pestaña es engañoso: es la canasta de pago seleccionada, no una pantalla de cuentas por pagar) y una pestaña con los datos generales del cliente.

**Componentes de interfaz:**
- Pestaña 1: tabla `CLIENTES` — columnas `NUMERO FACTURA`, `CC/NIT`, `NOMBRES/RAZON SOCIAL`, `FECHA`, `DIAS MORA`, `TOTAL`, `SUBTOTAL` (oculta), `IVA` (oculta), `estado` (oculta) + etiqueta `TOTAL_` con la suma visible.
- Pestaña 2: tabla `PAGAR` — columnas `NUMERO FACTURA` (o literal `"ABONO"` si la fila es un abono), `CC/NIT`, `NOMBRES/RAZON SOCIAL`, `TOTAL`, `SUBTOTAL` (oculta), `IVA` (oculta), `ID` (oculta) + etiqueta `TOTAL_2` con el neto a pagar.
- Pestaña 3: campos de solo lectura `cc`, `nombres`, `direccion`, `celular`, `email` + botón `jButton6`="Buscar cliente" (abre `buscarClientes`).
- Campo `BUSCAR`, `DateChooserCombo fecha`.
- Botones: `jButton1`="Pagar Cuentas", `jButton2`="Limpiar", `jButton3`="Cerrar", `jButton6`="Buscar cliente".
- Menú contextual sobre `CLIENTES`: "Detallar Venta" (jMenuItem2), "Estado de Cuentas" (jMenuItem1), separador, "Filtrar Cuentas" (jMenuItem3), "Cambiar Estado de Cuenta" (jMenuItem4).

**Funcionalidades atómicas (numeradas):**
1. Botón **Buscar cliente**: abre `buscarClientes(this, true)`; al seleccionar, llama a `cargarCliente(cc,nombres,direccion,celular,email,idCliente)` que rellena la pestaña 3 y ejecuta `cargarCuentas("DEBE", idCliente)`.
2. `cargarCuentas(Estado, idClientes)`: consulta `ventas_debito JOIN cliente JOIN ventas` filtrando por `ESTADO` y `IDCLIENTES`, calcula `DATEDIFF(CURDATE(),FECHA_DEBITO)` como días de mora, agrupa por factura, ordena por fecha de vencimiento; si no hay filas → "El cliente actual no tiene cuentas por cobrar."; siempre recalcula totales y selecciona la pestaña 0.
3. Doble clic sobre una factura en la pestaña 1: si la fila **no** está en estado "PAGO" (columna 8), la mueve a la canasta (pestaña 2, `PAGAR`) vía `ListarProductoCodigoBarras`, recarga automáticamente los abonos activos del mismo cliente como filas `"ABONO"` dentro de `PAGAR` (`cargarAbonos`), y cambia a la pestaña 2. Si ya está "PAGO" → "Solo se aceptan cuentas en estado DEBE."
4. Al intentar añadir una factura de **otro** cliente distinto al que ya está en la canasta → "Solo se acepta un cliente para pagar cuentas multiples." (solo se permite un cliente por canasta).
5. `calcularTotales2()` (canasta): recorre `PAGAR`; las filas `"ABONO"` **restan** del total a pagar (`TOTAL_2`) y se acumulan en `totalAbonos`; las demás **suman** y se acumulan en `totalCuentas`.
6. Botón **Pagar Cuentas**: si la canasta está vacía → "Debe seleccionar una cuenta para cancelar."; si el neto a pagar (`TOTAL_2`) es mayor a 0, abre el diálogo `Pago` (captura efectivo entregado, tipo de pago, puntos, impresión) pasando el total, CC y nombre; si el neto es 0 o negativo (los abonos cubren toda la deuda), llama directamente a `PagarCuentas(0,"Abonos",0,0,0, imprimir=true, acumularPuntos=false, tipoImpresora=false)` sin pasar por el diálogo.
7. `PagarCuentas(entregado, tipoPago, ...)`: por cada fila de la canasta — si no es "ABONO", marca la venta a crédito como pagada (`UPDATE VENTAS_DEBITO SET ESTADO='PAGO', FECHA_PAGO=<fecha>`); si no hay abonos en la canasta, inserta un movimiento de caja tipo "Entrada" por cada factura con su subtotal/IVA y el tipo de pago elegido; si hay abonos, aplica la lógica de consumo de abonos (ver reglas de negocio) y, si el total de abonos no supera la deuda total, inserta **un único** movimiento de caja "Entrada" por el remanente (`TOTAL_2`). Si se solicitó impresión, exporta/imprime el ticket `PagoCuentasI` (impresora de tinta) o `PagoCuentast` (impresora térmica) según corresponda. Finalmente muestra el diálogo `cambio` (vuelto) y limpia el formulario.
8. Menú contextual "Detallar Venta": abre `DetalleDeVenta` con el número de factura de la fila seleccionada.
9. Menú contextual "Estado de Cuentas": abre `DetalleCliente` con el CC de la fila seleccionada.
10. Menú contextual "Filtrar Cuentas": abre `FiltrarCuentas(this, true)` (ver ficha propia) que permite recargar la tabla filtrando solo por `estado` (el combo de proveedor queda deshabilitado en este contexto).
11. Menú contextual "Cambiar Estado de Cuenta": solo permitido si la fila está en "PAGO" (si está en "DEBE" → "Solo puede cambiar el estado a cuentas pagas."); pide confirmación "En realidad desea cambiar el estado de la cuenta seleccionada, de 'PAGÓ' A 'DEBE'?"; si se confirma, `UPDATE VENTAS_DEBITO SET ESTADO='DEBE'` y recarga.
12. Filtro de texto `BUSCAR` sobre columnas 0-4 de la tabla `CLIENTES`, recalcula totales tras filtrar.
13. `ESC`: cierra siempre hacia `Menunew`.

**Datos / tablas de BD leídas o escritas:**
- `ventas_debito` — SELECT (listar pendientes) y UPDATE (`ESTADO`, `FECHA_PAGO`) al pagar/revertir.
- `ventas`, `cliente` — SELECT (joins para mostrar datos).
- `abonos` — SELECT (abonos activos del cliente) y UPDATE (`SALDO`, `ESTADO`) al consumirlos.
- `CAJA` — INSERT del movimiento de pago (`guardarMovimientoCaja`).
- `TIPO_PAGO`, `movimiento` — SELECT de catálogos auxiliares (`ID_TIPO_PAGO`, `idMovimiento`).

**Reportes generados:** `PagoCuentasI` (tinta) / `PagoCuentast` (térmica) — formato de impresión de ticket de pago (ruta `REPORTES/CUENTAS`). Además se muestra el diálogo "cambio" con el vuelto calculado.

**Reglas de negocio / validaciones no obvias:**
- Consumo de abonos tipo FIFO manual: `totalCuentasAUX` se inicializa en `totalCuentas` y se va descontando a medida que se "gastan" abonos (`RestarAbonos`): si el saldo de un abono es menor o igual al remanente de deuda, el abono queda `INACTIVO` con `SALDO=0` y se descuenta su valor completo del remanente; si el saldo del abono es mayor al remanente, el abono queda `ACTIVO` con el nuevo saldo (`saldoAbono - remanente`) y el remanente se pone en 0 (abono parcialmente consumido).
- Solo se permite **un cliente por canasta de pago** simultáneamente.
- El cambio de estado DEBE→PAGO vía doble clic en factura no ocurre inmediatamente: solo se ejecuta al confirmar el pago con "Pagar Cuentas".

**Notas / peculiaridades:**
- El título de la pestaña 2 ("CUENTAS A PAGAR") es confuso porque en esta pantalla de cuentas **por cobrar** en realidad representa la canasta de facturas+abonos seleccionados para el cliente, no cuentas por pagar a proveedores.
- Comparte patrón casi idéntico (nombres de métodos, lógica de abonos) con `CuentasorporPagar.java`, su contraparte para proveedores.

---

### CuentasorporPagar.java — Cuentas por Pagar a Proveedores

**Propósito:** Equivalente de `CuentasPorCobrar.java` pero para compras a crédito con proveedores: lista facturas de compra pendientes (`estado='DEBE'`), permite armar una canasta de pago combinando facturas y abonos a proveedor, y liquidarlas.

**Cómo se invoca / navegación:** `JInternalFrame final`, constructor `CuentasorporPagar(String aux)` (`aux` usado solo para decidir el cierre, en la práctica solo se maneja `"Menunew"`).

**Componentes de interfaz:**
- Pestaña "CUENTAS POR PAGAR": tabla `CUENTAS` — columnas `id` (oculta), `NUMERO FACTURA`, `CC/NIT`, `NOMBRES/RAZON SOCIAL`, `FECHA COMPRA`, `DIAS MORA`, `TOTAL`, `estado` (oculta) + `TOTAL_`.
- Pestaña "CUENTAS A PAGAR" (canasta): tabla `PAGAR` — columnas `NUMERO FACTURA`/`"ABONO"`, `CC/NIT`, `NOMBRE/RAZON`, `TOTAL`, `ID` (oculta) + `TOTAL_2`.
- Pestaña de datos generales del proveedor: `cc`, `nombres`, `direccion`, `celular`, `email` (solo lectura) + botón `jButton6`="Buscar proveedor".
- Panel de filtro `jPanel1` con `proveedor` (combo) y `estado` (combo "*.*","DEBE","PAGO") — presentes en el formulario pero **sin lógica activa** (sus `ActionPerformed` están vacíos/comentados); el filtrado real se hace exclusivamente a través del diálogo `FiltrarCuentas`.
- Panel `jPanel2` con campos de una presunta "alta manual de factura" (`PROVEEDOR`, `FACTURA`, `TOTAL_FACTURA`, `FORMA_PAGO`, `Razon`, `fecha`, `VENCE_FACTURA`) — presentes en el código pero toda la lógica de guardado (`guardarCompras`, validaciones de `jButton1ActionPerformed`) está **comentada**; el botón "Pagar Cuentas" real solo ejecuta `PagarCuentas()`.
- Menú contextual sobre `CUENTAS`: "Eliminar Producto", "Modificar Cantidad/Precio Compra/Precio Venta", "Agregar Producto" (todos sin implementación activa) y, en un popup separado, "Filtrar Cuentas", "Cambiar Estado de Cuenta".

**Funcionalidades atómicas (numeradas):**
1. Botón **Buscar proveedor**: abre `buscarProveedor(this,true)`; callback `cargarProveedor(cc,nombres,direccion,celular,email,idProveedor)` rellena la pestaña de datos y llama a `cargarCuentas(idProveedor)` (sobrecarga de un solo parámetro, siempre filtra `ESTADO='DEBE'`).
2. `cargarCuentas(proveedor_, Estado)` (4 variantes combinando comodín `"*.*"` con valor concreto de proveedor/estado): consulta `compras JOIN proveedor`, calcula días de mora con `IF(ESTADO='DEBE', DATEDIFF(FECHA_VENCE_COMPRA_DEBITO,CURDATE()), 0)`, agrupa por `NUMERO_FACTURA`, ordena por fecha de vencimiento.
3. Doble clic en fila de `CUENTAS`: mueve la factura a la canasta `PAGAR` (`ListarProductoCodigoBarras`) y carga automáticamente los abonos a proveedor activos (`cargarAbonos`) como filas `"ABONO"`. Solo se acepta un proveedor por canasta ("Solo se acepta un proveedor para pagar cuentas multiples.").
4. **Posible bug de columna:** la condición que decide si la fila puede añadirse a la canasta compara la columna índice 3 (`NOMBRES/RAZON SOCIAL`) contra el texto `"DEBE"` (`if(!CUENTAS...getValueAt(...,3).equals("DEBE"))`) en lugar de la columna 7 (`estado`), que es la que realmente contiene ese valor. En la práctica esta condición es casi siempre verdadera (el nombre del proveedor rara vez es literalmente "DEBE"), por lo que el bloqueo de filas ya pagadas **no funciona como se espera** en esta pantalla (contraste con `CuentasPorCobrar.java`, que sí valida la columna correcta).
5. Botón **Pagar Cuentas** (`jButton1`): ejecuta directamente `PagarCuentas()` **sin** diálogo de captura de efectivo, sin ticket y sin pantalla de vuelto (a diferencia de `CuentasPorCobrar`).
6. `PagarCuentas()`: por cada fila de la canasta — si no es "ABONO", `editarCompras(NUMERO_FACTURA)` marca `COMPRAS.ESTADO='PAGO'` y sobreescribe `FECHA_VENCE_COMPRA_DEBITO` con la fecha actual del formulario (reutiliza el mismo campo para fecha de vencimiento y fecha de pago); si hay abonos en la canasta, aplica consumo de abonos análogo a `CuentasPorCobrar` pero sobre la tabla `ABONOSCUENTAS`. Al final muestra "Cuentas pagadas correctamente." y limpia.
7. **No se registra movimiento de caja** al pagar cuentas a proveedores (el código de `guardarMovimientoCaja` existe pero está completamente comentado) — asimetría respecto a `CuentasPorCobrar.java`, que sí inserta en `CAJA`.
8. Menú contextual "Filtrar Cuentas": abre `FiltrarCuentas(this,true)`.
9. Menú contextual "Cambiar Estado de Cuenta": solo si la fila está en "PAGO" (aquí sí compara correctamente la columna 7); pide confirmación y revierte `COMPRAS.ESTADO='DEBE'`.
10. Tecla `F5`: activa el campo de filtro secundario `BUSCAR22`. Tecla `ESC`: cierra solo si `aux.equals("Menunew")`; para cualquier otro valor de `aux` no hace nada (mismo patrón incompleto que se repite en otras pantallas de este grupo).

**Datos / tablas de BD leídas o escritas:**
- `compras`, `proveedor` — SELECT (listar pendientes) y UPDATE (`ESTADO`, `FECHA_VENCE_COMPRA_DEBITO`) al pagar/revertir.
- `abonoscuentas` — SELECT (abonos activos del proveedor) y UPDATE (`SALDO`,`ESTADO`) al consumirlos.
- `PROVEEDOR` — SELECT por NIT (`id_PROVEEDOR`).

**Reportes generados:** Ninguno (no hay impresión ni exportación en esta pantalla).

**Reglas de negocio / validaciones no obvias:** Igual lógica de consumo de abonos (FIFO con saldo parcial) que `CuentasPorCobrar.java`, aplicada sobre `ABONOSCUENTAS` en vez de `ABONOS`.

**Notas / peculiaridades:**
- Contiene una cantidad considerable de controles de interfaz y métodos para dar de alta facturas de compra manualmente (`PROVEEDOR`, `FACTURA`, `TOTAL_FACTURA`, `FORMA_PAGO`, `Razon`, `VENCE_FACTURA`, `guardarCompras`) cuya lógica de guardado está **comentada/desactivada**; funcionalmente la pantalla solo sirve para liquidar cuentas ya existentes, no para crearlas.
- El filtro rápido superior (`proveedor`/`estado` combos del `jPanel1`) está presente visualmente pero sin lógica — toda la funcionalidad de filtro equivalente se logra únicamente vía el diálogo `FiltrarCuentas`.
- Falta de registro en `CAJA` al pagar y falta de generación de comprobante/ticket son huecos funcionales evidentes frente a su contraparte de cuentas por cobrar — a tener en cuenta al replicar el comportamiento en Regenta (decidir si se replica el bug/vacío o se corrige).

---

### FiltrarCuentas.java — Filtro de Cuentas (proveedor/estado)

**Propósito:** Diálogo pequeño y reutilizable para filtrar la grilla principal de cuentas (por proveedor y/o estado) desde `CuentasorporPagar` o `CuentasPorCobrar`.

**Cómo se invoca / navegación:** `JDialog` modal, **título literal "Cantidad"** (no fue renombrado al copiar la plantilla — ver peculiaridades). Dos constructores: `FiltrarCuentas(CuentasorporPagar, boolean)` y `FiltrarCuentas(CuentasPorCobrar, boolean)`. Se abre desde el menú contextual "Filtrar Cuentas" de ambas pantallas de cuentas.

**Componentes de interfaz:**
- Combo `proveedor`: `"*.*"` + lista de `UPPER(RAZON_SOCIAL)` de todos los proveedores. **Deshabilitado** cuando se invoca desde `CuentasPorCobrar` (no aplica a clientes).
- Combo `estado`: `"*.*"`, `"DEBE"`, `"PAGO"`.
- Botón `mas` (icono "ok", confirma el filtro).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga el combo `proveedor` con `SELECT UPPER(RAZON_SOCIAL) FROM proveedor ORDER BY UPPER(RAZON_SOCIAL)` y selecciona `estado` en el índice 0 (`"*.*"`).
2. Botón `mas` o tecla **Enter**: si viene de `CuentasorporPagar` (`Obj != null`), llama `Obj.cargarCuentas(proveedorSeleccionado, estadoSeleccionado)`; si viene de `CuentasPorCobrar` (`Obj1 != null`), llama `Obj1.cargarCuentas(estadoSeleccionado, "")` (sin filtro de proveedor, ya que esa pantalla trabaja con un cliente ya seleccionado). Cierra el diálogo.
3. Tecla `ESC`: cierra sin aplicar ningún filtro.

**Datos / tablas de BD leídas o escritas:** Solo SELECT sobre `proveedor` (para poblar el combo).

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna validación; cualquier combinación de `"*.*"`/valor concreto es aceptada.

**Notas / peculiaridades:**
- El título de la ventana (`setTitle("Cantidad")`) es un remanente de copiar la plantilla de otro diálogo (posiblemente uno para capturar cantidades numéricas) y nunca se actualizó — detalle cosmético a corregir en la reconstrucción, no afecta funcionalidad.
- La lógica de confirmación está duplicada entre `masActionPerformed` y `keyPressed` (Enter) de forma casi idéntica.

---

### InformeCuentas.java — Informes de Cuentas por Cobrar

**Propósito:** Pantalla de generación de reportes/tickets relacionados con cuentas por cobrar (distinta de `CuentasPorCobrar.java`, que es operativa; esta es puramente de consulta/impresión).

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `InformeCuentas(String aux)` (el parámetro no se usa dentro del constructor visible). Dos pestañas: "INFORME DE CUENTAS POR COBRAR" (funcional) e "INFORME DE CUENTAS POR PAGAR" (panel vacío, sin componentes — funcionalidad no implementada para proveedores).

**Componentes de interfaz:**
- Tabla `CLIENTES` (misma estructura de 9 columnas que en `CuentasPorCobrar.java`: factura, CC/NIT, nombre, fecha, días mora, total, subtotal oculto, iva oculto, estado oculto) + etiqueta `TOTAL_`.
- 8 botones de reporte (`b1`...`b8`), cada uno con icono PDF o de impresora:
  - `b1` "Todas las cuentas por cobrar"
  - `b2` "Cuentas en un lapso de tiempo"
  - `b3` "Cuentas de un cliente especifico"
  - `b4` "Cuentas con mas días vencidos"
  - `b5` "Tickets de cuentas por cobrar de un cliente"
  - `b6` "Imprimir Tickets de cuentas por cobrar cliente"
  - `b7` "Facturas de cuentas por cobrar de un cliente"
  - `b8` "Imprimir facturas cuentas por cobrar cliente"
- Menú contextual sobre la tabla: "Detallar Venta", "Estado de Cuentas", "Filtrar Cuentas" (sin acción, código comentado), "Cambiar Estado de Cuenta".

**Funcionalidades atómicas (numeradas):**
1. **b1** "Todas las cuentas por cobrar": `cargarCuentas("","","","",1)` — trae **todas** las facturas a crédito sin filtrar por estado (incluye pagadas y pendientes, pese al nombre del botón) y exporta PDF `CUENTAS/TodasCuentasPorCobrar`.
2. **b2** "Cuentas en un lapso de tiempo": abre diálogo `fechas` para capturar fecha inicial/final; al confirmar, `informeFechas` filtra por rango de `FECHA_DEBITO` y exporta `CUENTAS/TodasCuentasPorCobrarLapso`.
3. **b3** "Cuentas de un cliente especifico": fija `aux="cce"`, abre `buscarClientes`; al seleccionar, filtra `ESTADO='DEBE'` para ese cliente y exporta `CUENTAS/TodasCuentasPorCobrarClienteS`.
4. **b4** "Cuentas con mas días vencidos": `cargarCuentas(...,4)` — misma consulta que b1 pero con `LIMIT 30`; **no ordena realmente por días de mora descendente** (usa el mismo `ORDER BY FECHA_DEBITO` que las demás variantes), por lo que el nombre del botón no refleja el comportamiento real. Exporta `CUENTAS/TodasCuentasPorCobrarDiasV`.
5. **b5** "Tickets de cuentas por cobrar de un cliente": `aux="fce"`, tras elegir cliente exporta PDF `CUENTAS/FacturasCuentasPorCobrarClientesTermica` usando `Ids()` (lista de IDs de las facturas actualmente filtradas).
6. **b6** "Imprimir Tickets...": `aux="ice"`, imprime (no exporta) el reporte `CUENTAS/Factura_venta` por cada factura filtrada, enviándolo a la impresora térmica configurada (`IMPRESORA_TERMICA`).
7. **b7** "Facturas de cuentas por cobrar de un cliente": `aux="fcec"`, exporta PDF `CUENTAS/FacturasCuentasPorCobrarClientesCarta`.
8. **b8** "Imprimir facturas cuentas por cobrar cliente": `aux="ifcec"`, imprime `CUENTAS/Factura_venta_Carta` por cada factura filtrada a la impresora de tinta (`IMPRESORA_TINTA`).
9. Menú contextual "Detallar Venta"/"Estado de Cuentas"/"Cambiar Estado de Cuenta": mismo comportamiento que en `CuentasPorCobrar.java` (abren `DetalleDeVenta`/`DetalleCliente`, o alternan `ESTADO` DEBE↔PAGO con confirmación).

**Datos / tablas de BD leídas o escritas:**
- `ventas_debito`, `ventas`, `cliente` — SELECT con 4 variantes (todas, por rango de fechas, por cliente+estado, limitado a 30) según el botón.
- `ventas` — SELECT auxiliar para armar lista de IDs a imprimir (`Ids()`).
- `VENTAS_DEBITO` — UPDATE en "Cambiar Estado de Cuenta".

**Reportes generados:** `CUENTAS/TodasCuentasPorCobrar`, `CUENTAS/TodasCuentasPorCobrarLapso`, `CUENTAS/TodasCuentasPorCobrarClienteS`, `CUENTAS/TodasCuentasPorCobrarDiasV` (PDF export); `CUENTAS/FacturasCuentasPorCobrarClientesTermica`, `CUENTAS/FacturasCuentasPorCobrarClientesCarta` (PDF export); `CUENTAS/Factura_venta` (impresión térmica), `CUENTAS/Factura_venta_Carta` (impresión tinta).

**Reglas de negocio / validaciones no obvias:**
- El reporte "Todas las cuentas por cobrar" (b1) y "Cuentas con más días vencidos" (b4) **no filtran por estado DEBE**, contrario a lo que su nombre sugiere; incluyen histórico completo de `ventas_debito`.
- El filtro "más días vencidos" no ordena realmente por mora; solo limita a 30 registros en el orden por fecha de vencimiento ascendente.

**Notas / peculiaridades:**
- La pestaña "INFORME DE CUENTAS POR PAGAR" existe en la interfaz pero está completamente vacía (`jPanel1` sin componentes) — funcionalidad de informes de cuentas por pagar a proveedores **no implementada** en esta pantalla.
- El menú contextual "Filtrar Cuentas" (jMenuItem3) tiene el cuerpo del manejador comentado — no hace nada al hacer clic.
- Todos los botones b3, b5, b6, b7, b8 reutilizan el **mismo diálogo** `buscarClientes` y el **mismo método callback** `cargarCliente`, diferenciándose únicamente por el valor previamente asignado a la variable de instancia `aux` — patrón de "modo" compartido que conviene preservar explícitamente como una máquina de estados al migrar.

---

### Abonos.java — Registro de Abonos de Clientes

**Propósito:** Pantalla para registrar abonos (pagos parciales/anticipos) de un cliente a su cuenta por cobrar, y consultar el estado de sus saldos de abono.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `Abonos(String aux)` con `aux` ∈ {`"Menunew"`, `"Abonos"`, `"CuentasPorCobrar"`} (determina a dónde regresar al cerrar).

**Componentes de interfaz:**
- Campos de solo lectura: `CC`, `NOMBRES`, `DIRECCION`, `TELEFONO`.
- Botón `jButton4` ("...") abre `buscarClientes` para seleccionar cliente.
- Tabla `CLIENTES` (lista de abonos del cliente) — columnas `CC/NIT`, `FECHA EMISION`, `ABONO`, `SALDO ABONO`, `Title5` (estado, oculta).
- Campo `TOTAL_` (solo lectura, verde) con la suma de saldos mostrados.
- Botones: `jButton1`="Abonar", `jButton2`="Limpiar", `jButton3`="Cerrar", `jButton5`="Todos los Abonos".
- Menú contextual sobre el título: "Remediar" (sin implementación, heredado de la plantilla).

**Funcionalidades atómicas (numeradas):**
1. Botón "..." (`jButton4`): abre `buscarClientes(this,true)`; callback `EscogerCliente(cc,cliente,direccion,telefono,idClientes)` rellena el formulario y ejecuta `CargarSaldosClinte(true, idClientes)` (solo abonos `ESTADO='ACTIVO'`).
2. Botón **Abonar** (`jButton1`): si `CC` vacío → "Debe seleccionar un cliente para hacer el abono."; si no, abre el diálogo `Pago` (entregado inicial 0, pasa CC y NOMBRES) para capturar el monto del abono, tipo de pago, puntos e impresión.
3. `PagarCuentas(entregado, tipoPago, ...)` (callback invocado por `Pago` al confirmar): si `entregado>0`, `INSERT INTO ABONOS (IDCLIENTES,FECHA,HORA,ABONO,SALDO,ESTADO,ID_USUARIO) VALUES (..., ABONO=SALDO=entregado, ESTADO='ACTIVO', ...)`; registra un movimiento de caja tipo "(Entrada) Efecivo Abono" en `CAJA`; si se pidió impresión, exporta/imprime el reporte **"AbonoT"** (ver peculiaridad); recarga `CargarSaldosClinte(true, idClientes)`.
4. Botón **"Todos los Abonos"** (`jButton5`): requiere `CC` no vacío ("Debe seleccionar un cliente para listar los abonos."); recarga la tabla con `CargarSaldosClinte(false, idClientes)` — **sin** filtrar por `ESTADO='ACTIVO'`, es decir incluye histórico de abonos inactivos/consumidos.
5. Botón **Limpiar**: borra campos y tabla, `TOTAL_="0"`.
6. Botón **Cerrar** / `ESC`: según `aux`, regresa a `Menunew` o `CuentasPorCobrar`.
7. Calcula el total de saldo (`calcularTotales`) sumando la columna `SALDO ABONO` de todas las filas visibles.

**Datos / tablas de BD leídas o escritas:**
- `abonos` — SELECT (listado por cliente, activo o completo) e INSERT (nuevo abono).
- `cliente` — SELECT (`id_cliente` por CC).
- `CAJA` — INSERT del movimiento "(Entrada) Efecivo Abono" (`idMovimiento` fijo `"3"`).
- `TIPO_PAGO` — SELECT (`ID_TIPO_PAGO`).

**Reportes generados:** `AbonoT` (ticket de abono). **Importante:** tanto para impresora de tinta como térmica el código llama `datosExportarImprimir("AbonoT")` — no distingue entre `AbonoI` y `AbonoT` según el tipo de impresora (ver peculiaridad).

**Reglas de negocio / validaciones no obvias:**
- Un abono recién creado tiene `ABONO == SALDO` (el saldo disponible inicial es igual al monto abonado completo); el consumo de ese saldo ocurre después, desde las pantallas de cuentas por cobrar al pagar facturas.
- No hay validación de monto máximo ni de cupo de crédito en esta pantalla; cualquier monto mayor a 0 es aceptado como abono.

**Notas / peculiaridades:**
- **Bug/inconsistencia de reporte:** a diferencia de lo que sugiere la convención de nombres del proyecto (`AbonoI` para tinta, `AbonoT` para térmica, según los archivos listados en `REPORTES/CUENTAS`), el código de `Abonos.java` usa literalmente `"AbonoT"` en ambas ramas (`if(tipo_impresora)` y `else`), por lo que el reporte de tinta nunca se genera con la plantilla `AbonoI`.
- El menú contextual "Remediar" (jMenuItem1) está presente pero su manejador está vacío — vestigio de copiar la plantilla de `Clientes.java`.

---

### AbonosCuentas.java — Registro de Abonos a Proveedores

**Propósito:** Equivalente de `Abonos.java` pero para registrar abonos/anticipos entregados a un proveedor contra su cuenta por pagar.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `AbonosCuentas(String aux)` con `aux` ∈ {`"Menunew"`, `"Abonos"`, `"CuentasPorCobrar"`} (mismos valores que `Abonos.java`, aparentemente copiados sin adaptar completamente a "Abonos a Proveedores"/"Cuentas por pagar" — ver peculiaridad).

**Componentes de interfaz:**
- Campos de solo lectura: `cc` (NIT), `nombres` (razón social), `direccion`, `celular`, `email`.
- Botón `jButton6`="Buscar proveedor" (abre `buscarProveedor`).
- Tabla `CLIENTES` (lista de abonos al proveedor) — columnas `CC/NIT`, `FECHA EMISION`, `ABONO`, `SALDO ABONO`, `Title5` (oculta).
- Campo `TOTAL_` con la suma de saldos.
- Botones: `jButton1`="Abonar", `jButton2`="Limpiar", `jButton3`="Cerrar", `jButton5`="Todos los Abonos".

**Funcionalidades atómicas (numeradas):**
1. Botón **"Buscar proveedor"**: abre `buscarProveedor(this,true)`; callback `cargarProveedor(cc_,nombres_,direccion_,celular_,email_,idCliente_)` rellena el formulario y ejecuta `CargarSaldosProveedor(true, idProveedor)` (solo abonos `ESTADO='ACTIVO'`).
2. Botón **Abonar**: si `cc` vacío → "Debe seleccionar un proveedor para hacer el abono."; abre `Pago` con `imprimirTikete` **pre-marcado** (`Pago.imprimirTikete.setSelected(true)`), a diferencia de `Abonos.java` donde ese checkbox queda en su valor por defecto.
3. `PagarCuentas(entregado, ...)`: `INSERT INTO ABONOSCUENTAS (ID_PROVEEDOR,FECHA,HORA,ABONO,SALDO,ESTADO,ID_USUARIO) VALUES (..., ESTADO='ACTIVO', ...)`. **No se registra ningún movimiento de caja** (`guardarMovimientoCaja` está implementado pero su llamada está comentada). Si se pidió impresión, exporta/imprime el reporte **"AbonoCuentas"** (mismo nombre para tinta y térmica — igual patrón que en `Abonos.java`). Recarga `CargarSaldosProveedor(true, idProveedor)`.
4. Botón **"Todos los Abonos"**: requiere `cc` no vacío ("Debe seleccionar un cliente para listar los abonos." — mensaje literal habla de "cliente" aunque la pantalla es de proveedores, otro vestigio de copiar la plantilla); recarga con `CargarSaldosProveedor(false, idProveedor)` (histórico completo).
5. Botón **Limpiar** / **Cerrar** / `ESC`: análogos a `Abonos.java`.

**Datos / tablas de BD leídas o escritas:**
- `abonoscuentas` — SELECT (listado por proveedor) e INSERT (nuevo abono).
- `PROVEEDOR` — SELECT (`id_proveedor` por NIT).
- (No se usa `CAJA` — ver peculiaridad).

**Reportes generados:** `AbonoCuentas` (ticket de abono a proveedor).

**Reglas de negocio / validaciones no obvias:** Misma semántica de saldo (`ABONO==SALDO` al crear) que en `Abonos.java`; el consumo real del saldo ocurre en `CuentasorporPagar.java` al pagar facturas de compra.

**Notas / peculiaridades:**
- **Hueco funcional:** a diferencia de `Abonos.java` (que sí inserta en `CAJA` un movimiento "(Entrada) Efecivo Abono"), aquí la llamada equivalente está comentada, por lo que un abono a proveedor **no deja rastro en caja** — asimetría a decidir si se replica o se corrige en Regenta.
- Varios textos de mensajes y nombres de variables (p. ej. "Debe seleccionar un cliente para listar los abonos.", nombre de clase interna `CLIENTES` para la tabla de abonos a proveedor) delatan que la pantalla fue derivada por copia de `Abonos.java` sin renombrar completamente todos los literales.
- El checkbox de impresión viene pre-marcado por defecto en esta pantalla, a diferencia de la de clientes.

---

### InformesClientes.java — Informe General / 360° del Cliente

**Propósito:** Pantalla de consulta integral de un cliente (compras, compras a crédito, abonos, notas crédito) con generación de reportes PDF exportables por categoría.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor sin parámetros `InformesClientes()`. Se activa desde el menú principal; el cierre siempre regresa a `Menunew` (no recibe parámetro `aux`).

**Componentes de interfaz:**
- Campos de solo lectura: `cc`, `nombres`, `direccion`, `celular`, `email`.
- Botón `jButton6`="Buscar cliente" (abre `buscarClientes`).
- Pestañas: "COMPRAS" (tabla `COMPRAS`: `NUMERO FACTURA`,`FECHA`,`TOTAL`,estado oculto), "COMPRAS CREDITO" (tabla `ComprasDebito`: `NUMERO FACTURA`,`FECHA`,`DIAS MORA`,`TOTAL`), "ABONOS" (tabla `Abonos`: `FECHA EMISION`,`ABONO`,`SALDO ABONO`), "NOTAS CREDITO" (tabla `NOTAS`: `FECHA`,`VALOR NOTA CREDITO`,`SALDO NOTA CREDITO`).
- 5 botones de reporte: `b1`="Informe general del cliente", `b2`="Informe de compras", `b3`="Informe de compras credito", `b4`="Informe de abonos", `b5`="Informe de notas credito" — **deshabilitados hasta que se carga un cliente**.
- Botón `jButton5`="Cerrar".

**Funcionalidades atómicas (numeradas):**
1. Botón **"Buscar cliente"**: abre `buscarClientes(this,true)`; callback `cargarCliente(cc_,nombres_,direccion_,celular_,email_,idCliente_)` rellena el formulario, carga **las 4 tablas de una sola vez** (`CargarComprasDebito`, `CargarSaldosClinte`, `CargarComprasClinte`, `CargarNotasCredito`) y habilita los 5 botones de informe.
2. Cada tabla agrega al final una fila "TOTALES" (`CargarTotales*`), acumulando en variables de instancia `totalCompras`, `totalComprasDebito`, `totalAbonos`, `total_NT` (estas variables **no se reinician entre cargas de tablas individuales**, solo se reinician en `limpiar()` — ver peculiaridad).
3. **b1** "Informe general del cliente": exporta PDF `CLIENTES/reporteGeneralCliente` con los 4 totales (compras, compras crédito, abonos, notas crédito) y los datos del cliente.
4. **b2** "Informe de compras": exporta `CLIENTES/reporteCompras` (solo `totalCompras`).
5. **b3** "Informe de compras credito": exporta `CLIENTES/reporteComprasCredito` (solo `totalComprasDebito`).
6. **b4** "Informe de abonos": exporta `CLIENTES/reporteAbonosCliente`, pasando `totalAbonos` bajo la clave de parámetro `"totalCompras"` (reutiliza el nombre de parámetro, ver peculiaridad).
7. **b5** "Informe de notas credito": exporta plantilla `CLIENTES/reporteNotasCredito`, pasando `total_NT` también bajo la clave `"totalCompras"`, pero el **nombre del archivo PDF generado** queda como `reporteAbonosCliente_<random>.pdf` (copiado del bloque de b4 sin actualizar el prefijo del nombre de archivo).
8. Doble clic en fila de `COMPRAS` o `ComprasDebito`: abre `DetalleDeVenta` con el número de factura.
9. Botón **Cerrar** (`jButton5`): siempre regresa a `Menunew`.

**Datos / tablas de BD leídas o escritas (solo SELECT):**
- `ventas`, `ventas_debito`, `cliente` — compras pagadas/pendientes (misma lógica de `UNION` que en `DetalleCliente.java`).
- `abonos` — abonos activos del cliente.
- `notascredito` — notas crédito activas del cliente.

**Reportes generados:** `CLIENTES/reporteGeneralCliente`, `CLIENTES/reporteCompras`, `CLIENTES/reporteComprasCredito`, `CLIENTES/reporteAbonosCliente`, `CLIENTES/reporteNotasCredito` (todos exportados a PDF vía `Imprimir_Exportar.Exporar()`).

**Reglas de negocio / validaciones no obvias:**
- Los botones de informe permanecen deshabilitados mientras no se haya cargado un cliente (`limpiar()` los deshabilita; `cargarCliente()` los habilita).
- Solo se consideran abonos y notas crédito en estado `ACTIVO` para los totales mostrados (igual que `DetalleCliente.java`).

**Notas / peculiaridades:**
- **Bug de nombre de archivo:** el PDF del informe de notas crédito (b5) se guarda con el prefijo de nombre de b4 (`reporteAbonosCliente_...pdf`) aunque usa la plantilla Jasper correcta (`reporteNotasCredito`); es solo el nombre físico del archivo exportado el que queda mal etiquetado, no el contenido del reporte.
- **Reuso de clave de parámetro:** en b4 y b5 se reutiliza la clave `"totalCompras"` del mapa `DATOS_OBLIGATORIOS` para transportar, respectivamente, el total de abonos y el total de notas crédito — depende de que cada plantilla `.jrxml` use ese mismo nombre de parámetro internamente; si se migra a Regenta conviene usar nombres de parámetro específicos por reporte.
- Las variables acumuladoras de totales (`totalAbonos`, `totalComprasDebito`, `totalCompras`, `total_NT`) son atributos de instancia que se van sumando dentro de cada `CargarTotales*`; solo se reinician a 0 en `limpiar()`, por lo que si se recarga el mismo cliente sin pasar por `limpiar()` los totales podrían duplicarse (riesgo a verificar/replicar con cuidado).

---

### InformesProductos.java — Informe de Productos (incluido por relación con Cuentas/Proveedores)

**Propósito:** Generador de reportes de inventario/productos con filtros dinámicos (categoría, nombre, precios, fechas, tipo de movimiento). Se documenta en este grupo porque comparte patrón de UI y porque algunos de sus informes (p. ej. "Diario de Existencias") dependen de movimientos de entrada/salida relacionados con compras a proveedores.

**Cómo se invoca / navegación:** `JInternalFrame`, constructor `InformesProductos(String aux)` (`aux` ∈ `{"Menunew", ...}`, usado solo para el cierre). Al abrir, ya carga un informe vacío por defecto embebido en pantalla.

**Componentes de interfaz:**
- Combo `INFORME` con 10 tipos de reporte: "Existencias", "Listado de Productos", "Almacenes", "Diario de Existencias", "Detalle de Diario de Existencias", "Detalle de Existencias", "Catalogo de Productos", "Etiqueta de Productos", "Catalogo de Ventas", "Codigo de Barras".
- Filtros: `CATEGORIAS` (combo poblado desde `categorias`), `codigoBarras` (texto), par condición+valor para `NOMBRE`/`NOMBRE_`, `PRECIOCOMPRA`/`PRECIOCOMPRA_`, `PRECIO_VENTA`/`PRECIO_VENTA_` (cada combo de condición ofrece: Ninguno/Igual/Distinto/Mayor/Menor/Mayor igual/Menor igual/Contiene), `fecha_i`/`fecha_f` (rango de fechas, solo habilitado para "Diario de Existencias"), `RAZON` (Ninguno/(Entrada)/(Salida), solo habilitado para "Diario de Existencias"), `sUCURSAL` (declarado y habilitado para "Diario de Existencias" pero **no usado** en la consulta SQL — filtro muerto).
- Panel `CONTENEDOR`: visor embebido del reporte Jasper generado (no es exportación a archivo, sino incrustación en pantalla).
- Botones: `jButton2`="Limpiar", `jButton3`="Cerrar", `jButton4`="Ejecutar Informe".

**Funcionalidades atómicas (numeradas):**
1. Al abrir, embebe un informe vacío por defecto en `CONTENEDOR` (`cargarInformes("")`), carga categorías en el combo y pone los `DateChooserCombo` en formato `yyyy-MM-dd`.
2. Cambiar `INFORME`: si se selecciona "Existencias" o "Listado de Productos" (índices 0,1), habilita categoría/código de barras/nombre/precios y deshabilita fecha/razón/sucursal; si se selecciona "Diario de Existencias" (índice 3), hace lo inverso (habilita fechas/razón/sucursal, deshabilita el resto). Los demás índices (2,4-9) **no tienen rama de habilitación/deshabilitación definida** — su comportamiento de filtros queda en el último estado que tenían.
3. `armarConsula()` construye dinámicamente la cláusula `WHERE` según los filtros activos y arma el SQL final **solo para 3 de los 10 tipos de informe**:
   - índice 0 "Existencias": `UNION` de productos con stock (`productos`+`stockproductos`) y productos sin fila de stock (cantidad=0), agrupado/ordenado por categoría.
   - índice 1 "Listado de Productos": `productos`+`categoria_impuesto`+`categorias`, calculando IVA = `(PRECIO_VENTA * valor_impuesto) + PRECIO_VENTA`.
   - índice 3 "Diario de Existencias": `movimiento_inventario` agregado (`SUM` condicional) de cantidades y totales de Entrada/Salida, con diferencia neta en unidades y en dinero, filtrado por prefijo de `MOVIMIENTO` (Entrada/Salida) y rango de fechas.
   - Para cualquier otro índice (2,4-9) el método retorna cadena vacía — el botón "Ejecutar Informe" no generará contenido útil para esos tipos.
4. Botón **"Ejecutar Informe"**: según el índice seleccionado asigna `nombre_reporte` = `"Existencias"`, `"listadoProductos"` o `"DiarioExistencias"` (solo para índices 0,1,3 respectivamente; para cualquier otro índice `nombre_reporte` queda vacío) y llama `cargarInformes(nombre_reporte)`, que invoca `Imprimir_Exportar.Incrustar()` para renderizar el reporte **dentro** del panel `CONTENEDOR` (no genera archivo PDF).
5. Botón **Limpiar**: restablece todos los filtros a sus valores por defecto y vuelve a dejar habilitados solo categoría/código/nombre/precios (modo "Existencias"/"Listado").
6. Botón **Cerrar** / `ESC` equivalente: cierra hacia `Menunew` solo si `aux.equals("Menunew")`; en cualquier otro caso de `aux`, no hace nada (mismo patrón incompleto visto en otras pantallas del grupo).

**Datos / tablas de BD leídas o escritas (solo SELECT):**
- `productos`, `stockproductos`, `categorias` — informe "Existencias".
- `productos`, `categoria_impuesto`, `categorias` — informe "Listado de Productos".
- `productos`, `movimiento_inventario`, `categorias` — informe "Diario de Existencias" (incluye movimientos originados por compras a proveedores, de ahí su relación con este grupo).
- `categorias` — SELECT para poblar el combo de filtro.

**Reportes generados:** Visor embebido (no exportación a archivo) para `Existencias`, `listadoProductos`, `DiarioExistencias`; los demás tipos de informe listados en el combo (`Almacenes`, `Detalle de Diario de Existencias`, `Detalle de Existencias`, `Catalogo de Productos`, `Etiqueta de Productos`, `Catalogo de Ventas`, `Codigo de Barras`) **no tienen implementación de consulta ni de generación** en este código fuente.

**Reglas de negocio / validaciones no obvias:** Ninguna validación de campos obligatorios; todos los filtros son opcionales y se combinan con `AND` solo si están activos.

**Notas / peculiaridades:**
- 7 de los 10 tipos de informe del combo (`Almacenes` hasta `Codigo de Barras`, excluyendo los 3 implementados) son **placeholders sin funcionalidad real** — al seleccionarlos y ejecutar, el método `armarConsula()` retorna cadena vacía y no sucede nada visible de utilidad.
- El filtro `sUCURSAL` está completamente cableado en la interfaz (habilitación, combo con ítems) pero nunca se usa dentro de `armarConsula()` — filtro muerto.
- El botón "Ejecutar Informe" reutiliza el mismo `cargarInformes()` que la carga inicial en blanco; si no hay un caso implementado para el índice elegido, el visor simplemente no se actualiza con nada relevante, sin mensaje de error al usuario.

---

## Grupo: Caja e Informes

> Fuente: código Java Swing de Sara POS en `SISTEMA\src\` (archivos `MovimientoCaja.java`, `CerrarCaja.java`, `cerrarCajaCalculado.java`, `buscarCierreCaja.java`, `DetalleMovimientoCaja.java`, `detellaMovimientoCaja.java`, `InformesCaja.java`) y clases auxiliares en `SISTEMA\src\OTRASCLASES\`. Base de datos `sarapos3`. Tablas relevantes: `caja`, `movimiento`, `tipo_pago`, `usuarios`, `configuracion`, `facturarenuente`, `remisionrenuente`, `ventas`.

### Convenciones y catálogos usados por todo el grupo

- Tabla `movimiento`: `1=Compra, 2=Venta, 3=Abono Cliente, 4=Abono Proveedores, 5=Entrada, 6=Salida`.
- Tabla `tipo_pago`: `0=Efectivo, 1=Cheque, 2=Vale, 3=Tarjeta, 4=Gratis, 5=Debito, 6=Banco`.
- Tabla `caja` (columnas en orden físico, relevante porque varias pantallas leen por índice posicional con `resultado.getString(n)` en lugar de por nombre):
  1. `ID` (entero) — **no es un identificador por fila**: es el número de secuencia/serie del *cierre de caja* (sesión). Se calcula una sola vez por sesión abierta con `SELECT MAX(ID) FROM CAJA WHERE ESTADO='CERRADO'` + 1, y se reutiliza en todos los movimientos insertados mientras la caja sigue `ABIERTO`. Es **global** (no filtra por `ID_USUARIO`), por lo que si dos usuarios tienen caja abierta al mismo tiempo pueden terminar compartiendo el mismo número de serie (condición de carrera potencial).
  2. `IDMOVIMIENTO` (FK a `movimiento`)
  3. `ID_FATURA` (varchar) — número de factura/remisión de venta, o referencia sintética `"MC"+secuencial` para entradas/salidas manuales (secuencial tomado y incrementado de `configuracion.CENTRADASALIDA`).
  4. `TOTAL_IMPUESTO` (double)
  5. `TOTAL_NETO` (double)
  6. `DESCRIPCION` (text)
  7. `FECHA_INICIO` (datetime) — fecha/hora en que se registró el movimiento.
  8. `FECHA_FIN` (date, nulo mientras está abierta) — fecha en que se cerró la caja a la que pertenece el movimiento.
  9. `ESTADO` (`ABIERTO` / `CERRADO`)
  10. `ID_USUARIO` (FK a `usuarios`)
  11. `SUM` (`SI`/`NO`, texto) — si el movimiento se suma o no a los totales de caja (permite excluir una factura del arqueo sin borrarla).
  12. `FACTURA_REMISION` (texto `"true"`/otro) — marca si el movimiento debe listarse en la grilla de cierre de caja.
  13. `ID_TIPO_PAGO` (FK a `tipo_pago`)
- Formateo de moneda: todas las pantallas usan `OTRASCLASES.FormatoMoneda`: `formatMoneda(String)` devuelve texto con `NumberFormat.getCurrencyInstance()` (ej. `$ 1.234,00` según locale del sistema); `formatoNumero(String)`/`formatoNumero2(String)` parsean ese texto formateado de vuelta a `int`/`double`.
- Patrón de cierre/retorno: las pantallas que son `JInternalFrame` (`MovimientoCaja`, `CerrarCaja`, `InformesCaja`) se ocultan (`setVisible(false)`) y llaman `MenuPrincipal.ActivarFrame("Menunew","","","","","")` para volver al menú principal dentro del `JDesktopPane`. Las que son `JDialog` (`buscarCierreCaja`, `DetalleMovimientoCaja`, `detellaMovimientoCaja`) se cierran con `dispose()`.

---

### MovimientoCaja.java — Movimientos de caja (Entrada / Salida de efectivo)

**Propósito:** Registrar manualmente una entrada o salida de dinero en efectivo de la caja actualmente abierta del usuario (ej. un retiro de efectivo o un aporte de capital), sin que corresponda a una venta o compra.

**Cómo se invoca / navegación:** Es un `JInternalFrame` (título interno `"MovimientoCaja"`, sin barra norte — `setNorthPane(null)`) que vive dentro del `JDesktopPane` del menú principal. Se abre desde el menú principal (no está en el código leído, pero sigue el mismo patrón `MenuPrincipal.ActivarFrame(...)` usado por las demás pantallas de este grupo). No recibe parámetros de entrada. Se cierra con el botón "Cerrar" o con la tecla **ESC**, devolviendo el control a `"Menunew"`.

**Componentes de interfaz:**
- `Razon` (JComboBox, oculto por un `JRadioButton` duplicado visualmente — ver notas): opciones `"(Entrada) Efecivo"`, `"(Salida) Efectivo"` (sic, con el typo "Efecivo").
- `Rentrada` / `Rsalida` (JRadioButton, agrupados en `buttonGroup1`): selector real de si el movimiento es una entrada o una salida de efectivo. Por defecto `Rentrada` queda seleccionado al limpiar el formulario.
- `total` (JTextField): monto del movimiento, formateado como moneda al perder el foco.
- `descripcion` (JTextArea, 5 filas x 20 columnas dentro de un JScrollPane): motivo/descripción del movimiento (obligatorio).
- `fecha` (DateChooserCombo, formato `yyyy-MM-dd`): **existe en el código y se configura, pero nunca se agrega al layout visible del formulario** (ver Notas/bugs). Se usa igualmente en el INSERT.
- Botón "Guardar" (`jButton1`, icono save): valida y guarda el movimiento.
- Botón "Limpiar" (`jButton2`): limpia el formulario.
- Botón "Cerrar" (`jButton3`, icono delete): cierra la pantalla.

**Funcionalidades atómicas (numeradas):**
1. Al abrir la pantalla, `limpiar()` deja `Rentrada` seleccionado, `total` vacío con foco y texto seleccionado, `descripcion` vacía.
2. El usuario selecciona `Rentrada` o `Rsalida` (clic en cualquiera de los dos reenvía el foco a `total` y selecciona todo su texto).
3. Al escribir en `total`, `totalKeyTyped` solo permite dígitos (`pasarFocusVerificarNumeros(evt, true)`; cualquier carácter no numérico se consume/descarta) y Enter transfiere el foco al siguiente componente.
4. Al ganar foco, `total` se resalta en color `(223,223,238)` y se reformatea con `FormatoMoneda.formatoNumero` (a número simple).
5. Al perder foco, `total` se reformatea como moneda (`FormatoMoneda.formatMoneda`), o se pone en `"0"` formateado si quedó vacío; el fondo vuelve a blanco.
6. En `totalKeyReleased` se actualiza la variable interna `total_` parseando el texto con `DecimalFormat("#")`.
7. Botón "Guardar" (`jButton1ActionPerformed`) valida en este orden:
   - Si `total` está vacío → mensaje de error **"Falta total del movimiento de caja."** (JOptionPane ERROR) y foco a `total`.
   - Si `descripcion` está vacía → mensaje de error **"Falta descripción del movimiento de caja."** y foco a `descripcion`.
   - Si `total_ <= 0` → mensaje de error **"Falta total del movimiento de caja."** y foco a `total`.
   - Si todo es válido, determina el tipo: si `Rentrada` está seleccionado, `idMovimiento="5"` (Entrada) y el signo de `total_` queda positivo; si no, `idMovimiento="6"` (Salida), `total_` se multiplica por `-1` y la etiqueta de confirmación usa `"(Salida) Efectivo"`.
   - Muestra confirmación **"Desea agregar un movimiento de '<Entrada|Salida> Efectivo' a la caja?"** (YES_NO). Si confirma:
     - Ejecuta el `INSERT INTO CAJA` (ver más abajo).
     - Si tiene éxito: mensaje informativo **"Movimiento de caja agregado correctamente."** y limpia el formulario.
     - Si falla: mensaje de error **"Error al insertar movimiento de caja"+e**.
8. Botón "Limpiar" (`jButton2ActionPerformed`) → `limpiar()`.
9. Botón "Cerrar" (`jButton3ActionPerformed`) → `cerrar()` (oculta la pantalla y vuelve a `Menunew`).
10. Tecla **ESC** en cualquier componente con `KeyListener` registrado (Razon, total, descripcion, los 3 botones) → `cerrar()`.

**Datos / tablas de BD leídas o escritas:**
- `INSERT INTO CAJA (ID, IDMOVIMIENTO, ID_FATURA, TOTAL_IMPUESTO, TOTAL_NETO, DESCRIPCION, FECHA_INICIO, FECHA_FIN, ESTADO, ID_USUARIO, SUM, FACTURA_REMISION, ID_TIPO_PAGO)` con:
  - `ID` = `maxIdMovimiento()` → `SELECT MAX(ID) FROM CAJA WHERE ESTADO='CERRADO'` + 1 (serie global de la caja abierta).
  - `IDMOVIMIENTO` = `"5"` (Entrada) o `"6"` (Salida).
  - `ID_FATURA` = `"MC" + maxEntradaSalida()`.
  - `TOTAL_IMPUESTO` = `"0"` fijo.
  - `TOTAL_NETO` = `total_` (positivo en entrada, negativo en salida).
  - `DESCRIPCION` = texto del `JTextArea`.
  - `FECHA_INICIO` = fecha/hora actual del sistema (`yyyy-MM-dd HH:mm:ss`).
  - `FECHA_FIN` = `fecha.getText()` del `DateChooserCombo` invisible (ver bug en Notas).
  - `ESTADO` = `"ABIERTO"` fijo.
  - `ID_USUARIO` = usuario logueado (`MenuPrincipal.DATOS_OBLIGATORIOS.get("ID_USUARIO")`).
  - `SUM` = `"SI"` fijo (el movimiento sí cuenta en los totales de caja).
  - `FACTURA_REMISION` = `"true"` fijo (texto literal).
  - `ID_TIPO_PAGO` = `"0"` fijo (Efectivo).
- `SELECT MAX(ID) FROM CAJA WHERE ESTADO='CERRADO'` (función `maxIdMovimiento`).
- `SELECT CENTRADASALIDA FROM configuracion` + `UPDATE configuracion SET CENTRADASALIDA = <id+1>` (función `maxEntradaSalida`, contador global incremental para el número de referencia `MC<n>`, no se reinicia por día ni por sesión).

**Reportes generados:** Ninguno desde esta pantalla.

**Reglas de negocio / validaciones no obvias:**
- El monto siempre se guarda en `TOTAL_NETO`; `TOTAL_IMPUESTO` siempre es `0` para entradas/salidas manuales.
- El signo de `TOTAL_NETO` es lo único que distingue una entrada (positivo) de una salida (negativo); ambas comparten `ID_TIPO_PAGO=0 (Efectivo)`.
- La serie (`ID`) de caja es compartida por todos los movimientos mientras la caja esté abierta y es global entre usuarios (no por usuario), lo que puede mezclar secuencias si dos cajas están abiertas simultáneamente.
- No hay control de permisos visible en este archivo sobre quién puede registrar movimientos (cualquier usuario logueado).

**Notas / peculiaridades:**
- El combo `Razon` se crea y se llena con las mismas opciones que los radio buttons (`Rentrada`/`Rsalida`), pero **no se usa en ningún lugar de la lógica de guardado** (el tipo de movimiento se decide solo por el estado de los radio buttons). Es un control redundante/muerto visualmente superpuesto.
- El campo `fecha` (DateChooserCombo) se instancia y se configura (`setDateFormat`) pero **nunca se agrega al `GroupLayout`**, por lo que no es visible ni editable por el usuario; sin embargo su `getText()` se usa para poblar `FECHA_FIN` en el INSERT. Esto es casi con certeza un bug heredado del copy-paste desde otra pantalla: el valor guardado en `FECHA_FIN` dependerá del valor por defecto que el componente `DateChooserCombo` tenga al instanciarse (posiblemente vacío o la fecha actual), no de una elección real del usuario. Al migrar, lo correcto es que `FECHA_FIN` quede `NULL` hasta el cierre real de caja (igual que hace `cerrarCajaCalculado`), no un valor de un campo invisible.
- Mensajes de error en español con errores ortográficos originales ("Efecivo") que deben replicarse tal cual si se busca paridad exacta de UI, o corregirse si se opta por una versión limpia (a decisión del equipo de Regenta).

---

### CerrarCaja.java — Caja (arqueo y totales de la sesión abierta)

**Propósito:** Pantalla principal de caja del usuario: lista todos los movimientos (ventas, compras, entradas, salidas, abonos) de la sesión de caja actualmente `ABIERTO` del usuario logueado, calcula los totales por forma de pago y da acceso al cierre de caja (arqueo).

**Cómo se invoca / navegación:** `JInternalFrame` (título interno `"CerrarCaja"`, sin barra norte), abierto desde el menú principal. Al construirse llama `limpiar()`, que a su vez recarga la tabla de movimientos (`CargarMovimientoCaja(true)`) y los totales (`CargarTotales()`). Se cierra con el botón "Cerrar" o tecla ESC, volviendo a `"Menunew"`. Desde aquí se abre el diálogo de cierre real (`cerrarCajaCalculado`), el detalle de un movimiento (`DetalleMovimientoCaja`) o el detalle de una venta (`DetalleDeVenta`, pantalla fuera de este grupo).

**Componentes de interfaz:**
- `serie` (JTextField, solo lectura): número de serie que tendrá el próximo cierre (`MAX(ID) WHERE ESTADO='CERRADO'` + 1).
- Panel "Fecha de Cierre" con `serie`.
- Panel de resumen (`jPanel4`): `n_pagos` (# Pagos), `n_ventas` (# Ventas), `total_neto` (Total Neto), `total_impuestos` (Impuestos), `total` (Total) — **todos estos 5 campos se declaran pero no se rellenan en ningún método leído** (ver Notas); los totales reales se muestran en el panel `jPanel1` descrito abajo.
- Tabla `MOVIMIENTO_CAJA` (JTable, sin selección de teclado — `setFocusable(false)`), columnas de modelo: `FECHA, MOVIMIENTO, # FACTURA, IMPUESTOS, TOTAL NETO, Title 6, Title 7` (las dos últimas, índices 5 y 6, están ocultas con ancho 0: contienen el flag `SUM` y el flag `RENUENTE/NO` respectivamente, usados solo para lógica/formato de color, no para mostrarse). La columna `IMPUESTOS` (índice 3, con el texto "Title 6"/"Title 7" sin usar en pantalla) también se oculta con ancho 0 según `ModificarZiseColomnasTablas`/el bloque de ocultar columnas 3,5,6 del constructor. Columnas visibles reales: FECHA, MOVIMIENTO, #FACTURA, TOTAL NETO.
- Menú contextual (clic derecho) sobre la tabla (`jPopupMenu1`): "Eliminar Entrada / Salida" (`borrarMovmiento`), "Detallar Movimiento" (`jMenuItem2`), "Detallar Venta" (`jMenuItem3`).
- Panel `jPanel1` (resumen de totales por forma de pago), con pares cantidad/total:
  - Efectivo: `c1`/`t1`; Cheque: `c2`/`t2`; Vale: `c3`/`t3`; Tarjeta: `c4`/`t4`; Gratis: `c5`/`t5`; Debito: `c6`/`t6`; Banco: `c7`/`t7`; Entrada: `c8`/`t8`; Salida: `c9`/`t9`.
  - `TO1` (cantidad total de movimientos/ventas), `TO2` (total facturado, en verde).
- Panel `jPanel5`: `fecha_inicio` (fecha de apertura más antigua entre TODAS las cajas abiertas del sistema, no solo la del usuario — ver bug), `fecha` (hoy), `usuario` (nombre del usuario logueado), `privilegios` (privilegio del usuario logueado). Todos de solo lectura.
- Botón "Cerrar Caja" (`BcerrarCaja`, icono recibo): abre el diálogo de arqueo `cerrarCajaCalculado`. Deshabilitado si no hay ventas/movimientos (`numero_ventas==0`).
- Botón "Cerrar" (`jButton3`, icono delete): cierra la pantalla (vuelve al menú).

**Funcionalidades atómicas (numeradas):**
1. Al abrir/limpiar, `CargarMovimientoCaja(true)` ejecuta una consulta `UNION` sobre `caja, movimiento, tipo_pago` (y `facturarenuente` en la segunda rama) filtrando `ESTADO='ABIERTO'`, `FACTURA_REMISION='true'` y `ID_USUARIO = <usuario logueado>`, separando las facturas que están en `facturarenuente` (marcadas internamente `'RENUENTE'`) de las que no (`'NO'`), ordenado por `ID_FATURA`. Cada fila agrega: fecha (`FECHA_INICIO`), `"<MOVIMIENTO> - <TIPO_PAGO>"` en mayúsculas, `ID_FATURA`, `TOTAL_IMPUESTO`, `TOTAL_IMPUESTO+TOTAL_NETO`, el flag `SUM`, y el flag `'RENUENTE'/'NO'`.
2. Tras cargar la tabla, el foco se pone en la primera fila (`changeSelection(0,0,...)`), y se llama `CargarTotales()`.
3. `CargarTotales()` ejecuta una consulta `UNION` que agrupa por `TIPO_PAGO.TIPO_PAGO` (excluyendo movimientos Entrada/Salida, id 5/6) y por `movimiento.Movimiento` (solo Entrada/Salida, id 5/6), todo filtrado por `ESTADO='ABIERTO'`, `SUM='si'`, `ID_USUARIO = <usuario>`. Según el nombre de categoría devuelto (`Efectivo, Cheque, Vale, Tarjeta, Gratis, Debito, Banco, Entrada, Salida`) rellena el par cantidad/total correspondiente (`c1..c9`/`t1..t9`) formateado como moneda.
4. **Regla de exclusión de totales:** al acumular `totales_neto`/`totales_impuesto` (que alimentan `TO2`), se excluyen explícitamente las categorías `"Debito"` y `"Gratis"` (aunque sí se muestran sus cantidades/montos individuales en `c5/t5` y `c6/t6`). `TO1` (cantidad) sí suma **todas** las categorías, incluidas Débito y Gratis y Entrada/Salida.
5. Si `numero_ventas > 0` → `BcerrarCaja` habilitado; si es `0` → deshabilitado (no se puede cerrar una caja sin movimientos).
6. `maxIdMovimiento()` calcula y muestra en `serie` el siguiente número de cierre (`MAX(ID) WHERE ESTADO='CERRADO'` + 1).
7. `minFechaCaja()` calcula y muestra en `fecha_inicio` la fecha de apertura mínima entre **todas** las cajas con `ESTADO='ABIERTO'` del sistema completo (sin filtrar por usuario — ver bug en Notas).
8. Doble clic sobre una fila de `MOVIMIENTO_CAJA`: si el `# FACTURA` de esa fila existe en la tabla `ventas`, abre `DetalleDeVenta` (pantalla de otro grupo); si no, abre `DetalleMovimientoCaja` (detalle de entrada/salida/compra/abono).
9. Menú contextual → "Eliminar Entrada / Salida" (`borrarMovmientoActionPerformed`): solo actúa si el texto de la columna `MOVIMIENTO` de la fila seleccionada es exactamente `"ENTRADA - EFECTIVO"` o `"SALIDA - EFECTIVO"` (en mayúsculas). Si se cumple, pide confirmación **"Desea eliminar por completo el movimiento de caja?"** (YES_NO); si confirma, ejecuta `DELETE FROM caja WHERE ID_FATURA = '<factura>'`, quita la fila de la tabla en memoria y recalcula `CargarTotales()`. Para cualquier otra fila (ventas, compras, abonos) la opción no hace nada y no informa al usuario por qué.
10. Menú contextual → "Detallar Movimiento" (`jMenuItem2ActionPerformed`): **el manejador está vacío, no hace nada** (funcionalidad sin implementar / bug, ver Notas).
11. Menú contextual → "Detallar Venta" (`jMenuItem3ActionPerformed`): toma el `# FACTURA` de la fila seleccionada y abre `DetalleDeVenta` directamente, **sin verificar** que esa factura exista realmente en `ventas` (si se usa sobre una entrada/salida, puede fallar silenciosamente, el error se captura y solo se imprime en consola).
12. Doble clic sobre la etiqueta "Cantidad" (`jLabel35`, cabecera del panel de totales) con una fila seleccionada en `MOVIMIENTO_CAJA`: alterna el flag `SUM` (SI↔NO) de **todos** los registros de `caja` con ese `ID_FATURA` (vía `editarMovimientoCaja`) y recalcula los totales. Es una función "oculta" (no hay botón visible, solo funciona haciendo doble clic sobre una etiqueta de texto) para excluir/incluir una factura puntual del arqueo sin borrarla.
13. Doble clic sobre la etiqueta "Usuario" del panel de datos generales (`jLabel10MouseClicked`, línea asociada a `fecha`/datos del usuario): ejecuta `IdCajaAbierto()`, que busca el `ID` de la primera caja `ABIERTO` del sistema y, vía `sumarNo(id)`, pone como *tooltip* del campo `usuario` (`jLabel4` en el código, mapeado al label de "Usuario") la suma de los movimientos de esa caja marcados `SUM='no'` (los excluidos del arqueo), formateada como moneda. Doble clic en esa misma etiqueta limpia el tooltip. También es una función oculta sin indicación visual.
14. Atajo de teclado **Ctrl+Alt+R** sobre la tabla `MOVIMIENTO_CAJA`: vuelve a cargar los movimientos con `CargarMovimientoCaja(false)`, que usa la misma consulta pero **sin** el filtro `FACTURA_REMISION = 'true'`, mostrando también movimientos no marcados como remisión. Es un "refrescar/mostrar todo" oculto, sin botón visible.
15. Botón "Cerrar Caja" (`BcerrarCajaActionPerformed`): abre `cerrarCajaCalculado`, pasándole precargados: las cantidades (`C1..C7`) y los totales facturados por tipo de pago (`TF1..TF7`, donde `TF1` = suma de Efectivo + Entrada + Salida: `t1+t8+t9`), y el total facturado general (`TC`). Ver detalle en la sección de `cerrarCajaCalculado.java`.
16. Botón "Cerrar" (`jButton3ActionPerformed`) y tecla ESC → `cerrar()` (oculta y regresa a `"Menunew"`).

**Datos / tablas de BD leídas o escritas:**
- `SELECT ... FROM caja, movimiento, tipo_pago [, facturarenuente]` (UNION, filtra `ESTADO='ABIERTO'`, `ID_USUARIO`) — carga de la grilla de movimientos.
- `SELECT tipo_pago.TIPO_PAGO, COUNT(...), SUM(TOTAL_IMPUESTO), SUM(TOTAL_NETO), SUM(TOTAL_IMPUESTO+TOTAL_NETO) FROM caja, tipo_pago, movimiento ... UNION SELECT movimiento.Movimiento, ... FROM caja, movimiento ...` — totales por forma de pago y por Entrada/Salida.
- `SELECT MAX(ID) FROM CAJA WHERE ESTADO='CERRADO'` — próxima serie.
- `SELECT MIN(FECHA_INICIO) FROM CAJA WHERE ESTADO='ABIERTO'` — fecha de apertura mostrada (global, sin filtro de usuario).
- `DELETE FROM caja WHERE ID_FATURA = '<factura>'` — al eliminar un movimiento de entrada/salida manual.
- `SELECT SUM FROM CAJA WHERE ID_FATURA='<factura>'` (`ESTADO_SUM`) y `UPDATE CAJA SET SUM = '<SI|NO>' WHERE ID_FATURA = '<factura>'` (`editarMovimientoCaja`) — alternar inclusión de una factura en el arqueo.
- `SELECT SUM(TOTAL_IMPUESTO+TOTAL_NETO) FROM caja WHERE ID = '<id>' and SUM = 'no'` (`sumarNo`) — suma de movimientos excluidos.
- `SELECT ID from caja where estado='ABIERTO'` (`IdCajaAbierto`) — caja abierta usada para el tooltip (sin filtrar por usuario).
- `SELECT ID from ventas where ID='<factura>'` (`verificarFactura`) — distingue venta real de movimiento de caja.

**Reportes generados:** Ninguno directamente desde esta pantalla (el cierre real y la impresión del recibo ocurren en `cerrarCajaCalculado`).

**Reglas de negocio / validaciones no obvias:**
- Cada usuario ve y cierra únicamente los movimientos de **su propia** caja abierta (filtro `ID_USUARIO`), salvo `fecha_inicio` e `IdCajaAbierto()`, que son globales (bug, ver Notas).
- El botón de cierre solo se habilita si hay al menos un movimiento sumado (`numero_ventas>0`), sin importar si el total es positivo, cero o negativo.
- Débito y Gratis se excluyen del total facturado (`TO2`) aunque se registran y se cuentan individualmente; esto sugiere que esas formas de pago no deben "cuadrarse" en efectivo/arqueo (p. ej. Débito puede corresponder a datáfono registrado aparte, Gratis a cortesías sin cobro real).
- Solo se puede eliminar directamente desde la grilla un movimiento manual de Entrada/Salida en efectivo; ventas, compras y abonos no se pueden borrar desde aquí.
- La serie de cierre (`ID`) y la fecha de apertura mostrada no distinguen usuarios, lo que en un entorno multiusuario puede mostrar datos de la caja de otro usuario.

**Notas / peculiaridades:**
- Los campos `n_pagos`, `n_ventas`, `total_neto`, `total_impuestos`, `total` del panel `jPanel4` están declarados y visibles en el formulario pero **no se asignan en ningún método** de los leídos (ni en `CargarTotales` ni en `limpiar`): quedan siempre vacíos. Parecen ser un resumen duplicado/abandonado del panel `jPanel1` (que sí se llena). Al migrar, probablemente se deban omitir o unificar con el panel de totales por forma de pago.
- "Detallar Movimiento" en el menú contextual no tiene implementación (`jMenuItem2ActionPerformed` vacío): funcionalidad visible en la UI pero inoperante.
- "Detallar Venta" no valida que la fila corresponda a una venta real antes de abrir `DetalleDeVenta` (a diferencia del doble clic normal sobre la tabla, que sí usa `verificarFactura`).
- Las funciones de doble clic sobre las etiquetas "Cantidad" y "Usuario" son atajos no documentados en la UI (no hay tooltip, ícono ni indicación de que esas etiquetas son interactivas).
- Fuerte dependencia cruzada con `Ventas`/`VentasDiarias` (no leídas en este bloque): los registros de `caja` con movimiento Compra/Venta/Abono se generan desde esas pantallas, no desde este grupo; `CerrarCaja` solo los lee y los agrupa.
- El renderer `CurrencyTableCellRenderer_cerrarCaja` (ver abajo) colorea en amarillo pálido las filas marcadas `RENUENTE` (columna oculta 6) y alterna blanco/azul claro/gris cada 3 filas para las demás; al seleccionarlas todas se ven en verde claro.

---

### cerrarCajaCalculado.java — Cierre de caja (arqueo, diferencias e impresión)

**Propósito:** Diálogo de arqueo final: el usuario digita cuánto dinero/valores cuenta físicamente por cada forma de pago, el sistema calcula la diferencia contra lo facturado, y al confirmar cierra la caja en base de datos y opcionalmente imprime el recibo de cierre.

**Cómo se invoca / navegación:** `JDialog` modal (`setTitle("Cerrar caja")`, no resizable), instanciado únicamente desde `CerrarCaja.BcerrarCajaActionPerformed`, recibiendo `(CerrarCaja obj, boolean modal, String serie)`. `CerrarCaja` precarga en el diálogo: `C1..C7` = cantidades por forma de pago (texto), `TF1` = Efectivo+Entrada+Salida combinados (`t1+t8+t9`), `TF2..TF7` = montos facturados de Cheque/Vale/Tarjeta/Gratis/Débito/Banco, `TC`/`TTF` = totales generales, y llama `editable()` antes de mostrarlo.

**Componentes de interfaz:**
- Por cada forma de pago (Efectivo, Cheque, Vale, Tarjeta, Gratis, Débito, Banco) una fila con: `Cn` (cantidad de movimientos, solo lectura), `TFn` (total facturado, solo lectura), `TCn` (total **contado** por el usuario — editable solo si `TFn>0`), `Rn` (diferencia `TFn - TCn`, solo lectura). Nota: Gratis (TC5) y Débito (TC6) quedan **siempre no editables** (`setEditable(false)` fijo en el diseño), aunque `editable()` intenta habilitarlos igual que los demás si tienen monto `>0` (contradicción, ver Notas).
- `TC` (total facturado general), `TTF` (total facturado general, duplicado), `TTC` (total contado = suma de `TC1+TC2+TC3+TC4+TC7`), `TR` (resta/diferencia general = `TTF - TTC`).
- Botón "Calcular" (`jButton3`): abre una calculadora en pantalla (clase externa `calculado`, no documentada en este bloque) que al confirmar llena `TC1` mediante el método público `calculado(double)`.
- Botón "Cerrar Caja" (`jButton4`, icono recibo): ejecuta el cierre real.

**Funcionalidades atómicas (numeradas):**
1. Al abrir (`editable()`): por cada forma de pago con total facturado `>0` (Efectivo, Cheque, Vale, Tarjeta, Banco) habilita el campo `TCn` correspondiente para edición; si el total facturado es `0`, deshabilita el campo. Inicializa todos los `TCn` visibles a `"$0"` formateado y llama `calcularResta()`. Pone el foco en `TC1`.
2. Al ganar foco cualquier `TCn` editable: fondo azul claro `(223,223,238)` y texto convertido a número simple (`formatoNumero2`) para facilitar la edición.
3. Al escribir en `TCn`: solo acepta dígitos y el carácter coma `,` (código 44) — `pasarFocusVerificarNumeros(evt,true)` con excepción explícita para la coma decimal; Enter transfiere el foco.
4. Al perder foco cualquier `TCn`: se reformatea como moneda (o `"$0"` si quedó vacío), fondo vuelve a blanco, y se recalcula `calcularResta()`.
5. `calcularResta()` (se ejecuta tras cada edición): calcula `totalesCalculado = TC1+TC2+TC3+TC4+TC7` (nota: TC5 y TC6 — Gratis y Débito — **no** se incluyen en el total contado, consistente con que tampoco se incluyen en el total facturado a cuadrar); `TTC` = ese total formateado; `TR` = `TTF - totalesCalculado` (diferencia general, sobra/falta); y por cada forma de pago individual `Rn = TFn - TCn` (diferencia puntual, positiva si falta dinero, negativa si hay sobrante).
6. Botón "Calcular" (`jButton3ActionPerformed`): abre el diálogo calculadora auxiliar `calculado`, cuyo resultado se inyecta en `TC1` (cantidad de efectivo contada) vía el método `calculado(double)`.
7. Botón "Cerrar Caja" (`jButton4ActionPerformed`): valida que `TC1` esté habilitado y que su valor sea `>0`; si no, mensaje de error **"Debe escribir la cantidad de efectivo que hay caja."** y foco a `TC1`. Si es válido, llama `cerrarCaja()`.
8. `cerrarCaja()`:
   - Pide confirmación **"Procede a cerrar caja, está seguro?"** (YES_NO). Si el usuario responde que no, el foco vuelve a `TC1` y no ocurre nada más.
   - Si confirma: ejecuta `UPDATE caja SET FECHA_FIN = <hoy>, ESTADO = 'CERRADO' WHERE ESTADO = 'ABIERTO' AND ID_USUARIO = <usuario logueado>` — cierra **todos** los movimientos abiertos del usuario de una sola vez.
   - Intenta `DELETE FROM FACTURARENUENTE WHERE ID_USUARIO = <usuario>` y `DELETE FROM remisionrenuente WHERE ID_USUARIO = <usuario>` (limpia las marcas de facturas "renuentes"/pendientes del usuario; si falla, solo se registra en consola).
   - Pregunta **"Desea imprimir recibo?"** (YES_NO):
     - Si **sí**: configura `MenuPrincipal.DATOS_OBLIGATORIOS["NOMBRE_REPORTE"]="CAJA/Recibo_cerrar_caja"` y `["ID"]=serie` (la serie de cierre recibida al construir el diálogo), crea `Imprimir_Exportar` y llama `Imprimir(<impresora térmica configurada>)` — imprime el recibo de cierre en la impresora térmica configurada en el sistema. Luego actualiza `fecha_inicio` en `CerrarCaja` a la fecha actual, llama `obj.limpiar()` (recarga la pantalla padre) y cierra el diálogo.
     - Si **no**: solo `obj.limpiar()` y cierra el diálogo (sin imprimir).

**Datos / tablas de BD leídas o escritas:**
- `UPDATE caja SET FECHA_FIN = '<hoy>', ESTADO = 'CERRADO' WHERE ESTADO = 'ABIERTO' AND ID_USUARIO = '<usuario>'` — operación central del cierre.
- `DELETE FROM FACTURARENUENTE WHERE ID_USUARIO = '<usuario>'`.
- `DELETE FROM remisionrenuente WHERE ID_USUARIO = '<usuario>'`.

**Reportes generados:** `CAJA/Recibo_cerrar_caja` (jrxml `Recibo_cerrar_caja`), impreso directamente en la impresora térmica configurada (`DATOS_OBLIGATORIOS["IMPRESORA_TERMICA"]`), parametrizado con `ID = serie` (el número de cierre).

**Reglas de negocio / validaciones no obvias:**
- El cierre exige que el usuario digite **al menos** el efectivo contado (`TC1 > 0`); las demás formas de pago no tienen esa validación explícita (pueden quedar en 0 sin bloquear el cierre).
- El cierre es irreversible desde esta pantalla: `UPDATE ... WHERE ESTADO='ABIERTO'` cierra TODOS los movimientos abiertos del usuario (no solo los de una fecha o rango), consolidándolos bajo una única `FECHA_FIN` (la fecha actual del sistema).
- Débito y Gratis quedan fuera del cálculo de diferencia (`TTC`/`TR`), igual que en `CerrarCaja`.
- No hay rol/permiso explícito verificado en este archivo para autorizar el cierre; cualquier usuario logueado con caja abierta puede cerrarla.

**Notas / peculiaridades:**
- `TC5` (Gratis) y `TC6` (Débito) se crean con `setEditable(false)` en el diseño de formulario, pero el método `editable()` intenta "habilitarlos" con `setEditable(true)` si su facturado es `>0`; como nunca se evalúa su total en `editable()` (solo se evalúan TF1,TF2,TF3,TF4,TF7), en la práctica **quedan siempre bloqueados**: el usuario nunca puede contar/cuadrar Gratis ni Débito, coherente con que tampoco entran en los totales.
- `R5`/`R6` (diferencia de Gratis/Débito) se fuerzan siempre a `"$0"` en `calcularResta()`, sin calcularse realmente.
- La clase auxiliar `calculado` (calculadora en pantalla) no fue parte de los archivos a documentar en detalle; solo se referencia como dependencia cruzada para poblar `TC1`.
- Dependencia directa con `CerrarCaja` (pasa su instancia como `obj` para refrescarla al cerrar) y con `Imprimir_Exportar` (módulo de impresión/reportes común a todo el sistema).

---

### buscarCierreCaja.java — Buscar cierre de caja

**Propósito:** Diálogo de búsqueda que permite localizar un cierre de caja histórico (por usuario y fecha de cierre) y cargarlo en la pantalla de Informes de Caja para ver su detalle.

**Cómo se invoca / navegación:** `JDialog` modal, instanciado desde `InformesCaja` al pulsar el botón "Buscar Cierre Caja" (`jButton6`), pasándole la instancia de `InformesCaja` (`Obj`) y `modal=true`. No está pensado como pantalla independiente: siempre requiere un `InformesCaja` padre para poder reportar la selección.

**Componentes de interfaz:**
- `fechaInicial`, `fechaFinal` (DateChooserCombo, formato `yyyy-MM-dd`, no enfocables directamente con tabulador — `setFocusable(false)`): filtro de rango de fechas de cierre.
- Tabla `cierresCaja` (JTable, no enfocable, una sola selección): columnas de modelo `FECHA, USUARIO, PRIVILEGIOS, TOTAL FACTURADO, idUsuario, id` — las dos últimas (`idUsuario`, `id`) ocultas con ancho 0, usadas solo como datos internos para la navegación. Tiene un `TableRowSorter` (`trsfiltro`) activo (permite ordenar columnas con clic en cabecera).
- Botón `mas` (icono "ok.png"): declarado con manejador vacío (sin funcionalidad, ver Notas).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, carga automáticamente (sin que el usuario pida nada) los **últimos 30** cierres: `SELECT FECHA_FIN, usuarios.NOMBRES_APELLIDO, usuarios.PRIVILEGIOS, SUM(TOTAL_NETO+TOTAL_IMPUESTO), usuarios.id_usuario, ID FROM caja, usuarios WHERE caja.ID_USUARIO = usuarios.ID_USUARIO AND caja.SUM='SI' AND caja.ID_TIPO_PAGO NOT IN (4,5) AND caja.ESTADO='CERRADO' GROUP BY FECHA_FIN, usuarios.NOMBRES_APELLIDO ORDER BY FECHA_FIN DESC LIMIT 30`. **Nota:** `ID_TIPO_PAGO NOT IN (4,5)` excluye Gratis(4) y Débito(5) del total mostrado, igual criterio que en `CerrarCaja`.
2. Al seleccionar `fechaInicial` (evento `onCommit`) o `fechaFinal` (evento `onCommit`), se vuelve a cargar la tabla filtrando por rango: `WHERE '<fechaInicial>' <= FECHA_FIN and FECHA_FIN <= '<fechaFinal>' and ...` (mismas condiciones de usuario/SUM/tipo de pago/estado). **Esta consulta filtrada selecciona solo 5 columnas (le falta el alias final `,ID`)** — ver bug en Notas, rompe la carga de filas.
3. Doble clic sobre una fila de `cierresCaja`: si hay un `InformesCaja Obj` asociado, convierte la fecha mostrada (`dd/MM/yyyy`) a formato `yyyy-MM-dd` (`StringADate`) y llama `Obj.cargarDetalleCierreCaja(fecha, idUsuario, totalFacturado, id)`, luego cierra el diálogo (`dispose()`).
4. Cualquier excepción durante la carga doble-clic se captura en silencio (bloque `catch` vacío): si falla, simplemente no pasa nada visible para el usuario.

**Datos / tablas de BD leídas o escritas:** Solo lectura (`SELECT`) sobre `caja` y `usuarios`, como se detalla arriba. No escribe nada en base de datos.

**Reportes generados:** Ninguno directamente; su función es alimentar a `InformesCaja`, que sí genera reportes.

**Reglas de negocio / validaciones no obvias:**
- Solo se listan cierres con `SUM='SI'` (movimientos que efectivamente se contaron en el arqueo) y `ESTADO='CERRADO'`.
- Se excluyen explícitamente Gratis y Débito del total mostrado por cierre, igual que en el resto del módulo de caja.
- El límite de 30 resultados aplica siempre, tanto en la carga inicial como en la carga filtrada por fecha.

**Notas / peculiaridades:**
- **Bug de columnas:** la consulta inicial del constructor selecciona 6 columnas (incluye `ID` al final); las consultas de `fechaInicialOnCommit`/`fechaFinalOnCommit` solo seleccionan 5 (falta `,ID`), pero `cargarCierresCaja` siempre intenta leer 6 columnas (`resultado.getString(6)`). Al filtrar por fecha, la lectura de la 6ª columna lanza una excepción que se captura genéricamente y aborta el llenado de la tabla a partir de la primera fila, dejando la grilla vacía o incompleta aunque existan cierres en el rango. **Al migrar, corregir la consulta filtrada para que incluya también `ID`.**
- El botón `mas` no tiene lógica implementada (`masActionPerformed` vacío) ni aparece usado en el flujo de doble clic; es un control visual sin función real en esta versión.
- Hay métodos muertos/comentados (`pasarFocusVerificarNumeros`, `VerificarFactura`) que replican lógica de otras pantallas de búsqueda de facturas pero no se usan en este flujo (restos de copiar/pegar desde una pantalla de búsqueda de ventas).
- Columna "TOTAL FACTURADO" se renderiza con `CurrencyTableCellRenderer_informeCaja` (formato moneda + color alternante cada 3 filas, ver sección de renderers).
- Dependencia directa: solo puede usarse desde `InformesCaja` (requiere su instancia para poder reportar la selección; el constructor no soporta un uso standalone funcional).

---

### DetalleMovimientoCaja.java — Detalle de un movimiento de caja individual

**Propósito:** Mostrar en solo lectura el detalle de un único registro de `caja` identificado por su `ID_FATURA` (usado para ver compras, abonos, entradas o salidas que no son ventas, ya que las ventas se muestran con la pantalla `DetalleDeVenta`).

**Cómo se invoca / navegación:** `JDialog` modal, sin opción de redimensionar. Tiene **dos constructores** equivalentes en funcionalidad, que solo cambian el tipo de pantalla padre que almacenan (sin usarla realmente, ver Notas):
- `DetalleMovimientoCaja(CerrarCaja parent, boolean modal, String id)` — invocado desde `CerrarCaja` al hacer doble clic en una fila que no es una venta.
- `DetalleMovimientoCaja(detellaMovimientoCaja parent, boolean modal, String id)` — invocado desde `detellaMovimientoCaja` al hacer doble clic en una fila que no es una venta.

**Componentes de interfaz:**
- `tf1` (Movimiento, solo lectura): muestra el **código numérico crudo** de `IDMOVIMIENTO` (columna 2 de `caja`), no el nombre textual (ver bug en Notas).
- `tf2` (Factura - Referencia, solo lectura): `ID_FATURA` (columna 3).
- `fecha` (Fecha, solo lectura, formato `dd/MM/yyyy`): `FECHA_INICIO` (columna 7).
- `tf3` (Total Impuesto, solo lectura, alineado a la derecha): `TOTAL_IMPUESTO` (columna 4) formateado como moneda.
- `tf4` (Total Neto, solo lectura, alineado a la derecha): `TOTAL_NETO` (columna 5) formateado como moneda.
- `tf5` (Descripción, JTextArea solo lectura, fondo gris): `DESCRIPCION` (columna 6).
- Botón "Cerrar" (`jButton3`, icono delete).

**Funcionalidades atómicas (numeradas):**
1. Al abrir, `ListarMovimiento(id)` ejecuta `SELECT * FROM caja WHERE ID_FATURA = '<id>'` y toma **solo el primer registro** devuelto (`if(resultado.next())`, no itera); si hay varias filas de `caja` con el mismo `ID_FATURA` (ej. un pago dividido en varias formas de pago), solo se muestra la primera según el orden natural de la consulta (sin `ORDER BY`).
2. Rellena los 6 campos descritos arriba a partir de esa única fila.
3. Botón "Cerrar" (`jButton3ActionPerformed`) → `dispose()`.
4. Tecla **ESC** (`keyReleased`) → `dispose()`.
5. Todos los `keyPressed`/`keyTyped` arrojan `UnsupportedOperationException` si llegaran a dispararse (no se usan realmente salvo `keyReleased`).

**Datos / tablas de BD leídas o escritas:** Solo lectura: `SELECT * FROM caja WHERE ID_FATURA = '<id>'`.

**Reportes generados:** Ninguno.

**Reglas de negocio / validaciones no obvias:** Ninguna validación de negocio; es una ventana de solo consulta.

**Notas / peculiaridades:**
- **Bug de presentación:** el campo etiquetado "Movimiento" (`tf1`) muestra el valor crudo de `IDMOVIMIENTO` (p. ej. `"1"`, `"2"`, `"5"`, `"6"`) en vez del nombre legible (`Compra`, `Venta`, `Entrada`, `Salida`), porque la consulta usa `SELECT *` y no hace `JOIN` con la tabla `movimiento`. Al migrar, debería mostrarse el nombre del movimiento.
- Si hay múltiples filas de `caja` con el mismo `ID_FATURA` (pagos mixtos), esta pantalla solo refleja una de ellas, pudiendo mostrar un total parcial engañoso.
- Los campos `obj`/`obj1` (referencias al padre `CerrarCaja`/`detellaMovimientoCaja`) se guardan pero **nunca se usan** dentro de la clase: no hay ninguna llamada de retorno hacia el padre. Parecen vestigios de un patrón de callback que no se llegó a implementar.
- Nombre de clase casi idéntico a `detellaMovimientoCaja.java` (con errata "detella" en vez de "detalla"); son archivos distintos con roles distintos (ver siguiente sección), fácil de confundir en el código fuente.

---

### detellaMovimientoCaja.java — Detalle de movimientos de un cierre de caja (listado completo)

**Propósito:** Mostrar, para un cierre de caja específico ya seleccionado (fecha de cierre + usuario), el listado completo de todos los movimientos que lo componen (ventas, compras, abonos, entradas, salidas) agrupados por factura, con una fila de total al final.

**Cómo se invoca / navegación:** `JDialog` modal, no redimensionable, título **"Detalle movimientos de caja"**. Se instancia únicamente desde `InformesCaja`, al pulsar el botón "Detalles de movimientos de caja" (`b1`), después de que ya se cargó el detalle de un cierre mediante `cargarDetalleCierreCaja` (desde `buscarCierreCaja` o similar). Constructor: `detellaMovimientoCaja(InformesCaja parent, boolean modal, String fechaCierre, String idUsuario, Double totalFacturado)`.

**Componentes de interfaz:**
- Tabla `MOVIMIENTO` (JTable, no enfocable): columnas `#, FECHA, USUARIO, PRIVILEGIOS, # FACTURA, MOVIMIENTO, TIPO PAGO, TOTAL`, todas de solo lectura, con formato de moneda aplicado por `CurrencyTableCellRenderer_detalleMovimientoCaja` a las 8 columnas.

**Funcionalidades atómicas (numeradas):**
1. Al abrir, `cargarDetalleCierreCaja(fechaCierre, idUsuario, totalFacturado)` ejecuta: `SELECT CAJA.FECHA_INICIO, usuarios.NOMBRES_APELLIDO, usuarios.PRIVILEGIOS, ID_FATURA, movimiento.Movimiento, tipo_pago.TIPO_PAGO, SUM(TOTAL_IMPUESTO+TOTAL_NETO) FROM caja, tipo_pago, movimiento, usuarios WHERE ... ESTADO='CERRADO' AND SUM='si' AND ID_USUARIO='<idUsuario>' AND FECHA_FIN='<fechaCierre>' GROUP BY ID_FATURA` (todo en mayúsculas en usuario/privilegios/movimiento/tipo de pago), numerando cada fila (`#`) de forma incremental.
2. Al final de la tabla agrega una fila de totales con la etiqueta `"TOTAL"` en la columna `TIPO PAGO` y el valor `totalFacturado` (recibido como parámetro, calculado originalmente en `InformesCaja`) en la columna `TOTAL`.
3. Doble clic sobre una fila: si el `# FACTURA` corresponde a una venta real (`verificarFactura`), abre `DetalleDeVenta`; si no, abre `DetalleMovimientoCaja` usando el segundo constructor (con `detellaMovimientoCaja` como padre).

**Datos / tablas de BD leídas o escritas:** Solo lectura: la consulta `SELECT` descrita arriba sobre `caja, tipo_pago, movimiento, usuarios`, y `SELECT ID from ventas where ID='<numero_factura>'` (verificación de venta en el doble clic).

**Reportes generados:** Ninguno directamente (solo alimenta la vista en pantalla).

**Reglas de negocio / validaciones no obvias:**
- Solo incluye movimientos con `SUM='si'` (los efectivamente contados en ese cierre) y `ESTADO='CERRADO'`.
- El total mostrado en la fila final no se recalcula localmente; se recibe ya calculado desde `InformesCaja` (consistencia dependiente de esa pantalla).

**Notas / peculiaridades:**
- Prácticamente el mismo propósito visual que `buscarCierreCaja` + `InformesCaja` combinados, pero a nivel de **detalle de movimientos individuales** de un cierre ya elegido, mientras que `buscarCierreCaja` lista **cierres completos** para elegir uno. No deben confundirse ni fusionarse al migrar: son dos niveles de navegación (cierre → detalle de movimientos del cierre).
- Comparte estructura casi idéntica con `DetalleMovimientoCaja.java` en el nombre (typo "detella" vs "detalla"), pero su contenido y responsabilidad son completamente distintos: esta clase es una **tabla de todos los movimientos de un cierre**, mientras que `DetalleMovimientoCaja.java` es el **detalle de un solo movimiento**.
- Mismo patrón de doble clic (venta real vs. movimiento de caja) replicado también en `CerrarCaja.MOVIMIENTO_CAJAMouseClicked`: código duplicado en tres lugares (`CerrarCaja`, `detellaMovimientoCaja`, implícitamente también relevante para `DetalleMovimientoCaja`), candidato a unificar en la migración.

---

### InformesCaja.java — Informes de caja

**Propósito:** Pantalla central de reportes de caja: permite listar cierres de caja (por rango de fechas o por secuencia puntual), ver el resumen de un cierre concreto (ventas, compras, entradas, salidas, totales), y disparar la generación/impresión de los distintos reportes Jasper del módulo de caja.

**Cómo se invoca / navegación:** `JInternalFrame`, abierto desde el menú principal. Al construirse, llama `cargarInformes("")` (incrusta el panel de reporte vacío) y `limpiar()`. Internamente abre: `buscarCierreCaja` (modal, para elegir un cierre histórico), `detellaMovimientoCaja` (modal, detalle de movimientos de un cierre), y una clase `fechas` (modal, no incluida en el alcance de este documento, para elegir rango de fechas del informe de cierres). Se cierra con el botón "Cerrar" (`jButton5`), volviendo a `"Menunew"`.

**Componentes de interfaz:**
- Combo `INFORME`: opciones `"Cierres de caja"` (índice 0) y `"Cierre de caja por secuencia"` (índice 1).
- `fecha_i` / `fecha_f` (DateChooserCombo, formato `yyyy-MM-dd`): rango de fechas para el informe tipo "Cierres de caja"; en el modo "por secuencia" solo se usa `fecha_i` (fecha de inicio exacta de una caja) y `fecha_f` se deshabilita.
- Botón "Ejecutar Informe" (`jButton4`): incrusta en pantalla (`CONTENEDOR`) el reporte `cierresCaja` o `cierresCajaSecuencia` según el combo.
- Botón "Imprimir Tirilla" (`tirilla`, solo habilitado en modo "por secuencia"): imprime el recibo térmico de cierre de la caja cuya `FECHA_INICIO` coincide con `fecha_i`.
- Botón "Limpiar" (`jButton2`), botón "Cerrar" (`jButton3`, sin lógica propia más allá de `cerrar()`… en realidad no se usa, ver Notas), botón "Cerrar" real (`jButton5`, el que efectivamente cierra la pantalla), botón "Buscar Cierre Caja" (`jButton6`, abre `buscarCierreCaja`).
- Panel de detalle de un cierre concreto (`jPanel3`), de solo lectura: `fecha_inicio` (apertura), `fecha_inicio1` (cierre), `usuario`, `usuario1` (privilegios), `ventas`, `ventasCredito`, `compras`, `comprasCredito`, `totalVentas`, `totalCompras`, `entradas`, `totalEntradas`, `salidas`, `totalSalidas`, `totalFacturado`.
- Panel de acciones (`jPanel4`) con 5 botones, todos actúan sobre el cierre actualmente cargado en `jPanel3` (excepto `b4`):
  - `b1` "Detalles de movimientos de caja" → abre `detellaMovimientoCaja`.
  - `b2` "Informe general de caja" → exporta a PDF el reporte `reporteGeneralCaja`.
  - `b3` "Informe detallado de caja" → exporta a PDF el reporte `reporteDetalladoCaja`.
  - `b4` "Informe de cierres de caja" → abre el diálogo `fechas` para elegir un rango y exportar `cierresCaja` (no depende de tener un cierre cargado).
  - `b5` "Imprimir tikete de caja" → imprime el recibo térmico `Recibo_cerrar_caja` del cierre cargado.

**Funcionalidades atómicas (numeradas):**
1. `limpiar()`: selecciona el combo en índice 0, habilita `fecha_i`/`fecha_f`, deshabilita `tirilla`, pone fechas de hoy en `fecha_inicio`/`fecha_inicio1`, vacía usuarios y todos los totales (a `"$0"`), y deshabilita `b1, b2, b3, b5` (nota: **`b4` no se deshabilita**, queda siempre activo).
2. Al cambiar el combo `INFORME` (`INFORMEActionPerformed`): si se selecciona índice 0 ("Cierres de caja"), habilita `fecha_i`/`fecha_f` y deshabilita `tirilla`; si se selecciona índice 1 ("Cierre de caja por secuencia"), deshabilita `fecha_f`, mantiene `fecha_i` habilitado, y habilita `tirilla`.
3. `armarConsula()` construye dinámicamente el SQL del reporte embebido: si `INFORME`=0, filtra por rango `'<fecha_i>' <= FECHA_INICIO and FECHA_FIN <= '<fecha_f>'` y agrupa cierres por `id` (`SELECT id, CONCAT(FECHA_FIN,' - ',FECHA_FIN) AS FECHA, SUM(TOTAL_IMPUESTO), SUM(TOTAL_NETO), SUM(ambos) FROM caja WHERE id<>0 ... GROUP BY id`); si `INFORME`=1, filtra por `'<fecha_i>' = FECHA_INICIO` y lista el detalle crudo de `CAJA` (sin agrupar) con `ID, FECHA_INICIO, MOVIMIENTO, ID_FATURA, TOTAL_IMPUESTO, TOTAL_NETO, total, SUM`.
4. Botón "Ejecutar Informe" (`jButton4ActionPerformed`): según el combo, fija `nombre_reporte` = `"cierresCaja"` o `"cierresCajaSecuencia"`, y llama `cargarInformes(nombre_reporte)`, que a su vez llama `datosExportarImprimir` (fija `NOMBRE_REPORTE`, `DIR="CAJA/"`, `consulta=armarConsula()`, `periodo="<fecha_i> - <fecha_f>"`) y luego `Imprimir_Exportar.Incrustar()` (renderiza el reporte Jasper embebido dentro del panel `CONTENEDOR`, dentro de la misma ventana, sin exportar archivo).
5. Botón "Imprimir Tirilla" (`tirillaActionPerformed`): obtiene `Secuencia()` = `SELECT id FROM caja WHERE FECHA_INICIO = '<fecha_i>' GROUP BY id` (primer resultado, o `"0"` si no hay). Si es `"0"` → mensaje de error **"Secuencia no encontrada."**. Si no, fija `DATOS_OBLIGATORIOS["ID"]=secuencia`, prepara `datosExportarImprimir("CAJA/Recibo_cerrar_caja","CAJA/")` e imprime directamente con `Imprimir_Exportar.Imprimir(<impresora térmica>)`.
6. `cargarDetalleCierreCaja(fechaCierre, idUsuario, totalFacturado, secuencia)` (llamado externamente desde `buscarCierreCaja` o `detellaMovimientoCaja`/flujo de selección): ejecuta un `SELECT` agregado por `movimiento.Movimiento` sobre `caja, tipo_pago, movimiento, usuarios` filtrando `ESTADO='CERRADO'`, `SUM='si'`, `ID_USUARIO`, `FECHA_FIN`, calculando además `EFECTIVO` (tipo_pago in 0,1,2,3,6) y `DEBITO` (tipo_pago in 4,5 — es decir Gratis+Débito juntos, pese a llamarse "DEBITO"). Según el nombre de movimiento devuelto (`Compra, Entrada, Salida, Venta`) rellena los campos correspondientes del panel de detalle (`compras`/`totalCompras`/`comprasCredito`, `entradas`/`totalEntradas`, `salidas`/`totalSalidas`, `ventas`/`totalVentas`/`ventasCredito`). Al terminar, muestra `totalFacturado` y habilita `b1, b2, b3, b4, b5`.
7. Botón "Detalles de movimientos de caja" (`b1`): abre `detellaMovimientoCaja` con los datos del cierre actualmente cargado (`fechaCierre`, `idUduario`, `todalFacturado`).
8. Botón "Informe general de caja" (`b2`): fija `NOMBRE_REPORTE="CAJA/reporteGeneralCaja"`, `DIR="CAJA/"`, `idUsuario`, `fechaFin=fechaCierre`, `todalFacturado`, `NOMBRE_ARCHIVO_EXPORTAR` = carpeta de exportación configurada + `"reporteGeneralCaja_<numero aleatorio>.pdf"`, y llama `Imprimir_Exportar.Exporar()` (genera el PDF en disco).
9. Botón "Informe detallado de caja" (`b3`): igual que `b2` pero con reporte `reporteDetalladoCaja` y archivo `reporteDetalladoCaja_<numero aleatorio>.pdf`.
10. Botón "Informe de cierres de caja" (`b4`): abre el diálogo `fechas` (no documentado en este bloque); su flujo termina llamando `informeFechas(fechaInicial, fechaFinal)`, que fija `NOMBRE_REPORTE="CAJA/cierresCaja"`, el rango de fechas, y exporta a PDF (`cierresCaja_<numero aleatorio>.pdf`) vía `Imprimir_Exportar.Exporar()`.
11. Botón "Imprimir tikete de caja" (`b5`): fija `NOMBRE_REPORTE="CAJA/Recibo_cerrar_caja"`, `ID=secuencia`, `idUsuario`, e imprime directamente (no exporta PDF) con `Imprimir_Exportar.Imprimir(<impresora térmica>)`.
12. Botón "Limpiar" (`jButton2`) → `limpiar()`.
13. Botón "Cerrar" real de la pantalla (`jButton5ActionPerformed`) → oculta la ventana y vuelve a `"Menunew"`.
14. Botón "Buscar Cierre Caja" (`jButton6ActionPerformed`) → abre `buscarCierreCaja(this, true)`.

**Datos / tablas de BD leídas o escritas:** Exclusivamente lectura (`SELECT`) sobre `caja, movimiento, tipo_pago, usuarios`, igual patrón que el resto del módulo (sin `INSERT/UPDATE/DELETE`): esta pantalla es puramente de consulta/reporte.

**Reportes generados (todos en `REPORTES/CAJA`):**
- `cierresCaja` — listado de cierres agrupados por rango de fechas, incrustado en pantalla (botón "Ejecutar Informe", `INFORME`=0) o exportado a PDF (`informeFechas`, vía `b4`/diálogo `fechas`).
- `cierresCajaSecuencia` — detalle crudo de una caja específica por fecha de inicio exacta, incrustado en pantalla (botón "Ejecutar Informe", `INFORME`=1).
- `reporteGeneralCaja` — exportado a PDF (`b2`), resumen general del cierre cargado.
- `reporteDetalladoCaja` — exportado a PDF (`b3`), detalle del cierre cargado.
- `Recibo_cerrar_caja` — tirilla térmica del cierre, impresa directamente (no exportada a archivo) tanto desde `tirilla` (modo "por secuencia") como desde `b5` ("Imprimir tikete de caja").

**Reglas de negocio / validaciones no obvias:**
- `b4` ("Informe de cierres de caja") es el único de los 5 botones de acción que permanece siempre habilitado, ya que no depende de tener un cierre específico cargado: abre su propio selector de rango de fechas.
- El bucket "DEBITO" en `cargarDetalleCierreCaja` realmente agrupa **Gratis + Débito** (tipo_pago 4 y 5), no solo Débito; se calcula pero no se expone visualmente en ningún campo de este archivo (es una columna calculada sin uso aparente en la UI actual).
- La búsqueda por "secuencia" (`Secuencia()`) empareja por `FECHA_INICIO` exacta, no por el número de serie visible al usuario; si dos cajas distintas comparten la misma `FECHA_INICIO` (poco probable pero posible si son del mismo instante), tomaría solo la primera.

**Notas / peculiaridades:**
- Existen **dos botones "Cerrar" distintos** en el código: `jButton3` (cuyo manejador llama a un método privado `cerrar()` que sí hace `setVisible(false)` + `ActivarFrame`) y `jButton5` (que repite literalmente la misma lógica de `cerrar()` inline, en vez de reutilizar el método). Ambos están declarados en la UI, pero por la disposición del formulario (sección de "Fecha de Cierre"/parámetros vs. pie de pantalla) probablemente solo uno sea visible al usuario final a la vez; de cualquier forma, es código duplicado.
- `cargarInformeCaja(Date, String)` está declarado pero con cuerpo **completamente vacío**: método muerto sin uso real.
- El reporte "DEBITO" calculado en `cargarDetalleCierreCaja` mezclando Gratis+Débito es inconsistente con el resto del sistema, que normalmente los trata como categorías separadas (`CerrarCaja`, `cerrarCajaCalculado`); se debe decidir en Regenta si esta agrupación es intencional o un descuido.
- Fuerte dependencia cruzada con `buscarCierreCaja` (quien le entrega el cierre a cargar) y `detellaMovimientoCaja` (detalle de movimientos), además de la clase `fechas` (selector de rango, fuera de este bloque) y el módulo común `Imprimir_Exportar` (impresión/exportación Jasper, usado también por `cerrarCajaCalculado`).

---

### Notas sobre los renderers de moneda (`OTRASCLASES`)

Las tres clases `CurrencyTableCellRenderer_*` solo controlan **presentación** de las tablas (color de fondo alternado/selección y alineación/formato de moneda), no lógica de negocio:

- **`CurrencyTableCellRenderer_cerrarCaja`** (usado en `CerrarCaja.MOVIMIENTO_CAJA`): si la columna oculta 6 (flag `RENUENTE`) de la fila vale `"RENUENTE"`, pinta la fila en amarillo pálido (no seleccionada) o verde claro (seleccionada); si no, alterna blanco / azul muy claro / gris cada 3 filas (patrón `(fila+1)%3` y luego `%2`). Formatea como moneda (`NumberFormat.getCurrencyInstance()`) las columnas 3 y 4 (IMPUESTOS y TOTAL NETO); alinea: col 0 centro, col 1 izquierda, col 2 centro, col 3 y 4 derecha.
- **`CurrencyTableCellRenderer_informeCaja`** (usado en `buscarCierreCaja.cierresCaja`): mismo alternado de color de fondo cada 3 filas (sin caso especial "RENUENTE"). Formatea como moneda solo la columna 3 (TOTAL FACTURADO); alinea: col 0 centro, col 1 izquierda, col 2 centro, col 3 derecha.
- **`CurrencyTableCellRenderer_detalleMovimientoCaja`** (usado en `detellaMovimientoCaja.MOVIMIENTO`): si la columna 6 (TIPO PAGO) de la fila vale literalmente `"TOTAL"` (la fila de total agregada al final de la tabla), la pinta en gris `(204,204,204)` con fuente en negrita; si no, aplica el mismo alternado cada 3 filas que las otras dos. Formatea como moneda solo la columna 7 (TOTAL); alinea: col 0 y 1 centro, columnas 2 a 6 izquierda, col 7 derecha.

Ninguna de las tres aplica formato de moneda a más de una o dos columnas por tabla; el resto de columnas numéricas ocultas (usadas solo como datos de control: flags `SUM`/`RENUENTE`, ids internos) no llevan renderer de moneda porque no son visibles.
