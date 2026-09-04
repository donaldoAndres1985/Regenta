# Pantallas de Regenta — índice para desarrollo

Una fila por pantalla. Cada una existe en dos composiciones: móvil (390×844) y web (1440×900).
El HTML es la referencia exacta de medidas y color; el PNG es para mirar rápido.

Bajo cada pantalla, en el propio HTML, hay una franja gris con las tablas que usa.

La fila **Comportamiento** apunta al archivo donde se escriben las reglas de la pantalla
—foco, validaciones, estados vacíos, sin conexión, permisos— en *dado / cuando / entonces*.
Se llenan antes de implementar; ver `design/comportamiento/LEEME.md`.


## Núcleo

### Login y elección de negocio

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/LoginMovil.html` · `design/png/LoginMovil.png` |
| Web | `design/pantallas/LoginWeb.html` · `design/png/LoginWeb.png` |
| Paquete Flutter | `packages/core` |
| Microservicio | `servicio-usuarios` |
| DDL | `modelo-datos/sql/01-servicio-usuarios.sql` |
| Tablas | `usuarios` · `negocios` · `planes` · `patrones_operativos` · `refresh_tokens` |
| Comportamiento | `design/comportamiento/Login.md` |

El correo es único por negocio, no global: la misma persona puede trabajar en varios. El JWT sale de aquí con negocio_id, plan, patrón y roles.

### Panel de inicio

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/InicioMovil.html` · `design/png/InicioMovil.png` |
| Web | `design/pantallas/InicioWeb.html` · `design/png/InicioWeb.png` |
| Paquete Flutter | `packages/core + reportes` |
| Microservicio | `servicio-reportes` |
| DDL | `modelo-datos/sql/14-servicio-reportes.sql` |
| Tablas | `reportes.agregados_diarios` · `reportes.ranking_productos` · `alertas.alertas` · `ventas.ventas` |
| Comportamiento | `design/comportamiento/Inicio.md` |

Todo lo que se ve aquí sale del servicio de Reportes, que vive de eventos: ninguna consulta cruza a la base de Ventas.

### Clientes y cartera

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/ClientesMovil.html` · `design/png/ClientesMovil.png` |
| Web | `design/pantallas/ClientesWeb.html` · `design/png/ClientesWeb.png` |
| Paquete Flutter | `packages/core` |
| Microservicio | `servicio-clientes` |
| DDL | `modelo-datos/sql/02-servicio-clientes.sql` |
| Tablas | `crm.clientes` · `crm.cliente_metricas` · `crm.cuentas_por_cobrar` · `crm.recaudos` |
| Comportamiento | `design/comportamiento/Clientes.md` |

cliente_metricas es una proyección alimentada por los tres eventos de cierre — venta_completada, estancia_finalizada y pedido_completado.

### Configuración del negocio

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/ConfiguracionMovil.html` · `design/png/ConfiguracionMovil.png` |
| Web | `design/pantallas/ConfiguracionWeb.html` · `design/png/ConfiguracionWeb.png` |
| Paquete Flutter | `packages/core` |
| Microservicio | `servicio-usuarios` |
| DDL | `modelo-datos/sql/01-servicio-usuarios.sql` |
| Tablas | `configuracion_negocio` · `impuestos` · `sucursales` · `negocios` |
| Comportamiento | `design/comportamiento/Configuracion.md` |

Los impuestos los define cada negocio; las sucursales solo aparecen con el módulo Multi-sucursal, pero sucursal_id existe en el modelo desde el día 1.

### Usuarios, roles y permisos

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/UsuariosMovil.html` · `design/png/UsuariosMovil.png` |
| Web | `design/pantallas/UsuariosWeb.html` · `design/png/UsuariosWeb.png` |
| Paquete Flutter | `packages/usuarios` |
| Microservicio | `servicio-usuarios` |
| DDL | `modelo-datos/sql/01-servicio-usuarios.sql` |
| Tablas | `usuarios` · `roles` · `rol_permisos` · `usuario_roles` · `permisos` · `plantillas_rol` |
| Comportamiento | `design/comportamiento/Usuarios.md` |

El motor de permisos no cambia entre patrones. Solo cambian las plantillas que el negocio instancia: Vendedor y Cajero aquí; Recepcionista o Mesero en otro patrón.

### Factura electrónica

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/FacturaMovil.html` · `design/png/FacturaMovil.png` |
| Web | `design/pantallas/FacturaWeb.html` · `design/png/FacturaWeb.png` |
| Paquete Flutter | `packages/facturacion` |
| Microservicio | `servicio-facturacion` |
| DDL | `modelo-datos/sql/11-servicio-facturacion.sql` |
| Tablas | `facturacion.facturas` · `factura_lineas` · `factura_impuestos` · `resoluciones` · `transmisiones` |
| Comportamiento | `design/comportamiento/Factura.md` |

El emisor y el adquiriente son snapshots JSONB: una factura emitida no cambia si mañana editan el cliente. El consecutivo sale del rango de la resolución DIAN.

### Reportes y dashboards

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/ReportesMovil.html` · `design/png/ReportesMovil.png` |
| Web | `design/pantallas/ReportesWeb.html` · `design/png/ReportesWeb.png` |
| Paquete Flutter | `packages/reportes` |
| Microservicio | `servicio-reportes` |
| DDL | `modelo-datos/sql/14-servicio-reportes.sql` |
| Tablas | `reportes.hechos_venta` · `dim_producto` · `dim_fecha` · `agregados_diarios` · `ranking_productos` |
| Comportamiento | `design/comportamiento/Reportes.md` |

Esquema en estrella con dimensiones SCD tipo 2: cambiar el nombre de un producto no reescribe el histórico.


## Venta directa

### Inventario

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/InventarioMovil.html` · `design/png/InventarioMovil.png` |
| Web | `design/pantallas/InventarioWeb.html` · `design/png/InventarioWeb.png` |
| Paquete Flutter | `packages/inventario` |
| Microservicio | `servicio-inventario` |
| DDL | `modelo-datos/sql/03-servicio-inventario.sql` |
| Tablas | `inventario.productos` · `existencias` · `bodegas` · `categorias` |
| Comportamiento | `design/comportamiento/Inventario.md` |

El stock es (producto, bodega), no una columna de productos. Es la corrección más importante sobre el documento original.

### Ficha de producto

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/ProductoMovil.html` · `design/png/ProductoMovil.png` |
| Web | `design/pantallas/ProductoWeb.html` · `design/png/ProductoWeb.png` |
| Paquete Flutter | `packages/inventario` |
| Microservicio | `servicio-inventario` |
| DDL | `modelo-datos/sql/03-servicio-inventario.sql` |
| Tablas | `productos` · `atributos_categoria` · `existencias` · `lotes` · `movimientos_inventario` |
| Comportamiento | `design/comportamiento/Producto.md` |

Los campos del recuadro punteado los define el negocio y viven en el JSONB productos.atributos. Una vidriería o una veterinaria no necesitan código nuevo.

### Categorías y atributos

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/CategoriasMovil.html` · `design/png/CategoriasMovil.png` |
| Web | `design/pantallas/CategoriasWeb.html` · `design/png/CategoriasWeb.png` |
| Paquete Flutter | `packages/inventario` |
| Microservicio | `servicio-inventario` |
| DDL | `modelo-datos/sql/03-servicio-inventario.sql` |
| Tablas | `categorias` · `atributos_categoria` · `productos.atributos` |
| Comportamiento | `design/comportamiento/Categorias.md` |

La pantalla que sostiene toda la tesis del sistema: aquí es donde ferretería, papelería y droguería dejan de ser módulos distintos y pasan a ser configuración.

### Nueva venta / POS

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/POSMovil.html` · `design/png/POSMovil.png` |
| Web | `design/pantallas/POSWeb.html` · `design/png/POSWeb.png` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-ventas` |
| DDL | `modelo-datos/sql/04-servicio-ventas.sql` |
| Tablas | `ventas.ventas` · `venta_lineas` · `productos` · `existencias` · `reservas_stock` · `sagas` |
| Comportamiento | `design/comportamiento/POS.md` |

Al confirmar no se descuenta stock de una: se pide reserva a Inventario y se espera respuesta. Si no alcanza, la venta vuelve a borrador.

### Cliente de la venta

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/ClienteVentaMovil.html` · `design/png/ClienteVentaMovil.png` |
| Web | `design/pantallas/ClienteVentaWeb.html` · `design/png/ClienteVentaWeb.png` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-clientes` |
| DDL | `modelo-datos/sql/02-servicio-clientes.sql` |
| Tablas | `crm.clientes` · `ventas.ventas.cliente_id` · `ventas.ventas.cliente_snapshot` · `facturacion.facturas.cliente_snapshot` |
| Comportamiento | `design/comportamiento/ClienteVenta.md` |

El cliente es opcional en la venta —cliente_id NULL es consumidor final—, pero facturas.cliente_snapshot es NOT NULL: al emitir siempre se congela un adquiriente, aunque sea el genérico sin identificar.

### Cobro y caja

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/CobroMovil.html` · `design/png/CobroMovil.png` |
| Web | `design/pantallas/CobroWeb.html` · `design/png/CobroWeb.png` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-caja` |
| DDL | `modelo-datos/sql/12-servicio-caja.sql` |
| Tablas | `pagos_venta` · `caja.sesiones_caja` · `movimientos_caja` · `arqueo_denominaciones` |
| Comportamiento | `design/comportamiento/Cobro.md` |

Caja es transversal a los tres patrones: también recibe pagos de comandas y anticipos de reservas. Por eso no vive dentro de Ventas.

### Recepción de compra

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/RecepcionMovil.html` · `design/png/RecepcionMovil.png` |
| Web | `design/pantallas/RecepcionWeb.html` · `design/png/RecepcionWeb.png` |
| Paquete Flutter | `packages/compras` |
| Microservicio | `servicio-compras` |
| DDL | `modelo-datos/sql/05-servicio-compras.sql` |
| Tablas | `compras.ordenes_compra` · `recepciones` · `recepcion_lineas` · `inventario.lotes` · `cuentas_por_pagar` |
| Comportamiento | `design/comportamiento/Recepcion.md` |

El lote y el vencimiento se capturan al recibir, no en la ficha del producto: el mismo medicamento entra con lotes distintos cada semana.


## Reserva

### Calendario de disponibilidad

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/CalendarioMovil.html` · `design/png/CalendarioMovil.png` |
| Web | `design/pantallas/CalendarioWeb.html` · `design/png/CalendarioWeb.png` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| DDL | `modelo-datos/sql/07-servicio-reservas.sql` |
| Tablas | `reservas.reservas (periodo tstzrange)` · `recursos.recursos` · `bloqueos_recurso` |
| Comportamiento | `design/comportamiento/Calendario.md` |

Cada barra es un rango de tiempo. Que dos no puedan solaparse lo garantiza un constraint de exclusión GiST en PostgreSQL, no la aplicación.

### Ficha de recurso

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/RecursoMovil.html` · `design/png/RecursoMovil.png` |
| Web | `design/pantallas/RecursoWeb.html` · `design/png/RecursoWeb.png` |
| Paquete Flutter | `packages/recursos` |
| Microservicio | `servicio-recursos` |
| DDL | `modelo-datos/sql/06-servicio-recursos.sql` |
| Tablas | `recursos` · `tipos_recurso` · `atributos_tipo_recurso` · `tarifas` · `bloqueos_recurso` |
| Comportamiento | `design/comportamiento/Recurso.md` |

Espejo exacto de la ficha de producto: tipos_recurso + atributos_tipo_recurso hacen para un hotel lo que categorias + atributos_categoria hacen para una ferretería.

### Nueva reserva

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/NuevaReservaMovil.html` · `design/png/NuevaReservaMovil.png` |
| Web | `design/pantallas/NuevaReservaWeb.html` · `design/png/NuevaReservaWeb.png` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| DDL | `modelo-datos/sql/07-servicio-reservas.sql` |
| Tablas | `reservas` · `tarifas` · `servicios_adicionales` · `politicas_cancelacion` · `cupos_tipo_recurso` |
| Comportamiento | `design/comportamiento/NuevaReserva.md` |

El periodo es semiabierto: la salida de las 11:00 no choca con una entrada a las 11:00 del mismo día. Eso lo da el tipo de rango, no una regla escrita a mano.

### Estancia y check-out

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/EstanciaMovil.html` · `design/png/EstanciaMovil.png` |
| Web | `design/pantallas/EstanciaWeb.html` · `design/png/EstanciaWeb.png` |
| Paquete Flutter | `packages/reservas` |
| Microservicio | `servicio-reservas` |
| DDL | `modelo-datos/sql/07-servicio-reservas.sql` |
| Tablas | `estancias` · `consumos_estancia` · `ocupantes` · `pagos_reserva` |
| Comportamiento | `design/comportamiento/Estancia.md` |

Al cerrar se publica estancia_finalizada: el equivalente exacto de venta_completada. Facturación, Reportes, CRM y Caja lo consumen igual.

### Tarifas y temporadas

| | |
|---|---|
| Patrón | Reserva |
| Móvil | `design/pantallas/TarifasMovil.html` · `design/png/TarifasMovil.png` |
| Web | `design/pantallas/TarifasWeb.html` · `design/png/TarifasWeb.png` |
| Paquete Flutter | `packages/recursos` |
| Microservicio | `servicio-recursos` |
| DDL | `modelo-datos/sql/06-servicio-recursos.sql` |
| Tablas | `tarifas` · `politicas_cancelacion` · `reglas_disponibilidad` |
| Comportamiento | `design/comportamiento/Tarifas.md` |

Varias tarifas pueden aplicar a la misma noche. El campo prioridad resuelve el empate sin obligar al negocio a ordenarlas o borrarlas.


## Comanda

### Plano del salón

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/MesasMovil.html` · `design/png/MesasMovil.png` |
| Web | `design/pantallas/MesasWeb.html` · `design/png/MesasWeb.png` |
| Paquete Flutter | `packages/mesas` |
| Microservicio | `servicio-mesas` |
| DDL | `modelo-datos/sql/09-servicio-mesas.sql` |
| Tablas | `mesas.mesas` · `zonas` · `sesiones_mesa` · `sesion_mesas` · `comandas` |
| Comportamiento | `design/comportamiento/Mesas.md` |

Entre la mesa y la comanda va una sesión de mesa: es lo que permite unir mesas, medir la rotación y que al cerrar quede «por limpiar» en vez de libre.

### Toma de comanda

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/ComandaMovil.html` · `design/png/ComandaMovil.png` |
| Web | `design/pantallas/ComandaWeb.html` · `design/png/ComandaWeb.png` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| DDL | `modelo-datos/sql/10-servicio-comandas.sql` |
| Tablas | `comandas.comandas` · `comanda_lineas` · `comanda_linea_modificadores` · `menu.items_menu` · `modificadores` |
| Comportamiento | `design/comportamiento/Comanda.md` |

Aquí está la diferencia real con una venta: cada línea tiene su propio ciclo de vida, y anular una ya enviada a cocina genera merma.

### Cocina · KDS

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/KDSMovil.html` · `design/png/KDSMovil.png` |
| Web | `design/pantallas/KDSWeb.html` · `design/png/KDSWeb.png` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| DDL | `modelo-datos/sql/10-servicio-comandas.sql` |
| Tablas | `tickets_cocina` · `ticket_cocina_lineas` · `comanda_lineas` · `menu.estaciones_cocina` |
| Comportamiento | `design/comportamiento/KDS.md` |

Fondo oscuro a propósito: es una pantalla que se mira de lejos, en una cocina, con las manos ocupadas. Al marcar «Listo» se publica linea_lista.

### Dividir cuenta

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/CuentaMovil.html` · `design/png/CuentaMovil.png` |
| Web | `design/pantallas/CuentaWeb.html` · `design/png/CuentaWeb.png` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| DDL | `modelo-datos/sql/10-servicio-comandas.sql` |
| Tablas | `comandas.cuentas` · `cuenta_lineas` · `pagos_comanda` |
| Comportamiento | `design/comportamiento/Cuenta.md` |

cuenta_lineas guarda una proporción, así que un plato compartido se reparte entre dos cuentas. Sin esto, «pagamos por separado» obliga a rehacer la comanda.

