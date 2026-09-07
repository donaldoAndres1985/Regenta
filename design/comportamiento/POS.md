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
`/api/inventario/productos/buscar?q=…` con un rebote de 300 ms: sólo la última pulsación de una
ráfaga dispara la llamada.

### R6 · Agregar un producto lo lleva al carrito y limpia la búsqueda
**Dado** un resultado de búsqueda, **cuando** lo toco, **entonces** se suma como línea con
cantidad 1, el término y la lista de resultados se limpian, y el foco vuelve al carrito. Si el
mismo producto ya estaba, **entonces** no se duplica la línea: se le suma 1 a la cantidad.

### R7 · El escáner cae al ingreso manual cuando no hay cámara
**Dado** el botón de escanear, **cuando** lo toco y hay cámara disponible, **entonces** abro el
lector y el código leído resuelve contra `/api/inventario/productos/codigo/{codigo}`. **Cuando**
no hay cámara (web, o permiso negado), **entonces** abro un diálogo *Ingresar código* con un
campo de texto; al confirmar, ese código resuelve por la misma vía. El código que no resuelve a
ningún producto muestra el mensaje de error del backend y no agrega nada.

### R8 · Un producto sin stock se agrega igual y bloquea el cobro
**Dado** un producto con nivel de stock CERO, **cuando** lo agrego, **entonces** la línea entra
igual con el fondo `critSoft` y el subtítulo *SKU · sin stock*, aparece el aviso
*Hay líneas sin stock* sobre el botón, y *Cobrar* queda deshabilitado. **Cuando** quito esa
línea (o bajo su cantidad a 0), **entonces** el aviso desaparece y *Cobrar* se rehabilita si
queda al menos una línea.

### R9 · El stepper y el escáner cumplen el objetivo de toque de 44 px
**Dado** el POS en móvil, **cuando** mido el botón de escanear y los botones `+` / `−` de cada
línea, **entonces** cada uno mide al menos 44 px (`RegentaSpacing.hitTarget`) de lado. Se opera
de pie y con una mano.

### R10 · Una sola pantalla, dos ramas por `LayoutBuilder`
**Dado** el mismo widget `PantallaPos`, **cuando** el ancho disponible es menor a 900 px
(`kBreakpointEscritorio`), **entonces** se muestra la rama móvil (`Key('pos-movil')`): buscador
y escáner arriba, y debajo los resultados o el carrito según haya búsqueda activa. **Cuando** es
900 px o más, **entonces** la rama escritorio (`Key('pos-escritorio')`) muestra la rejilla de
resultados y el carrito a la vez, en dos paneles. No son dos widgets: es un `if` sobre las
restricciones.

### R11 · Cobrar arma la venta de una y la confirma
**Dado** un carrito con al menos una línea y sin líneas sin stock, **cuando** toco *Cobrar*,
**entonces** se crea el borrador (`POST /api/ventas` con la bodega activa), se mandan las líneas
(`POST /api/ventas/{id}/lineas`) y se confirma (`POST /api/ventas/{id}/confirmacion`). Mientras
corre, el botón dice *Cobrando…* y no se puede volver a tocar. Al terminar, el carrito queda
vacío, se avisa *Venta {número} cobrada* y se dispara `onVentaCobrada`.


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
  la pantalla vacía el carrito.

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
- **Móvil** (`< 900`): buscador + escáner fijos arriba; debajo, un solo panel que muestra los
  resultados mientras hay búsqueda y el carrito cuando no. Totales y *Cobrar* fijos abajo.
  Objetivo de toque de 44 px en escáner y steppers (R9).
- **Escritorio** (`>= 900`): dos paneles lado a lado — resultados a la izquierda, carrito +
  totales a la derecha (ancho fijo 380 px). Ambos visibles siempre.
- **Escáner con cámara:** sólo tiene sentido en móvil/Android. En web el botón abre directo el
  diálogo de ingreso manual (R7).
- Atajos de teclado y orden de tabulación en escritorio: _sin definir_.
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
- **No** llamar al backend en cada tecla: la búsqueda tiene rebote de 300 ms y mínimo de 3
  caracteres (R5).
- **No** duplicar una línea al agregar dos veces el mismo producto: se suma la cantidad (R6).
- **No** confiar en el nivel de stock del cliente para autorizar la venta: el descuento real y
  el anti-overbooking los resuelve la saga de confirmación contra Inventario. El aviso de
  *sin stock* del POS es una ayuda visual, no la validación.
- Golden test contra los mockups: pendiente (la CI no corre goldens por la brecha de
  baseline en Linux). Por ahora, tests de estructura de widget.
