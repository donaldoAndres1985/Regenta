# E03 · Inventario · catálogo del patrón Venta directa

El catálogo configurable, el stock por bodega y el libro mayor de movimientos. Aquí vive la generalización por configuración.

| | |
|---|---|
| Historias | 11 |
| Puntos | 65 |
| Plan mínimo | Básico |

---

### HU-026 · Categorías del negocio con jerarquía

**Como** administrador del negocio, **quiero** crear mis propias categorías de producto **para** organizar el catálogo con las palabras de mi negocio, no con las del sistema

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 3 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `categorias` |
| Pantalla | `design/pantallas/CategoriasWeb.html` |
| Depende de | HU-006 (Row-Level Security activa en todas las tablas de negocio) · HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado mi negocio, cuando creo una categoría, entonces queda asociada a mi `negocio_id` y no la ve ningún otro negocio.
2. Dada una categoría con el mismo nombre y padre, cuando la creo de nuevo, entonces responde 409.
3. Dada una categoría con productos, cuando intento eliminarla, entonces responde 409 y me pide moverlos primero.
4. Dada una subcategoría, cuando la creo, entonces hereda los atributos de su padre marcados como heredables.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CategoriasWeb.html` en medidas y color.
- [ ] Las categorías nunca vienen precargadas en el código.
- [ ] Revisada en PR por otra persona.

---

### HU-027 · Definir los atributos que exige cada categoría

**Como** administrador del negocio, **quiero** decidir qué campos extra pide un producto según su categoría **para** que una droguería exija lote y vencimiento sin que nadie programe nada

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `atributos_categoria` |
| Pantalla | `design/pantallas/CategoriasWeb.html` |
| Depende de | HU-026 (Categorías del negocio con jerarquía) |
| Etiquetas | `inventario` · `backend` · `flutter` · `clave` |

> Esta es la historia que sostiene la tesis del producto: ferretería, papelería y droguería usan las mismas tablas y solo cambian estas filas.

**Criterios de aceptación**

1. Dada una categoría, cuando defino un atributo, entonces elijo su tipo entre TEXTO, NUMERO, DECIMAL, FECHA, BOOLEANO, LISTA y MULTILISTA.
2. Dado un atributo de tipo LISTA, cuando lo guardo sin opciones, entonces responde 422.
3. Dado un atributo marcado obligatorio, cuando ya existen productos sin ese valor, entonces se me advierte y los existentes conservan lo que tienen.
4. Dado un `nombre_campo` repetido en la misma categoría, cuando lo creo, entonces responde 409.
5. Dado un atributo con validación de rango, cuando un producto se sale del rango, entonces el guardado del producto falla con 422.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CategoriasWeb.html` en medidas y color.
- [ ] La pantalla muestra en vivo cómo quedará el formulario de producto.
- [ ] Revisada en PR por otra persona.

---

### HU-028 · Crear producto con validación de atributos dinámicos

**Como** administrador del negocio, **quiero** crear un producto y que el sistema me exija los campos de su categoría **para** no tener que pedir un desarrollo cada vez que aparece un tipo de producto nuevo

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `productos` · `categorias` · `atributos_categoria` · `unidades_medida` |
| Pantalla | `design/pantallas/ProductoWeb.html` |
| Depende de | HU-027 (Definir los atributos que exige cada categoría) |
| Etiquetas | `inventario` · `backend` · `flutter` · `clave` |

**Criterios de aceptación**

1. Dada la categoría «Medicamentos» que exige `lote` y `fecha_vencimiento`, cuando creo un producto sin ellos, entonces responde 422 nombrando los campos que faltan.
2. Dado un producto válido, cuando lo guardo, entonces sus atributos quedan en la columna JSONB `atributos` y no en columnas nuevas.
3. Dado un SKU repetido en mi negocio, cuando lo guardo, entonces responde 409.
4. Dado un código de barras repetido, cuando lo guardo, entonces responde 409 — pero dos productos sin código de barras conviven sin problema.
5. Dado un producto marcado `perecedero`, cuando lo guardo sin `maneja_lotes`, entonces el CHECK de la base lo rechaza.
6. Dado un atributo de tipo NUMERO, cuando envío texto, entonces responde 422.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ProductoWeb.html` en medidas y color.
- [ ] Índice GIN sobre `atributos` para poder filtrar por ellos.
- [ ] La validación contra `atributos_categoria` la hace el servicio: PostgreSQL no valida la forma del JSON.
- [ ] Revisada en PR por otra persona.

---

### HU-029 · Bodegas y existencias por producto y bodega

**Como** administrador del negocio, **quiero** saber cuánto tengo de cada producto en cada bodega **para** no vender lo que está en otra sede o ya está apartado

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `bodegas` · `existencias` |
| Pantalla | `design/pantallas/InventarioWeb.html` |
| Depende de | HU-028 (Crear producto con validación de atributos dinámicos) |
| Etiquetas | `inventario` · `backend` · `clave` |

> El stock es `(producto, bodega)`, no una columna de `productos`. Es la corrección más importante sobre el diseño original: moverlo después obliga a reescribir todas las consultas de venta.

**Criterios de aceptación**

1. Dado un producto en tres bodegas, cuando consulto su stock, entonces veo la cantidad por bodega y el total.
2. Dada una existencia, cuando consulto `cantidad_disponible`, entonces es `cantidad - cantidad_reservada` calculada por la base, no por la app.
3. Dado un negocio nuevo, cuando se crea, entonces tiene una bodega principal por defecto.
4. Dada una bodega con existencias, cuando intento eliminarla, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/InventarioWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-030 · Libro mayor de movimientos de inventario

**Como** auditor, **quiero** que todo cambio de stock quede registrado y no se pueda alterar **para** poder explicar cualquier descuadre sin adivinar

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-inventario` |
| Tablas | `movimientos_inventario` · `existencias` |
| Depende de | HU-029 (Bodegas y existencias por producto y bodega) |
| Etiquetas | `inventario` · `backend` · `clave` |

**Criterios de aceptación**

1. Dado cualquier cambio de stock, cuando ocurre, entonces se inserta un movimiento con tipo, signo, cantidad, saldo posterior, origen y usuario.
2. Dado un movimiento ya insertado, cuando alguien intenta actualizarlo o borrarlo, entonces la operación se rechaza — la tabla es append-only.
3. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces choca contra `UNIQUE(negocio_id, idempotency_key)` y el stock no se descuenta dos veces.
4. Dado el libro completo, cuando sumo los movimientos de un producto en una bodega, entonces el resultado coincide exactamente con `existencias.cantidad`.
5. Dado un error de registro, cuando hay que corregirlo, entonces se hace con un movimiento contrario, no editando el original.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Particionado mensual: es la tabla que más crece del sistema.
- [ ] Test que reconstruye el saldo desde el libro y lo compara con la proyección.
- [ ] Revisada en PR por otra persona.

---

### HU-031 · Lotes y fechas de vencimiento

**Como** administrador de droguería, **quiero** controlar lotes con su fecha de vencimiento **para** cumplir la norma sanitaria y no vender producto vencido

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `lotes` · `existencias_lote` |
| Depende de | HU-030 (Libro mayor de movimientos de inventario) |
| Etiquetas | `inventario` · `backend` |

**Criterios de aceptación**

1. Dado un producto que maneja lotes, cuando entra mercancía, entonces se exige el código de lote.
2. Dado un lote, cuando consulto su existencia, entonces la veo desglosada por bodega.
3. Dado un producto perecedero, cuando se vende, entonces el sistema sugiere el lote de vencimiento más próximo (FEFO).
4. Dado un lote vencido, cuando intento venderlo, entonces se bloquea salvo autorización explícita, y esa autorización queda registrada.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-032 · Traslados entre bodegas

**Como** administrador del negocio, **quiero** mover mercancía de una bodega a otra con trazabilidad **para** que lo que sale de una sede y aún no llega a la otra no desaparezca del inventario

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `traslados` · `traslado_lineas` · `movimientos_inventario` |
| Depende de | HU-030 (Libro mayor de movimientos de inventario) |
| Etiquetas | `inventario` · `backend` |

**Criterios de aceptación**

1. Dado un traslado en estado `EN_TRANSITO`, cuando consulto el inventario, entonces la mercancía figura en la bodega de tránsito, no perdida.
2. Dado un traslado, cuando lo recibo, entonces se generan dos movimientos: salida de origen y entrada en destino.
3. Dada una cantidad recibida mayor que la enviada, cuando la registro, entonces se rechaza.
4. Dado un traslado con bodega origen igual a la destino, cuando lo creo, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-033 · Ajustes de inventario con motivo

**Como** administrador del negocio, **quiero** ajustar el stock tras un conteo físico o una avería **para** que el sistema refleje lo que hay de verdad, con constancia de por qué cambió

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `ajustes_inventario` · `ajuste_lineas` · `movimientos_inventario` |
| Depende de | HU-030 (Libro mayor de movimientos de inventario) |
| Etiquetas | `inventario` · `backend` |

**Criterios de aceptación**

1. Dado un ajuste por conteo físico, cuando lo cargo, entonces registro cantidad de sistema y cantidad física, y la diferencia se calcula sola.
2. Dado un ajuste en borrador, cuando lo aplico, entonces se generan los movimientos y ya no se puede editar.
3. Dado un ajuste sin motivo, cuando intento aplicarlo, entonces responde 422.
4. Dado un ajuste aplicado, cuando reviso la auditoría, entonces consta quién lo hizo y quién lo aprobó.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-034 · Reserva y liberación de stock para la saga de ventas

**Como** arquitecto, **quiero** que Inventario pueda apartar stock temporalmente y liberarlo si la venta no se concreta **para** que dos cajas no vendan la última unidad al mismo tiempo

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-inventario` |
| Tablas | `reservas_stock` · `existencias` |
| Depende de | HU-030 (Libro mayor de movimientos de inventario) |
| Etiquetas | `inventario` · `backend` · `eventos` · `clave` |

> Sin la expiración, si Ventas muere entre la solicitud y la confirmación, el stock queda bloqueado para siempre.

**Criterios de aceptación**

1. Dado un evento `solicitar_reserva_stock` con stock suficiente, cuando llega, entonces se crea la reserva, sube `cantidad_reservada` y se publica `stock_reservado`.
2. Dado stock insuficiente, cuando llega la solicitud, entonces se publica `stock_reserva_fallida` con el detalle de qué faltó.
3. Dada una reserva que expira sin confirmarse, cuando pasa el tiempo, entonces un job la libera y `cantidad_reservada` baja.
4. Dadas dos solicitudes concurrentes por la última unidad, cuando llegan a la vez, entonces solo una obtiene la reserva.
5. Dada una reserva confirmada, cuando llega `venta_completada`, entonces se convierte en salida real y se registra el movimiento.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Test de concurrencia real con dos hilos peleando por la misma unidad.
- [ ] Revisada en PR por otra persona.

---

### HU-035 · Búsqueda de productos y escaneo de código de barras

**Como** vendedor, **quiero** encontrar un producto por nombre, SKU o escaneando su código **para** no demorar la venta buscando en una lista larga

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Paquete Flutter | `inventario` |
| Tablas | `productos` · `producto_codigos` |
| Pantalla | `design/pantallas/InventarioMovil.html` |
| Depende de | HU-028 (Crear producto con validación de atributos dinámicos) |
| Etiquetas | `inventario` · `flutter` |

**Criterios de aceptación**

1. Dado el buscador, cuando escribo tres caracteres, entonces filtra por nombre, SKU o código de barras.
2. Dado un celular Android, cuando uso el escáner, entonces al leer un código válido abre el producto directamente.
3. Dado el navegador, cuando el acceso a la cámara falla o no está disponible, entonces siempre hay captura manual del código como alternativa.
4. Dado un código de barras alterno (caja de 12), cuando lo escaneo, entonces se resuelve al producto con su factor de conversión.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/InventarioMovil.html` en medidas y color.
- [ ] La alternativa manual no es opcional en web: es el camino principal.
- [ ] Revisada en PR por otra persona.

---

### HU-036 · Listas de precios y precios por volumen

**Como** administrador del negocio, **quiero** manejar precios distintos por lista de precios y por cantidad **para** poder vender al mayorista a otro precio sin duplicar el catálogo

| | |
|---|---|
| Épica | `E03` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-inventario` |
| Paquete Flutter | `inventario` |
| Tablas | `listas_precios` · `precios_producto` |
| Depende de | HU-028 (Crear producto con validación de atributos dinámicos) |
| Etiquetas | `inventario` · `backend` |

**Criterios de aceptación**

1. Dado un cliente con lista de precios asignada, cuando lo selecciono en una venta, entonces los precios cambian solos.
2. Dado un precio por volumen desde 12 unidades, cuando vendo 15, entonces aplica ese precio.
3. Dada una lista con vigencia vencida, cuando intento usarla, entonces no aparece entre las opciones.
4. Dado un descuento mayor al máximo permitido de la lista, cuando lo aplico, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---
