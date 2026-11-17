# E03 · Inventario · catálogo del patrón Venta directa

El catálogo configurable, el stock por bodega y el libro mayor de movimientos. Aquí vive la generalización por configuración.

| | |
|---|---|
| Historias | 29 |
| Puntos | 140 |
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

## Paso de Sara POS a Regenta (HU-147 a HU-163)

> Borrador 2026-11-16, pendiente de aprobación. Modelo, glosario e invariantes en
> `docs/dominio/inventario.md`: las historias citan las invariantes (`I1`…`I21`) y no las
> repiten. Etapas aprobadas: 1 IVA · 2 ficha · 3 inventario general · 4 unidades y fracciones ·
> 5 ajustes · 6 detallar · 7 importación. Cada historia es un PR directo contra `main`.

---

### HU-147 · Tarifa de IVA en el producto y precio con IVA incluido

**Como** administrador del negocio, **quiero** que cada producto tenga su tarifa de IVA y que su precio sea el de la etiqueta **para** que el POS cobre el impuesto correcto

| | |
|---|---|
| Épica | `E03` · Etapa 1 · IVA |
| Puntos | 5 |
| Módulo | `inventario` |
| Tablas | `inventario.productos` |
| Invariantes | `I1` · `I2` · `I6` · `I21` |
| Etiquetas | `inventario` · `backend` · `migracion` |

**Criterios de aceptación**

1. Dado un producto nuevo, cuando lo creo sin tarifa de IVA, entonces se rechaza con «Elige la tarifa de IVA del producto.».
2. Dado un producto de $ 35.500 con `IVA_19`, cuando consulto su precio, entonces la base es $ 29.831,93 y el IVA $ 5.668,07.
3. Dado un producto de $ 35.500 `EXENTO` o `EXCLUIDO`, cuando consulto su precio, entonces la base es $ 35.500 y el IVA $ 0.
4. Dado un producto de $ 35.500 con `IVA_19`, cuando cambio su tarifa a `IVA_5`, entonces el precio sigue en $ 35.500 y la base pasa a $ 33.809,52.
5. Dados los productos existentes antes de la migración, cuando se aplica, entonces todos quedan en `IVA_19` con «tarifa por revisar» y su precio no cambia.
6. Dados dos negocios, cuando consulto los productos del negocio B, entonces no veo tarifas ni marcas de revisión del negocio A.

---

### HU-148 · La venta toma el IVA del catálogo, no del POS

**Como** dueño del negocio, **quiero** que el backend decida el precio y el IVA de cada línea **para** que nadie venda con IVA 0 por error

| | |
|---|---|
| Épica | `E03` · Etapa 1 · IVA |
| Puntos | 5 |
| Módulos | `inventario` (expone en `InventarioApi`) · `ventas` (usa y copia en la línea) |
| Tablas | `ventas.venta_lineas` · `ventas.cotizacion_lineas` |
| Depende de | HU-147 · HU-018 (régimen fiscal) |
| Invariantes | `I3` · `I4` · `I5` |
| Etiquetas | `ventas` · `inventario` · `backend` |

**Criterios de aceptación**

1. Dado un negocio responsable de IVA y un producto `IVA_19` de $ 35.500, cuando agrego una unidad a una venta, entonces la línea guarda tarifa `IVA_19`, base $ 29.831,93 e IVA $ 5.668,07, aunque el POS haya mandado otro porcentaje.
2. Dado un negocio **no** responsable de IVA, cuando agrego ese producto, entonces la línea lleva IVA $ 0 y base $ 35.500.
3. Dada una línea con descuento del 10 %, cuando se calcula, entonces el total es $ 31.950 y base e IVA se recalculan en proporción.
4. Dada una venta ya creada, cuando después cambio la tarifa o el precio del producto, entonces la venta no cambia.
5. Dada una cotización, cuando agrego líneas, entonces siguen la misma regla que la venta.
6. Dada una venta con productos de varias tarifas, cuando consulto sus totales, entonces veo base e IVA por tarifa.

---

### HU-149 · El ticket y la factura muestran el IVA real

**Como** cajero, **quiero** que el ticket muestre la base y el IVA por tarifa **para** entregarle al cliente un comprobante correcto

| | |
|---|---|
| Épica | `E03` · Etapa 1 · IVA |
| Puntos | 3 |
| Módulos | `ventas` (Flutter: POS y ticket) · `facturacion` (recibe las tarifas reales) |
| Depende de | HU-148 |
| Invariantes | `I2` · `I4` |
| Etiquetas | `ventas` · `facturacion` · `flutter` |

**Criterios de aceptación**

1. Dado el POS, cuando agrego un producto, entonces la columna «% IVA» muestra la tarifa que devolvió el backend, no un 0 fijo.
2. Dado un ticket con productos al 19 % y exentos, cuando se imprime, entonces muestra subtotal (base), IVA y total, con el desglose por tarifa.
3. Dada una venta cobrada, cuando se emite su factura, entonces cada línea lleva su tarifa real (19 %, 5 %, exento o excluido), y el excluido no lleva tributo.

---

### HU-150 · Editar un producto desde la ficha

**Como** administrador, **quiero** editar nombre, categoría, unidad, tarifa, precio y costo de un producto **para** corregir el catálogo sin pedir ayuda técnica

| | |
|---|---|
| Épica | `E03` · Etapa 2 · Ficha |
| Puntos | 5 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_PRODUCTO_EDITAR` (nuevo) |
| Invariantes | `I2` · `I6` · `I7` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto, cuando cambio su precio a $ 38.000, entonces se guarda y la ficha muestra base, IVA, margen `(base − costo) / costo` y beneficio `(base − costo) / base`.
2. Dado un producto, cuando le pongo un SKU o un código de barras que ya tiene otro producto del negocio, entonces se rechaza con «Ese SKU ya lo usa otro producto.» o «Ese código de barras ya lo usa otro producto.».
3. Dado un producto con «tarifa por revisar», cuando guardo la ficha con su tarifa, entonces la marca desaparece.
4. Dado un usuario sin `INVENTARIO_PRODUCTO_EDITAR`, cuando intenta editar, entonces recibe 403 y la ficha se ve sin poder guardar.
5. Dado que dos personas editan el mismo producto, cuando la segunda guarda con una versión vieja, entonces se rechaza con «Alguien cambió este producto; recarga antes de guardar.».

---

### HU-151 · Desactivar y reactivar un producto

**Como** administrador, **quiero** desactivar un producto que ya no vendo **para** que no salga en el POS sin perder su historial

| | |
|---|---|
| Épica | `E03` · Etapa 2 · Ficha |
| Puntos | 2 |
| Módulos | `inventario` · `ventas` (búsqueda del POS) |
| Permiso | `INVENTARIO_PRODUCTO_EDITAR` |
| Invariantes | `I8` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto activo, cuando pido desactivarlo, entonces se pide confirmación con el texto de I8, y si tiene existencia se avisa cuántas unidades hay y en qué bodega.
2. Dado un producto inactivo, cuando busco en el POS, entonces no aparece, ni por nombre ni por código de barras.
3. Dado un producto inactivo, cuando miro el inventario general con el filtro «inactivos», su historial o un reporte, entonces sí aparece.
4. Dado un producto inactivo, cuando lo reactivo, entonces vuelve a aparecer en el POS.
5. Dado cualquier producto, cuando busco una forma de borrarlo, entonces no existe ni en la API ni en la app.

---

### HU-152 · Ubicación del producto en cada bodega

**Como** vendedor, **quiero** ver en qué estante está un producto **para** encontrarlo rápido en el mostrador

| | |
|---|---|
| Épica | `E03` · Etapa 2 · Ficha |
| Puntos | 2 |
| Módulos | `inventario` · Paquetes Flutter `inventario` y `ventas` |
| Tablas | `inventario.existencias` |
| Invariantes | `I9` · `I21` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto en dos bodegas, cuando le pongo «Pasillo 3 – E2» en Principal y «Estiba 5» en Bodega 2, entonces cada bodega guarda la suya.
2. Dado un producto con ubicación en la bodega de la venta, cuando abro su detalle en el POS, entonces veo esa ubicación.
3. Dada una ubicación de más de 60 caracteres, cuando la guardo, entonces se rechaza.
4. Dados dos negocios, cuando consulto ubicaciones, entonces no veo las del otro negocio.

---

### HU-153 · Inventario general con filtros y valor

**Como** dueño, **quiero** ver todos mis productos con su existencia y su valor **para** saber cuánto tengo invertido y qué hay que revisar

| | |
|---|---|
| Épica | `E03` · Etapa 3 · Inventario general |
| Puntos | 5 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_EXISTENCIA_VER` |
| Invariantes | `I2` · `I6` · `I8` · `I10` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el inventario general, cuando lo abro, entonces veo por producto: SKU, nombre, categoría, tarifa, existencia, costo promedio, precio, valor a costo y valor a precio, paginado.
2. Dado el filtro por categoría, estado (activos/inactivos), «tarifa por revisar» o «existencia negativa», cuando lo aplico, entonces solo veo esos productos, y los totales de valor se recalculan sobre lo filtrado.
3. Dado un texto de búsqueda, cuando escribo varias palabras, entonces encuentra los productos que contienen todas, en cualquier orden.
4. Dada una bodega elegida, cuando miro el listado, entonces existencias y valores son los de esa bodega; sin elegir, son la suma de todas.

---

### HU-154 · Cambiar la tarifa de IVA en bloque

**Como** administrador, **quiero** cambiar la tarifa de todos los productos de una categoría o de los que seleccione **para** corregir en minutos los 2.736 productos migrados

| | |
|---|---|
| Épica | `E03` · Etapa 3 · Inventario general |
| Puntos | 3 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_PRODUCTO_EDITAR` |
| Depende de | HU-147 · HU-153 |
| Invariantes | `I2` · `I6` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dados productos seleccionados (o una categoría completa), cuando les aplico `IVA_5`, entonces se pide confirmar «¿Cambiar la tarifa de 37 productos a IVA 5 %? El precio no cambia.» y al aceptar todos quedan en `IVA_5`, sin «tarifa por revisar», con el precio igual.
2. Dados productos marcados «tarifa por revisar» que ya están bien en 19 %, cuando los confirmo en bloque, entonces solo se quita la marca.
3. Dado un cambio en bloque que falla en un producto, cuando ocurre, entonces no se cambia ninguno.

---

### HU-155 · Unidades de medida del negocio

**Como** administrador, **quiero** crear y editar unidades (UND, GLN, KG, MTS) con su factor **para** que las fracciones y el detallado sepan en qué se mide cada producto

| | |
|---|---|
| Épica | `E03` · Etapa 4 · Unidades y fracciones |
| Puntos | 3 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Tablas | `inventario.unidades_medida` (existe) |
| Invariantes | `I16` · `I21` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el negocio, cuando creo la unidad «GLN · Galón» que admite decimales, entonces aparece para elegir en la ficha del producto.
2. Dada una unidad con el mismo código que otra del negocio, cuando la guardo, entonces se rechaza con «Ya existe la unidad GLN.».
3. Dada una unidad que usan productos, cuando intento desactivarla, entonces se rechaza y dice cuántos productos la usan.
4. Dados dos negocios, cuando listo unidades, entonces no veo las del otro.

---

### HU-156 · Tablas de fracciones y asignación de productos

**Como** administrador, **quiero** armar una tabla de fracciones con sus precios y asignarle muchos productos **para** cambiar un precio una sola vez y que aplique a todos

| | |
|---|---|
| Épica | `E03` · Etapa 4 · Unidades y fracciones |
| Puntos | 8 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_FRACCION_GESTIONAR` (nuevo) |
| Tablas | `inventario.tablas_fracciones` · `inventario.fracciones` (nuevas) |
| Invariantes | `I12` · `I13` · `I14` · `I21` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el negocio, cuando creo la tabla «Esmalte Pintulux galón» con ½ (0,50, $ 34.000), ¾ (0,75, $ 48.000), ¼ (0,25, $ 18.000) y ⅛ (0,125, $ 10.000), entonces queda guardada con esos precios exactos, sin calcular proporcionales.
2. Dada una fracción con factor 0, 1 o mayor que 1, o una medida repetida en la tabla, cuando la guardo, entonces se rechaza.
3. Dada la búsqueda «pintulux galon» (o la marca Pintulux, o una selección manual), cuando asigno los 20 resultados a la tabla, entonces los 20 quedan con sus fracciones.
4. Dada la tabla con 20 productos, cuando cambio el ¼ a $ 19.000, entonces los 20 cobran $ 19.000 por ¼.
5. Dado un producto ya asignado a otra tabla, cuando lo asigno a esta, entonces se pide confirmar el cambio de tabla.
6. Dados dos negocios, cuando listo tablas, entonces no veo las del otro ni puedo asignarles productos ajenos.

---

### HU-157 · Precio propio de una fracción para un producto

**Como** administrador, **quiero** que un producto tenga un precio distinto en una fracción **para** manejar el color o la referencia que se sale de la regla

| | |
|---|---|
| Épica | `E03` · Etapa 4 · Unidades y fracciones |
| Puntos | 2 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Depende de | HU-156 |
| Invariantes | `I14` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado Pintulux blanco en la tabla con ¼ a $ 18.000, cuando le pongo precio propio de ¼ a $ 20.000, entonces el blanco cobra $ 20.000 y los demás siguen en $ 18.000.
2. Dado ese precio propio, cuando cambio el ¼ de la tabla, entonces el blanco sigue en $ 20.000.
3. Dado el precio propio, cuando lo quito, entonces el blanco vuelve al precio de la tabla.
4. Dada la ficha del producto, cuando la abro, entonces veo cada fracción con su precio y si viene de la tabla o es propio.

---

### HU-158 · Vender fracciones en el POS con datos reales

**Como** vendedor, **quiero** elegir ¼ de galón de un producto y que se cobre su precio **para** vender al menudeo sin calcular a mano

| | |
|---|---|
| Épica | `E03` · Etapa 4 · Unidades y fracciones |
| Puntos | 5 |
| Módulos | `inventario` (fracciones en `InventarioApi`) · `ventas` (línea y POS) |
| Depende de | HU-156 · HU-157 · HU-148 |
| Invariantes | `I4` · `I13` · `I14` · `I15` |
| Etiquetas | `ventas` · `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado Pintulux rojo en la tabla, cuando pulso `Ctrl+D` en el POS, entonces veo solo sus fracciones con sus precios.
2. Dado un producto sin fracciones, cuando pulso `Ctrl+D`, entonces veo «Este producto no se vende fraccionado.».
3. Dado ¼ de Pintulux rojo con cantidad 3, cuando cobro, entonces la línea cobra 3 × $ 18.000 = $ 54.000, con el IVA de la tarifa del producto, y descuenta 0,75 de su existencia.
4. Dada una línea de fracción, cuando el backend la recibe, entonces el precio lo toma del catálogo, no del POS.
5. Dado el POS, cuando busco las fracciones escritas en el código (`unidadParaProducto` y la lista fija de galones), entonces ya no existen.

---

### HU-159 · Ajustes de existencia desde la app

**Como** administrador, **quiero** sumar, restar o fijar por conteo físico la existencia de un producto con un motivo **para** corregir el inventario dejando rastro

| | |
|---|---|
| Épica | `E03` · Etapa 5 · Ajustes |
| Puntos | 5 |
| Módulo | `inventario` (backend HU-033 existe) · Paquete Flutter `inventario` |
| Permisos | `INVENTARIO_AJUSTE_CREAR` · `INVENTARIO_AJUSTE_APLICAR` |
| Invariantes | `I10` · `I11` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto con 8 unidades, cuando registro una salida de 5 por «Avería», entonces queda en 3 y el historial muestra la salida con motivo, usuario y fecha.
2. Dado un producto con 3 unidades, cuando intento restar 5, entonces se rechaza con «Solo hay 3 unidades de {producto} en {bodega}. No puedes sacar 5.» y nada cambia.
3. Dado un producto con existencia −2, cuando hago un conteo físico de 4, entonces queda en 4 con un movimiento de entrada de 6.
4. Dado un ajuste, cuando lo guardo, entonces no toca el stock mínimo ni el máximo del producto.

---

### HU-160 · Historial de movimientos del producto en la app

**Como** administrador, **quiero** ver por qué cambió la existencia de un producto **para** encontrar errores o pérdidas

| | |
|---|---|
| Épica | `E03` · Etapa 5 · Ajustes |
| Puntos | 3 |
| Módulo | Paquete Flutter `inventario` (backend HU-030 existe) |
| Permiso | `INVENTARIO_MOVIMIENTO_VER` |
| Invariantes | `I11` |
| Etiquetas | `inventario` · `flutter` |

**Criterios de aceptación**

1. Dado un producto, cuando abro su historial, entonces veo cada movimiento con fecha, tipo (venta, compra, ajuste, traslado, detallado), cantidad con signo, saldo después del movimiento, motivo y usuario, del más nuevo al más viejo.
2. Dado un rango de fechas o una bodega, cuando filtro, entonces solo veo esos movimientos.
3. Dado un movimiento, cuando lo miro, entonces no hay forma de editarlo ni de borrarlo.

---

### HU-161 · Equivalencias de detallado

**Como** administrador, **quiero** guardar que 1 cuñete equivale a 5 galones de otro producto **para** detallar sin escribir el factor cada vez

| | |
|---|---|
| Épica | `E03` · Etapa 6 · Detallar |
| Puntos | 3 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_DETALLAR` (nuevo) |
| Tablas | `inventario.equivalencias_detallado` (nueva) |
| Depende de | HU-155 |
| Invariantes | `I16` · `I21` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dados Pintulux blanco cuñete y Pintulux blanco galón, ambos en galones, cuando guardo «1 cuñete = 5 galones», entonces queda la equivalencia.
2. Dados dos productos con distinta unidad base, o el mismo producto como origen y destino, o un factor ≤ 0, cuando guardo, entonces se rechaza con el motivo.
3. Dada la búsqueda «pintulux cuñete», cuando replico la equivalencia a los 20 colores, entonces cada cuñete queda unido al galón de su mismo color, y los que no tienen par se listan sin crear nada.
4. Dado un par que ya tiene equivalencia, cuando creo otra, entonces se rechaza.

---

### HU-162 · Detallar un producto

**Como** vendedor, **quiero** abrir un cuñete y que sus galones entren al inventario **para** venderlos sueltos

| | |
|---|---|
| Épica | `E03` · Etapa 6 · Detallar |
| Puntos | 5 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_DETALLAR` |
| Depende de | HU-161 · HU-159 |
| Invariantes | `I10` · `I11` · `I17` · `I18` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dados 2 cuñetes con costo promedio $ 250.000 y 3 galones con costo $ 48.000, cuando detallo 1 cuñete, entonces quedan 1 cuñete y 8 galones, y el costo del galón pasa a `(3 × 48.000 + 5 × 50.000) / 8 = $ 49.250`.
2. Dado un detallado, cuando miro el historial, entonces hay una salida del cuñete y una entrada del galón, ambas con motivo «Detallado», el mismo usuario y la misma fecha.
3. Dado 1 cuñete en existencia, cuando intento detallar 2, entonces se rechaza y nada cambia.
4. Dado un fallo al escribir la entrada del galón, cuando ocurre, entonces tampoco se descuenta el cuñete.
5. Dado un producto sin equivalencia, cuando intento detallarlo, entonces se avisa «Este producto no tiene equivalencia de detallado.».

---

### HU-163 · Detallar desde el POS cuando falta existencia

**Como** vendedor, **quiero** que el POS me ofrezca abrir un cuñete cuando no hay galones **para** no perder la venta

| | |
|---|---|
| Épica | `E03` · Etapa 6 · Detallar |
| Puntos | 3 |
| Módulos | `inventario` · Paquete Flutter `ventas` |
| Depende de | HU-162 |
| Invariantes | `I10` · `I17` |
| Etiquetas | `ventas` · `inventario` · `flutter` |

**Criterios de aceptación**

1. Dado Pintulux blanco galón sin existencia y 2 cuñetes con equivalencia, cuando lo agrego al POS, entonces se ofrece «¿Detallar 1 cuñete → 5 galones?».
2. Dada esa oferta, cuando la acepto, entonces se detalla como en HU-162 y la línea queda vendible.
3. Dado un usuario sin `INVENTARIO_DETALLAR`, cuando falta existencia, entonces no se ofrece detallar.
4. Dado un producto sin equivalencia que lo tenga como destino, cuando falta existencia, entonces el POS se comporta como hoy.

---

### HU-164 · Importar productos desde Excel

**Como** administrador de un negocio nuevo, **quiero** cargar mis productos desde un Excel **para** no crearlos uno por uno

| | |
|---|---|
| Épica | `E03` · Etapa 7 · Importación |
| Puntos | 8 |
| Módulo | `inventario` · Paquete Flutter `inventario` |
| Permiso | `INVENTARIO_IMPORTAR` (nuevo) |
| Invariantes | `I1` · `I2` · `I7` · `I19` · `I20` · `I21` |
| Etiquetas | `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el botón «Descargar plantilla», cuando lo uso, entonces obtengo un `.xlsx` con las columnas SKU, código de barras, nombre, categoría, unidad, tarifa de IVA, precio con IVA, costo, existencia inicial y bodega.
2. Dado un archivo con 100 filas, cuando lo subo, entonces veo una vista previa con cada fila y sus errores: SKU o código de barras repetido (en el archivo o en el negocio), tarifa inválida, categoría o unidad inexistente, obligatorios vacíos, números mal escritos.
3. Dada una vista previa con errores, cuando intento importar, entonces no se graba nada (ver P3).
4. Dada una vista previa sin errores, cuando importo, entonces se crean los productos y la existencia de cada uno entra como ajuste de «carga inicial» en el historial.
5. Dados dos negocios, cuando uno importa, entonces el chequeo de duplicados es solo contra sus propios productos.

---
