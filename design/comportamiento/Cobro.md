# Comportamiento · Cobro y caja

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/CobroMovil.html` |
| Web | `design/pantallas/CobroWeb.html` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-caja` |
| Tablas | `pagos_venta` · `caja.sesiones_caja` · `movimientos_caja` · `arqueo_denominaciones` |
| Historias | HU-039 (Registrar el pago de una venta, incluso mixto) · HU-059 (Abrir y cerrar sesión de caja) · HU-062 (Arqueo por denominaciones) |

Caja es transversal a los tres patrones: también recibe pagos de comandas y anticipos de reservas. Por eso no vive dentro de Ventas.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · El adquiriente se ve antes de confirmar
**Dado** el cobro de una venta, **cuando** la factura electrónica está activada, **entonces** el
panel muestra a nombre de quién sale: nombre, documento, régimen y el correo al que se envía.
Nadie debería enterarse de que facturó mal después de emitir.

### R2 · Sin cliente se avisa, no se bloquea
**Dada** una venta sin cliente, **cuando** llego al cobro, **entonces** aparece la fila
*Consumidor final · Agregar cliente* y la advertencia de que el adquiriente queda genérico. El
cobro sigue disponible: se cobra igual.

### R3 · Agregar cliente desde el cobro vuelve al cobro
**Dado** el cobro en curso, **cuando** agrego el cliente, **entonces** vuelvo al cobro con las
formas de pago y los montos ya digitados intactos.

### R4 · El snapshot se congela al emitir
**Dada** una factura emitida, **cuando** después editan el cliente, **entonces** la factura no
cambia: `facturas.cliente_snapshot` guarda los datos del momento de la emisión.


## Al abrir

<!-- Qué se carga y en qué orden, qué campo toma el foco, qué se ve mientras carga, qué se
recuerda de la última vez (filtros, sucursal, orden de la tabla). -->

_Sin definir._

## Validaciones

<!-- Campo por campo: qué se rechaza, con qué mensaje exacto, y cuándo se valida — al
escribir, al salir del campo o al enviar. -->

_Sin definir._

## Estados vacíos y de error

<!-- Qué se ve cuando no hay datos todavía, cuando la búsqueda no encuentra nada, y cuando
el servicio responde con error. Los tres son distintos. -->

_Sin definir._

## Sin conexión

<!-- Qué se puede seguir haciendo, qué se encola para sincronizar después, qué se bloquea, y
cómo se entera la persona de en cuál de los tres está. -->

_Sin definir._

## Móvil y web

<!-- Dónde el comportamiento se separa: atajos de teclado, orden de tabulación, columnas que
se ocultan en móvil, acciones que solo tienen sentido con teclado o solo con el dedo. -->

_Sin definir._

## Permisos

<!-- Qué ve y qué puede hacer cada rol en esta pantalla, y qué pasa exactamente cuando no
tiene el permiso: no se ve, se ve deshabilitado, o falla al intentar. -->

_Sin definir._

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

_Sin definir._
