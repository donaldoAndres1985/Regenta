# Comportamiento · Factura electrónica

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/FacturaMovil.html` |
| Web | `design/pantallas/FacturaWeb.html` |
| Paquete Flutter | `packages/facturacion` |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturacion.facturas` · `factura_lineas` · `factura_impuestos` · `resoluciones` · `transmisiones` |
| Historias | HU-052 (Administrar resoluciones de numeración DIAN) · HU-053 (Emitir factura desde cualquiera de los tres patrones) · HU-058 (Consulta y envío de facturas al cliente) |

El emisor y el adquiriente son snapshots JSONB: una factura emitida no cambia si mañana editan el cliente. El consecutivo sale del rango de la resolución DIAN.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

> R1–R5 son **HU-052** (`servicio-facturacion`, `/api/facturacion/resoluciones`).
> R6–R9 son **HU-054** (`AsignadorDeConsecutivos`, sin endpoint propio: lo usa la emisión).
> R10–R15 son **HU-053** (emisión desde eventos; consulta en `/api/facturacion/facturas`).

### R1 · Cargar una resolución
**Dado** que un administrador carga una resolución (`POST /api/facturacion/resoluciones` con
`tipoDocumento`, `numeroResolucion`, `prefijo`, `rangoDesde`, `rangoHasta`, `claveTecnica`,
`vigenteDesde`, `vigenteHasta`, `ambiente`), **entonces** queda `VIGENTE` con
`consecutivoActual = rangoDesde`. Exige `FACTURACION_RESOLUCION_EDITAR`. `numeroResolucion`
repetido en el negocio es 409.

### R2 · El rango tiene que ir hacia adelante
**Dado** un rango con `rangoHasta < rangoDesde` (o una vigencia que termina antes de empezar),
**cuando** lo guardo, **entonces** es 422. La base lo cuida además con el CHECK `ck_rango`.

### R3 · Una sola resolución vigente por tipo y sucursal
**Dada** una resolución `VIGENTE` de un tipo y sucursal, **cuando** cargo otra del mismo tipo y
sucursal, **entonces** es 409 («anúlala antes de activar otra»). El índice parcial
`uq_resolucion_vigente` es la última barrera. Al anular la primera (`DELETE
/api/facturacion/resoluciones/{id}` → estado `ANULADA`), se libera el lugar.

### R4 · Aviso cuando queda menos del 10% del rango
**Dada** una resolución con menos del 10% de su rango disponible
(`numerosDisponibles < totalDelRango × 0.10`), **cuando** el asignador de consecutivos la
consume al emitir (HU-054), **entonces** se publica `resolucion_por_agotarse` con
`negocio_id`, `resolucion_id`, `numero_resolucion`, `tipo_documento`, `disponibles` y
`total_rango`. Alertas lo consume.

### R5 · No se factura con una resolución vencida
**Dada** una resolución cuya `vigenteHasta` ya pasó (o cuyo estado no es `VIGENTE`), **cuando**
se pide la vigente para emitir (`GET /api/facturacion/resoluciones/vigente?tipoDocumento=…`),
**entonces** es 422 nombrando la fecha de vencimiento, o 404 si no hay ninguna de ese tipo.

### R6 · El número se toma con la resolución bloqueada, dentro de la transacción
**Dada** una emisión, **cuando** se asigna el número, **entonces** la resolución vigente se
lee con `SELECT … FOR UPDATE` en la misma transacción que guarda la factura. Se hace en una
sola consulta (no cargar sin bloqueo y después bloquear), para que la fila llegue con su
`@Version` fresca.

### R7 · Números continuos y sin huecos, aún concurrentes
**Dadas** N emisiones a la vez sobre la misma resolución, **cuando** ocurren, **entonces** cada
una recibe un número distinto y los números salen consecutivos desde `consecutivoActual`, sin
huecos ni repetidos. El `numeroCompleto` es `prefijo + número` (p. ej. `FE990000001`). Probado
con 50 emisiones simultáneas.

### R8 · Al tomar el último, la resolución queda AGOTADA
**Dado** que se toma el último número del rango, **cuando** se asigna, **entonces**
`consecutivoActual` queda en `rangoHasta + 1` (lo permite `ck_consecutivo`) y el estado pasa a
`AGOTADA`. Las siguientes emisiones se rechazan: ya no hay resolución `VIGENTE` de ese tipo.

### R9 · Un rollback no consume el número
**Dado** que la transacción de emisión hace rollback, **cuando** ocurre, **entonces** el
avance de `consecutivoActual` se deshace con ella: el siguiente número vuelve a estar
disponible. No es un `BIGSERIAL` justamente por esto.

### R10 · Un cierre emite su factura, venga del patrón que venga
**Dado** un evento `venta_completada` / `estancia_finalizada` / `pedido_completado`, **cuando**
`ConsumidorDeCierresFacturables` lo recibe, **entonces** se emite una factura con
`origen_tipo` = `VENTA` / `RESERVA` / `COMANDA` según la clave de enrutamiento y `origen_id` el
del documento de origen. Es el mismo código para los tres: `servicio-facturacion` no conoce
las bases de Ventas, Reservas ni Comandas. La factura queda en estado `GENERADA` (la firma es
HU-055).

### R11 · El evento trae lo que hace falta para facturar
**Dado** el payload del cierre, **entonces** trae `negocio_id`, el id del origen
(`origen_id` / `venta_id` / `estancia_id` / `pedido_id`), `cliente_id` (opcional), `emisor` y
`cliente` (objetos que se congelan tal cual), y `lineas` con sus `impuestos`. Sin `lineas` la
emisión falla (`ReglaDeNegocioException`). El emisor de estos eventos (hoy `servicio-ventas`)
debe enriquecer su payload; hasta entonces la emisión real no se dispara.

### R12 · Los totales se recalculan desde las líneas
**Dadas** las líneas del evento, **cuando** se emite, **entonces** `subtotal`,
`descuento_total`, `base_gravable`, `impuestos_total`, `retenciones_total` y `total` se calculan
sumando las líneas y sus impuestos (escala 4, HALF_UP); `total = base_gravable + impuestos −
retenciones + propina`. Las líneas y sus impuestos quedan en `factura_lineas` y
`factura_impuestos`.

### R13 · Una factura por documento de origen
**Dado** el mismo evento entregado dos veces (misma `message-id`, o distinta pero mismo
`origen_id`), **cuando** llega el duplicado, **entonces** no se emite una segunda factura. Lo
cuidan el Inbox y, además, el índice parcial `uq_factura_origen`
(`negocio_id, origen_tipo, origen_id` para facturas de venta no anuladas).

### R14 · El emisor y el cliente quedan congelados
**Dada** la emisión, **cuando** se guarda, **entonces** `emisor_snapshot` y `cliente_snapshot`
quedan en JSONB con los datos de ese momento. Si mañana cambian los datos del cliente,
`GET /api/facturacion/facturas/{id}` sigue mostrando los de la fecha de emisión.

### R15 · La emisión no tiene endpoint
**Dado** que la factura nace de un evento, **entonces** no hay `POST` para emitir: solo
`GET /api/facturacion/facturas` (listado) y `GET /api/facturacion/facturas/{id}` (detalle con
líneas, impuestos y snapshots). Ambos exigen `FACTURACION_FACTURA_VER`.

## Al abrir

<!-- Qué se carga y en qué orden, qué campo toma el foco, qué se ve mientras carga, qué se
recuerda de la última vez (filtros, sucursal, orden de la tabla). -->

_Sin definir._

## Validaciones

<!-- Campo por campo: qué se rechaza, con qué mensaje exacto, y cuándo se valida — al
escribir, al salir del campo o al enviar. -->

_Sin definir._

## Estados vacíos y de error

<!-- Qué se ve cuando no hay datos todavía, cuando la búsqueda no encuentra nada, y cuando
el servicio responde con error. Los tres son distintos. -->

_Sin definir._

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

Resoluciones (HU-052): `FACTURACION_RESOLUCION_VER` para listar / consultar / ver la vigente;
`FACTURACION_RESOLUCION_EDITAR` para cargar y anular. Sin el permiso, 403. Módulo
`FACTURACION`, plan Profesional o superior.

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- **No** dos resoluciones `VIGENTE` del mismo tipo y sucursal (índice `uq_resolucion_vigente`).
- **No** un rango con `rangoHasta < rangoDesde` (CHECK `ck_rango`), ni `consecutivoActual` fuera
  de `[rangoDesde, rangoHasta + 1]` (CHECK `ck_consecutivo`).
- **No** facturar con una resolución vencida, agotada o anulada.
- **No** un `BIGSERIAL` para el consecutivo: se toma con bloqueo sobre la resolución dentro de
  la transacción de emisión (HU-054).
- **No** dos facturas para el mismo `(origen_tipo, origen_id)` (Inbox + `uq_factura_origen`).
- **No** que `servicio-facturacion` consulte las tablas de Ventas, Reservas ni Comandas: todo
  llega por evento.
- **No** que una factura emitida cambie porque se editó el cliente: el snapshot es inmutable.
