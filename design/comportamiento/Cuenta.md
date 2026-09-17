# Comportamiento · Dividir cuenta

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/CuentaMovil.html` |
| Web | `design/pantallas/CuentaWeb.html` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| Tablas | `comandas.cuentas` · `cuenta_lineas` · `pagos_comanda` |
| Historias | HU-089 (Dividir la cuenta entre comensales) · HU-090 (Cerrar la comanda con propina y descuento de insumos) |

cuenta_lineas guarda una proporción, así que un plato compartido se reparte entre dos cuentas. Sin esto, «pagamos por separado» obliga a rehacer la comanda.

> **HU-089** puso crear cuentas, marcar/desmarcar líneas (por ítem o repartidas) y dividir en
> partes iguales. **HU-090** llenó el «Cobrar»: método de pago, propina aparte del total, y lo
> que dispara al cerrarse la última cuenta — `pedido_completado` (que servicio-menu explota
> contra las recetas, HU-079) y `comanda_cerrada` (que servicio-mesas usa para dejar la mesa
> `SUCIA`, HU-082). No hay una pantalla nueva para HU-090: llena el diálogo de cobro de esta
> misma.

## Reglas

### R1 · Marcar una línea en una cuenta no pide una proporción a mano
**Dado** una línea sin marcar, **cuando** la marco en una cuenta (`PUT
/api/comandas/{id}/cuentas/{cuentaId}/lineas/{lineaId}`), **entonces** le queda asignado el
100% de esa línea. No hay un campo de «proporción»: la casilla de la rejilla es la única
interacción. Desmarcarla (`DELETE` la misma ruta) la libera.

### R2 · Una línea marcada en varias cuentas se reparte igual entre todas
**Dado** que una línea ya está marcada en una cuenta, **cuando** la marco también en otra,
**entonces** el reparto se recalcula: cada una de las `N` cuentas que la marcan queda con
`1/N` (dos cuentas → 50%/50%, y la columna «Reparto» de la rejilla muestra «1 / 2»). La
suma de proporciones de esa línea siempre da 100%; lo calcula el backend, no la persona.

### R3 · Partes iguales reparte todo el total, no ítem por ítem
**Dado** una comanda sin cuentas todavía, **cuando** pido dividir en partes iguales entre
`N`, **entonces** se crean `N` cuentas y cada línea viva queda marcada en las `N`, así que
cada cuenta lleva exactamente `1/N` del total. Si la comanda ya tiene cuentas, se rechaza
(409): partes iguales es una operación de arranque, no se mezcla con lo ya dividido por
ítem.

### R4 · Una cuenta pagada no admite que le muevan líneas
**Dado** una cuenta `PAGADA`, **cuando** intento marcarle o desmarcarle una línea,
**entonces** se rechaza (409) y la casilla de esa columna queda deshabilitada en la rejilla.

### R5 · Al pagarse la última cuenta abierta, la comanda se cierra sola
**Dado** que todas las cuentas de la comanda quedan `PAGADA` (o `ANULADA`), **cuando** la
última se cobra (`POST /cuentas/{id}/pago`), **entonces** la comanda pasa a `CERRADA` sin
que nadie la cierre a mano. Mientras quede al menos una cuenta `ABIERTA`, la comanda sigue
como estaba.

### R6 · Una línea sin enviar a cocina bloquea el cierre, y el pago no queda a medias (HU-090 criterio 1)
**Dado** que la comanda tiene una línea todavía `PENDIENTE`, **cuando** el pago de la última
cuenta intenta cerrarla, **entonces** se rechaza (409 «Hay líneas sin enviar a cocina: no se
puede cerrar la comanda») y **el pago tampoco se registra**: toda la operación se revierte
junto con el intento de cierre. Enviar esa línea a cocina (aunque sea sola) desbloquea el
cobro.

### R7 · La propina se cobra aparte del total, y el cliente la puede cambiar (HU-090 criterio 2)
**Dado** el 10% del subtotal de la cuenta como propina sugerida, **cuando** cobro
(`POST /cuentas/{id}/pago` con `propina`), **entonces** esa propina —aceptada tal cual o
cambiada por otro número— se suma al total y queda en `pagos_comanda.propina`, aparte del
subtotal y del impuesto: en Colombia es voluntaria y no hace parte de la base gravable.

### R8 · Cobrar exige un método; no hay uno por defecto (HU-090 criterio 5)
**Dado** que cobro una cuenta sin decir el método (`efectivo`, `tarjeta_débito`, …),
**entonces** se rechaza: a diferencia del curso de una línea o el modo de división, no hay
un método razonable al que caer. Cada cobro queda en `pagos_comanda` con su método, lo que
recibió y el cambio si fue en efectivo — eso es lo que hace de caja aquí, no hay un módulo
de Caja aparte en el patrón Comanda.

### R9 · Cerrar publica lo que otros servicios necesitan (HU-090 criterios 3 y 5)
**Dado** el cierre, **cuando** ocurre, **entonces** se publican `pedido_completado`
(`servicio-menu` lo explota contra las recetas y de ahí sale `insumos_consumidos` que
descuenta inventario, HU-079) y `comanda_cerrada` (`servicio-mesas` lo usa para cerrar la
sesión y dejar la mesa `SUCIA`, no `LIBRE`, HU-082). `servicio-comandas` no le habla a esos
servicios directo: solo publica los eventos.

## Al abrir

- Se piden la comanda (`GET /api/comandas/{id}`) y sus cuentas (`GET
  /api/comandas/{id}/cuentas`) en paralelo lógico; hasta que ambas responden, un spinner
  centrado.
- Sin cuentas todavía, la rejilla no se dibuja: solo el aviso «Todavía no hay cuentas.
  Añade una o divide en partes iguales.» y los botones «Añadir cuenta» / «Partes iguales».

## Validaciones

- **Partes iguales**: el número de cuentas debe ser ≥ 2; el diálogo no envía nada por
  debajo de eso (el backend igual lo revalida, criterio de `SolicitudDeDivisionIgual`).
- **Etiqueta de cuenta**: opcional, hasta 40 caracteres; sin ella la cuenta se muestra como
  «Cuenta N» a secas.
- No hay campo de proporción que validar (R1): la única entrada es la casilla.
- **Método de pago** (al cobrar): obligatorio, sin valor por defecto (R8); el diálogo trae
  «Efectivo» preseleccionado, pero igual viaja explícito.
- **Propina** (al cobrar): opcional; sin ella se cobra 0, no el 10% sugerido — el campo ya
  viene prellenado con la sugerencia, así que «dejarlo vacío a propósito» es una acción
  aparte de «aceptar la sugerencia».
- **Monto recibido y referencia** (al cobrar): opcionales; monto recibido solo tiene sentido
  en efectivo (el diálogo lo oculta con otros métodos).

## Estados vacíos y de error

- **Sin cuentas**: mensaje de invitación (ver «Al abrir»), no un error.
- **Error de red al cargar**: «No se pudo cargar la comanda» con «Reintentar».
- **409 al marcar/desmarcar/cobrar**: aviso en la franja superior; la rejilla y las
  tarjetas se quedan como estaban (no se optimista-actualizan sobre un rechazo).
- **409 al cobrar por líneas sin enviar (R6)**: el mismo aviso; el diálogo de cobro ya se
  cerró (se cerró al confirmar), así que la persona ve la cuenta seguir `ABIERTA` y el
  mensaje explicando por qué.

## Sin conexión

- Fuera de alcance de HU-089: sin red, la pantalla no carga y ofrece «Reintentar». No hay
  cola de acciones offline para dividir ni cobrar.

## Móvil y web

- **Un solo widget** que se adapta con `LayoutBuilder` en `kBreakpointEscritorio`.
- **Escritorio**: la rejilla de casillas (una columna por cuenta, una fila por línea) más
  «Añadir cuenta», con las tarjetas de cuenta y el resumen «Falta por cobrar» a la derecha
  — como `CuentaWeb.html`.
- **Móvil**: sin rejilla (no cabe una casilla por cuenta de pie, con una mano — sigue
  `CuentaMovil.html` literal, que tampoco la trae). Solo las tarjetas de cuenta ya armadas
  y una barra fija abajo con «Falta por cobrar» + «Cobrar cuenta N» para la primera
  pendiente. Para asignar ítem por ítem en el celular hay que pasar a escritorio o usar
  «Partes iguales», que no depende de la rejilla.
- El chip «Monto fijo» se ve pero no hace nada todavía: `MONTO_FIJO` existe como valor de
  `modo_division` en la base, pero ningún endpoint de HU-089 ni de HU-090 lo implementa.
- El diálogo de cobro (HU-090) es el mismo en móvil y en escritorio: un `AlertDialog`
  centrado, no una hoja inferior — son pocos campos y se llenan sentado en la caja, no de
  pie con una mano.

## Permisos

- `COMANDAS_COMANDA_VER`: abre la pantalla y ve cuentas y rejilla. Sin él, no abre.
- `COMANDAS_COMANDA_EDITAR`: añadir cuenta, marcar/desmarcar líneas, partes iguales y
  cobrar. No hay un permiso propio de «cuentas»: reutiliza los de Comandas (HU-085/086),
  igual que hizo el KDS (HU-088) con los suyos.

## Qué NO debe pasar

- Que la suma de lo que llevan las cuentas de una línea compartida no dé el 100%.
- Que una cuenta ya pagada cambie de líneas o de total después de cobrada.
- Que la comanda se cierre con una cuenta todavía `ABIERTA`.
- Que «partes iguales» se pueda aplicar dos veces, o encima de una división por ítem ya
  empezada, duplicando el reparto de una línea.
- Que se pueda dividir en partes iguales una comanda sin líneas vivas.
- Que la comanda se cierre con una línea `PENDIENTE` sin enviar a cocina (R6).
- Que un pago quede registrado si el cierre que disparó terminó rechazado (R6: todo o nada).
- Que la propina se mueva después de que la cuenta ya está `PAGADA`.
- Que `pedido_completado` o `comanda_cerrada` se publiquen sin que la comanda haya quedado
  `CERRADA` de verdad.
