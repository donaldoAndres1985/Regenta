# Comportamiento · Motor de reglas de alerta

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Core (todos) |
| Paquete Flutter | `core` (centro de alertas: HU-095) |
| Microservicio | `servicio-alertas` |
| Tablas | `alertas.tipos_alerta` · `reglas_alerta` · `alertas` · `entregas` |
| Pantalla | `design/pantallas/InicioWeb.html` (panel de alertas) |
| Historias | HU-092 (Motor de reglas configurable) · HU-093 (Alertas de inventario) · HU-094 (Entrega) · HU-095 (Centro de alertas) |

Un motor de reglas, no condiciones escritas en el código. "Alertar vencimientos en droguería"
es una fila en `reglas_alerta` con tipo `VENCIMIENTO_LOTE`, no un módulo.

## Reglas

### R1 · Una regla escoge un tipo del catálogo y lo configura
**Dado** el catálogo global `tipos_alerta` (sin `negocio_id`, sin RLS; sembrado por migración),
**cuando** creo una regla (`POST /api/alertas/reglas` con `tipoCodigo`, `nombre`, `condicion`,
`severidad`, `canales`, `destinatariosRoles`, `destinatariosUsuarios`, `frecuencia`,
`silenciarHoras`), **entonces** queda en `reglas_alerta`, activa. Un `tipoCodigo` que no está
en el catálogo responde **422**; un `nombre` repetido para el mismo tipo en el negocio, **409**
(`uq_regla`). Si no mando `severidad`, se toma la del tipo.

### R2 · La condición es JSON declarativo
La condición se evalúa contra los campos del hecho:
`{}` → siempre; `{"campo":"dias_para_vencer","op":"<=","valor":30}` → comparación;
`{"todas":[…]}` → Y; `{"alguna":[…]}` → O. Operadores: `== != < <= > >=` y `contiene` para
texto. Un campo que no viene en el hecho no coincide.

### R3 · Un hecho en rango genera la alerta
**Dado** una regla `VENCIMIENTO_LOTE` con `dias_para_vencer <= 30`, **cuando** el motor evalúa
un hecho de ese tipo con `dias_para_vencer = 20` (`POST /api/alertas/reglas/evaluacion` o, en
HU-093, un evento), **entonces** se genera una fila en `alertas` con el título y el mensaje
renderizados de las plantillas del tipo (marcadores `{clave}` sustituidos por los campos), la
severidad de la regla y la ruta de la app para navegar.

### R4 · Una sola alerta por huella
**Dado** una alerta ya generada por una regla para una entidad, **cuando** la misma condición
se repite y la alerta sigue activa (`NUEVA`/`VISTA`/`EN_CURSO`) **o** está dentro de la ventana
`silenciar_horas`, **entonces** no se crea otra: `uq_alerta_huella` (`negocio_id`, `huella`) lo
impide y el motor devuelve vacío (criterio 3). La `huella` es determinista:
`sha256(regla_id | tipo | entidad_tipo | entidad_id)`.

### R5 · Pasada la ventana y resuelta, se reabre
**Dado** una alerta `RESUELTA`/`DESCARTADA` cuya `generada_en` quedó fuera de `silenciar_horas`
(con `silenciar_horas = 0`, siempre), **cuando** la condición se repite, **entonces** la misma
fila vuelve a `NUEVA` con `generada_en` de ahora — no se duplica.

### R6 · Regla desactivada no genera nada
**Dado** una regla con `activa = false` (`PATCH /api/alertas/reglas/{id}/activa`), **cuando** se
cumple su condición, **entonces** el motor la ignora (criterio 4).

### R7 · La alerta llega a todos los destinatarios
**Dado** una regla con `destinatariosRoles = [GERENTE]` y `destinatariosUsuarios = [u]`,
**cuando** se genera la alerta, **entonces** se crea una `entrega` `PENDIENTE` por cada usuario
resuelto (los del rol, vía el directorio de usuarios, más los explícitos) y por cada canal de
la regla (criterio 5). El envío real —push, correo, reintento con backoff— es HU-094.

### R8 · Aislamiento por negocio
**Dado** una regla o alerta de un negocio, **cuando** otro negocio consulta, **entonces** no la
ve: la RLS de `reglas_alerta`, `alertas` y `entregas` lo corta. `tipos_alerta` es global.

### R9 · Permisos
`ALERTAS_ALERTA_VER` para el catálogo, listar y consultar reglas; `ALERTAS_ALERTA_EDITAR` para
crear, editar, activar/desactivar y disparar una evaluación o el barrido manual. Sin el
permiso, **403**. Módulo `ALERTAS`, plan Profesional o superior.

### R10 · Stock bajo el mínimo genera la alerta (HU-093)
**Dado** una regla `STOCK_MINIMO`, **cuando** llega el evento `stock_bajo_minimo` de
Inventario (`{negocio_id, producto_id, producto_nombre, existencia, minimo, bodega_id,
bodega_nombre}`), **entonces** el consumidor lo pasa por el Inbox y el motor genera la alerta
con `entidad_tipo = Producto`, `ruta_app = /inventario/productos/{id}` y, en `datos`,
`sugerencia_compra_ruta` — así se abre al producto y a la sugerencia de compra (criterio 3). El
mismo evento repetido no genera otra (Inbox + huella).

### R11 · Barrido de lotes por vencer (HU-093)
**Dado** que hay reglas `VENCIMIENTO_LOTE` activas, **cuando** corre el barrido (`POST
/api/alertas/vigilancia/vencimientos`, o el `@Scheduled` diario cuando exista el recorrido
multi-negocio), **entonces** consulta al inventario los lotes dentro de una ventana de 90 días
y publica un `lote_por_vencer` por cada uno. El consumidor de ese evento llama al motor con
tipo `VENCIMIENTO_LOTE`: la condición de cada regla (`dias_para_vencer <= N`) decide cuáles
generan alerta (criterio 2).

### R12 · El stock repuesto resuelve la alerta sola (HU-093)
**Dado** una alerta `STOCK_MINIMO` abierta de un producto, **cuando** llega
`stock_normalizado` (`{negocio_id, producto_id}`), **entonces** todas las alertas activas
(`NUEVA`/`VISTA`/`EN_CURSO`) de ese producto y tipo pasan a `RESUELTA` sin usuario que las
resuelva (criterio 4).

> El lado que emite `stock_bajo_minimo` / `stock_normalizado` y el que corre el job diario de
> lotes (en `servicio-inventario`) quedan como seguimiento, igual que el consumidor de
> `recepcion_registrada`.

### R13 · Push a los dispositivos activos del usuario (HU-094)
**Dado** un usuario con al menos un `dispositivos_push` activo de plataforma `ANDROID`/`IOS`
(`POST /api/alertas/dispositivos` con `tokenFcm`), **cuando** se genera una alerta con canal
`PUSH` para él, **entonces** el despacho llama a la pasarela FCM por cada token y la entrega
queda `ENVIADA` si alguno acepta (criterio 1). Registrar el mismo token otra vez no duplica la
fila. Sin dispositivo activo, la entrega queda `FALLIDA` con el motivo.

### R14 · In-app queda en el centro del usuario (HU-094)
**Dado** una alerta con canal `IN_APP`, **cuando** se genera, **entonces** la entrega queda
`ENTREGADA` (no hay envío externo: la alerta ya está en la base) y aparece en `GET
/api/alertas/mias` del usuario, con las nuevas primero y por severidad (criterio 2).

### R15 · Reintento con backoff (HU-094)
**Dado** una entrega cuyo envío falla, **cuando** ocurre, **entonces** queda `FALLIDA` con el
`error`, `intentos` sube y `proximo_intento` se agenda con espera creciente (2, 4, 8, 16 min).
`POST /api/alertas/entregas/reintento` reprocesa las que ya cumplieron su hora; tras
`MAX_INTENTOS` (5) la entrega se da por perdida (criterio 3). El `@Scheduled` multi-negocio
queda diferido.

### R16 · "No molestar" retiene, salvo CRÍTICA (HU-094)
**Dado** una `preferencias_notificacion` del usuario con franja `no_molestar_desde/hasta`
(`PUT /api/alertas/preferencias`), **cuando** la alerta cae en esa franja y su severidad no es
`CRITICA`, **entonces** la entrega se retiene (`retenida_hasta` = fin de la franja, estado
sigue `PENDIENTE`) y el reintento la toma al terminar; una alerta `CRITICA` se envía igual
(criterio 4). Si la preferencia no acepta el canal, la entrega queda registrada como `FALLIDA`
sin enviarse.

### R17 · Token FCM inválido desactiva el dispositivo (HU-094)
**Dado** que la pasarela responde que el token no está registrado, **cuando** ocurre,
**entonces** ese `dispositivos_push` queda `activo = false` y no se le vuelve a intentar
(criterio 5).

> FCM y SMTP reales, y el cliente Flutter en `core` que registra el token al arrancar, quedan
> como seguimiento (parte del cableado de E16). El `@Scheduled` que recorre todos los negocios
> necesita el rol privilegiado que está diferido en el proyecto.

### R18 · El centro de alertas ordena las pendientes (HU-095)
**Dado** el `CentroDeAlertas` de `packages/core`, **cuando** lo abro (`GET /api/alertas/mias`),
**entonces** veo solo las pendientes (`NUEVA`/`VISTA`/`EN_CURSO`), las nuevas primero y dentro
de cada grupo por severidad (`CRITICA` → `BAJA`) y luego por fecha (criterio 1). Las resueltas
y descartadas no se listan.

### R19 · Resolver saca la alerta de las pendientes (HU-095)
**Dado** una alerta en el centro, **cuando** toco *Resolver* (`POST
/api/alertas/mias/{id}/resolucion`), **entonces** pasa a `RESUELTA` con `resuelta_por` = el
usuario, desaparece de la lista de pendientes (el cliente la quita de una; si el POST falla,
vuelve) (criterio 2). Solo se puede resolver una alerta que le llegó al usuario (tiene una
`entrega` suya); si no, **404**.

### R20 · Tocar la alerta lleva a su entidad (HU-095)
**Dado** una fila del centro, **cuando** la toco (fuera del botón *Resolver*), **entonces** el
widget llama `onAbrirEntidad(alerta)` con su `rutaApp` y su `entidad_tipo`/`entidad_id` — el
shell navega con `go_router` (criterio 3).

### R21 · La campana muestra el indicador con alertas nuevas (HU-095)
**Dado** el `CampanaDeAlertas`, **cuando** hay alguna alerta `NUEVA`, **entonces** muestra un
punto (`Key('campana-indicador')`) (criterio 4). Al abrir el centro, las nuevas del usuario
pasan a `VISTA` (`POST /api/alertas/mias/vistas`) y el punto se apaga; el cliente lo apaga de
una y el backend confirma.

## Qué NO debe pasar

- **No** una regla con un tipo que no está en `tipos_alerta`.
- **No** dos reglas con el mismo `(tipo, nombre)` en un negocio.
- **No** dos alertas con la misma `huella` en un negocio — ni por carrera: el motor toma la
  fila con `SELECT … FOR UPDATE` antes de decidir.
- **No** generar nada desde una regla desactivada.
- **No** condiciones en el código: un tipo de negocio nuevo se resuelve con filas en
  `reglas_alerta`.
