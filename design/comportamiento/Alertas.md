# Comportamiento · Motor de reglas de alerta

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Core (todos) |
| Paquete Flutter | `core` (centro de alertas: HU-095) |
| Microservicio | `servicio-alertas` |
| Tablas | `alertas.tipos_alerta` · `reglas_alerta` · `alertas` · `entregas` |
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

## Qué NO debe pasar

- **No** una regla con un tipo que no está en `tipos_alerta`.
- **No** dos reglas con el mismo `(tipo, nombre)` en un negocio.
- **No** dos alertas con la misma `huella` en un negocio — ni por carrera: el motor toma la
  fila con `SELECT … FOR UPDATE` antes de decidir.
- **No** generar nada desde una regla desactivada.
- **No** condiciones en el código: un tipo de negocio nuevo se resuelve con filas en
  `reglas_alerta`.
