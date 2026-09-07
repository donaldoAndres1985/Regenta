# Comportamiento · Clientes y cartera

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/ClientesMovil.html` |
| Web | `design/pantallas/ClientesWeb.html` |
| Paquete Flutter | `packages/core` |
| Microservicio | `servicio-clientes` |
| Tablas | `crm.clientes` · `crm.cliente_metricas` · `crm.cuentas_por_cobrar` · `crm.recaudos` |
| Historias | HU-021 (Crear y consultar clientes) · HU-022 (Cupo de crédito y cartera del cliente) · HU-024 (Historial de interacciones con el cliente) · HU-025 (Listado de clientes en móvil con búsqueda y filtros) |

cliente_metricas es una proyección alimentada por los tres eventos de cierre — venta_completada, estancia_finalizada y pedido_completado.

> R1–R6 son el contrato del backend de **HU-021** (`servicio-clientes`, `POST/GET /api/clientes`).
> R7–R10 son **HU-023** (proyección `cliente_metricas` alimentada por eventos).
> R11–R13 son **HU-024** (historial de interacciones y recordatorio de seguimiento).
> La pantalla `ClientesWeb.html` / `ClientesMovil.html` (incluida la ficha con el timeline de
> interacciones) la cablea **HU-025**; el bloque de cartera es **HU-022**.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · El documento identifica al cliente dentro del negocio
**Dado** un cliente ya registrado con un `(tipo_documento, numero_documento)`, **cuando** creo
otro con el mismo par en el mismo negocio, **entonces** el API responde **409**. El mismo par
en otro negocio sí se permite: son universos aparte (RLS por `negocio_id`).

### R2 · Persona jurídica sin razón social
**Dada** una solicitud con `tipo_persona = JURIDICA` y sin `razon_social`, **cuando** la
guardo, **entonces** el API responde **422** con un mensaje que nombra el campo
(«La razón social es obligatoria para una persona jurídica»). Igual para una `NATURAL` con
documento y sin `nombres`.

### R3 · Búsqueda por nombre parcial
**Dado** que busco por nombre, **cuando** escribo menos de 3 caracteres, **entonces** el API
responde 422 y no consulta. **Con** 3 o más, filtra por `nombre_display` o por
`numero_documento` (ambos parciales), acotado al negocio, sin barrer la tabla: sobre 10.000
clientes responde dentro del presupuesto de 300 ms. El índice `ix_clientes_busqueda` es GIN
trigram sobre `nombre_display::text`; hoy el planificador resuelve la búsqueda por
`ix_clientes_negocio` + filtro `ILIKE` (índice, acotado al negocio) porque bajo `FORCE ROW
LEVEL SECURITY` los operadores `LIKE`/`ILIKE` no son `LEAKPROOF`. Marcar
`textlike`/`texticlike` como `LEAKPROOF` (superusuario, una vez por base) engancha además el
índice trigram. **Nunca** un `Seq Scan` de la tabla completa.

### R4 · Consumidor final
**Dada** una solicitud con `tipo_documento = SIN_IDENTIFICAR` (o sin documento), **cuando** la
creo, **entonces** se permite, `numero_documento` queda en NULL, no choca con otros iguales, y
si no se dio ningún nombre el `nombre_display` es «Consumidor final».

### R5 · El alta publica `cliente_creado`
**Dado** un cliente recién creado, **cuando** termina la transacción, **entonces** queda en el
outbox un evento `cliente_creado` con `negocio_id`, `cliente_id`, `tipo_persona`,
`tipo_documento`, `numero_documento` y `nombre_display`. Si la transacción aborta, no se
publica nada (Outbox).

### R6 · Permisos
**Dado** un usuario sin `CLIENTES_CLIENTE_CREAR`, **cuando** intenta crear un cliente,
**entonces** el API responde **403**. Ver y buscar exigen `CLIENTES_CLIENTE_VER`; editar,
`CLIENTES_CLIENTE_EDITAR`.

### R7 · Un cierre suma a las métricas del cliente
**Dado** un evento `venta_completada` con `negocio_id`, `cliente_id`, `total` y `ocurrido_en`,
**cuando** el servicio lo consume, **entonces** en `cliente_metricas` de ese cliente
`total_documentos` sube 1, `monto_total` suma el `total`, `ticket_promedio` se recalcula, y
`primera_compra_en` / `ultima_compra_en` quedan en el mínimo / máximo de las fechas vistas. El
`UPSERT` es atómico (no hay columna `version`): dos eventos a la vez para el mismo cliente no
se pisan. Si el `cliente_id` no viene (venta a consumidor final) o no existe en este negocio,
el evento no crea fila.

> Contrato pendiente en el emisor: hoy `servicio-ventas` publica `venta_completada` y
> `venta_anulada` **sin** `cliente_id` (ni `total` en la anulación). Añadirlos va con **HU-113**
> (asignar cliente a la venta). Mientras tanto la proyección solo se mueve con eventos que ya
> traen `cliente_id`; los patrones Reserva y Comanda aún no emiten `estancia_finalizada` /
> `pedido_completado`.

### R8 · El mismo evento dos veces no cuenta doble
**Dado** un evento ya procesado, **cuando** RabbitMQ lo reentrega con el mismo `message-id`,
**entonces** el Inbox lo descarta y las métricas no cambian. El registro en el Inbox y el
efecto sobre la métrica van en la misma transacción: o quedan los dos, o ninguno.

### R9 · Los tres patrones alimentan la misma proyección
**Dado** un evento `estancia_finalizada` (Reserva) o `pedido_completado` (Comanda) con la
misma forma de payload, **cuando** llega, **entonces** actualiza `cliente_metricas` igual que
`venta_completada`. Una sola tabla para los tres patrones; ninguna consulta cruza a la base de
Ventas, Reservas ni Comandas.

### R10 · Una venta anulada deja de contar
**Dado** un evento `venta_anulada` con `cliente_id` y `total`, **cuando** llega, **entonces**
`total_documentos` baja 1 y `monto_total` resta el `total`, nunca por debajo de cero (si el
`venta_completada` original no se había procesado, no queda métrica negativa).

### R11 · Registrar una interacción
**Dado** un cliente del negocio, **cuando** registro una interacción (`POST
/api/clientes/{id}/interacciones` con `tipo` de `LLAMADA|EMAIL|VISITA|WHATSAPP|NOTA|RECLAMO`),
**entonces** queda con ese tipo, `ocurrido_en` (el que mande o ahora), el `usuario_id` del que
la registró como autor, y el `detalle`. Un `tipo` fuera de la lista es 422; un cliente que no
existe en el negocio es 404. Exige `CLIENTES_CLIENTE_EDITAR`.

### R12 · La ficha muestra el historial de la más reciente a la más antigua
**Dado** un cliente con interacciones, **cuando** pido su historial (`GET
/api/clientes/{id}/interacciones`), **entonces** vienen ordenadas por `ocurrido_en`
descendente. Exige `CLIENTES_CLIENTE_VER`.

### R13 · El seguimiento vencido avisa al responsable
**Dada** una interacción con `seguimiento_en`, **cuando** llega esa fecha y corre el barrido
(`POST /api/clientes/seguimientos/barrido`), **entonces** se publica un evento
`recordatorio_de_seguimiento` con `negocio_id`, `cliente_id`, `interaccion_id`, `responsable_id`
(el `usuario_id` de la interacción), `tipo`, `asunto` y `seguimiento_en`, y la interacción
queda con `seguimiento_notificado_en` — el barrido no la vuelve a avisar. Un seguimiento con
fecha futura no dispara nada. El `@Scheduled` y el barrido multi-tenant se conectan con el rol
privilegiado, igual que el publicador de outbox y el barrido de la saga (E04).

## Al abrir

<!-- Qué se carga y en qué orden, qué campo toma el foco, qué se ve mientras carga, qué se
recuerda de la última vez (filtros, sucursal, orden de la tabla). -->

_Sin definir._

## Validaciones

<!-- Campo por campo: qué se rechaza, con qué mensaje exacto, y cuándo se valida — al
escribir, al salir del campo o al enviar. -->

Al enviar (backend, HU-021):

- `tipo_persona` / `tipo_documento`: si vienen, deben ser un valor del CHECK; si no, 422
  «Valor no válido: …». Por defecto `NATURAL` / `CC`.
- `JURIDICA` exige `razon_social` → 422. `NATURAL` con documento exige `nombres` → 422.
- `(tipo_documento, numero_documento)` repetido en el negocio → 409.
- `SIN_IDENTIFICAR`: se ignora cualquier `numero_documento` y `digito_verificacion` que venga.
- Longitudes: `numero_documento` ≤ 30, `nombres`/`apellidos` ≤ 120, `razon_social` ≤ 200,
  `email` ≤ 150 y con formato, `telefono`/`telefono_alterno` ≤ 30, `segmento` ≤ 40.
- Validación de la pantalla (al salir del campo, mensajes en la UI): _sin definir_ — la fija
  HU-025 contra `ClientesWeb.html`.

## Estados vacíos y de error

<!-- Qué se ve cuando no hay datos todavía, cuando la búsqueda no encuentra nada, y cuando
el servicio responde con error. Los tres son distintos. -->

Backend: la búsqueda sin coincidencias devuelve lista vacía (200), no error. Menos de 3
caracteres es 422. La pantalla (lista vacía / sin resultados / error de servicio, que son tres
estados distintos): _sin definir_ — HU-025.

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

Backend (revalidado siempre, no solo ocultando el botón): `CLIENTES_CLIENTE_VER` para ver y
buscar, `CLIENTES_CLIENTE_CREAR` para crear, `CLIENTES_CLIENTE_EDITAR` para editar. Sin el
permiso, 403. El módulo `CLIENTES` es plan Profesional o superior.

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- **No** un `Seq Scan` de `crm.clientes` completa al buscar por nombre (R3).
- **No** dos clientes con el mismo `(tipo_documento, numero_documento)` en un negocio; sí con
  `SIN_IDENTIFICAR`.
- **No** ver ni tocar clientes de otro `negocio_id`: la RLS lo corta aunque falte el `WHERE`.
- **No** publicar `cliente_creado` si el alta no llegó a `commit`.
- **No** confiar en el chequeo de unicidad de Java como única barrera: el índice único parcial
  `uq_cliente_documento` lo garantiza en la base.
- **No** contar dos veces un cierre reentregado (Inbox por `message-id`), ni dejar
  `cliente_metricas` en negativo tras una anulación.
- **No** calcular `cliente_metricas` con un JOIN a Ventas/Reservas/Comandas: es proyección,
  se alimenta solo de eventos.
- **No** avisar dos veces del mismo seguimiento: `seguimiento_notificado_en` lo marca; el
  índice parcial `ix_interacciones_seguimiento` solo cubre los que faltan.
