# Comportamiento · Calendario de disponibilidad

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/CalendarioMovil.html` |
| Web | `design/pantallas/CalendarioWeb.html` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| Tablas | `reservas.reservas (periodo tstzrange)` · `recursos.recursos` · `bloqueos_recurso` |
| Historias | HU-069 (Consultar disponibilidad en un periodo) · HU-075 (Calendario visual de ocupación) |

Cada barra es un rango de tiempo. Que dos no puedan solaparse lo garantiza un constraint de exclusión GiST en PostgreSQL, no la aplicación.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · Un recurso con una reserva que ocupa el periodo no aparece libre (HU-069)
**Dado** un periodo `[desde, hasta)`, **cuando** se consulta la disponibilidad, **entonces** se
excluye todo recurso que tenga una reserva en estado `PENDIENTE`, `CONFIRMADA` o `CHECK_IN`
cuyo tramo (con el buffer de su tipo) pise el periodo, y todo recurso con un bloqueo que lo
pise. Cada recurso excluido vuelve con su `motivo`: `RESERVA` o `BLOQUEO`.

### R2 · Una reserva `CANCELADA` o `NO_SHOW` deja el recurso libre (HU-069)
**Dado** un recurso con una reserva `CANCELADA`, `NO_SHOW`, `CHECK_OUT` o `EXPIRADA` que se
solapa con el periodo, **cuando** se consulta, **entonces** el recurso aparece libre: esos
estados no ocupan. Son los mismos que excluye el `EXCLUDE USING gist` de la tabla.

### R3 · Un check-out libera el recurso desde la hora del check-out (HU-069)
**Dado** una reserva que termina a las 11:00 y un tipo de recurso sin buffer de limpieza,
**cuando** se consulta la disponibilidad desde las 11:00 del mismo día, **entonces** el recurso
aparece libre. El periodo es medio abierto: `[…, 11:00)` no se solapa con `[11:00, …)`. Lo
mismo vale para dos reservas donde una empieza justo cuando la otra termina.

### R4 · El buffer de limpieza del tipo se respeta (HU-069)
**Dado** un tipo de recurso con `buffer_despues_min = 120`, **cuando** se consulta desde la
hora del check-out, **entonces** el recurso sigue ocupado durante esas dos horas y recién
después aparece libre. El `buffer_antes_min` adelanta la ocupación antes del check-in de la
misma forma.

### R5 · La consulta de un mes sobre un catálogo grande responde rápido (HU-069)
**Dado** un negocio con ~200 recursos y varios meses de reservas, **cuando** se consulta la
disponibilidad de un mes, **entonces** responde en menos de 500 ms. La consulta a
`reservas.reservas` se apoya en el índice GiST sobre `periodo` y en el filtro por `negocio_id`
de la RLS.

### R6 · El calendario es una rejilla: una fila por recurso, una columna por día (HU-075)
**Dado** el calendario abierto sobre una semana, **cuando** lo miro, **entonces** hay una fila
por cada recurso reservable y una columna por cada día de la ventana; las reservas se dibujan
como barras que arrancan en su día de entrada y terminan en el de salida. El backend
(`GET /api/reservas/calendario?desde=&hasta=`) devuelve `recursos`, `reservas` (con
`recurso_id` asignado y estado `PENDIENTE`/`CONFIRMADA`/`CHECK_IN`/`CHECK_OUT`) y `bloqueos`
por separado. Rango invertido o de más de 62 días → **422**.

### R7 · El color de la barra indica el estado de la reserva (HU-075)
**Dado** una barra de reserva, **cuando** la miro, **entonces** su color sale del estado:
`PENDIENTE` ámbar (`warnSoft`/`warn`), `CONFIRMADA` teal (`reservaSoft`/`reserva`), `CHECK_IN`
verde (`okSoft`/`ok`), `CHECK_OUT` gris (`sunken`/`muted`). El filo izquierdo de 3 px lleva el
tono fuerte; el fondo, el suave. Los mismos hex que el mockup y `RegentaColors`.

### R8 · Un bloqueo se distingue de una reserva (HU-075)
**Dado** un bloqueo de recurso en la ventana, **cuando** lo miro, **entonces** aparece como una
barra roja (`critSoft` con filo `crit`) rotulada `Bloqueo · <motivo>`, sin color de estado y
sin acción al tocarla. Nunca se confunde con una reserva.

### R9 · En móvil se ven tres días y se desplaza en horizontal (HU-075)
**Dado** el calendario en una pantalla angosta (`< kBreakpointEscritorio`), **cuando** lo abro,
**entonces** entran tres columnas de día a la vez y la rejilla se desplaza en horizontal para
alcanzar el resto de la semana. En escritorio entran los siete días sin desplazamiento. Los
botones ‹ / › llevan a la semana anterior y a la siguiente (vuelven a pedir los datos).

### R10 · Tocar una barra de reserva abre esa reserva (HU-075)
**Dado** una barra de reserva, **cuando** la toco, **entonces** se abre esa reserva (la
navegación la resuelve el shell con el `id` de la reserva). Las barras de bloqueo no son
tocables.

## Al abrir

<!-- Qué se carga y en qué orden, qué campo toma el foco, qué se ve mientras carga, qué se
recuerda de la última vez (filtros, sucursal, orden de la tabla). -->

Se carga la semana que arranca hoy (o la `desdeInicial` que pase el shell). Mientras llega la
respuesta se muestra un indicador de carga; si falla, el mensaje de error del backend. Si el
negocio no tiene recursos, un estado vacío ("No hay recursos que mostrar en esta semana.").

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

Un solo `LayoutBuilder` cortando en `kBreakpointEscritorio` (900). Móvil: tres columnas de día
visibles, la rejilla dentro de un `SingleChildScrollView` horizontal. Escritorio: las siete
columnas caben sin desplazamiento. La columna de la izquierda (código y nombre del recurso) es
más angosta en móvil.

## Permisos

<!-- Qué ve y qué puede hacer cada rol en esta pantalla, y qué pasa exactamente cuando no
tiene el permiso: no se ve, se ve deshabilitado, o falla al intentar. -->

`RESERVAS_RESERVA_VER` para consultar disponibilidad y el calendario de ocupación. Sin el
permiso, la llamada falla en el backend con `403`. Módulo `RESERVAS`, patrón Reserva, plan
Básico o superior.

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- Que `servicio-reservas` consulte la base de `servicio-recursos`: el catálogo de recursos y
  los bloqueos llegan por un puerto (`CatalogoDeRecursos`). Hoy es un stub; la integración real
  (REST o réplica por eventos) es un pendiente.
- Que una consulta con un periodo de fin anterior o igual al inicio pase: responde `422`.
- Que la disponibilidad mezcle reservas de dos negocios: la consulta a `reservas.reservas` va
  con RLS y filtra por `negocio_id`.
- Que una reserva sin `recurso_id` asignado (solo cupo de tipo) excluya un recurso concreto —
  esas se gestionan por cupo, no por recurso.
- Que el calendario muestre reservas `CANCELADA`, `NO_SHOW` o `EXPIRADA`: no ocupan, no se
  pintan.
- Que una barra de bloqueo abra algo al tocarla, o se pinte con el color de un estado de
  reserva.
- Que el cálculo de posiciones de las barras viva en el backend: el servicio devuelve los
  tramos; el cliente los traduce a píxeles.
