# Base de Datos y Migraciones Multi-Schema (Fase 5)

## 1. Conexión Unificada a PostgreSQL

En la arquitectura de 17 microservicios, cada servicio se conectaba a una base de datos PostgreSQL independiente (o schemas separados) con su propio pool de conexiones HikariCP, consumiendo recursos de conexión y memoria duplicados.

En el **Monolito Modular**:
- Existe un único `DataSource` configurado en `application.yml` (`spring.datasource`).
- Un único pool de conexiones HikariCP (`maximum-pool-size: 15`).
- Todas las tablas de todos los módulos conviven en la misma base de datos física, pero con **estricto aislamiento por schema** en PostgreSQL.

---

## 2. Los 15 Schemas y Rutas de Migración

| # | Módulo | Schema PostgreSQL | Directorio de Migraciones | Entidades Principales |
|---|---|---|---|---|
| 1 | `usuarios` | `core_identidad` | `classpath:db/migration/usuarios` | `Usuario`, `Negocio`, `Rol`, `Permiso`, `Invitacion` |
| 2 | `clientes` | `crm` | `classpath:db/migration/clientes` | `Cliente`, `CuentaCredito`, `MovimientoCredito` |
| 3 | `recursos` | `recursos` | `classpath:db/migration/recursos` | `Recurso`, `TipoRecurso`, `BloqueoRecurso` |
| 4 | `inventario` | `inventario` | `classpath:db/migration/inventario` | `Producto`, `Bodega`, `Lote`, `MovimientoInventario` |
| 5 | `compras` | `compras` | `classpath:db/migration/compras` | `OrdenDeCompra`, `Proveedor`, `Recepcion` |
| 6 | `menu` | `menu` | `classpath:db/migration/menu` | `ItemMenu`, `CategoriaMenu`, `Receta` |
| 7 | `mesas` | `mesas` | `classpath:db/migration/mesas` | `Mesa`, `Zona` |
| 8 | `reservas` | `reservas` | `classpath:db/migration/reservas` | `Reserva`, `Estancia`, `ConsumoEstancia` |
| 9 | `comandas` | `comandas` | `classpath:db/migration/comandas` | `Comanda`, `LineaComanda` |
| 10 | `ventas` | `ventas` | `classpath:db/migration/ventas` | `Venta`, `LineaVenta`, `Devolucion`, `SagaVenta` |
| 11 | `caja` | `caja` | `classpath:db/migration/caja` | `Caja`, `SesionDeCaja`, `MovimientoDeCaja`, `ArqueoDenominacion` |
| 12 | `facturacion` | `facturacion` | `classpath:db/migration/facturacion` | `Factura`, `FacturaLinea`, `FacturaImpuesto`, `Resolucion`, `Certificado` |
| 13 | `alertas` | `alertas` | `classpath:db/migration/alertas` | `Alerta`, `ReglaAlerta`, `TipoAlerta`, `PreferenciaNotificacion`, `DispositivoPush`, `Entrega` |
| 14 | `reportes` | `reportes` | `classpath:db/migration/reportes` | `DimCliente`, `DimProducto`, `DimSucursal`, `DimUsuario`, Data Mart particionado |
| 15 | `auditoria` | `auditoria` | `classpath:db/migration/auditoria` | `ConflictoSync`, `Dispositivo`, `OperacionSync`, bitácoras particionadas |

---

## 3. Estrategia de Migración Flyway Multi-Schema

Dado que Spring Boot por defecto solo soporta un único schema y una única tabla `flyway_schema_history`, se implementó `com.regenta.shared.bbdd.ConfiguracionMultiSchemaFlyway`:
1. **Desactivación del Flyway por defecto:** `spring.flyway.enabled: false`.
2. **`BeanPostProcessor` para `EntityManagerFactory`:**
   - Intercepta la inicialización de JPA / Hibernate.
   - Ejecuta las migraciones Flyway de forma secuencial para los 15 módulos antes de que Hibernate valide el modelo (`ddl-auto: validate`).
3. **Aislamiento por Schema:**
   - Cada módulo define su schema: `Flyway.configure().schemas(m.schema()).defaultSchema(m.schema()).locations(m.location())`.
   - Cada schema contiene su propia tabla `flyway_schema_history`, manteniendo los checksums y la auditoría de versiones exactamente igual que en la arquitectura de microservicios.
4. **Idempotencia:**
   - Cada script `V1__esquema_inicial.sql` inicia con `CREATE SCHEMA IF NOT EXISTS <schema>; SET search_path TO <schema>, public;`.

---

## 4. Mapeo Explícito de Entidades JPA

Todas las entidades de todos los módulos han sido actualizadas con la anotación explícita de schema:
```java
@Table(name = "...", schema = "<schema_del_modulo>")
```
Esto garantiza que Hibernate genere y ejecute queries siempre prefijadas con el schema correcto (por ejemplo, `SELECT ... FROM crm.clientes`, `INSERT INTO facturacion.facturas`, etc.), independientemente del `search_path` de la sesión.
