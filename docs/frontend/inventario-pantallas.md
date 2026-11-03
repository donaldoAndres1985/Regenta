# Inventario de pantallas del frontend

Estado al 3 de noviembre de 2026. Compara lo que hay en `app/` (paquetes Flutter y rutas de
`apps/regenta/lib/src/navegacion/catalogo.dart`) contra los controllers del monolito
(`backend/regenta-app-server/src/main/java/com/regenta/<modulo>/web`).

**Estados**
- **Completa:** listado o vista principal, detalle y acciones del flujo, con carga, vacío y error.
- **Incompleta:** existe, pero le falta parte del flujo (crear, editar, detalle, filtros) o
  muestra datos de ejemplo.
- **Falta:** no hay pantalla. Si la ruta existe en el menú, abre *"todavía no está disponible"*.

**Prioridad:** P1 = flujo de venta diario · P2 = operación del negocio · P3 = gestión y
análisis · P4 = patrones Reserva/Comanda y administración avanzada.

---

## 0. Hallazgo que va antes que cualquier pantalla

**El backend no valida la activación de módulos.** `@RequiereModulo` y su aspecto
`GuardiaDeModulos` existen en `shared/security`, pero **ningún controller los usa** (ni en el
monolito ni en los `servicio-*` antiguos). Hoy solo se valida `@RequierePermiso`. Un negocio
sin el módulo FACTURACIÓN, con un usuario que tenga el permiso, puede llamar a
`/api/facturacion/**`. El menú y la guardia de rutas del frontend lo ocultan, pero
`CLAUDE.md` §5 exige que la validación viva en el backend.
→ **Pendiente de backend (P1):** anotar cada controller con `@RequiereModulo("<CODIGO>")` y
probarlo con MockMvc (negocio con y sin el módulo).

---

## 1. Inventario por módulo

| Módulo | Pantalla | Estado | Endpoints que usa / debería usar | Prioridad |
|---|---|---|---|---|
| Inicio | Panel (`PantallaInicio`) | **Incompleta**: KPIs, gráfico, últimas ventas y alertas son datos de ejemplo | Debería: `GET /api/reportes/panel/hoy`, `GET /api/reportes/panel/mes`, `GET /api/alertas/mias` | P1 |
| Ventas | Nueva venta (`PantallaPos`) | **Completa** (rediseñada) | `GET /api/inventario/productos/buscar`, `GET /api/inventario/productos/codigo/{codigo}`, `POST /api/ventas/offline`, `GET /api/ventas/reglas-de-cobro` | P1 |
| Ventas | Selector de cliente (`PantallaClienteDeVenta`) | **Completa** | `GET /api/clientes`, `POST /api/clientes/expres` | P1 |
| Ventas | Cobro con varios medios de pago | **Falta** | Existe `POST /api/ventas/{id}/pagos` + `POST /api/ventas/{id}/cierre`, pero el POS cobra por `POST /api/ventas/offline`, que no lleva medios de pago → **pendiente de backend** (contrato) | P1 |
| Ventas | Historial de ventas | **Falta** | Solo existe `GET /api/ventas/{ventaId}`; **no hay listado** → **pendiente de backend** (`GET /api/ventas?desde&hasta&estado&q`) | P1 |
| Ventas | Detalle de venta / anulación | **Falta** | `GET /api/ventas/{id}`, `POST /api/ventas/{id}/anulacion` | P1 |
| Ventas | Devoluciones | **Falta** | `POST /api/ventas/{ventaId}/devoluciones` (depende del detalle de venta) | P1 |
| Ventas | Cotizaciones | **Falta** | `POST /api/cotizaciones`, `GET /api/cotizaciones/{id}`, `POST /api/cotizaciones/{id}/conversion`; **no hay listado** → pendiente de backend | P3 |
| Caja | Abrir / cerrar caja, sesión activa | **Falta** (ruta `/caja` con aviso) | `GET /api/caja/cajas`, `GET /api/caja/sesiones/activa`, `POST /api/caja/sesiones`, `POST /api/caja/sesiones/{id}/cierre` | P1 |
| Caja | Arqueo, ingresos y retiros | **Falta** | `GET/PUT /api/caja/sesiones/{id}/arqueo`, `POST /api/caja/sesiones/{id}/ingresos`, `POST /api/caja/sesiones/{id}/retiros`, `GET /api/caja/sesiones/{id}/movimientos` | P1 |
| Caja | Configuración de caja | **Falta** | `GET/PUT /api/caja/config`, `POST /api/caja/cajas` | P3 |
| Inventario | Productos (`PantallaInventario`) | **Incompleta**: busca y escanea; no crea, no edita, sin detalle | `GET /api/inventario/productos/buscar`, `GET /api/inventario/productos/codigo/{codigo}`. Debería: `POST /api/inventario/productos`, `GET /api/inventario/productos/{id}`. **No hay `PUT` de producto** → editar es **pendiente de backend** | P2 |
| Inventario | Categorías y atributos configurables | **Falta** | `GET/POST/PUT/DELETE /api/inventario/categorias`, `GET/POST /api/inventario/categorias/{id}/atributos`, `PUT/DELETE /api/inventario/categorias/atributos/{id}` | P2 |
| Inventario | Existencias por bodega | **Falta** | `GET /api/inventario/productos/{id}/stock`, `GET /api/inventario/movimientos/producto/{id}`, `GET /api/inventario/movimientos/saldo` | P2 |
| Inventario | Ajustes de inventario | **Falta** | `POST /api/inventario/ajustes`, `POST /api/inventario/ajustes/{id}/aplicacion`, `GET /api/inventario/ajustes/{id}`; **no hay listado** → pendiente de backend | P2 |
| Inventario | Bodegas | **Falta** | `GET/POST /api/inventario/bodegas`, `DELETE /api/inventario/bodegas/{id}` (no hay `PUT`) | P2 |
| Inventario | Traslados | **Falta** | `POST /api/inventario/traslados`, `.../envio`, `.../recepcion`, `GET /{id}`; sin listado → pendiente de backend | P3 |
| Inventario | Lotes y vencimientos | **Falta** | `GET /api/inventario/lotes/por-vencer`, `/fefo`, `POST /entradas`, `/salidas` | P3 |
| Inventario | Listas de precios | **Falta** | `GET/POST /api/inventario/listas-precios`, `POST /precios`, `GET /resolver` | P3 |
| Clientes | Listado (`PantallaClientes`) | **Incompleta**: lista, busca y filtra; sin detalle, crear ni editar | `GET /api/clientes`. Debería: `GET /api/clientes/{id}`, `POST /api/clientes`, `PUT /api/clientes/{id}` | P2 |
| Clientes | Detalle / CRM (interacciones, métricas) | **Falta** | `GET/POST /api/clientes/{id}/interacciones`, `GET /api/clientes/{id}/metricas` | P2 |
| Clientes | Cartera y cuentas por cobrar | **Falta** | `GET /api/clientes/{id}/cartera`, `PUT /api/clientes/{id}/credito`; **no hay listado global de cartera** → pendiente de backend | P2 |
| Clientes | Recaudos | **Falta** | `POST /api/clientes/cuentas/{cuentaId}/recaudos` | P2 |
| Compras | Recepción de mercancía (`PantallaRecepcion`) | **Incompleta**: funciona, pero solo si se llega con el id de una orden; no está en ninguna ruta | `GET /api/compras/ordenes/{id}`, `POST /api/compras/recepciones`, `POST /api/compras/recepciones/{id}/confirmacion` | P2 |
| Compras | Órdenes de compra (listado, crear, aprobar, enviar) | **Falta** (ruta `/compras` con aviso) | `GET/POST /api/compras/ordenes`, `PUT /{id}/lineas`, `POST /{id}/aprobacion`, `POST /{id}/envio` | P2 |
| Compras | Proveedores | **Falta** | `GET/POST/PUT/DELETE /api/compras/proveedores`, `GET/POST /{id}/productos` | P2 |
| Compras | Cuentas por pagar | **Falta** | `GET/POST /api/compras/cuentas-por-pagar`, `POST /{id}/pagos` | P3 |
| Compras | Sugerencias de compra | **Falta** | `GET /api/compras/sugerencias`, `POST /aceptacion`, `POST /{id}/descarte` | P3 |
| Facturación | Detalle de factura (`PantallaFactura`) | **Completa** para el detalle | `GET /api/facturacion/facturas/{id}`, `POST /{id}/envio-cliente`, `GET /{id}/transmisiones` | P2 |
| Facturación | Listado de facturas | **Falta** (`/facturas` sin id muestra "Elige una factura") | `GET /api/facturacion/facturas` | P2 |
| Facturación | Notas crédito | **Falta** | `POST /api/facturacion/facturas/{id}/notas-credito` | P3 |
| Facturación | Resoluciones, certificados, contingencias | **Falta** | `/api/facturacion/resoluciones`, `/certificados`, `/contingencias` (DIAN va con proveedor tecnológico: ver memoria del proyecto) | P4 |
| Mesas | Plano y sesiones (`PantallaMesas`) | **Completa** | `/api/mesas`, `/api/mesas/plano`, `/api/mesas/zonas`, sesiones | P4 |
| Comandas | Comanda (`PantallaComanda`), dividir cuenta (`PantallaDividirCuenta`) | **Completa** | `/api/comandas/**`, `/api/comandas/{id}/cuentas/**` | P4 |
| Cocina | KDS (`PantallaKds`) | **Completa** | `/api/cocina/estaciones/{id}/tickets`, `/api/cocina/tickets/{id}/avance` | P4 |
| Menú | Cartas, ítems, modificadores, estaciones, recetas | **Falta** (sin ruta) | `/api/menu/cartas/**`, `/api/menu/items/**`, `/api/menu/modificadores/**`, `/api/menu/estaciones/**`, recetas | P4 |
| Reservas | Calendario de ocupación (`PantallaCalendario`) | **Incompleta**: muestra el calendario; no crea, confirma ni cancela; **no está en el catálogo de rutas** | `GET /api/reservas/calendario`. Debería: `POST /api/reservas`, `/{id}/confirmacion`, `/cancelacion`, `/check-in`, `/check-out` | P4 |
| Recursos | Recursos, tipos, tarifas, bloqueos | **Falta** | `/api/recursos/**` | P4 |
| Reportes | Sesiones de caja (`ListaDeSesionesDeCaja`) | **Incompleta**: el widget existe, pero no está en ninguna ruta (`/reportes` muestra aviso) | `GET /api/caja/reportes/sesiones`, `GET /api/caja/reportes/sesiones/{id}` | P3 |
| Reportes | Ventas por categoría, rotación, métricas por patrón, exportaciones | **Falta** | `/api/reportes/ventas/**`, `/api/reportes/metricas/**`, `/api/reportes/exportaciones` | P3 |
| Alertas | Centro de alertas (campana, en el núcleo) | **Completa** | `/api/alertas/mias`, `/vistas`, `/{id}/resolucion` | P3 |
| Alertas | Reglas de alerta (stock bajo, vencimientos) | **Falta** | `/api/alertas/reglas/**`, `/api/alertas/preferencias` | P3 |
| Configuración | Datos del negocio, impuestos | **Falta** (ruta `/configuracion` con aviso) | `GET/PUT /api/usuarios/configuracion`, `/api/usuarios/impuestos/**` | P3 |
| Configuración | Sucursales | **Falta** | `/api/usuarios/sucursales/**` | P3 |
| Configuración | Módulos y plan | **Falta** | `GET /api/usuarios/modulos`, `POST /modulos/{codigo}/activar`, `DELETE /modulos/{codigo}`, `POST /negocios/plan` | P3 |
| Usuarios | Usuarios e invitaciones | **Falta** | `/api/usuarios/usuarios/**` | P3 |
| Usuarios | Roles y permisos | **Falta** | `GET /api/usuarios/roles`, `GET /api/usuarios/permisos`, `POST/PUT/DELETE /roles` | P3 |
| Usuarios | Sesiones abiertas del usuario | **Falta** | `GET /api/usuarios/auth/sesiones`, `DELETE /sesiones/{dispositivoId}` | P4 |
| Auditoría | Eventos y conflictos de sincronización | **Falta** | `GET /api/auditoria/eventos`, `/por-entidad`, `/por-usuario`, `/api/auditoria/conflictos` | P4 |
| Sesión | Entrada (`PantallaLogin`) | **Completa** | `/api/usuarios/auth/login`, `/refrescar` | — |

## 2. Antes y después de esta entrega

| | Antes | Después |
|---|---|---|
| Pantallas Flutter | 16 (`Pantalla*` + `ListaDeSesionesDeCaja`, `CalendarioDeOcupacion`, `CentroDeAlertas`) | Las mismas; **Nueva venta** rediseñada |
| Filas *completa* (comanda y dividir cuenta cuentan como una) | 8 | 8 |
| Filas *incompleta* | 6 | 6 |
| Filas *falta* | 36 | 36 |

Esta entrega rediseñó Nueva venta y levantó este inventario. Las pantallas faltantes se
construyen **un módulo por vez**, como manda `CLAUDE.md` (§13.2 y §15), con TDD a partir de
los criterios de su historia y su archivo de `design/comportamiento/`.

## 3. Endpoints pendientes de backend

No se inventaron en el frontend; cada uno bloquea la pantalla indicada.

| # | Endpoint propuesto | Para qué pantalla | Prioridad |
|---|---|---|---|
| 1 | `@RequiereModulo` en todos los controllers (no es un endpoint: es la validación que falta) | Todas | **P1** |
| 2 | `GET /api/ventas?desde&hasta&estado&clienteId&q&pagina` | Historial de ventas, devoluciones | P1 |
| 3 | Medios de pago en `POST /api/ventas/offline` (`pagos: [{medio, monto, referencia}]`) o decidir que el POS cobre por `POST /api/ventas/{id}/pagos` + `/cierre` | Cobro con varios medios | P1 |
| 4 | Descuento y nota en `POST /api/ventas/offline` (`descuento`, `nota`), validado contra `GET /api/inventario/listas-precios/validar-descuento` | Botones *Descuento* y *Nota* del POS (hoy deshabilitados) | P1 |
| 5 | `PUT /api/inventario/productos/{productoId}` | Editar producto | P2 |
| 6 | `GET /api/inventario/ajustes` (listado) | Ajustes de inventario | P2 |
| 7 | `GET /api/clientes/cartera` (cuentas por cobrar de todos los clientes, con vencidas) | Cartera | P2 |
| 8 | `PUT /api/inventario/bodegas/{bodegaId}` | Bodegas (editar) | P3 |
| 9 | `GET /api/inventario/traslados` (listado) | Traslados | P3 |
| 10 | `GET /api/cotizaciones` (listado) | Cotizaciones | P3 |
| 11 | Búsqueda de clientes por teléfono en `GET /api/clientes?q=` (verificar si ya lo cubre) | Selector de cliente del POS | P2 |

**Búsqueda exacta por código o referencia en el POS:** no hizo falta un endpoint nuevo.
`GET /api/inventario/productos/codigo/{codigo}` resuelve códigos de barras propios y alternos,
y la referencia (SKU) exacta se filtra sobre `GET /api/inventario/productos/buscar?q=`. Un
`GET /api/inventario/productos/referencia/{sku}` sería más directo, pero es opcional.

## 4. Orden propuesto para construir lo que falta

1. **Backend:** `@RequiereModulo` en todos los controllers (#1).
2. **Caja:** abrir y cerrar, arqueo, ingresos y retiros. El backend está completo.
3. **Inicio con datos reales** (`/api/reportes/panel/*`).
4. **Ventas:** historial, detalle, anulación y devoluciones. Necesita #2.
5. **Inventario:** productos (crear y detalle), categorías y atributos, existencias, bodegas y
   ajustes. Necesita #5 y #6 para editar y listar ajustes.
6. **Clientes:** detalle, crear y editar, CRM, cartera y recaudos. Necesita #7.
7. **Compras:** proveedores, órdenes y recepción enlazada.
8. **Facturación:** listado y notas crédito.
9. Menú, recursos y reservas (patrones Comanda y Reserva).
10. Reportes y reglas de alertas.
11. Configuración, usuarios, roles y permisos, y auditoría.
