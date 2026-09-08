# Comportamiento · Tarifas y temporadas

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/TarifasMovil.html` |
| Web | `design/pantallas/TarifasWeb.html` |
| Paquete Flutter | `packages/recursos` |
| Microservicio | `servicio-recursos` |
| Tablas | `tarifas` · `servicios_adicionales` · `politicas_cancelacion` · `reglas_disponibilidad` |
| Historias | HU-066 (Tarifas por temporada, día y franja con prioridad) · HU-068 (Servicios adicionales y políticas de cancelación) |

Varias tarifas pueden aplicar a la misma noche. El campo prioridad resuelve el empate sin obligar al negocio a ordenarlas o borrarlas.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · Entre varias tarifas que aplican a la misma noche, gana la de mayor prioridad
**Dado** un recurso con dos tarifas activas que aplican a la noche del 16 de mayo —una "Base"
con `prioridad 0` y una "Puente" con `prioridad 5`—, **cuando** se cotiza esa noche,
**entonces** la noche se cobra a la tarifa "Puente" y en el desglose figura su `tarifa_id`.
El empate exacto de prioridad lo rompe primero la tarifa puntual (de un `recurso_id`) sobre la
de tipo, y luego el `id` de la tarifa, para que el resultado sea estable.

### R2 · Una noche fuera del rango de vigencia no aplica
**Dado** una tarifa con `vigente_desde = 2026-12-24` y `vigente_hasta = 2026-12-26`, **cuando**
la noche cotizada es el 23 o el 27 de diciembre, **entonces** la tarifa no entra en el cálculo
de esa noche. Los dos extremos del rango sí aplican. Si `vigente_desde` o `vigente_hasta` están
vacíos, ese lado del rango es abierto.

### R3 · Una tarifa restringida a ciertos días de la semana no aplica fuera de ellos
**Dado** una tarifa con `dias_semana = {5,6,7}` (viernes, sábado, domingo; 1 = lunes),
**cuando** la noche cotizada cae un martes, **entonces** la tarifa no aplica a esa noche.
Una tarifa sin días definidos aplica los siete días.

### R4 · Una estancia de varias noches cobra cada noche a su propia tarifa
**Dado** una estancia de tres noches que entra el 23 de diciembre con una tarifa "Base"
(`prioridad 0`, sin vigencia) y una "Temporada alta" (`prioridad 10`, vigente del 24 al 26),
**cuando** se cotiza, **entonces** la noche del 23 se cobra a "Base" y las del 24 y 25 a
"Temporada alta"; el total es la suma de las tres. El desglose devuelve una entrada por noche
con su `fecha`, `tarifa_id`, `tarifa_nombre`, `prioridad` y `precio`.

### R5 · Una tarifa con estancia mínima no aplica a estancias más cortas
**Dado** una tarifa con `estancia_minima = 2`, **cuando** la estancia cotizada es de una sola
noche, **entonces** la tarifa no aplica y esa noche se resuelve con la siguiente tarifa que sí
aplique. Con dos o más noches, la tarifa entra en el cálculo.

### R6 · Si ninguna tarifa aplica a una noche, la cotización queda incompleta
**Dado** un recurso cuya única tarifa está restringida a fines de semana, **cuando** se cotiza
una noche de lunes, **entonces** esa noche aparece en el desglose con `tarifa_id` nulo y
`precio 0`, la cotización se marca `completa = false` y el total no incluye esa noche. El
cliente que reserva decide si acepta una cotización incompleta.

### R7 · El precio por persona adicional se suma en cada noche a partir de la segunda persona
**Dado** una tarifa con `precio_base = 100000` y `precio_persona_adicional = 20000`, **cuando**
se cotiza para tres personas, **entonces** cada noche vale `100000 + 2 × 20000 = 140000`. Con
una persona, cada noche vale solo el `precio_base`.

### R8 · El destino de una tarifa es un tipo de recurso o un recurso puntual, nunca ninguno
**Dado** el alta de una tarifa, **cuando** no se indica ni `tipo_recurso_id` ni `recurso_id`,
**entonces** se rechaza con `422` y el mensaje "La tarifa necesita un tipo de recurso o un
recurso puntual". Si el tipo o el recurso indicado no existe en el negocio, responde `404`. Un
tipo o recurso de otro negocio se trata como inexistente.

### R9 · Las tarifas no se ordenan ni se borran: se desactivan
**Dado** una tarifa que el negocio ya no quiere aplicar, **cuando** se desactiva, **entonces**
deja de entrar en cualquier cotización pero sigue visible en el listado marcada como inactiva.
El listado ordena por `prioridad` descendente y luego por `nombre`.

### R10 · El modo de cobro de un servicio adicional decide sus unidades (HU-068)
**Dado** un servicio adicional con `modo_cobro`, **cuando** se cotiza para una reserva de
`personas` y `noches`, **entonces** las unidades son: `POR_ESTANCIA` → 1, `POR_NOCHE` →
`noches`, `POR_PERSONA` → `personas`, `POR_PERSONA_NOCHE` → `personas × noches` (2 personas y
3 noches = 6), `POR_UNIDAD` → la cantidad que se pida. El subtotal es `precio × unidades` con
escala de 4. El `codigo` del servicio es único por negocio (**409** si se repite).

### R11 · Consumir un servicio enlazado a un producto descuenta inventario (HU-068)
**Dado** un servicio adicional con `producto_id`, **cuando** se registra su consumo en una
reserva, **entonces** se publica `servicio_adicional_consumido` (con `producto_id` y la
cantidad = unidades cobradas) para que `servicio-inventario` descuente stock. Un servicio sin
`producto_id` no publica nada: la respuesta trae `descuentaInventario = false`. El descuento
real vive en inventario; aquí solo se emite la instrucción por el outbox.

### R12 · Una política de cancelación define anticipo requerido y penalización (HU-068)
**Dado** una política con `horas_antes`, `penalizacion_pct` y `anticipo_requerido_pct` (los
dos como fracción: `0.25` = 25 %), **cuando** se aplica a una reserva de monto `M` que empieza
en `entrada` y se cancela ahora, **entonces**: el anticipo requerido es `M × anticipo_requerido_pct`
siempre; la penalización es `0` si faltan `horas_antes` o más para la `entrada` (límite
inclusivo), y `M × penalizacion_pct` si se cancela más tarde. Un `pct` fuera de `0..1` o unas
`horas_antes` negativas se rechazan (**422**).

### R13 · Solo una política de cancelación es la de por defecto (HU-068)
**Dado** varias políticas de un negocio, **cuando** se marca una como de por defecto (al
crearla, al editarla o con la acción explícita), **entonces** se desmarca la que lo fuera
antes: nunca hay dos. Aplicar sin indicar política usa la de por defecto; si el negocio no
tiene ninguna, responde **404**. Desactivar una política le quita el `es_default`.

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

Las tarifas, los servicios adicionales y las políticas de cancelación son parte del catálogo
del recurso, así que comparten sus permisos:

- `RECURSOS_RECURSO_VER` — listar y ver tarifas, servicios y políticas; cotizar una estancia o
  un servicio; aplicar una política.
- `RECURSOS_RECURSO_CREAR` — crear una tarifa, un servicio o una política.
- `RECURSOS_RECURSO_EDITAR` — editar/desactivar tarifas, servicios y políticas; consumir un
  servicio; marcar la política por defecto.

Sin el permiso, la llamada falla en el backend con `403` aunque el cliente haya ocultado el
botón. La cotización y la aplicación de política son solo lectura y no exigen un permiso propio
de reservas: las usa tanto el administrador que arma el catálogo como el recepcionista que
responde por teléfono.

## Qué NO debe pasar

<!-- Los casos que hay que impedir a propósito. Esta sección es la que más bugs evita y la
que más se olvida. -->

- Que una cotización mezcle tarifas de dos negocios: `candidatas` sale siempre filtrada por
  `negocio_id`, y el recurso se busca con `find...AndNegocioId`. Un recurso de otro negocio
  responde `404`, no una cotización vacía.
- Que dos tarifas con la misma prioridad devuelvan un total distinto entre llamadas: el
  desempate es determinista (puntual sobre tipo, luego `id`).
- Que desactivar una tarifa cambie cotizaciones ya emitidas: la cotización no se guarda, se
  calcula al vuelo; la reserva que la use congela el precio en su propio módulo (HU-070).
- Que se pueda crear una tarifa "suelta" sin tipo ni recurso, o colgada de un tipo/recurso de
  otro negocio.
- Que el cálculo del precio viva en el cliente Flutter: el cliente muestra el desglose que
  devuelve `POST /api/recursos/cotizaciones`, no lo recompone.
- Que un servicio adicional descuente stock desde `servicio-recursos`: solo emite
  `servicio_adicional_consumido`; el movimiento de inventario lo hace `servicio-inventario`.
- Que queden dos políticas de cancelación con `es_default = true` en un negocio.
- Que se pueda aplicar una política de otro negocio, o cobrar un servicio de otro negocio: se
  buscan siempre con `find...AndNegocioId` y responden `404`.
