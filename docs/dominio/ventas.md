# Contexto: Ventas

> Estado: **borrador para revisión** · 2026-11-13
> Este documento crece por secciones. Por ahora solo contiene la **venta acumulada**,
> que viene del triage de Sara POS ("factura renuente/terca", `Ctrl+G`). El resto del
> contexto (venta POS, cotización, devolución, saga de stock) ya está en el código y se
> documentará aquí cuando se toque.

## Venta acumulada

### 1. Propósito

Los negocios pequeños hacen muchas ventas de poco valor a consumidor final y no entregan un
documento POS por cada una. La venta acumulada junta esas ventas pequeñas en **un solo
documento** que se va llenando durante el turno, sin imprimir nada, y se emite como venta POS
cuando llega a un límite o cuando se cierra la caja.

Cada venta pequeña **descuenta inventario en el momento** y **entra a la caja en el momento**:
lo único que se agrupa es el documento.

### 2. Lenguaje ubicuo

| Término | Definición | En Sara POS |
|---|---|---|
| **Venta acumulada** | Documento que agrupa ventas pequeñas de un turno hasta su emisión. | factura renuente / terca |
| **Aporte** | Una venta pequeña que se suma a la venta acumulada abierta. | cada `Ctrl+G` |
| **Límite de acumulación** | Monto en pesos que, al alcanzarse, provoca la emisión. Lo configura cada negocio. | límite de la factura renuente |
| **Emisión** | Momento en que la venta acumulada se cierra y pasa a ser una venta POS. | "se manda como venta POS" |

### 3. Modelo

```
VentaAcumulada (agregado raíz)
  id, negocio, sesionDeCaja, numero, estado (ABIERTA | EMITIDA)
  total, abiertaEn, emitidaEn, motivoEmision (LIMITE | CIERRE_DE_CAJA)
  └── Aporte (entidad)
        id, lineas (producto, cantidad, precio, impuestos), total, registradoEn, cajero
```

Comportamiento del agregado:

| Método | Qué hace |
|---|---|
| `abrir(sesionDeCaja)` | Crea la venta acumulada de la sesión |
| `aportar(lineas, cajero)` | Suma un aporte; si el total alcanza el límite, emite |
| `emitir(motivo)` | Cierra el documento y lo convierte en venta POS |

**Propuesta de diseño:** es un agregado propio y no un estado más de `Venta`, porque sus
reglas son distintas (no imprime, solo efectivo, tiene límite, vive atada a una sesión de
caja). Al emitirse produce la venta POS normal, que es la que sigue a facturación.

### 4. Invariantes

| # | Invariante | Origen |
|---|---|---|
| A1 | La venta acumulada solo funciona si el negocio configuró un **límite de acumulación** > 0. Sin límite configurado, la opción no está disponible. No hay valor por defecto. | Usuario: "configurable, no tengo el dato" |
| A2 | Hay como máximo **una** venta acumulada `ABIERTA` por sesión de caja. | Sara: una por usuario |
| A3 | Solo se puede aportar con una sesión de caja `ABIERTA`. | |
| A4 | Si no hay venta acumulada abierta, el primer aporte la crea. | Usuario: "si no existe la crea" |
| A5 | Todo aporte se paga en **efectivo**. No pasa por el diálogo de pago. | Usuario |
| A6 | Un aporte **no imprime** nada. | Usuario |
| A7 | Cada aporte **descuenta inventario en el momento**, con las mismas reglas de stock que una venta POS (incluida la configuración de stock en cero o negativo). | Usuario: "para que el inventario no se desactualice" |
| A8 | Cada aporte entra a la caja como cobro en efectivo en el momento. La emisión **no vuelve a sumar** a la caja: el dinero entra una sola vez. | |
| A9 | Cuando el total alcanza o supera el límite, la venta acumulada se emite y el siguiente aporte abre una nueva. | Usuario |
| A10 | Al cerrar la sesión de caja, la venta acumulada abierta **se emite aunque no haya llegado al límite**. Si no tiene aportes, no se emite nada. | Usuario: "sí" |
| A11 | Una venta acumulada emitida es inmutable. Las correcciones van por devolución. | |
| A12 | Toda lectura y escritura está aislada por `negocio_id` (RLS). | |
| A13 | La venta acumulada **no tiene cliente asignado**: siempre es a **consumidor final**. Ni los aportes ni la emisión aceptan un cliente. | Usuario |
| A14 | Un aporte **nunca se parte**: si hace que el total pase el límite, entra completo y la venta acumulada se emite con ese total (puede quedar por encima del límite). | Usuario: opción a |

### 5. Eventos

| Dirección | Evento | Para qué |
|---|---|---|
| Publica | `aporte_venta_acumulada_registrado` | Caja registra el cobro en efectivo (A8); inventario descuenta (A7) |
| Publica | `venta_acumulada_emitida` | Facturación emite el documento POS; reportes |
| Consume | `caja_cerrada` | Emitir la venta acumulada abierta de esa sesión (A10) |

### 6. Configuración

| Parámetro | Dónde | Valor |
|---|---|---|
| `limiteVentaAcumulada` | Configuración del negocio | Monto en pesos, > 0. Vacío = función desactivada |

### 7. Interfaz (para `design/comportamiento/POS.md`)

- En el POS, `Ctrl+G` registra el carrito actual como aporte a la venta acumulada, sin abrir
  el diálogo de pago y sin imprimir.
- Si el negocio no tiene límite configurado, `Ctrl+G` no hace nada y avisa por qué.

### 8. Pendientes y decisiones abiertas

- **Riesgo fiscal (DIAN):** agrupar varias ventas en un solo documento POS electrónico tiene
  reglas propias. Validar con el contador o el proveedor tecnológico **antes de implementar**.
- **Remisión acumulada** (`Ctrl+Alt+J` en Sara): **pendiente**, decisión del usuario.
