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
> R16–R21 son **HU-055** (firma, transmisión, certificados). Los adaptadores de DIAN, KMS y
> storage son *stubs* con puertos; la integración real es un follow-up.
> R22–R25 son **HU-056** (notas crédito). R26–R29 son **HU-057** (contingencia).
> R30–R33 son **HU-058** (pantalla `packages/facturacion` + envío al cliente).

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

### R16 · Al firmar se calcula el CUFE y el XML va fuera de la base
**Dada** una factura `GENERADA`, **cuando** se firma, **entonces** se calcula el CUFE, el XML
firmado se guarda en un storage externo (`AlmacenDeDocumentos`) y en `facturas` quedan solo
`cufe` y `xml_url`. La factura pasa a `FIRMADA`. La clave privada la lee `BovedaDeSecretos` de
`certificados.referencia_kms`; nunca toca la base ni el repositorio. Firmar dos veces es
idempotente. `factura_emitida` (HU-053) dispara firma + transmisión.

### R17 · La transmisión queda registrada completa
**Dada** la transmisión, **cuando** la DIAN responde, **entonces** se inserta una fila en
`transmisiones` (`evento = ENVIO`) con el `request` y el `response` completos, el `http_status`
y la duración. Es evidencia legal.

### R18 · Un rechazo deja la factura RECHAZADA y alerta
**Dado** un rechazo de la DIAN, **cuando** llega, **entonces** la factura queda `RECHAZADA`,
`respuesta_dian` guarda `{codigo, mensaje}` y se publica `factura_rechazada` (con el
`codigo_error`) para Alertas. Una aceptación deja la factura `ACEPTADA` con `aceptada_en` y
publica `factura_aceptada`.

### R19 · Un fallo de red no pierde la factura
**Dado** que la DIAN no responde (`DianNoDisponibleException`), **cuando** ocurre, **entonces**
la factura queda `ENVIADA` con `intentos_envio` + 1 y una fila en `transmisiones` con
`codigo_error = SIN_RESPUESTA`; la excepción **no** se relanza (abortar la transacción borraría
el intento y el registro). `reintentarPendientes()` la reintenta; el backoff lo pone el
`@Scheduled` (diferido, como el resto de barridos), con tope de 8 intentos.

### R20 · Certificado por vencer → alerta
**Dado** un certificado `ACTIVO` cuya `vigente_hasta` cae dentro de los próximos 30 días,
**cuando** corre `revisarCertificadosPorVencer()`, **entonces** se publica
`certificado_por_vencer` (`negocio_id`, `certificado_id`, `alias`, `vigente_hasta`).

### R21 · El certificado se guarda por referencia, nunca el archivo
**Dado** el alta de un certificado (`POST /api/facturacion/certificados`), **entonces** solo se
recibe y se guarda `referenciaKms` (la clave en el gestor de secretos) más los metadatos
(alias, emisor, serie, vigencia). Sin `referenciaKms` es 422; `alias` repetido, 409. La tabla
`certificados` no tiene columna para el `.p12`: es estructural. Exige
`FACTURACION_RESOLUCION_EDITAR`.

### R22 · Una factura emitida se corrige con una nota crédito
**Dada** una factura **aceptada**, **cuando** se emite una NC
(`POST /api/facturacion/facturas/{id}/notas-credito`), **entonces** se crea otra factura con
`tipo_documento = NOTA_CREDITO`, `factura_origen_id` = la factura de origen y `codigo_nota` =
el concepto DIAN (1 devolución, 2 anulación, 4 descuento, 5 otros). Toma su consecutivo de una
resolución `NOTA_CREDITO` (HU-054). Si no viene el detalle, la NC copia las líneas de la
factura de origen. Sobre una factura que no está aceptada es 422. Exige
`FACTURACION_FACTURA_ANULAR`.

### R23 · Sin factura de origen no hay nota crédito
**Dada** una NC sin `factura_origen_id`, **cuando** se intenta guardar, **entonces** el CHECK
`ck_nota_referencia` de la base la rechaza. El servicio además valida antes (404 si la factura
de origen no existe).

### R24 · La devolución emite su nota crédito
**Dado** un evento `devolucion_registrada` (`negocio_id`, `venta_id`, `devolucion_id`,
`motivo`, `lineas`), **cuando** llega, **entonces** se busca la factura de venta por
`venta_id` y se emite una NC por lo devuelto, con `origen_tipo = DEVOLUCION` y
`origen_id = devolucion_id`. Idempotente por el Inbox y por el índice parcial
`uq_nota_credito_origen`: una devolución reentregada no genera dos NC.

### R25 · La factura de origen se ve enlazada a sus notas crédito
**Dada** una factura con NC emitidas, **cuando** se consulta
(`GET /api/facturacion/facturas/{id}`), **entonces** el detalle trae `notasCredito` con el
número, el `codigo_nota`, el total y el estado de cada una.

### R26 · La DIAN caída abre una contingencia
**Dado** que una factura acumula {@code >= 3} intentos sin acuse
(`DianNoDisponibleException`), **cuando** se supera ese umbral y no hay ya una contingencia
abierta, **entonces** se abre una (`contingencias`, `fin_en IS NULL`) y se publica
`contingencia_abierta`. Solo una abierta por negocio: índice parcial
`uq_contingencia_abierta`.

### R27 · Con contingencia abierta se factura en contingencia
**Dada** una contingencia abierta, **cuando** se emite/transmite una factura, **entonces** NO
se llama a la DIAN: la factura se firma (tiene CUFE, se entrega al cliente) y queda en estado
`CONTINGENCIA`, y `contingencias.facturas_afectadas` sube 1. El contador se incrementa por SQL
atómico (no hay columna `version`).

### R28 · Al cerrar la contingencia se retransmite en orden
**Dado** que la DIAN vuelve, **cuando** se cierra la contingencia
(`POST /api/facturacion/contingencias/{id}/cierre`), **entonces** `fin_en` y `regularizada`
quedan puestos y las facturas `CONTINGENCIA` (y las `ENVIADA` / `RECHAZADA` sin acuse) se
retransmiten de la más antigua a la más nueva. Exige `FACTURACION_FACTURA_CREAR`.

### R29 · La contingencia dice cuántas facturas afectó
**Dada** una contingencia, **cuando** se consulta (`GET /api/facturacion/contingencias/{id}`),
**entonces** trae `inicio_en`, `fin_en`, `motivo`, `facturas_afectadas`, `abierta` y
`regularizada`. Exige `FACTURACION_FACTURA_VER`.

### R30 · La pantalla muestra la factura completa
**Dada** una factura, **cuando** se abre `PantallaFactura` (`GET
/api/facturacion/facturas/{id}` + `.../transmisiones`), **entonces** se ven emisor,
adquiriente (con su documento), líneas (descripción, código, `cantidad × precio`, total),
totales (subtotal, descuento, base gravable, cada impuesto, total), el CUFE, el QR y la
trazabilidad. El emisor y el adquiriente salen del snapshot: no cambian aunque después editen
al cliente.

### R31 · El CUFE se ve completo y se copia
**Dado** el celular, **cuando** abro la factura, **entonces** el CUFE se muestra entero
(seleccionable) y hay un botón *Copiar CUFE* de 44 px que lo lleva al portapapeles. El QR se
arma con el CUFE (`…/searchqr?documentkey=<CUFE>`); la imagen del QR queda para una iteración.

### R32 · El rechazo se ve sin buscarlo
**Dada** una factura `RECHAZADA`, **cuando** la abro, **entonces** justo bajo la cabecera hay
un aviso rojo con el código y el mensaje del rechazo, y el chip de estado dice «Rechazada por
la DIAN». El botón *Enviar* queda deshabilitado (solo se envía una aceptada).

### R33 · Enviar al cliente
**Dada** una factura **aceptada**, **cuando** toco *Enviar* (`POST
/api/facturacion/facturas/{id}/envio-cliente`, cuerpo opcional `{correo}`), **entonces** el
backend manda el correo con el XML y el PDF adjuntos, lo registra en `transmisiones`
(`evento = EMAIL_CLIENTE`) y publica `factura_enviada_al_cliente`; la pantalla avisa a quién se
envió. Sin `correo` se usa el del snapshot del cliente; si no hay ninguno, 422. El envío de
SMTP real es un stub con puerto (`EnviadorDeCorreo`). Exige `FACTURACION_FACTURA_VER`.

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

`PantallaFactura` (HU-058) es un solo widget con `LayoutBuilder` en `kBreakpointEscritorio`
(900 px): móvil = una columna (`Key('factura-movil')`); escritorio = la misma tarjeta centrada
con ancho máximo ~720 px (`Key('factura-escritorio')`). Objetivo de toque de 44 px en el botón
*Copiar CUFE* y en *Enviar* / *Imprimir*. La barra de navegación inferior de `FacturaMovil.html`
es presentacional (shell). *Imprimir* (generación del PDF) queda para el shell / una iteración.

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
- **No** el `.p12` ni el XML firmado en la base: KMS y storage externo, en la base solo la
  referencia y la URL.
- **No** relanzar el fallo de red al transmitir: perdería el intento y el log; la factura queda
  `ENVIADA` y se reintenta.
- **No** editar ni borrar una factura emitida: se corrige con una nota crédito (HU-056).
- **No** una NC sin `factura_origen_id` (CHECK `ck_nota_referencia`), ni dos NC para la misma
  devolución (`uq_nota_credito_origen`).
- **No** una NC de una factura que aún no está aceptada.
- **No** dos contingencias abiertas a la vez por negocio (`uq_contingencia_abierta`).
- **No** llamar a la DIAN mientras haya una contingencia abierta: se factura en contingencia y
  se transmite al cerrarla.
- **No** enviar al cliente una factura que no está aceptada por la DIAN.
- **No** recortar el CUFE en la pantalla: se muestra entero y copiable.
