# Comportamiento · Toma de comanda

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/ComandaMovil.html` |
| Web | `design/pantallas/ComandaWeb.html` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| Tablas | `comandas.comandas` · `comanda_lineas` · `comanda_linea_modificadores` · `menu.items_menu` · `modificadores` |
| Historias | HU-078 (Modificadores con mínimos y máximos) · HU-085 (Abrir comanda y agregar líneas mientras el servicio avanza) · HU-086 (Ciclo de vida propio de cada línea) · HU-091 (Toma de comanda desde el celular del mesero) |

Aquí está la diferencia real con una venta: cada línea tiene su propio ciclo de vida, y anular una ya enviada a cocina genera merma.

> **Alcance de HU-085.** La cabecera + las líneas + los totales + «Añadir» + «Enviar a
> cocina». El avance por línea (ENVIADA → EN_PREPARACION → LISTA → ENTREGADA) es HU-086;
> anular una línea es HU-087; dividir la cuenta y cobrar son HU-088/090.

## Reglas

### R1 · La comanda se abre sobre una sesión de mesa, y solo una a la vez
**Dado** una sesión de mesa sin comanda, **cuando** se abre una (`POST /api/comandas`),
**entonces** recibe un número consecutivo por negocio (`CMD-0001`, `CMD-0002`…) y queda
`ABIERTA`. **Dado** una sesión que ya tiene una comanda no cerrada, **cuando** intento abrir
otra, **entonces** responde 409 «Esa sesión ya tiene una comanda abierta». En la base lo
respalda `uq_comanda_sesion_abierta`.

### R2 · Cada línea guarda copia del nombre y el precio del ítem
**Dado** que agrego un ítem, **cuando** se crea la línea, **entonces** guarda el
`nombre_snapshot` y el `precio_unitario` del ítem en ese momento. Si mañana cambia el precio
en la carta, la línea ya cobrada no se mueve.

### R3 · Los totales se recalculan en cada adición y se persisten
**Dado** cualquier cambio de líneas (agregar, cambiar cantidad), **cuando** ocurre,
**entonces** el subtotal, el impuesto, el costo y el total de la comanda se recalculan desde
las líneas vivas y se guardan. La propina sugerida es el 10 % del subtotal. Al recargar la
pantalla se ve el mismo total.

### R4 · A una comanda en cocina se le siguen agregando líneas
**Dado** una comanda `EN_COCINA`, **cuando** agrego más ítems, **entonces** se permite y las
líneas nuevas quedan `PENDIENTE` (no heredan el estado de las que ya se enviaron). Solo una
comanda `CERRADA` o `ANULADA` deja de admitir líneas (409).

### R5 · Un ítem con modificadores obligatorios exige elegirlos
**Dado** un ítem con un grupo de modificadores de mínimo 1, **cuando** lo agrego sin elegir
ninguno, **entonces** el backend responde 422 y la hoja de «Añadir» no se cierra: muestra
«Faltan modificadores obligatorios del ítem». Pasarse del máximo de un grupo también se
rechaza. La validación la hace `servicio-menu` (HU-078); `servicio-comandas` la consulta.

### R6 · «Enviar a cocina» pasa las líneas pendientes a ENVIADA
**Dado** una comanda con líneas `PENDIENTE`, **cuando** pulso «Enviar a cocina»,
**entonces** esas líneas pasan a `ENVIADA` con su marca de tiempo, la comanda pasa a
`EN_COCINA` (la primera vez) y se publica `comanda_enviada_cocina`. Las líneas que ya se
habían enviado no se tocan.

## Al abrir

- Se llama `GET /api/comandas/{id}` una vez. Mientras responde, un spinner centrado.
- El foco no va a ningún campo; el campo «Código» del ítem toma el foco al abrir «Añadir».
- El botón «Añadir» solo está activo si la pantalla recibió la carta del menú (`cartaId`).

## Validaciones

- **Cantidad** (al añadir): número > 0. Vacío o ≤ 0 → «Elige un ítem y una cantidad válida»
  (cliente), no se envía.
- **Ítem**: obligatorio elegir uno; los agotados salen deshabilitados en la lista.
- **Modificadores**: los mínimos y máximos los valida el backend (422, R5).
- **Notas**: ≤ 200 caracteres, opcional.
- Se valida al enviar, no al escribir.

## Estados vacíos y de error

- **Comanda sin líneas**: «Todavía no hay líneas. Pulsa «Añadir».».
- **Error de red al cargar**: «No se pudo cargar la comanda» con «Reintentar».
- **422 al añadir**: mensaje dentro de la hoja, la hoja sigue abierta.
- **Error al enviar a cocina**: aviso en la franja superior; la comanda no cambia.

## Sin conexión

- HU-085 no encola: sin red, la comanda no carga y se ofrece «Reintentar». La toma offline
  con cola de salida es HU-091.

## Móvil y web

- **Un solo widget** que se adapta con `LayoutBuilder` en `kBreakpointEscritorio` (900 px).
- Móvil: la lista de líneas ocupa la pantalla, la barra de total + acciones va fija abajo.
- Web: el mismo layout, centrado y con ancho máximo.

## Permisos

- `COMANDAS_COMANDA_VER`: ve la comanda. Sin él, la pantalla no abre.
- `COMANDAS_COMANDA_CREAR`: abre comandas.
- `COMANDAS_COMANDA_EDITAR`: agrega líneas y envía a cocina.
- `COMANDAS_COMANDA_ANULAR`: anula líneas y la comanda (HU-087).
- Sin el permiso, la acción no se ve.

## Qué NO debe pasar

- Que dos comandas queden abiertas sobre la misma sesión de mesa.
- Que una línea agregada después del envío arrastre el estado `ENVIADA`.
- Que el total mostrado no cuadre con la suma de las líneas vivas.
- Que un cambio de precio en la carta mueva una línea ya tomada.
- Que se pueda agregar un ítem con un grupo obligatorio de modificadores sin resolverlo.
