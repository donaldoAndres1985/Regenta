# Contexto: Inventario

> Estado: **borrador, pendiente de aprobación** · 2026-11-16. Las 11 decisiones de la sección 8
> están aprobadas una por una por el usuario; falta aprobar el documento completo y las historias.
> Origen: triage de `FUNCIONALIDADES.md` § "Grupo: Productos e Inventario" (Sara POS) contra el
> módulo `com.regenta.inventario`. Este documento es la referencia de dominio: las historias de
> `historias/epicas/E03-inventario.md` (HU-147 a HU-164) lo citan y no lo repiten. Si una regla
> cambia, cambia primero aquí.

## 1. Propósito

Saber qué vende el negocio, a qué precio y con qué impuesto, cuánto hay de cada cosa en cada
bodega y por qué cambió cada unidad. Inventario es el **catálogo** del patrón Venta directa: no
vende ni cobra; Ventas le pregunta precio, tarifa y existencia, y le pide reservar y descontar.

Lo que ya existía antes de esta especificación y **no cambia**: categorías y atributos
(HU-026/027), bodegas y existencias con costo promedio (HU-029), libro mayor de movimientos solo
de agregar (HU-030), lotes y vencimientos (HU-031), traslados (HU-032), ajustes con motivo
(HU-033), reservas para la saga de ventas (HU-034), búsqueda y escaneo (HU-035), listas de
precios (HU-036, sin pantalla: ver Pendientes).

## 2. Lenguaje ubicuo

| Término | Definición | En Sara POS |
|---|---|---|
| **Producto** | Lo que se vende y se cuenta. Tiene SKU, nombre, categoría, unidad, tarifa de IVA, precio y costo. | `productos` |
| **SKU** | Referencia del producto, **obligatoria** y única en el negocio. No se autogenera. | `REFERENCIA` (se autogeneraba) |
| **Tarifa de IVA** | Cómo grava el IVA al producto: `IVA_19`, `IVA_5`, `EXENTO` (gravado a 0 %), `EXCLUIDO` (no causa IVA). Exento y excluido **no son lo mismo** para la DIAN. | `categoria_impuesto` |
| **Precio** | Lo que paga el cliente, **con IVA incluido**: el número de la etiqueta. Es lo que se guarda. | `PVP+IVA` (se guardaba la base) |
| **Base** | Precio sin IVA: `precio / (1 + tarifa)`, a 2 decimales. Se calcula, no se escribe. | `PRECIO_VENTA` |
| **IVA del precio** | `precio − base`. | — |
| **Responsable de IVA** | El negocio cobra IVA. Si no lo es, ninguna venta lleva IVA. Sale de `regimen_fiscal` de la configuración fiscal (HU-018). | `REGIMEN_COMUN_SIMPLIFICADO` |
| **Tarifa por revisar** | Marca de un producto cuya tarifa fue asignada por la migración (19 %) y nadie confirmó todavía. | — |
| **Activo / inactivo** | Un producto inactivo no se vende ni aparece en la búsqueda de venta, pero conserva su historial. Nunca se borra. | `ACTIVO` |
| **Ubicación** | Texto libre de dónde está un producto **dentro de una bodega** ("Pasillo 3 – E2"). | `ESTANTE` |
| **Unidad de medida** | Cómo se cuenta un producto (UND, GLN, KG, MTS), si admite decimales y su factor respecto de la unidad base. | `unidades` (`VALOR`) |
| **Fracción** | Pedazo del **mismo** producto que se vende con precio propio: ½, ¾, ¼, ⅛ de galón. Tiene medida, factor y precio. | `detalle_producto` / código |
| **Factor de la fracción** | Cuánto descuenta del inventario, en unidades del producto: ¼ = 0,25. | `VALOR` |
| **Tabla de fracciones** | Lista de fracciones con precios fijos que comparten varios productos ("Esmalte Pintulux galón"). | — (Sara usaba unidad + categoría) |
| **Precio propio de fracción** | Precio de una fracción para **un** producto, que le gana al de su tabla. | — |
| **Detallar** | Abrir un producto y pasar sus unidades a **otro** producto: 1 cuñete → 5 galones. | `DetallarProductos` |
| **Equivalencia de detallado** | Regla guardada "1 origen = N destino" entre dos productos de la misma unidad base. | — (se escribía cada vez) |
| **Ajuste** | Cambio manual de existencia con motivo: sumar, restar, conteo físico, merma, avería. | `MovimientoExistencias` |
| **Conteo físico** | Ajuste que deja la existencia **exactamente** en lo contado. | — |
| **Inventario general** | Listado de productos con filtros y su valor a costo y a precio. | `InventarioGeneral` |
| **Valor a costo / a precio** | Existencia × costo promedio; existencia × precio (con IVA). | `TOTAL PRECIO COMPRA/VENTA` |

Términos de Sara que **no existen** en Regenta: "eliminar producto de la base de datos",
"eliminar existencias negativas", "producto rápido" que borra precios, precios 2 y 3 en el
producto, fracciones escritas en el código, referencia con prefijo `1-`.

## 3. Modelo

```
Producto (agregado)                       TablaDeFracciones (agregado)
  sku, nombre, categoria, unidad            nombre
  tarifaIva, tarifaPorRevisar               fracciones: [Fraccion(medida, factor, precio)]
  precio (con IVA), costoUltimo
  activo                                  EquivalenciaDeDetallado (agregado)
  tablaDeFraccionesId?                      origenId, destinoId, factor
  preciosPropios: [medida → precio]
                                          Ajuste (agregado, existente)
Existencia (producto × bodega, existente)   lineas, motivo, estado
  cantidad, reservada, costoPromedio
  ubicacion                               Detallado (operación)
                                            → MovimientoInventario SALIDA_DETALLE (origen)
MovimientoInventario (existente,            → MovimientoInventario ENTRADA_DETALLE (destino)
  solo se agrega)
```

- **Producto** guarda el precio con IVA y la tarifa. Base e IVA son cálculos puros
  (`PrecioConIva`), sin base de datos, iguales en backend y app.
- **TablaDeFracciones** es independiente de los productos: un producto **se asigna** a una tabla
  (a lo sumo una). Cambiar un precio de la tabla cambia el de todos sus productos.
- **EquivalenciaDeDetallado** une dos productos. Detallar no es un agregado guardado: es una
  operación que escribe dos movimientos en la misma transacción.
- **Ventas** no lee tablas de inventario: pide a `InventarioApi` el precio, la tarifa y las
  fracciones del producto, y copia lo que use en la línea.

## 4. Invariantes

### IVA y precio
- **I1** · Todo producto tiene una tarifa de IVA de la lista cerrada (`IVA_19`, `IVA_5`,
  `EXENTO`, `EXCLUIDO`).
- **I2** · El precio guardado es el precio final con IVA incluido. Base = `precio / (1 + t)`
  redondeada a 2 decimales; IVA = `precio − base`. Con `EXENTO` o `EXCLUIDO`, base = precio.
  Cambiar la tarifa **no** cambia el precio: cambia la base.
- **I3** · Si el negocio **no** es responsable de IVA, toda línea de venta lleva IVA 0 y su base
  es el precio completo, cualquiera que sea la tarifa del producto.
- **I4** · La tarifa y el precio de una línea de venta o cotización los decide el **backend**
  desde el catálogo, nunca el cliente. La línea guarda una copia: cambiar después el producto no
  cambia ventas ni cotizaciones existentes.
- **I5** · Un descuento se aplica sobre el precio con IVA; base e IVA se recalculan en proporción.
- **I6** · Los productos migrados sin tarifa quedan en `IVA_19` con "tarifa por revisar"; su precio
  actual se toma como precio final con IVA (el cliente sigue pagando lo mismo). Confirmar o
  cambiar la tarifa quita la marca.

### Producto
- **I7** · El SKU es obligatorio y único por negocio; el código de barras, si existe, también.
- **I8** · Un producto nunca se borra. Desactivar pide confirmación; un inactivo no aparece en el
  POS ni en la búsqueda de venta, sí en inventario general, historiales y reportes. Se reactiva
  cuando se quiera.
- **I9** · La ubicación es opcional, por producto y bodega, texto de hasta 60 caracteres.

### Existencia
- **I10** · Un ajuste, un detallado o un traslado **nunca** deja la existencia de una bodega en
  negativo: se rechaza entero. La venta sigue su propia regla (`permiteVentaSinStock` y la
  configuración del negocio). Una existencia ya negativa se corrige con un conteo físico.
- **I11** · Todo cambio de existencia escribe su movimiento en el libro mayor, con motivo,
  usuario y fecha (HU-030). El libro mayor no se edita ni se borra.

### Fracciones
- **I12** · El factor de una fracción está entre 0 y 1 (excluidos); las medidas no se repiten en
  una tabla.
- **I13** · El precio de una fracción lo escribe el usuario, con IVA incluido; **no es
  proporcional** al precio del producto. Usa la tarifa del producto.
- **I14** · Precio de una fracción para un producto = su precio propio si lo tiene; si no, el de
  su tabla. Un producto sin tabla y sin precios propios no se vende fraccionado.
- **I15** · Vender `n` de una fracción descuenta `n × factor` de la existencia del producto y
  cobra `n × precio de la fracción`.

### Detallar
- **I16** · Una equivalencia une dos productos **distintos**, con la **misma unidad base**, y un
  factor mayor que 0. Hay a lo sumo una equivalencia por par origen–destino.
- **I17** · Detallar `n` del origen saca `n` del origen y entra `n × factor` al destino, en la
  misma bodega y en la misma transacción, con motivo "Detallado". O pasan los dos movimientos o
  ninguno.
- **I18** · El costo que entra al destino es el costo promedio del origen dividido por el factor,
  y se promedia con el que ya tenía el destino.

### Importación
- **I19** · Una importación muestra primero una vista previa con los errores de cada fila (SKU o
  código de barras repetido —en el archivo o en el negocio—, tarifa inválida, campos
  obligatorios vacíos, números mal escritos). Al confirmar, **las filas con error no se
  importan y las válidas sí** (decidido 2026-11-16). Si dos filas del archivo traen el mismo SKU
  o código de barras, las dos son error: no se adivina cuál vale. Al terminar se ve cuántas se
  importaron y la lista de las omitidas con su error, descargable para corregir y volver a subir.
- **I20** · La existencia inicial de cada producto importado entra como ajuste de "carga inicial"
  en el libro mayor, no como un número escrito directo.

### Multi-tenant
- **I21** · Todo lo anterior vive por negocio: tablas de fracciones, equivalencias, ubicaciones y
  marcas de revisión llevan `negocio_id` con RLS; un negocio nunca ve ni asigna lo de otro.

## 5. Eventos

Esta especificación **no agrega eventos**. Ventas consulta precio, tarifa y fracciones por
`InventarioApi` (llamada síncrona: bloquea al usuario). Los movimientos de detallado y ajuste
se registran como los demás movimientos del libro mayor.

## 6. Permisos

| Permiso | Para qué | Estado |
|---|---|---|
| `INVENTARIO_PRODUCTO_VER` / `_CREAR` | Ver y crear productos | Existe |
| `INVENTARIO_PRODUCTO_EDITAR` | Editar, desactivar, reactivar, cambiar tarifa (también en bloque), ubicación | **Nuevo** |
| `INVENTARIO_FRACCION_GESTIONAR` | Tablas de fracciones, asignación y precios propios | **Nuevo** |
| `INVENTARIO_DETALLAR` | Equivalencias y detallar | **Nuevo** |
| `INVENTARIO_IMPORTAR` | Importar productos desde Excel | **Nuevo** |
| `INVENTARIO_AJUSTE_CREAR` / `_APLICAR` | Ajustes y conteo físico | Existe |
| `INVENTARIO_EXISTENCIA_VER` / `_MOVIMIENTO_VER` | Inventario general e historial | Existen |

Por defecto los permisos nuevos van a **Administrador** y **Gerente**; `INVENTARIO_DETALLAR`
también a Vendedor/Cajero, para poder detallar desde el POS.

## 7. Pantallas de Sara que alimentan este contexto

| Pantalla de Sara | Qué se trae | Qué se descarta |
|---|---|---|
| `Productos`, `InventarioFacil`, `AgregarExistecias` | Ficha, IVA, margen y beneficio, duplicados | Referencia autogenerada, precios 2 y 3, forzar IVA y unidad "1" |
| `ProductoRapidoNuevo` | — | Todo: borraba precios sin avisar |
| `InventarioGeneral` | Listado, filtros, valoración, activar/desactivar | Activar sin confirmación |
| `MovimientoExistencias`, sumar/restar de `InventarioFacil` | Ajustes con motivo, bloqueo de negativos | Guardar stock mín/máx en cada ajuste; "eliminar existencias negativas" |
| `DetalleProducto`, `AgregarDetalleProducto` | Fracciones con precio propio | Plantilla por unidad + categoría, fracciones en código |
| `DetallarProductos`, `DetallarProductosFacil` | Detallar validando unidad y existencia | Factor escrito cada vez; la pantalla "fácil" sin validaciones |
| `Unidades`, `Categorias` | Unidades con factor (categorías ya existían) | — |
| `ImportarProductos` | Importación con vista previa | Contador propio de referencias, crear categorías sin preguntar |
| `GestionInventario`, `CargarCompra`, `Verificar*`, `detalleCompra`, `Iva`, `PrecioCompra`, `PrecioVenta`, `CalcularPrecioVenta` | — | Son del contexto **Compras (E05)**: especificación aparte |

## 8. Decisiones aprobadas (2026-11-16)

| # | Decisión |
|---|---|
| 1 | El precio se escribe y se guarda **con IVA incluido** (I2). |
| 2 | Los 2.736 productos existentes quedan en 19 % con "tarifa por revisar" (I6); filtro y cambio de tarifa en bloque por categoría. |
| 3 | Fracciones con **tablas compartidas** de precio fijo, asignación por nombre, marca o selección, y precio propio por producto (I12-I15). |
| 4 | Detallar con **equivalencia guardada** y replicable (I16-I18), ofrecido también desde el POS. |
| 5 | Ajustes, detallado y traslados **bloquean** existencia negativa (I10). |
| 6 | Precios mayorista/VIP **fuera de alcance** (Pendientes P1). |
| 7 | SKU obligatorio, **sin** autogenerar (I7). |
| 8 | Nunca se borra: desactivar con confirmación y reactivar (I8). |
| 9 | Ubicación por producto y bodega (I9). |
| 10 | Importación desde Excel **dentro** del alcance, como última etapa (I19-I20). Las filas con error se omiten; las válidas se importan. |
| 11 | Orden: 1 IVA · 2 ficha · 3 inventario general · 4 unidades y fracciones · 5 ajustes · 6 detallar · 7 importación. |

## 9. Pendientes

- **P1** · Precios por lista (mayorista/VIP): la lista asignada al cliente se aplica en el POS;
  precios de lista con IVA incluido; las fracciones no dependen de la lista. El backend de
  HU-036 se conserva sin pantalla.
- ~~**P2**~~ · Resuelto 2026-11-20 en `docs/dominio/compras.md` (I14, decisión 3): costo sin IVA;
  si el negocio es responsable, el IVA de la compra es descontable, si no, suma al costo.
- ~~**P3**~~ · Resuelto 2026-11-16: las filas con error se omiten y las válidas se importan (I19).
- **P4** · Impuesto al consumo (INC 8 %) del patrón Comanda: el modelo de tarifas lo admite, fuera
  de alcance.
