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
| Historias | HU-064 (Tipos de recurso con atributos configurables) · HU-065 (Administrar recursos individuales) |

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

`RECURSOS_RECURSO_VER` para listar y consultar tipos y sus atributos; `RECURSOS_RECURSO_CREAR`
para crear un tipo; `RECURSOS_RECURSO_EDITAR` para editar el tipo, sus atributos y
desactivarlo. Sin el permiso, **403**. Módulo `RECURSOS`, plan Básico o superior.

## Qué NO debe pasar

- **No** dos tipos con el mismo `nombre` en un negocio.
- **No** dos atributos con el mismo `nombre_campo` en un tipo.
- **No** un atributo `LISTA`/`MULTILISTA` sin `opciones`.
- **No** guardar un recurso cuyos `atributos` no cumplen el contrato de su tipo — se valida en
  la aplicación porque PostgreSQL no comprueba la forma del JSONB.
- **No** una `unidadTiempo` fuera de `MINUTO`/`HORA`/`NOCHE`/`DIA`/`SESION`.
