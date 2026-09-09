# Comportamiento · Estancia y check-out

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/EstanciaMovil.html` |
| Web | `design/pantallas/EstanciaWeb.html` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| Tablas | `estancias` · `consumos_estancia` · `ocupantes` · `pagos_reserva` |
| Historias | HU-072 (Check-in con asignación de recurso) · HU-073 (Cargar consumos a la estancia) · HU-074 (Check-out, liquidación y cierre de estancia) |
| Estado historias | HU-072 ✅ · HU-073 ✅ · HU-074 ✅ |

Al cerrar se publica estancia_finalizada: el equivalente exacto de venta_completada. Facturación, Reportes, CRM y Caja lo consumen igual.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · El check-in asigna la habitación concreta (HU-072)
**Dado** una reserva `CONFIRMADA` vendida por tipo (sin `recurso_id`), **cuando** se hace el
check-in sin indicar recurso, **entonces** se le asigna uno libre de ese tipo para el periodo
de la reserva. Si la recepción indica un recurso, se usa ese. Si la reserva ya traía recurso y
no se indica otro, se conserva. Sin recursos libres del tipo, responde **409**.

### R2 · El anti-overbooking del recurso asignado lo hace la base (HU-072)
**Dado** un recurso ya tomado por otra reserva que ocupa (`PENDIENTE`/`CONFIRMADA`/`CHECK_IN`)
en un periodo que se solapa, **cuando** se intenta asignarlo en el check-in, **entonces** el
`EXCLUDE USING gist` de `reservas` lo rechaza y la API responde **409**. No hay un "¿está
libre?" en Java: la asignación es un `UPDATE reservas SET recurso_id = …, estado = 'CHECK_IN'` y
el constraint decide.

### R3 · El check-in abre la estancia y mueve la reserva a CHECK_IN (HU-072)
**Dado** una reserva `CONFIRMADA` sin estancia, **cuando** se registra el check-in, **entonces**
se crea la fila en `estancias` (`estado = EN_CURSO`, `check_out_previsto` = fin de la reserva,
`deposito` el que se indique) y la reserva pasa a `CHECK_IN`. Solo se hace check-in desde
`CONFIRMADA` y una sola vez (segundo intento → **409**).

### R4 · El titular queda identificado con su documento (HU-072)
**Dado** los ocupantes que se registran en el check-in, **cuando** alguno es titular
(`es_titular = true`), **entonces** tiene que traer `tipo_documento` y `numero_documento`; si
faltan, responde **422**. Un acompañante puede ir sin documento. La `nacionalidad`, si viene,
es el código ISO de dos letras.

### R5 · Al completar el check-in, el recurso pasa a OCUPADO (HU-072)
**Dado** el check-in completo, **cuando** se registra, **entonces** se publica
`check_in_registrado` (con `recurso_id`) para que servicio-recursos ponga la habitación en
estado `OCUPADO`. El cambio de estado del recurso vive en servicio-recursos, no aquí.

### R6 · Cargar un consumo suma al total de la estancia (HU-073)
**Dado** una estancia `EN_CURSO`, **cuando** se carga un consumo (`descripcion`, `cantidad`,
`precio_unitario`, `impuesto_pct` como fracción), **entonces** se guarda en `consumos_estancia`
con su `origen` (`MINIBAR`/`RESTAURANTE`/`SPA`/`LAVANDERIA`/`TELEFONO`/`OTRO`) y su
`cargado_en`, el `total` del cargo es `cantidad × precio_unitario × (1 + impuesto_pct)`, y
`estancias.consumo_total` sube en ese importe. La respuesta trae `saldoConConsumos` = saldo de
la reserva + total de la estancia. `cantidad` debe ser mayor que cero y `descripcion` no puede
ir vacía (**422**).

### R7 · Un consumo enlazado a un producto queda listo para descontar stock (HU-073)
**Dado** un consumo con `producto_id`, **cuando** se carga, **entonces** se guarda ese
`producto_id`; al cerrar la estancia (HU-074) se publica el evento que Inventario consume para
descontar stock. Un consumo sin `producto_id` no mueve inventario.

### R8 · Una comanda de restaurante cargada a la habitación queda enlazada (HU-073)
**Dado** una comanda de restaurante, **cuando** se carga a la habitación, **entonces** el
consumo guarda su `comanda_id`, de modo que se puede rastrear de qué comanda vino.

### R9 · A una estancia cerrada no se le cargan consumos (HU-073)
**Dado** una estancia que ya no está `EN_CURSO` (`FINALIZADA` o `EXTENDIDA`), **cuando** se
intenta cargar un consumo, **entonces** responde **409**.

### R10 · La liquidación suma alojamiento, servicios y consumos, menos el anticipo (HU-074)
**Dado** una estancia, **cuando** se calcula la liquidación, **entonces**
`subtotal = total de la reserva (alojamiento) + servicios adicionales + consumos de la
estancia` y `saldo_pendiente = subtotal − lo abonado` (pagos de tipo `ANTICIPO`, `SALDO`,
`DEPOSITO`, `CONSUMO`). Se puede consultar sin cerrar la estancia (`GET .../liquidacion`).

### R11 · El check-out cierra la estancia y pasa la reserva a CHECK_OUT (HU-074)
**Dado** una estancia `EN_CURSO` de una reserva en `CHECK_IN`, **cuando** se confirma el
check-out, **entonces** la estancia pasa a `FINALIZADA` (con `check_out_en` y
`check_out_usuario_id`) y la reserva a `CHECK_OUT`. El evento `CHECK_IN → CHECK_OUT` queda en
`reserva_eventos`. Segundo check-out o check-out de una reserva sin check-in → **409**.

### R12 · Al cerrar se publica `estancia_finalizada` (HU-074)
**Dado** el check-out completo, **cuando** se registra, **entonces** se publica
`estancia_finalizada` —el equivalente de `venta_completada`— con `origen_tipo = RESERVA`, el
desglose en `lineas` (una de `ALOJAMIENTO` más una por cada consumo, con su `producto_id`
cuando lo tiene), los totales y los `pagos`. Facturación lo consume y emite el documento con
origen `RESERVA` (HU-053).

### R13 · Al cerrar, el recurso pasa a LIMPIEZA, no directo a disponible (HU-074)
**Dado** el check-out, **cuando** se completa, **entonces** se publica `check_out_registrado`
con `recurso_id` y `estado_recurso_sugerido = LIMPIEZA` para que servicio-recursos ponga la
habitación en `LIMPIEZA`. No queda disponible hasta que alguien la marque limpia.

### R14 · Un saldo pendiente exige confirmación explícita (HU-074)
**Dado** una liquidación con `saldo_pendiente > 0`, **cuando** se intenta cerrar sin
`confirmarConSaldo = true` y sin un pago que lo cubra, **entonces** responde **409** avisando
del monto. Con la confirmación, o con un pago final que deje el saldo en cero, el check-out
procede. Si el consumo tenía `producto_id`, además se publica `insumos_consumidos` para que
Inventario descuente stock.

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

`RESERVAS_RESERVA_EDITAR` para hacer check-in, registrar ocupantes, cargar consumos y hacer el
check-out; `RESERVAS_RESERVA_VER` para consultar la estancia y la liquidación. Sin el permiso,
la llamada falla en el backend con `403`. Módulo `RESERVAS`, patrón Reserva. La plantilla de rol
`RECEPCIONISTA` los trae.

## Qué NO debe pasar

- Que el check-in compruebe la disponibilidad del recurso con un `SELECT` y luego lo asigne:
  la asignación va contra el `EXCLUDE` de la tabla, que gana la carrera.
- Que se abra una segunda estancia para la misma reserva (`estancias.reserva_id` es único).
- Que un titular quede sin documento.
- Que servicio-reservas ponga el recurso en `OCUPADO` por su cuenta: lo hace servicio-recursos
  al consumir `check_in_registrado`.
- Que se carguen consumos a una estancia cerrada.
- Que el descuento de stock de un consumo con producto lo haga servicio-reservas: solo guarda
  el `producto_id`; el descuento lo dispara el cierre de la estancia (HU-074, evento
  `insumos_consumidos`) y lo aplica Inventario.
- Que se borre una estancia o una reserva para "liberar" la habitación: se cierra y el
  `EXCLUDE` deja de contarla; el registro queda.
- Que el check-out ponga el recurso directamente en `DISPONIBLE`: siempre pasa por `LIMPIEZA`.
- Que se cierre una estancia con saldo pendiente sin que alguien lo confirme a propósito.

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

_Sin definir._
