# Comportamiento · Cuentas por pagar

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Venta directa (Compras) |
| Paquete Flutter | `packages/compras` (pendiente) |
| Microservicio | `servicio-compras` |
| Tablas | `compras.cuentas_por_pagar` · `pagos_proveedor` · `recepciones` |
| Historias | HU-049 (Cuentas por pagar y pagos a proveedores) |

La cuenta nace de una recepción confirmada (HU-048). HU-048 ya la abre cuando la recepción
trae la factura al confirmarse; HU-049 agrega registrarla después, los pagos y el listado.

## Reglas

### R1 · La factura del proveedor abre la cuenta
**Dado** una recepción `CONFIRMADA` sin cuenta todavía, **cuando** registro su factura (`POST
/api/compras/cuentas-por-pagar` con `recepcionId` y `numeroFactura`), **entonces** se crea la
cuenta con `monto` = `saldo` = total de la recepción, `fecha_emision` = hoy, `fecha_vencimiento`
= `fecha_emision + dias_credito` del proveedor y estado `PENDIENTE` (criterio 1). Registrar la
factura de una recepción que no está confirmada, o que ya tiene cuenta, responde **409**.

### R2 · El pago baja el saldo y mueve el estado
**Dado** una cuenta con saldo, **cuando** registro un pago (`POST
/api/compras/cuentas-por-pagar/{id}/pagos` con `monto`, `metodo`, `referencia`), **entonces**
el `saldo` baja en ese monto; si queda algo, la cuenta pasa a `PARCIAL`; si se salda, a
`PAGADA` (criterio 2). Un pago mayor que el saldo responde **422**; pagar una cuenta ya
`PAGADA` o `ANULADA` responde **409**. El `metodo` es uno de `EFECTIVO`, `TRANSFERENCIA`,
`CHEQUE`, `TARJETA`, `OTRO`; otro valor es **422**. El CHECK `ck_saldo_cxp` (`0 ≤ saldo ≤
monto`) lo respalda en la base.

### R3 · El listado va por antigüedad y marca las vencidas
**Dado** varias cuentas, **cuando** consulto el listado (`GET
/api/compras/cuentas-por-pagar`), **entonces** vienen ordenadas por `fecha_vencimiento`
ascendente (la más vieja primero) y cada una trae `vencida` y `diasDeMora`: `vencida` es
`true` cuando tiene saldo y el vencimiento ya pasó (criterio 3). Con `?soloVencidas=true` solo
vienen esas.

### R4 · Una factura por proveedor
**Dado** una cuenta con `(proveedor, numero_factura)`, **cuando** registro otra con el mismo
par en el mismo negocio, **entonces** responde **409** (índice `uq_cxp`). El mismo número para
otro proveedor —o en otro negocio— sí entra.

### R5 · Aislamiento por negocio
**Dado** una cuenta o un pago de un negocio, **cuando** otro negocio consulta, **entonces** no
los ve: la RLS de `cuentas_por_pagar` y `pagos_proveedor` lo corta aunque falte el `WHERE`.

### R6 · Permisos
`COMPRAS_COMPRA_VER` para el listado y el detalle; `COMPRAS_COMPRA_CREAR` para registrar la
factura y los pagos. Sin el permiso, **403**. Módulo `COMPRAS`, plan Profesional o superior.

## Qué NO debe pasar

- **No** una cuenta con `saldo` fuera de `[0, monto]` — lo garantiza `ck_saldo_cxp`.
- **No** dos cuentas con el mismo `(proveedor, numero_factura)` en un negocio.
- **No** facturar dos veces la misma recepción.
- **No** pagar de más ni pagar una cuenta ya saldada.
