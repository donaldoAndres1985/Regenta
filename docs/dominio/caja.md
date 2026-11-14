# Contexto: Caja

> Estado: **aprobado** · 2026-11-13 (incluidas I4b, I8, I8b e I9b, confirmadas por el usuario)
> Origen: triage de `FUNCIONALIDADES.md` § "Grupo: Caja e Informes" (Sara POS) contra el
> módulo `com.regenta.caja`. Este documento es la referencia de dominio: las historias de
> `historias/epicas/E07-caja-pos.md` lo citan y no lo repiten. Si una regla cambia, cambia
> primero aquí.

## 1. Propósito

Hacer responsable a una persona del dinero que entra y sale durante un turno, y saber al
cerrarlo, método por método, si lo que se contó coincide con lo que el sistema esperaba.

Caja **no vende ni factura**: registra el dinero que producen otros contextos (ventas,
comandas, reservas, recaudos de cartera) y los movimientos manuales del cajero.

## 2. Lenguaje ubicuo

| Término | Definición | En Sara POS |
|---|---|---|
| **Caja** | Punto físico de cobro de una sucursal (un cajón, un computador). | — (no existía; la caja era el usuario) |
| **Sesión de caja** | Turno de un cajero sobre una caja, desde la apertura con base hasta el cierre. | caja `ABIERTO` + serie `ID` |
| **Base** | Efectivo con el que se abre la sesión. Es el primer movimiento. | — |
| **Movimiento de caja** | Entrada o salida de dinero dentro de una sesión, con un método de pago. **Todo movimiento suma al arqueo.** | fila de `caja` |
| **Movimiento manual** | Ingreso, retiro o gasto que registra el cajero, sin venta detrás. Exige concepto. | Entrada / Salida (`MC<n>`) |
| **Anulación** | Movimiento compensatorio que revierte un movimiento manual. El original no se borra. | `DELETE FROM caja` |
| **Método de pago** | `EFECTIVO`, `TARJETA_DEBITO`, `TARJETA_CREDITO`, `TRANSFERENCIA`, `QR`, `BONO`, `CREDITO`, `OTRO`. | `tipo_pago` |
| **Método cuadrable** | Método cuyo dinero se cuenta físicamente al cerrar. Lo configura cada negocio. | todos menos Débito y Gratis (fijo) |
| **Esperado** | Lo que el sistema calcula que debería haber, por método. | total facturado `TFn` |
| **Contado** | Lo que el cajero declara que contó, por método cuadrable. | `TCn` |
| **Diferencia** | Contado − esperado. **Negativa = falta dinero (faltante); positiva = sobra (sobrante).** Es la convención que ya usa la base (`sesiones_caja.diferencia`); Sara usaba el signo contrario. | `Rn` (signo invertido) |
| **Arqueo** | El conjunto de esperado, contado y diferencia de cada método al cerrar. | `cerrarCajaCalculado` |
| **Cierre** | Acto que termina la sesión con su arqueo. Después la sesión es inmutable. | `UPDATE caja SET ESTADO='CERRADO'` |
| **Tirilla de cierre** | Comprobante térmico del cierre. Se puede reimprimir. | `Recibo_cerrar_caja` |
| **Supervisor** | Usuario con permiso para anular movimientos manuales. | — (no había control) |

Términos que **no existen** en Regenta: "movimiento excluido del arqueo" (`SUM='NO'`),
"serie global de caja".

## 3. Modelo

```
Caja (agregado)                    ConfigDeCaja (agregado, una por negocio)
  id, sucursal, nombre               metodosNoCuadrables: Set<MetodoPago>
                                     exigeEfectivoAlCerrar: true

SesionDeCaja (agregado raíz)
  id, caja, numero (consecutivo por caja), cajero, estado
  base, abiertaEn, cerradaEn
  ├── MovimientoDeCaja (entidad, solo se crea a través de la sesión)
  │     tipo, metodo, monto, signo, origen (VENTA/COMANDA/RESERVA/RECAUDO/MANUAL),
  │     concepto, anulaA?, autorizadoPor?
  └── Arqueo (objeto de valor, se fija al cerrar)
        List<LineaDeArqueo(metodo, cuadrable, esperado, contado, diferencia)>
        ArqueoDenominacion[] (conteo de billetes y monedas → contado de EFECTIVO)
```

El agregado es `SesionDeCaja`: **todo cambio de dinero pasa por un método suyo**, que es
donde viven las invariantes. Ningún servicio crea un `MovimientoDeCaja` por su cuenta.

Comportamiento del agregado (nombres del negocio, sin setters):

| Método | Qué hace |
|---|---|
| `abrir(caja, cajero, base)` | Crea la sesión y su movimiento de base en efectivo |
| `registrarCobro(origen, metodo, monto)` | Movimiento que llega de otro contexto |
| `registrarIngreso / Retiro / Gasto(metodo, monto, concepto)` | Movimiento manual |
| `anular(movimientoId, motivo, supervisor)` | Movimiento compensatorio |
| `esperadoPorMetodo()` | Suma de movimientos por método |
| `cerrar(contados, config, usuario)` | Calcula el arqueo y fija el estado |

## 4. Invariantes

Cada una es un test del dominio (JUnit puro) **y**, cuando la base puede garantizarla, una
restricción en PostgreSQL probada con Testcontainers.

| # | Invariante |
|---|---|
| I1 | Una caja tiene como máximo **una** sesión `ABIERTA`. (También en la base: índice único parcial.) |
| I2 | Solo se registran movimientos en una sesión `ABIERTA`. |
| I3 | **Todo movimiento suma al arqueo.** No hay forma de excluir uno. |
| I4 | Un movimiento manual exige monto > 0 y concepto no vacío. |
| I4b | La diferencia de cada método = contado − esperado, y la calcula la base (columna generada), no la app. |
| I5 | Solo se anulan movimientos **manuales** (ingreso, retiro, gasto), y cada uno una sola vez. |
| I6 | Anular exige permiso `CAJA_MOVIMIENTO_ANULAR` (rol supervisor), motivo no vacío y sesión `ABIERTA`. La anulación es un movimiento de signo contrario que apunta al original; el original no se modifica ni se borra. |
| I7 | El esperado de cada método = suma con signo de sus movimientos (la base cuenta en `EFECTIVO`). |
| I8 | Un método es cuadrable salvo que esté en `ConfigDeCaja.metodosNoCuadrables`. Valor por defecto: `TARJETA_DEBITO`, `TARJETA_CREDITO`, `BONO`, `CREDITO`. **`EFECTIVO` siempre es cuadrable** (se deduce de I10: no se puede exigir contar el efectivo si no se cuadra). |
| I8b | Qué métodos son cuadrables se decide con la configuración vigente **al cerrar**, y queda congelado en el arqueo: cambiar la configuración después no altera cierres pasados. |
| I9 | En un método no cuadrable el contado no se pide: contado = esperado y diferencia = 0. |
| I9b | En un método cuadrable distinto de efectivo, si el cajero no declara contado, cuenta como 0 (como en Sara: no bloquea el cierre, pero deja la diferencia a la vista). |
| I10 | Para cerrar, el efectivo contado debe ser **mayor que 0**. |
| I11 | Al cerrar, la sesión queda `CUADRADA` si todas las diferencias son 0; si no, `DESCUADRADA`. |
| I12 | Una sesión cerrada es inmutable: no admite movimientos, anulaciones ni un nuevo arqueo. |
| I13 | Toda lectura y escritura está aislada por `negocio_id` (RLS). |

## 5. Eventos

| Dirección | Evento | Para qué |
|---|---|---|
| Consume | `venta_completada`, `pedido_completado`, `anticipo_reserva_cobrado`, `estancia_finalizada` | Registrar el cobro en la sesión abierta |
| Consume | `aporte_venta_acumulada_registrado` *(nuevo)* | Registrar en efectivo cada aporte a la venta acumulada. La emisión de la venta acumulada **no** genera otro cobro |
| Publica | `caja_descuadrada` | Alertar cuando el cierre no cuadra (ya existe) |
| Publica | `caja_cerrada` *(nuevo)* | Reportes, auditoría, y que Ventas emita la venta acumulada abierta de la sesión |
| Publica | `movimiento_de_caja_anulado` *(nuevo)* | Auditoría: quién anuló, qué y por qué |

## 6. Permisos

| Permiso | Quién | Para qué |
|---|---|---|
| `CAJA_TURNO_VER` / `_CREAR` / `_EDITAR` | Cajero | Ver, abrir y cerrar su sesión; arqueo |
| `CAJA_MOVIMIENTO_VER` / `_CREAR` | Cajero | Ver y registrar movimientos manuales |
| `CAJA_MOVIMIENTO_ANULAR` *(nuevo)* | Supervisor | Anular un movimiento manual |

## 7. Pantallas de Sara que alimentan este contexto

| Funcionalidad | Decisión |
|---|---|
| Entrada / salida manual | Adoptar (ya existe: `/ingresos`, `/retiros`) |
| Movimientos y totales por método de la sesión abierta | Adoptar |
| Arqueo por método con diferencia | **Adoptar** — hoy `cerrar()` compara un solo total |
| Métodos que no se cuadran | **Adaptar** — configurable por negocio (I8) |
| Efectivo contado obligatorio | Adoptar, como Sara (I10) |
| Calculadora de denominaciones | Adoptar (ya existe) |
| Eliminar entrada/salida | **Adaptar** — anulación compensatoria por supervisor (I5, I6) |
| Tirilla de cierre y reimpresión | Adoptar |
| Detalle de un movimiento | Adoptar, con nombre del movimiento y todos sus métodos |
| Buscar cierres por fecha y usuario | Adoptar, con filtros y paginación (sin tope de 30) |
| Movimientos de un cierre con total | Adoptar |
| Resumen del cierre (ventas/compras contado y crédito, entradas, salidas) | Adoptar |
| Informes PDF (general, detallado, cierres por rango) | Adoptar después, HU aparte |

**Descartado:** movimiento excluido del arqueo y sus atajos ocultos; serie global entre
usuarios; fecha de apertura tomada de otras cajas; fecha de cierre de un campo invisible;
paneles y botones sin función; `Ctrl+Alt+R`; buscar una caja por fecha exacta de apertura;
total "DEBITO" que mezcla Gratis y Débito; cerrar "todo lo abierto del usuario".

## 8. Pendientes

- ~~Venta acumulada~~ — resuelto: vive en `docs/dominio/ventas.md`. Caja recibe cada aporte
  en efectivo y publica `caja_cerrada` para que Ventas la emita. Caja no conoce sus reglas.
- Abono a proveedor: ¿sale de la caja o de otra cuenta?
