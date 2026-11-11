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

### R5 · Cobrar en el POS congela el carrito y recién ahí arma la venta real
**Dado** el botón *Cobrar* del POS con el carrito ya validado (R8/R11 de `POS.md`), **cuando**
se toca, **entonces** se abre esta pantalla con el total, el cliente y las líneas ya congeladas;
nada se manda al backend todavía. **Cuando** se elige un método que no es `EFECTIVO` ni
`CREDITO`, **entonces** se agrega de una vez un pago por el saldo pendiente (HU-039: "pagar todo
con este método"). `EFECTIVO` pide el monto recibido antes de agregar el pago, para calcular el
cambio. Varios pagos se pueden combinar (pago mixto) hasta cubrir el saldo.

### R6 · Confirmar cobro arma la venta por los pasos reales, no de una sola llamada
**Dado** el pago ya completo (`saldoPendiente == 0`), **cuando** se toca *Confirmar cobro*,
**entonces** se crea el borrador (`POST /api/ventas`), se mandan las líneas
(`POST /{id}/lineas`), se confirma (`POST /{id}/confirmacion`) y se sondea la venta
(`GET /{id}`) hasta que la saga de stock de HU-038 la deje `CONFIRMADA` o la devuelva a
`BORRADOR`. Solo si queda `CONFIRMADA` se registra cada pago (`POST /{id}/pagos`) y se cierra
(`POST /{id}/cierre`). Mientras tanto el botón muestra el paso en curso ("Creando venta…",
"Confirmando disponibilidad…", "Registrando pago…", "Cerrando venta…") y no se puede volver a
tocar.

### R7 · Sin stock, no se cobra nada y no se pierde el carrito
**Dada** la saga de HU-038, **cuando** devuelve la venta a `BORRADOR` por falta de stock,
**entonces** se avisa "No hay stock suficiente para completar la venta" y no se registra ningún
pago ni se cierra nada. El carrito del POS sigue intacto: la persona puede ajustar cantidades y
volver a intentar.

### R8 · Crédito confirma distinto: antes de la saga, no después
**Dado** un cliente ya asignado a la venta, **cuando** se elige *Crédito* en vez de un método de
pago, **entonces** no se arma ningún pago: se llama `POST /{id}/credito` directamente sobre el
borrador (HU-040), que valida cupo y cartera y confirma la venta por su cuenta —no hay
`POST /confirmacion` ni `POST /pagos` separados para este camino—. Sin cliente asignado, el botón
de Crédito no se puede tocar (R4 de `POS.md`).

### R9 · Cartera vencida se rechaza una vez; el reintento autorizado es deliberado
**Dado** un cliente con cartera vencida, **cuando** se intenta la venta a crédito,
**entonces** el backend la rechaza (409) y la pantalla ofrece "Autorizar y vender a crédito
igual". **Cuando** se toca ese botón, **entonces** se reintenta con `autorizado=true`; si quien
cobra no tiene `VENTAS_CREDITO_APROBAR`, el backend lo vuelve a rechazar (403) y eso sí se
muestra como error sin más reintentos automáticos.

### R10 · Sin conexión, no se pide método: se cobra como siempre se cobró sin señal
**Dado** que el primer paso (crear el borrador) falla por falta de red, **entonces** esta
pantalla no muestra ningún error: se cierra sola y quien la abrió debe caer al camino
`POST /api/ventas/offline` existente (HU-043), que no pide método de pago porque asume que la
venta ya se cobró físicamente. Si la red se cae a mitad del flujo (después de crear el borrador
pero antes de cerrar la venta), el borrador queda huérfano en `BORRADOR` en el servidor —no
afecta inventario ni caja— y la persona simplemente reintenta; limpiarlo es trabajo pendiente,
no algo que haya que resolver ahora.


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
