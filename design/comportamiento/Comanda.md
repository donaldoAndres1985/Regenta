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
| HU-091 | `design/pantallas/ComandaMovil.html` — ese mockup es la lista de líneas ya tomadas; la hoja de «Añadir» por categorías con botones grandes que pide HU-091 criterio 1 no tiene mockup propio, se resolvió al implementar (ver R12–R15) |

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

### R7 · Cada línea avanza por su propio ciclo (HU-086)
**Dado** una línea, **cuando** la toco y confirmo, **entonces** avanza al siguiente estado:
`PENDIENTE → ENVIADA → EN_PREPARACION → LISTA → ENTREGADA`. No se puede avanzar una línea
`ANULADA` ni pasar de `ENTREGADA` (422). Cada transacción guarda su marca de tiempo
(`enviada_en`, `lista_en`, `entregada_en`) y publica `linea_estado_cambiado`.

### R8 · La comanda muestra el estado por línea, no uno solo (HU-086 criterio 2)
**Dado** que consulto la comanda, **entonces** cada línea muestra su propio estado en su
propio chip. Dos líneas de la misma comanda pueden estar en estados distintos a la vez.

### R9 · La demora se mide entre ENVIADA y LISTA (HU-086 criterio 3)
**Dado** una línea que pasó por `ENVIADA` y llegó a `LISTA`, **entonces** su `demoraMin` es
la diferencia en minutos entre esas dos marcas, y la pantalla la muestra en la línea.

### R10 · El curso y la secuencia marcan el orden en cocina (HU-086 criterio 4)
**Dado** una línea de curso `POSTRE` con `secuencia_envio` 2, **entonces** la comanda la
lleva así y la pantalla lo indica («postre · va #2»). El curso y la secuencia solo se
ajustan mientras la línea sigue `PENDIENTE`.

### R11 · Cuando todas las líneas quedan entregadas, la comanda pasa a SERVIDA (HU-086)
**Dado** una comanda `EN_COCINA`, **cuando** su última línea viva pasa a `ENTREGADA`,
**entonces** la comanda pasa a `SERVIDA`.

### R12 · La carta se navega por categorías, con botones grandes (HU-091 criterio 1)
**Dado** que abro «Añadir» en el celular, **cuando** la hoja carga, **entonces** veo pestañas
de categoría (las que trae `GET /api/menu/cartas/{id}/menu`) y, debajo, una rejilla de
tarjetas grandes —una por ítem, ≥ `RegentaSpacing.hitTarget` de alto— en vez del selector de
lista de escritorio. Un ítem agotado (`disponible = false`) se ve apagado y no se puede
tocar.

### R13 · Un toque agrega; un ítem con modificadores pide antes de sumar (HU-091 criterios 1 y 2)
**Dado** un ítem sin grupos de modificadores, **cuando** lo toco, **entonces** se agrega de
una vez con cantidad 1, sin abrir nada más («un toque», criterio 1). **Dado** un ítem con al
menos un grupo de modificadores, **cuando** lo toco, **entonces** se abre la hoja de
modificadores —igual que en escritorio— y solo se agrega al confirmarla (criterio 2); un
grupo obligatorio sin elegir la deja abierta con el 422 del backend, como ya hacía HU-085.
Una pulsación larga sobre cualquier ítem abre esa misma hoja aunque no tenga modificadores,
para quien quiera agregar una nota o más de una unidad sin dos viajes.

### R14 · La nota viaja con la línea hasta cocina (HU-091 criterio 3)
**Dado** que escribo una nota (p. ej. «sin cebolla») en la hoja de modificadores o de
pulsación larga, **cuando** agrego la línea, **entonces** la nota queda en
`comanda_lineas.notas` y se ve en la tarjeta KDS de esa línea (HU-088) y en el ticket físico.
Sin conexión (R15), la nota viaja igual dentro de la operación encolada.

### R15 · Sin señal, la toma sigue local y sube sola al reconectar (HU-091 criterio 4)
**Dado** que agrego una línea o envío a cocina sin señal, **cuando** ocurre, **entonces** la
operación se encola (`ClienteHttp` con `encolable: true`, la cola de HU-112) y la pantalla
sigue mostrando la línea agregada —con un marcador local, no el id que asignará el
servidor— y el aviso «Sin señal: la línea se guardó y subirá sola cuando vuelva.». Se puede
seguir agregando líneas encoladas una tras otra. Al reconectar, la cola las sube en orden;
la próxima vez que la pantalla recarga la comanda, ve los ids reales del servidor.

### R16 · Enviar a cocina confirma antes de mandar (HU-091 criterio 5)
**Dado** que toco «Enviar a cocina» (botón ≥ `RegentaSpacing.hitTarget`, del tamaño del
pulgar), **cuando** lo toco, **entonces** aparece un diálogo «¿Enviar a cocina?» con
«Cancelar»/«Enviar»; solo al confirmar se llama al backend (o se encola, R15). Tocar fuera o
«Cancelar» no manda nada.

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

- **Cargar la comanda** (HU-085): no encola. Sin red, la comanda no carga y se ofrece
  «Reintentar» — no hay nada que mostrar todavía.
- **Agregar una línea o enviar a cocina** (HU-091 criterio 4, R15): sí encola. Ver R15.

## Móvil y web

- **Un solo widget** que se adapta con `LayoutBuilder` en `kBreakpointEscritorio` (900 px).
- Móvil: la lista de líneas ocupa la pantalla, la barra de total + acciones va fija abajo.
- Web: el mismo layout, centrado y con ancho máximo.
- **La hoja de «Añadir» sí se separa** (HU-091 R12): en escritorio sigue siendo el selector
  de lista de HU-085 (se elige de una vez con teclado y mouse, no hace falta una rejilla de
  botones grandes); en móvil es la rejilla por categorías de R12–R13. Las dos llaman al
  mismo `ControladorDeComanda.agregarLinea`.

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
- Que «Enviar a cocina» mande la petición sin pasar por el diálogo de confirmación (R16).
- Que una línea encolada sin señal se pierda o se agregue dos veces al reconectar.
- Que un ítem agotado se pueda tocar en la rejilla de la carta (R12).
