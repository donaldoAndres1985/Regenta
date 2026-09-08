# Comportamiento · Nueva reserva

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/NuevaReservaMovil.html` |
| Web | `design/pantallas/NuevaReservaWeb.html` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| Tablas | `reservas` · `tarifas` · `servicios_adicionales` · `politicas_cancelacion` · `cupos_tipo_recurso` |
| Historias | HU-070 (Crear una reserva sin posibilidad de overbooking) |

El periodo es semiabierto: la salida de las 11:00 no choca con una entrada a las 11:00 del mismo día. Eso lo da el tipo de rango, no una regla escrita a mano.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · El anti-overbooking lo hace la base, no la aplicación (HU-070)
**Dado** dos recepcionistas reservando el mismo recurso y periodo al mismo tiempo, **cuando**
ambas confirman, **entonces** solo una lo logra y la otra recibe **409** con "El recurso ya
está reservado en ese periodo". No hay un "¿está libre?" seguido de un INSERT —tiene ventana
de carrera—: lo impide el `EXCLUDE USING gist (negocio_id =, recurso_id =, periodo &&)` sobre
`reservas.periodo`, que solo aplica a los estados que ocupan (`PENDIENTE`, `CONFIRMADA`,
`CHECK_IN`). El servicio traduce la violación del constraint al 409.

### R2 · Un periodo que se solapa con una reserva que ocupa se rechaza (HU-070)
**Dado** una reserva `PENDIENTE`/`CONFIRMADA`/`CHECK_IN` de un recurso, **cuando** se intenta
crear otra del mismo recurso cuyo periodo la pisa, **entonces** PostgreSQL la rechaza por el
constraint de exclusión y la API responde **409**. El `negocio_id` va en el constraint: dos
negocios distintos sí pueden tener el mismo `recurso_id` y periodo (no comparten recursos).

### R3 · Dos reservas que se tocan en el borde se permiten (HU-070)
**Dado** una reserva que termina a las 11:00, **cuando** se crea otra del mismo recurso que
empieza a las 11:00 del mismo día, **entonces** se permite: el rango es medio abierto,
`[…, 11:00)` no se solapa con `[11:00, …)`.

### R4 · Cada noche se cobra a su tarifa (HU-070)
**Dado** el periodo y el recurso, **cuando** se crea la reserva, **entonces** el `subtotal` y
el `total` salen de la cotización noche por noche que devuelve servicio-recursos (cada noche
toma la tarifa de mayor prioridad que le aplique, HU-066). La respuesta trae el desglose por
noche (`fecha`, `tarifa_id`, `precio`); la cabecera guarda como referencia la tarifa de la
primera noche. Si alguna noche se queda sin tarifa, `cotizacionCompleta` es false.

### R5 · Al crear la reserva se calcula el anticipo requerido (HU-070)
**Dado** una política de cancelación (la indicada o la de por defecto del negocio), **cuando**
se crea la reserva, **entonces** se calcula el anticipo requerido sobre el `total` y se guarda
en la reserva; el `saldo` es `total − anticipo_requerido`. La política aplicada queda
referenciada en `politica_cancelacion_id`.

### R6 · Un periodo con fin anterior o igual al inicio se rechaza (HU-070)
**Dado** una solicitud con `hasta <= desde`, **cuando** se envía, **entonces** responde
**422** antes de tocar la base (además del `ck_periodo` de la tabla como red de seguridad).

### R7 · El número de reserva corre por negocio (HU-070)
**Dado** un negocio, **cuando** se crean reservas, **entonces** el `numero` es `RES-1`,
`RES-2`, … asignado con un consecutivo transaccional (`INSERT … ON CONFLICT … RETURNING`) que
no repite número entre reservas concurrentes. Cada negocio lleva su propia serie.

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

`RESERVAS_RESERVA_CREAR` para crear la reserva; `RESERVAS_RESERVA_VER` para consultarla. Sin el
permiso, la llamada falla en el backend con `403`. Módulo `RESERVAS`, patrón Reserva, plan
Básico o superior. La plantilla de rol `RECEPCIONISTA` los trae.

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- Que el anti-overbooking se compruebe con un `SELECT` en Java: siempre por el `EXCLUDE` de la
  base. El test de concurrencia con dos hilos lo verifica.
- Que la reserva se cree sin recurso asignado: `recurso_id` es obligatorio en HU-070 (el cupo
  por tipo sin recurso concreto es de otra historia).
- Que el cálculo del precio o del anticipo viva en `servicio-reservas`: la cotización la hace
  servicio-recursos (tarifas de HU-066) y el anticipo sale de la política de HU-068. Hoy llega
  por un puerto con stub; la integración real es un pendiente.
- Que una reserva de un negocio sea visible o modificable desde otro: `reservas` va con RLS y
  `ver` comprueba el `negocio_id`.
