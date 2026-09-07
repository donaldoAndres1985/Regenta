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

> R1–R5 son **HU-052** (`servicio-facturacion`, `/api/facturacion/resoluciones`). La asignación
> del consecutivo dentro del rango es **HU-054**; la emisión, **HU-053**.

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
