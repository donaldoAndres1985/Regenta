# Comportamiento · Ficha de recurso

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/RecursoMovil.html` |
| Web | `design/pantallas/RecursoWeb.html` |
| Paquete Flutter | `packages/recursos` |
| Microservicio | `servicio-recursos` |
| Tablas | `recursos` · `tipos_recurso` · `atributos_tipo_recurso` · `tarifas` · `bloqueos_recurso` |
| Historias | HU-064 (Tipos de recurso con atributos configurables) · HU-065 (Administrar recursos individuales) · HU-067 (Bloqueos de recurso por mantenimiento) |

Espejo exacto de la ficha de producto: tipos_recurso + atributos_tipo_recurso hacen para un hotel lo que categorias + atributos_categoria hacen para una ferretería.

## Reglas

<!-- Una regla por bloque. Formato:

### R1 · Título corto de la regla
**Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
"Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

Cuanto más aburrida y literal la frase, mejor test sale de ella. -->

### R1 · Un tipo de recurso configura atributos como una categoría (HU-064)
**Dado** un tipo de recurso, **cuando** le defino un atributo (`POST
/api/recursos/tipos/{id}/atributos` con `nombreCampo`, `etiqueta`, `tipo`, `obligatorio`,
`opciones`), **entonces** funciona igual que los de `atributos_categoria` en Inventario: es el
contrato que valida el JSONB `recursos.atributos`. Definir el mismo `nombreCampo` otra vez
actualiza la fila, no la duplica (`uq_atr_tipo_recurso`). Un atributo `LISTA`/`MULTILISTA` sin
`opciones` responde **422**.

### R2 · La unidad de tiempo del tipo (HU-064)
**Dado** un tipo, **cuando** fijo su `unidadTiempo`, **entonces** es una de `MINUTO`, `HORA`,
`NOCHE`, `DIA`, `SESION`; cualquier otra responde **422**. El tipo también lleva
`duracionMinimaMin` e `incrementoMin` (la granularidad de reserva).

### R3 · Los buffers se descuentan entre reservas (HU-064)
**Dado** un tipo con `bufferAntesMin` / `bufferDespuesMin`, **cuando** se calcula la
disponibilidad (HU-069), **entonces** la franja que una reserva bloquea de verdad es
`[inicio − bufferAntes, fin + bufferDespues]` — no solo la reservada. Sin buffers, la ventana
es exactamente la reserva.

### R4 · Un atributo obligatorio ausente es 422 (HU-064)
**Dado** un atributo `obligatorio`, **cuando** se guarda un recurso sin ese campo en su
`atributos`, **entonces** responde **422** nombrando el campo que falta. Un valor del tipo
equivocado (un texto donde va un número, una opción fuera de la lista) también es **422**. Un
atributo opcional ausente no molesta.

### R5 · El nombre del tipo es único por negocio
**Dado** un tipo `nombre` ya registrado, **cuando** creo otro con el mismo nombre en el mismo
negocio, **entonces** responde **409**. El mismo nombre en otro negocio sí entra.

### R6 · Aislamiento por negocio
**Dado** un tipo o atributo de un negocio, **cuando** otro negocio consulta, **entonces** no lo
ve: la RLS de `tipos_recurso` y `atributos_tipo_recurso` lo corta.

### R7 · El código del recurso es único por negocio (HU-065)
**Dado** un recurso `codigo` ya registrado, **cuando** creo otro con el mismo código en el
mismo negocio (`POST /api/recursos`), **entonces** responde **409** (`uq_recurso_codigo`). El
mismo código en otro negocio sí entra.

### R8 · MANTENIMIENTO saca al recurso de los disponibles (HU-065)
**Dado** un recurso, **cuando** cambio su `estado` a `MANTENIMIENTO` (o cualquiera que no sea
`DISPONIBLE`, vía `PATCH /api/recursos/{id}/estado`), **entonces** deja de aparecer en `GET
/api/recursos?soloDisponibles=true` y su `disponible` es `false`; sigue en el listado completo.

### R9 · No se borra un recurso con reservas futuras (HU-065)
**Dado** un recurso con reservas futuras (lo dice `servicio-reservas`), **cuando** intento
eliminarlo (`DELETE /api/recursos/{id}`), **entonces** responde **409**. Sin reservas futuras
se borra en blando (`eliminado_en`, `activo = false`) y deja de listarse.

### R10 · Los atributos del recurso se validan contra su tipo (HU-065)
**Dado** un recurso, **cuando** guardo sus `atributos`, **entonces** se validan contra los
`atributos_tipo_recurso` de su tipo con el mismo validador de HU-064: un obligatorio ausente o
un valor del tipo equivocado responde **422**; lo válido queda en el JSONB `recursos.atributos`.
Editar el recurso revalida.

### R11 · Un bloqueo saca al recurso de disponible en su periodo (HU-067)
**Dado** un recurso, **cuando** creo un bloqueo `[desde, hasta)` con un `motivo`
(`MANTENIMIENTO`/`LIMPIEZA`/`EVENTO`/`FERIADO`/`OTRO`), **entonces** una consulta de
disponibilidad de ese recurso sobre un periodo que toca el bloqueo responde
`disponible = false` y lista los bloqueos que lo pisan. Un periodo que solo comparte el
instante límite con el bloqueo (`[hasta, …)` contra `[…, hasta)`) no cuenta como solape: el
rango es medio abierto. El fin del bloqueo debe ser posterior al inicio, si no **422**.

### R12 · Dos bloqueos del mismo recurso no se pueden solapar (HU-067)
**Dado** un recurso con un bloqueo, **cuando** creo un segundo bloqueo que se solapa con él,
**entonces** PostgreSQL lo rechaza por `EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&)`
y la API responde **409**. Dos bloqueos adyacentes (uno empieza justo donde el otro termina) sí
se permiten. La garantía vive en la base, no en el servicio: el test inserta el caso prohibido
y espera la violación de PostgreSQL.

### R13 · Un bloqueo sobre reservas confirmadas se crea igual, pero se advierte (HU-067)
**Dado** un periodo con reservas confirmadas del recurso, **cuando** creo un bloqueo que lo
cubre, **entonces** el bloqueo queda creado y la respuesta lista las reservas afectadas
(código, entrada, salida, huésped) para que el administrador las gestione. No se rechaza: un
recurso en obra con huéspedes dentro es justamente lo que hay que hacer visible. El dato de las
reservas lo aporta `servicio-reservas` (por REST cuando exista; hasta entonces, un stub).

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

`RECURSOS_RECURSO_VER` para listar y consultar tipos, atributos, recursos, tarifas, bloqueos y
disponibilidad; `RECURSOS_RECURSO_CREAR` para crear un tipo o un recurso; `RECURSOS_RECURSO_EDITAR`
para editar el tipo/recurso, sus atributos, desactivar el tipo, cambiar el estado del recurso y
crear o levantar bloqueos; `RECURSOS_RECURSO_ELIMINAR` para eliminar un recurso. Sin el permiso,
**403**. Módulo `RECURSOS`, plan Básico o superior.

## Qué NO debe pasar

- **No** dos tipos con el mismo `nombre` en un negocio.
- **No** dos atributos con el mismo `nombre_campo` en un tipo.
- **No** un atributo `LISTA`/`MULTILISTA` sin `opciones`.
- **No** guardar un recurso cuyos `atributos` no cumplen el contrato de su tipo — se valida en
  la aplicación porque PostgreSQL no comprueba la forma del JSONB.
- **No** una `unidadTiempo` fuera de `MINUTO`/`HORA`/`NOCHE`/`DIA`/`SESION`.
- **No** dos bloqueos del mismo recurso con periodos que se solapan — lo corta el `EXCLUDE`
  de la base, no un `SELECT` previo del servicio.
- **No** rechazar un bloqueo porque haya reservas confirmadas: se crea y se devuelven las
  afectadas.
- **No** un bloqueo con `hasta` anterior o igual a `desde`.
