# Historias de usuario de Regenta

Backlog completo del producto: **112 historias** en **17 épicas**, **645 puntos**.

Cada historia declara qué tablas del modelo toca, qué pantalla del diseño implementa y de
qué otras historias depende. Nada aquí es genérico: los criterios de aceptación salen de las
restricciones reales del modelo de datos.

## Épicas

| Épica | Nombre | HU | Puntos | Plan mínimo | Archivo |
|---|---|---:|---:|---|---|
| `E00` | Fundación | 10 | 49 | Todos | [`E00-fundacion.md`](epicas/E00-fundacion.md) |
| `E01` | Core · Tenant e identidad | 10 | 42 | Todos | [`E01-core.md`](epicas/E01-core.md) |
| `E02` | Clientes y cartera | 5 | 20 | Profesional | [`E02-clientes-cartera.md`](epicas/E02-clientes-cartera.md) |
| `E03` | Inventario · catálogo del patrón Venta directa | 11 | 65 | Básico | [`E03-inventario.md`](epicas/E03-inventario.md) |
| `E04` | Ventas · transacción del patrón Venta directa | 9 | 60 | Básico | [`E04-ventas.md`](epicas/E04-ventas.md) |
| `E05` | Compras y proveedores | 6 | 31 | Profesional | [`E05-compras-proveedores.md`](epicas/E05-compras-proveedores.md) |
| `E06` | Facturación electrónica DIAN | 7 | 57 | Profesional | [`E06-facturacion-electronica-dian.md`](epicas/E06-facturacion-electronica-dian.md) |
| `E07` | Caja y POS | 5 | 19 | Empresarial | [`E07-caja-pos.md`](epicas/E07-caja-pos.md) |
| `E08` | Recursos · catálogo del patrón Reserva | 5 | 24 | Básico | [`E08-recursos.md`](epicas/E08-recursos.md) |
| `E09` | Reservas · transacción del patrón Reserva | 7 | 55 | Básico | [`E09-reservas.md`](epicas/E09-reservas.md) |
| `E10` | Menú · catálogo del patrón Comanda | 5 | 24 | Básico | [`E10-menu.md`](epicas/E10-menu.md) |
| `E11` | Mesas · el salón | 4 | 18 | Básico | [`E11-mesas.md`](epicas/E11-mesas.md) |
| `E12` | Comandas · transacción del patrón Comanda | 7 | 50 | Básico | [`E12-comandas.md`](epicas/E12-comandas.md) |
| `E13` | Alertas y notificaciones | 4 | 21 | Profesional | [`E13-alertas-notificaciones.md`](epicas/E13-alertas-notificaciones.md) |
| `E14` | Reportes y dashboards | 5 | 39 | Profesional | [`E14-reportes-dashboards.md`](epicas/E14-reportes-dashboards.md) |
| `E15` | Auditoría y sincronización | 5 | 32 | Empresarial | [`E15-auditoria-sincronizacion.md`](epicas/E15-auditoria-sincronizacion.md) |
| `E16` | App Flutter · núcleo | 7 | 39 | Todos | [`E16-app-flutter.md`](epicas/E16-app-flutter.md) |

## Todas las historias

| ID | Historia | Épica | Puntos | Depende de |
|---|---|---|---:|---|
| `HU-001` | Scaffolding del monorepo de backend | `E00` | 5 | — |
| `HU-002` | Scaffolding del monorepo Flutter con melos | `E00` | 5 | — |
| `HU-003` | Levantar el entorno local con Docker Compose | `E00` | 3 | `HU-001` |
| `HU-004` | Crear una base de datos por microservicio | `E00` | 5 | `HU-003` |
| `HU-005` | Migraciones versionadas con Flyway por servicio | `E00` | 5 | `HU-004` |
| `HU-006` | Row-Level Security activa en todas las tablas de negocio | `E00` | 8 | `HU-005` |
| `HU-007` | API Gateway con validación de JWT y enrutamiento | `E00` | 5 | `HU-001` |
| `HU-008` | Outbox e Inbox como librería compartida | `E00` | 8 | `HU-005` |
| `HU-009` | Pipeline de CI para ambos repos | `E00` | 3 | `HU-001` `HU-002` |
| `HU-010` | Documentación OpenAPI por servicio | `E00` | 2 | `HU-001` |
| `HU-011` | Registrar un negocio nuevo con su plan y patrón | `E01` | 5 | `HU-006` |
| `HU-012` | Crear el primer usuario administrador del negocio | `E01` | 3 | `HU-011` |
| `HU-013` | Autenticación con emisión de JWT | `E01` | 5 | `HU-012` |
| `HU-014` | Rotación y revocación de refresh tokens | `E01` | 3 | `HU-013` |
| `HU-015` | Gestión de usuarios del negocio con límite por plan | `E01` | 5 | `HU-012` |
| `HU-016` | Motor de roles y permisos por negocio | `E01` | 5 | `HU-015` |
| `HU-017` | Validación de módulo por plan en el backend | `E01` | 5 | `HU-007` `HU-011` |
| `HU-018` | Configuración fiscal y de operación del negocio | `E01` | 5 | `HU-011` |
| `HU-019` | Sucursales y bodegas del negocio | `E01` | 3 | `HU-017` `HU-018` |
| `HU-020` | Consulta del plan y los módulos activos desde la app | `E01` | 3 | `HU-017` |
| `HU-021` | Crear y consultar clientes | `E02` | 5 | `HU-011` |
| `HU-022` | Cupo de crédito y cartera del cliente | `E02` | 5 | `HU-021` |
| `HU-023` | Métricas del cliente alimentadas por eventos | `E02` | 5 | `HU-008` `HU-021` |
| `HU-024` | Historial de interacciones con el cliente | `E02` | 2 | `HU-021` |
| `HU-025` | Listado de clientes en móvil con búsqueda y filtros | `E02` | 3 | `HU-021` |
| `HU-026` | Categorías del negocio con jerarquía | `E03` | 3 | `HU-006` `HU-011` |
| `HU-027` | Definir los atributos que exige cada categoría | `E03` | 8 | `HU-026` |
| `HU-028` | Crear producto con validación de atributos dinámicos | `E03` | 8 | `HU-027` |
| `HU-029` | Bodegas y existencias por producto y bodega | `E03` | 5 | `HU-028` |
| `HU-030` | Libro mayor de movimientos de inventario | `E03` | 8 | `HU-029` |
| `HU-031` | Lotes y fechas de vencimiento | `E03` | 5 | `HU-030` |
| `HU-032` | Traslados entre bodegas | `E03` | 5 | `HU-030` |
| `HU-033` | Ajustes de inventario con motivo | `E03` | 5 | `HU-030` |
| `HU-034` | Reserva y liberación de stock para la saga de ventas | `E03` | 8 | `HU-030` |
| `HU-035` | Búsqueda de productos y escaneo de código de barras | `E03` | 5 | `HU-028` |
| `HU-036` | Listas de precios y precios por volumen | `E03` | 5 | `HU-028` |
| `HU-037` | Crear una venta en borrador con sus líneas | `E04` | 8 | `HU-028` `HU-036` |
| `HU-038` | Saga de confirmación de venta con reserva de stock | `E04` | 13 | `HU-035` `HU-037` |
| `HU-039` | Registrar el pago de una venta, incluso mixto | `E04` | 5 | `HU-037` |
| `HU-040` | Venta a crédito con validación de cupo | `E04` | 5 | `HU-022` `HU-039` |
| `HU-041` | Anular una venta con reintegro de stock | `E04` | 5 | `HU-037` |
| `HU-042` | Devoluciones totales y parciales | `E04` | 5 | `HU-041` |
| `HU-043` | Sincronización de ventas creadas sin conexión | `E04` | 8 | `HU-038` `HU-111` |
| `HU-044` | Cotizaciones que se convierten en venta | `E04` | 3 | `HU-036` |
| `HU-045` | Pantalla de POS en móvil y en web | `E04` | 8 | `HU-037` `HU-035` |
| `HU-046` | Administrar proveedores | `E05` | 3 | `HU-011` |
| `HU-047` | Órdenes de compra con aprobación | `E05` | 5 | `HU-046` |
| `HU-048` | Recepción de mercancía con captura de lotes | `E05` | 8 | `HU-047` `HU-031` |
| `HU-049` | Cuentas por pagar y pagos a proveedores | `E05` | 5 | `HU-048` |
| `HU-050` | Sugerencia de compra a partir de stock bajo mínimo | `E05` | 5 | `HU-046` `HU-093` |
| `HU-051` | Recepción desde el celular en la bodega | `E05` | 5 | `HU-048` |
| `HU-052` | Administrar resoluciones de numeración DIAN | `E06` | 5 | `HU-018` |
| `HU-053` | Emitir factura desde cualquiera de los tres patrones | `E06` | 13 | `HU-054` `HU-038` |
| `HU-054` | Asignación del consecutivo dentro del rango autorizado | `E06` | 8 | `HU-052` |
| `HU-055` | Firma digital y transmisión a la DIAN | `E06` | 13 | `HU-053` |
| `HU-056` | Notas crédito | `E06` | 8 | `HU-055` |
| `HU-057` | Modo de contingencia cuando la DIAN no responde | `E06` | 5 | `HU-055` |
| `HU-058` | Consulta y envío de facturas al cliente | `E06` | 5 | `HU-053` |
| `HU-059` | Abrir y cerrar sesión de caja | `E07` | 5 | `HU-018` |
| `HU-060` | Movimientos de caja desde los tres patrones | `E07` | 5 | `HU-059` |
| `HU-061` | Ingresos, retiros y gastos de caja | `E07` | 3 | `HU-059` |
| `HU-062` | Arqueo por denominaciones | `E07` | 3 | `HU-059` |
| `HU-063` | Reporte de cierre de caja | `E07` | 3 | `HU-062` |
| `HU-064` | Tipos de recurso con atributos configurables | `E08` | 5 | `HU-011` |
| `HU-065` | Administrar recursos individuales | `E08` | 3 | `HU-064` |
| `HU-066` | Tarifas por temporada, día y franja con prioridad | `E08` | 8 | `HU-064` |
| `HU-067` | Bloqueos de recurso por mantenimiento | `E08` | 5 | `HU-065` |
| `HU-068` | Servicios adicionales y políticas de cancelación | `E08` | 3 | `HU-064` |
| `HU-069` | Consultar disponibilidad en un periodo | `E09` | 8 | `HU-065` `HU-067` |
| `HU-070` | Crear una reserva sin posibilidad de overbooking | `E09` | 13 | `HU-069` `HU-066` |
| `HU-071` | Confirmar, cancelar y marcar no-show | `E09` | 5 | `HU-070` |
| `HU-072` | Check-in con asignación de recurso | `E09` | 8 | `HU-071` |
| `HU-073` | Cargar consumos a la estancia | `E09` | 5 | `HU-072` |
| `HU-074` | Check-out, liquidación y cierre de estancia | `E09` | 8 | `HU-073` |
| `HU-075` | Calendario visual de ocupación | `E09` | 8 | `HU-069` |
| `HU-076` | Cartas y categorías de menú por horario | `E10` | 3 | `HU-011` |
| `HU-077` | Ítems de menú con estación de cocina | `E10` | 5 | `HU-076` |
| `HU-078` | Modificadores con mínimos y máximos | `E10` | 5 | `HU-077` |
| `HU-079` | Recetas: el puente entre la comanda y el inventario | `E10` | 8 | `HU-077` `HU-029` |
| `HU-080` | Disponibilidad diaria de ítems | `E10` | 3 | `HU-077` |
| `HU-081` | Zonas y mesas con su posición en el plano | `E11` | 5 | `HU-011` |
| `HU-082` | Sesión de mesa: abrir, ocupar y liberar | `E11` | 5 | `HU-081` |
| `HU-083` | Unir mesas para un grupo grande | `E11` | 3 | `HU-082` |
| `HU-084` | Plano del salón en tiempo real | `E11` | 5 | `HU-082` |
| `HU-085` | Abrir comanda y agregar líneas mientras el servicio avanza | `E12` | 8 | `HU-078` `HU-082` |
| `HU-086` | Ciclo de vida propio de cada línea | `E12` | 5 | `HU-085` |
| `HU-087` | Anular una línea: antes y después de enviarla a cocina | `E12` | 5 | `HU-086` |
| `HU-088` | Enviar a cocina y pantalla KDS por estación | `E12` | 8 | `HU-086` |
| `HU-089` | Dividir la cuenta entre comensales | `E12` | 8 | `HU-085` |
| `HU-090` | Cerrar la comanda con propina y descuento de insumos | `E12` | 8 | `HU-089` `HU-079` |
| `HU-091` | Toma de comanda desde el celular del mesero | `E12` | 8 | `HU-085` |
| `HU-092` | Motor de reglas de alerta configurable | `E13` | 8 | `HU-008` |
| `HU-093` | Alertas de inventario: stock bajo y lotes por vencer | `E13` | 5 | `HU-092` `HU-030` |
| `HU-094` | Entrega por push, correo y dentro de la app | `E13` | 5 | `HU-092` |
| `HU-095` | Centro de alertas en la app | `E13` | 3 | `HU-094` |
| `HU-096` | Esquema en estrella alimentado por eventos | `E14` | 13 | `HU-008` `HU-037` |
| `HU-097` | Agregados diarios para el dashboard | `E14` | 5 | `HU-096` |
| `HU-098` | Reportes de ventas, márgenes y rotación | `E14` | 8 | `HU-097` |
| `HU-099` | Métricas propias de Reserva y de Comanda | `E14` | 8 | `HU-096` |
| `HU-100` | Exportar y programar reportes | `E14` | 5 | `HU-098` |
| `HU-101` | Bitácora de auditoría append-only | `E15` | 8 | `HU-008` |
| `HU-102` | Cola de sincronización de operaciones offline | `E15` | 8 | `HU-042` |
| `HU-103` | Detección y resolución de conflictos de sincronización | `E15` | 8 | `HU-102` |
| `HU-104` | Descarga incremental de cambios del servidor | `E15` | 5 | `HU-102` |
| `HU-105` | Consulta de auditoría desde la app | `E15` | 3 | `HU-101` |
| `HU-106` | Tema y sistema de diseño en código | `E16` | 3 | `HU-002` |
| `HU-107` | Navegación con rutas protegidas por rol y módulo | `E16` | 5 | `HU-020` `HU-106` |
| `HU-108` | Composición adaptativa entre móvil y escritorio | `E16` | 5 | `HU-106` |
| `HU-109` | Sesión, token seguro y refresco automático | `E16` | 5 | `HU-013` `HU-014` |
| `HU-110` | Base de datos local con Drift en Android y Web | `E16` | 8 | `HU-002` |
| `HU-111` | Cola de sincronización en segundo plano | `E16` | 8 | `HU-110` `HU-102` |
| `HU-112` | Cliente HTTP con manejo uniforme de errores | `E16` | 5 | `HU-109` |

## Cómo se lee una historia

Cada historia trae, además del *como / quiero / para*:

- **Microservicio y paquete Flutter** donde vive el trabajo.
- **Tablas** del modelo que toca — el DDL está en `modelo-datos/sql/`.
- **Pantalla** del diseño que implementa — el HTML está en `design/pantallas/`.
- **Criterios de aceptación** en formato *dado / cuando / entonces*, escritos para poder
  convertirse en tests sin reinterpretarlos.
- **Terminado cuando**: la lista que hay que marcar antes de cerrar la historia.

Las historias marcadas con la etiqueta `clave` son las que sostienen una decisión de
arquitectura: si se implementan mal, el problema no se arregla después sin reescribir.

## Estimación

Puntos en escala de Fibonacci. Como referencia:

| Puntos | Significa |
|---:|---|
| 2 | Un CRUD sencillo sobre una tabla, sin reglas raras |
| 3 | Un CRUD con validaciones o una pantalla sin lógica compleja |
| 5 | Varias tablas, reglas de negocio, o una pantalla con estado |
| 8 | Concurrencia, eventos, o una garantía que hay que probar con tests dedicados |
| 13 | Coordinación entre servicios o una máquina de estados persistida |

## Archivos

| Archivo | Para qué |
|---|---|
| `epicas/*.md` | El detalle de cada historia, agrupado por épica |
| `TABLERO.md` | El orden de ejecución y el grafo de dependencias |
| `historias.csv` | Importación a Jira, Linear o GitHub Projects |
| `crear-issues.sh` | Crea las issues en GitHub con `gh` (bash / Git Bash) |
| `crear-issues.ps1` | Lo mismo desde PowerShell |
