# Comportamiento · Nueva venta / POS

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/POSMovil.html` |
| Web | `design/pantallas/POSWeb.html` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-ventas` |
| Tablas | `ventas.ventas` · `venta_lineas` · `productos` · `existencias` · `reservas_stock` · `sagas` |
| Historias | HU-037 (Crear una venta en borrador con sus líneas) · HU-045 (Pantalla de POS en móvil y en web) |

> R1–R4 describen el bloque de cliente completo; su cableado al selector `ClienteVenta`
> llega con HU-113/HU-114 (dependen de E02). HU-045 deja la fila punteada *Consumidor
> final · Agregar cliente* y el callback `onAgregarCliente`, sin selector todavía.
> R5–R10 son lo que HU-045 sí implementa: búsqueda, escaneo, aviso de stock y las dos
> ramas del layout.

Al confirmar no se descuenta stock de una: se pide reserva a Inventario y se espera respuesta. Si no alcanza, la venta vuelve a borrador.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · La venta arranca sin cliente
**Dada** una venta nueva, **cuando** se crea, **entonces** `cliente_id` queda en NULL y la fila
punteada muestra *Consumidor final · Agregar cliente*. No hay paso obligatorio de cliente: en
mostrador la mayoría de las ventas no lo tienen.

### R2 · La fila de cliente lleva al selector
**Dada** la fila de cliente, **cuando** la toco, **entonces** abre `ClienteVenta` sin perder el
carrito ni el borrador de la venta.

### R3 · Con cliente asignado se ve quién compra
**Dada** una venta con cliente, **cuando** miro el panel, **entonces** veo nombre, documento y
su condición de crédito, con *Cambiar* y *Quitar* a la vista. La composición web muestra este
estado; la móvil muestra el estado sin cliente.

### R4 · El crédito exige cliente
**Dada** una venta sin cliente, **cuando** intento pagar a crédito, **entonces** se pide asignar
el cliente primero: el cupo y la cartera son de alguien.

### R5 · La búsqueda arranca en el tercer carácter
**Dado** el campo *Buscar o escanear*, **cuando** escribo menos de 3 caracteres, **entonces** no
se llama al backend y la lista de resultados queda vacía. **Con** 3 o más, se consulta
`/api/inventario/productos/buscar?q=…` con un rebote de 250 ms: sólo la última pulsación de una
ráfaga dispara la llamada.

### R6 · Agregar un producto lo lleva a la venta y deja la búsqueda lista
**Dado** un resultado de búsqueda, **cuando** lo toco (o lo elijo con Enter), **entonces** se
suma como línea con cantidad 1, el término y la lista de resultados se limpian y el foco
**se queda en el buscador**, listo para el siguiente producto. Si el mismo producto ya estaba,
**entonces** no se duplica la línea: se le suma 1 a la cantidad.

### R7 · El mismo buscador lee códigos de barras
No hay botón de escanear: el buscador recibe el código. Los lectores USB/Bluetooth escriben
el código y mandan Enter, así que el camino es el mismo que el de una persona.
**Dado** un texto de 6 o más dígitos, **cuando** pulso Enter, **entonces** se resuelve como
código exacto contra `/api/inventario/productos/codigo/{codigo}` (código propio o alterno) y,
si no, contra la referencia (SKU) o el código exactos entre los resultados de
`/api/inventario/productos/buscar`. Si hay coincidencia exacta, se agrega directo sin abrir el
desplegable. Si no, aviso *No se encontró «texto»* y no se agrega nada.

### R7b · Enter agrega sin tocar el mouse
**Dado** un texto que no es un código, **cuando** pulso Enter, **entonces** se agrega el
producto resaltado del desplegable: el primero, salvo que me haya movido con ↑ ↓. Si la
búsqueda todavía no había salido (Enter antes del rebote), se busca en ese momento y se agrega
el primero. Sin resultados, aviso *No se encontró «texto»* y no se agrega nada.

### R7c · Atajos de teclado
`F2` lleva el foco al buscador, `F12` cobra (si se puede cobrar) y `Esc` cierra el desplegable
sin borrar lo escrito.

### R8 · Un producto sin stock se agrega igual y bloquea el cobro
**Dado** un producto con nivel de stock CERO, **cuando** lo agrego, **entonces** la línea entra
igual con el fondo `critSoft` y el subtítulo *SKU · sin stock*, aparece el aviso
*Hay líneas sin stock* sobre el botón, y *Cobrar* queda deshabilitado. **Cuando** quito esa
línea (o bajo su cantidad a 0), **entonces** el aviso desaparece y *Cobrar* se rehabilita si
queda al menos una línea.

### R9 · Buscador, stepper y Cobrar cumplen el objetivo de toque de 44 px
**Dado** el POS en móvil, **cuando** mido el buscador, los botones `+` / `−` de cada línea y
*Cobrar*, **entonces** cada uno mide al menos 44 px (`RegentaSpacing.hitTarget`) de lado. Se opera
de pie y con una mano.

### R10 · Cliente, buscador y venta, de arriba hacia abajo
**Dado** el mismo widget `PantallaPos`, en cualquier ancho, **entonces** se ve de arriba hacia
abajo: la franja de cliente, el buscador a todo el ancho y la venta a todo el ancho
(`Key('pos-carrito')`). No hay columna lateral. **Cuando** el ancho es 900 px o más
(`Key('pos-escritorio')`), la venta es una tabla —código, descripción, cantidad, precio, IVA,
total, quitar— y debajo del listado, dentro de la misma tarjeta, van *Descuento*, *Nota* y
*Poner en espera* a la izquierda y subtotal, IVA, total y *Cobrar* a la derecha. **Cuando** es
menor (`Key('pos-movil')`), cada línea es una tarjeta y el total con *Cobrar* queda fijo abajo.

### R10a · La franja de cliente
**Sin cliente:** ícono, *CLIENTE*, *Consumidor final* y el botón *Agregar cliente*. **Con
cliente:** iniciales, nombre, documento y condición de crédito, con *Cambiar cliente* y
*Quitar*. *Agregar*/*Cambiar* abre el selector de cliente (HU-113/114), que busca y permite
crear uno rápido; lo elegido queda en la venta activa.

### R10b · Los resultados se despliegan sobre la venta
**Dado** el POS, **cuando** no hay búsqueda activa, **entonces** no se ve ninguna lista de
resultados. **Cuando** escribo 3 o más caracteres, **entonces** los resultados
(`Key('pos-resultados')`) se despliegan bajo el buscador, encima de la venta, con código,
nombre, precio y existencia. **Cuando** agrego un producto, borro la búsqueda o pulso `Esc`,
**entonces** la lista se cierra.

### R11 · Cobrar abre la pantalla de Cobro; ahí se arma la venta de verdad
**Dado** un carrito con al menos una línea y sin líneas sin stock, **cuando** toco *Cobrar* (o
`F12`), **entonces** se abre `PantallaCobro` con el carrito ya congelado (líneas, cliente, nota,
descuento) — ver `design/comportamiento/Cobro.md` R5-R10 para el método de pago, el pago mixto,
la venta a crédito y el camino sin conexión. Al terminar con éxito, la pestaña queda vacía, se
avisa *Venta {número} cobrada* y se dispara `onVentaCobrada`, igual que antes.

Esto reemplaza el camino anterior (una sola llamada a `/api/ventas/offline` sin pedir método de
pago): ese llamado sigue existiendo tal cual —y se sigue usando— pero solo como red de seguridad
cuando no hay conexión (R10 de `Cobro.md`), no como el camino normal con señal.

### R12 · Varias ventas a la vez
**Dado** el POS, **cuando** se abre, **entonces** arriba a la derecha hay tres pestañas *1*, *2*
y *3* (`Key('venta-1')`…) y un *+* (`Key('venta-nueva')`) que abre otra (hasta 9); está activa
la venta 1. *Poner en espera* deja la venta activa guardada y pasa a una en blanco. **Cuando** tengo productos en la venta 1 y
toco *2*, **entonces** veo una venta en blanco, con su propio carrito y su propio cliente.
**Cuando** vuelvo a *1*, **entonces** está tal como la dejé. Cobrar o vaciar una venta sólo
afecta a esa venta. Un botón cuya venta tiene productos muestra cuántos lleva, para que no se
olvide una venta a medias. El término de búsqueda no viaja entre ventas.

Las ventas en paralelo viven en memoria: recargar la pantalla las vacía todas, igual que hoy
pasa con la única venta (ver *Al abrir · pendiente*).

### R13 · Venta rápida: efectivo exacto sin pasar por Cobro
**Dado** un carrito que ya puede cobrarse (sin líneas sin stock, y con el comprador
identificado si HU-137 lo exige), **cuando** se pulsa el botón *Efectivo exacto* (junto a
*Descuento*/*Nota*/*Poner en espera*) o `Shift+F12`, **entonces** se arma la venta por el mismo
flujo real por pasos de R11 pero sin abrir `PantallaCobro`: se crea el borrador, se mandan las
líneas, se confirma y, si la saga de HU-038 la deja `CONFIRMADA`, se registra un único pago en
`EFECTIVO` por el total exacto —sin pedir monto recibido, así que nunca hay cambio— y se cierra.
La pestaña queda vacía y se avisa *⚡ Venta {número} cobrada en efectivo exacto*, disparando
`onVentaCobrada` igual que R11.

**Cuando** el carrito no puede cobrarse todavía, **entonces** se avisa el motivo exacto que
daría *Cobrar* (líneas sin stock, o el aviso de HU-137 si falta identificar al comprador) y no
se intenta nada contra el backend. **Cuando** la saga no confirma por falta de stock,
**entonces** se avisa *No hay stock suficiente para completar la venta* y no se registra pago ni
se cierra nada: el carrito sigue intacto, igual que R7 de `Cobro.md`. **Sin conexión**, el primer
paso falla y cae al mismo camino `/api/ventas/offline` que usa *Cobrar* (R10 de `Cobro.md`).

## Al abrir

<!-- Qué se carga y en qué orden, qué campo toma el foco, qué se ve mientras carga, qué se
recuerda de la última vez (filtros, sucursal, orden de la tabla). -->

La pantalla abre con el carrito vacío y el mensaje *Busca o escanea un producto para empezar.*
No hay carga de red al abrir: la primera llamada es la búsqueda cuando la persona escribe. La
bodega de venta la fija el shell (sucursal / caja) vía `bodegaDeVentaProvider`; el POS no la
elige. El campo de búsqueda **no** toma el foco automáticamente en móvil (evita abrir el
teclado encima del carrito); en escritorio, _sin definir_ — resolver al cablear el shell web.

## Al abrir · pendiente

- Qué campo toma el foco en escritorio.
- Si se recuerda un borrador de venta a medio armar entre sesiones. Por ahora no: recargar
  la pantalla vacía el carrito. El backend ya expone `GET /api/ventas/borradores` (los
  borradores abiertos del cajero en su bodega) pensado para esto, pero el POS todavía no lo
  consume: el carrito en construcción solo llega al servidor al tocar *Cobrar*, no mientras se
  arma. Conectar esto de verdad pide además poder borrar una línea ya mandada al servidor
  (`DELETE /api/ventas/{id}/lineas/{linea}`), que tampoco existe hoy.

## Validaciones

<!-- Campo por campo: qué se rechaza, con qué mensaje exacto, y cuándo se valida — al
escribir, al salir del campo o al enviar. -->

_Sin definir._

## Estados vacíos y de error

<!-- Qué se ve cuando no hay datos todavía, cuando la búsqueda no encuentra nada, y cuando
el servicio responde con error. Los tres son distintos. -->

- **Carrito vacío:** texto centrado *Busca o escanea un producto para empezar.*
- **Búsqueda sin resultados** (3+ caracteres, lista vacía): texto centrado
  *Sin resultados para «{término}».*
- **Buscando:** `CircularProgressIndicator` centrado mientras la llamada está en vuelo.
- **Error de red o del backend** (búsqueda, resolución de código o cobro): el mensaje del
  error se muestra en un `SnackBar`; el carrito no se toca. El detalle de reintentos y el
  texto exacto por código HTTP está _sin definir_ — hoy se muestra el mensaje que llega del
  `ClienteHttp`.

## Sin conexión

<!-- Qué se puede seguir haciendo, qué se encola para sincronizar después, qué se bloquea, y
cómo se entera la persona de en cuál de los tres está. -->

_Sin definir._

## Móvil y web

<!-- Dónde el comportamiento se separa: atajos de teclado, orden de tabulación, columnas que
se ocultan en móvil, acciones que solo tienen sentido con teclado o solo con el dedo. -->

- **Corte:** `kBreakpointEscritorio` (900 px) sobre el ancho que da `LayoutBuilder`, no sobre
  el tamaño del dispositivo. Una ventana web angosta usa la rama móvil.
- **Móvil** (`< 900`): cliente y buscador arriba; las líneas como tarjetas; total y *Cobrar*
  fijos abajo. Objetivo de toque de 44 px en buscador, steppers y *Cobrar* (R9).
- **Escritorio** (`>= 900`): cliente, buscador y la venta como tabla a todo el ancho, con los
  totales y *Cobrar* debajo del listado en la misma tarjeta (R10).
- **Escáner:** sin botón de cámara; el código entra por el buscador (R7).
- Atajos: `F2`, `F12`, `Shift+F12` (venta rápida, R13) y `Esc` (R7c). Orden de tabulación:
  _sin definir_.
- No se portaron más atajos del sistema legado (`FUNCIONALIDADES.md`): esa pantalla no tenía
  aceleradores de teclado documentados más allá del teclado numérico en pantalla del login, que
  no aplica aquí.
- La barra de navegación inferior y la tarjeta de *sin conexión* de los mockups son
  presentacionales; las cablea el shell de la app, no esta pantalla.

## Permisos

<!-- Qué ve y qué puede hacer cada rol en esta pantalla, y qué pasa exactamente cuando no
tiene el permiso: no se ve, se ve deshabilitado, o falla al intentar. -->

_Sin definir._

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- **No** cobrar con el carrito vacío ni con líneas sin stock: *Cobrar* está deshabilitado en
  ambos casos (R8).
- **No** dispararse dos veces si se toca *Cobrar* rápido: mientras confirma, el botón queda
  deshabilitado y con texto *Cobrando…* (R11).
- **No** llamar al backend en cada tecla: la búsqueda tiene rebote de 250 ms y mínimo de 3
  caracteres (R5).
- **No** agregar nada cuando Enter no encuentra producto: sólo el aviso (R7, R7b).
- *Descuento* y *Nota* ya viajan de verdad cuando hay conexión (Etapa 0 del cobro real): el
  descuento por línea se manda tal cual, y el descuento general se prorratea entre las líneas
  según su bruto antes de mandarlas (`EstadoDelPos.lineasParaEnviar`), porque el contrato de
  ventas solo tiene descuento por línea. La nota viaja como `notaCliente` al crear el borrador.
  El camino `/api/ventas/offline` (sin conexión) sí manda el descuento por línea desde este
  cambio, pero **no** la nota: ese contrato no tiene ese campo todavía — pendiente si hace falta
  para ventas genuinamente sin señal.
- **No** duplicar una línea al agregar dos veces el mismo producto: se suma la cantidad (R6).
- **No** confiar en el nivel de stock del cliente para autorizar la venta: el descuento real y
  el anti-overbooking los resuelve la saga de confirmación contra Inventario. El aviso de
  *sin stock* del POS es una ayuda visual, no la validación.
- Golden test contra los mockups: pendiente (la CI no corre goldens por la brecha de
  baseline en Linux). Por ahora, tests de estructura de widget.

---

## Fracciones, unidades de medida y detalle de producto (Etapa 5 y 6)

### Regla: Unidad de medida y selector de fracciones adaptativo
- **Dado** un producto en el carrito del POS:
  - Si es por metros (ej. manto, cable, manguera, tubo): su unidad es `MTS` y al pulsar el icono de fracción muestra **exclusivamente** medidas de longitud en metros (1 m, 1/2 m / 50 cm, 1/4 m / 25 cm, 3/4 m / 75 cm, 10 cm, 20 cm, 1.5 m, 2.5 m, etc.), sin mostrar opciones de galón.
  - Si es pintura o líquido (ej. vinilo, esmalte, pintura, cuñete, balde): su unidad es `GL` y muestra opciones de galón y cuñete (1 Cuñete / Balde = 5.0 galones, 1/2 Cuñete = 2.5 gal, 1 Galón = 1.0, 1/2 Galón = 0.5, 1/4 Galón = 0.25, 1/8 Galón = 0.125, 1/16 Galón = 0.0625, 1/32 Galón = 0.03125).
  - Si es cemento o producto en kilogramos: su unidad es `KG` y muestra fracciones de peso (1 kg, 1/2 kg, 1/4 kg, 100 g, 2 kg, 5 kg, 25 kg, 50 kg).
- **Formato de números (truncado de ceros redundantes):**
  - Todo valor numérico entero se muestra sin decimales (ej. `1.0` -> `1`, `5.0` -> `5`).
  - Todo decimal significativo se conserva pero se truncan los ceros sobrantes a la derecha (ej. `0.2500` -> `0.25`, `0.03125` -> `0.03125`, `0.2563` -> `0.2563`, `29.9700` -> `29.97`).
- **Contraste visual garantizado:**
  - En los diálogos modales (detalle de producto y fracciones), los fondos y textos manejan alto contraste: textos en blanco nítido y gris claro (`#CBD5E1`), chips no seleccionados con fondo oscuro y texto claro legible, y chips seleccionados con fondo azul vibrante y borde brillante.
  - El campo de edición manual de cantidad en el stepper muestra fondo claro / visible con borde azul y texto de alto contraste.
- **Foco permanente en el buscador:**
  - Al escoger cliente, quitar cliente, agregar producto, modificar cantidad en stepper, eliminar línea, cerrar modales de detalle o fracción, el foco regresa inmediatamente a la caja de búsqueda del producto.

