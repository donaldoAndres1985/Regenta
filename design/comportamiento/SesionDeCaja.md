# Comportamiento · Sesión de caja

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Transversal (Venta directa, Reserva, Comanda) |
| Móvil / Web | `design/pantallas/CobroMovil.html` · `design/pantallas/CobroWeb.html` |
| Paquete Flutter | `packages/ventas` (pantalla pendiente) |
| Microservicio | `servicio-caja` |
| Tablas | `caja.cajas` · `sesiones_caja` · `movimientos_caja` · `caja.consecutivos` |
| Historias | HU-059 (Abrir y cerrar sesión de caja) · HU-060 (Movimientos desde los tres patrones) |

Apertura, arqueo y cierre. Caja no vive dentro de Ventas: en Comanda también recibe pagos de
comandas y en Reserva anticipos. Plan Empresarial.

## Reglas

### R1 · Una sola sesión abierta por caja
**Dado** una caja con una sesión `ABIERTA`, **cuando** intento abrir otra (`POST
/api/caja/sesiones` con `cajaId`, `montoApertura`), **entonces** responde **409** (criterio 1).
La garantía vive en la base: el índice parcial `uq_sesion_caja_abierta` (`caja_id WHERE estado
= 'ABIERTA'`) rechaza el segundo `INSERT` aunque el chequeo de la app falle.

### R2 · La base entra como primer movimiento
**Dado** la apertura con `montoApertura > 0`, **cuando** se abre la sesión, **entonces** se
registra un `movimientos_caja` con `tipo = APERTURA`, `signo = 1`, `metodo_pago = EFECTIVO` y
ese monto (criterio 2). El `numero` de la sesión es `CAJA-N`, corrido por negocio
(`caja.consecutivos`, sin huecos).

### R3 · El cierre calcula la diferencia, no la declara la app
**Dado** una sesión `ABIERTA`, **cuando** la cierro (`POST /api/caja/sesiones/{id}/cierre` con
`montoDeclarado`), **entonces** `monto_esperado` = suma firmada de los movimientos en efectivo
de la sesión, `monto_declarado` = lo contado, y `diferencia` la calcula la base (columna
generada `monto_declarado − monto_esperado`) (criterio 3). Cerrar una sesión ya cerrada
responde **409**.

### R4 · Diferencia distinta de cero → DESCUADRADA + evento
**Dado** el cierre con `diferencia ≠ 0`, **cuando** cierro, **entonces** la sesión queda
`DESCUADRADA` y se publica `caja_descuadrada` (`{negocio_id, sesion_id, caja_id, numero,
monto_esperado, monto_declarado, diferencia}`) por el outbox (criterio 4). Con `diferencia = 0`
queda `CUADRADA`.

### R5 · La caja libre admite una nueva sesión
**Dado** una caja cuya última sesión está `CUADRADA`/`DESCUADRADA`, **cuando** abro otra,
**entonces** se crea sin problema (el índice parcial solo bloquea las `ABIERTA`).

### R6 · El cliente sabe si la caja tiene sesión abierta
**Dado** una caja, **cuando** consulto `GET /api/caja/sesiones/activa?cajaId=…`, **entonces**
devuelve la sesión `ABIERTA` o **404**. El cliente lo usa para advertir al cajero que no salga
sin cerrar (criterio 5); la advertencia es de la pantalla.

### R7 · Aislamiento por negocio
**Dado** una sesión o movimiento de un negocio, **cuando** otro negocio consulta, **entonces**
no los ve: la RLS de `sesiones_caja` y `movimientos_caja` lo corta.

### R8 · Permisos
`CAJA_TURNO_VER` para consultar; `CAJA_TURNO_CREAR` para registrar una caja y abrir una sesión;
`CAJA_TURNO_EDITAR` para cerrarla; `CAJA_MOVIMIENTO_VER` para ver los movimientos. Sin el
permiso, **403**. Módulo `CAJA`, plan Empresarial.

### R9 · Todo cobro entra a la caja, venga del patrón que venga (HU-060)
**Dado** un evento de cobro con `sesion_caja_id` y `pagos` (`venta_completada` → origen
`VENTA`, `pedido_completado` → `COMANDA`, `anticipo_reserva_cobrado` → `RESERVA`), **cuando**
lo consume Caja, **entonces** registra un `movimientos_caja` por cada pago (signo `+1`, método
mapeado, `origen_tipo`/`origen_id`/`documento_ref` del evento) en esa sesión (criterios 1-3).
Un cobro mixto genera un movimiento por método. Un cobro sin `sesion_caja_id`, o para una
sesión que no está abierta, no genera nada.

### R10 · El cobro no entra dos veces (HU-060)
**Dado** el mismo evento entregado dos veces, **cuando** llega el duplicado, **entonces** no se
registra otra vez: lo corta el Inbox del consumidor y, si llegara con otro `message-id`, el
`idempotency_key` determinista (`<origen>:<origen_id>:pago:<i>`) contra `uq_mov_caja_idem`
(criterio 4).

### R11 · El efectivo cobrado cuadra el arqueo
**Dado** movimientos en efectivo de la sesión, **cuando** se cierra (R3), **entonces**
`monto_esperado` los suma firmados junto con la base de apertura. Los pagos con tarjeta o
transferencia no cuentan para el efectivo esperado.

## Qué NO debe pasar

- **No** dos sesiones `ABIERTA` de la misma caja — ni por la API ni por un `INSERT` directo.
- **No** una `diferencia` calculada por la app: es columna generada.
- **No** cerrar dos veces la misma sesión.
- **No** un `numero` con huecos: sale de `caja.consecutivos`.
