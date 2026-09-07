# Comportamiento · Inventario

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/InventarioMovil.html` |
| Web | `design/pantallas/InventarioWeb.html` |
| Paquete Flutter | `packages/inventario` |
| Microservicio | `servicio-inventario` |
| Tablas | `inventario.productos` · `existencias` · `bodegas` · `categorias` · `producto_codigos` |
| Historias | HU-029 (Bodegas y existencias por producto y bodega) · HU-035 (Búsqueda de productos y escaneo de código de barras) |

El stock es (producto, bodega), no una columna de productos. Es la corrección más importante sobre el documento original.

## Reglas

> Lo de HU-035 se decidió al implementarla (no estaba escrito). El resto sigue `_Sin definir._`.

### R1 · La búsqueda filtra desde el tercer carácter

**Dado** el buscador vacío, **cuando** abro la pantalla, **entonces** se listan todos los
productos activos ordenados por nombre. **Cuando** escribo uno o dos caracteres, **entonces**
la lista no cambia (sigue mostrando todo, no se llama al backend). **Cuando** escribo el
tercero, **entonces** tras un rebote de 300 ms se pide al backend
`GET /api/inventario/productos/buscar?q=…` y la lista pasa a los que coinciden por **nombre,
SKU, código de barras propio o un código alterno**. Al borrar el campo vuelve a todos.

### R2 · El botón de escanear resuelve un código y devuelve el producto

**Dado** un dispositivo con cámara y permiso (Android/iOS), **cuando** toco el botón de
escanear, **entonces** se abre la cámara; al leer un código se cierra y se llama a
`GET /api/inventario/productos/codigo/{codigo}`. **Si resuelve**, se invoca
`onProductoSeleccionado(productoId, factor)` — quien monta la pantalla decide si abre la ficha
o lo agrega a la venta. **Si no resuelve** (404), un aviso "Ningún producto tiene el código
«…»" y no se hace nada más.

### R3 · La captura manual del código no es opcional en web

**Dado** un navegador (o cámara denegada, o sin cámara), **cuando** toco el botón de escanear,
**entonces** se abre directamente un diálogo para teclear el código —es el camino principal,
no una opción escondida—. **Dado** Android con cámara, **cuando** la cámara falla al abrir,
**entonces** también se cae a ese diálogo. El código tecleado se resuelve igual que R2.

### R4 · El código alterno arrastra su factor de conversión

**Dado** un código alterno (la caja de 12), **cuando** se resuelve, **entonces**
`onProductoSeleccionado` recibe `factor = 12`; el código propio del producto resuelve con
`factor = 1`.

## Al abrir

Se listan todos los productos activos (sin término), ordenados por nombre. El foco va al campo
de búsqueda. Mientras carga, un indicador centrado. El filtro de categoría / «bajo mínimo»
elegido se mantiene en memoria mientras la pantalla vive (no se persiste entre sesiones —
`_Sin definir._` si debería).

La **bodega** y los **chips de categoría con nombre** del mockup quedan pendientes: el chip
"Bodega Centro" necesita el selector de bodega (HU-029) y los chips "Herramientas /
Tornillería / …" necesitan traer el árbol de categorías (HU-026). Por ahora la barra de chips
tiene solo **Todas** y **Bajo mínimo** (este último activa `soloBajoMinimo`).

## Validaciones

El término de búsqueda de 1–2 caracteres no se envía (se muestra todo). Con 3 o más, el
backend responde 422 si por alguna razón llega uno más corto; la app no debería provocarlo.

## Estados vacíos y de error

- **Sin resultados con término**: "No hay productos que coincidan con «término»".
- **Sin resultados sin término**: "No hay productos todavía."
- **Sin conexión**: "Sin conexión. Revisa tu red e intenta de nuevo." con botón **Reintentar**.
- **403**: "No tienes permiso para ver el inventario."
- **5xx**: "El servidor tuvo un problema. Intenta en un momento." con **Reintentar**.

Los tres primeros son visualmente el mismo componente (texto centrado gris, con o sin botón);
lo que cambia es el texto y si hay acción.

## Sin conexión

La búsqueda es lectura: **no se encola**. Se muestra el error de red con Reintentar. No hay
caché local de inventario en esta historia (`_Sin definir._` si debería haberla).

## Móvil y web

El único punto donde se separan hoy: en Android el botón de escanear abre la cámara; en web
abre siempre el diálogo de código a mano (R3). El resto es la misma pantalla.

## Permisos

Requiere `INVENTARIO_PRODUCTO_VER`. Si el backend responde 403, la pantalla muestra el mensaje
de permiso; no se oculta la pantalla entera (de eso se encarga la guardia de rutas del núcleo,
HU-107). Agregar un código alterno requiere `INVENTARIO_PRODUCTO_CREAR`.

## Qué NO debe pasar

- Que escribir rápido dispare una búsqueda por cada tecla: hay rebote de 300 ms y cada
  búsqueda descarta el resultado de una anterior más lenta.
- Que cancelar la cámara a propósito (Android) fuerce el diálogo manual: solo se cae a manual
  si la cámara **no está disponible** o **falla**.

## Pendiente (`_Sin definir._`)

- Selector de bodega y su efecto en el stock mostrado.
- Chips de categoría con nombre.
- Persistir filtros entre sesiones.
- Caché offline del catálogo.
- Qué hace exactamente `onProductoSeleccionado` en el shell (¿ficha? ¿agregar a venta?).
