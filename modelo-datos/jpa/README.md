# Entidades JPA — Regenta

Esqueleto de entidades por servicio, siguiendo las convenciones del `CLAUDE.md`:
paquetes **por feature**, no por capa (`com.regenta.ventas.domain`, no
`com.regenta.domain.ventas`).

No se incluyen las 155 tablas: se modelan los **agregados raíz** de cada
servicio y las piezas transversales. El resto sigue exactamente los mismos
patrones que se ven aquí.

## Piezas transversales (paquete `comun`)
| Archivo | Para qué |
|---|---|
| `EntidadTenant.java` | `@MappedSuperclass` con `negocio_id`, auditoría y `@Version` |
| `TenantContext.java` + `TenantFilterAspect.java` | Fija `app.negocio_id` por transacción para que funcione la RLS |
| `RequiereModulo.java` + `ModuloInterceptor.java` | La validación de plan del `CLAUDE.md`, en el backend |
| `OutboxEvento.java` | Transactional Outbox |

## Notas de mapeo
- `atributos JSONB` → `Map<String,Object>` con `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6). No hace falta `hibernate-types`.
- `periodo TSTZRANGE` → **no** tiene tipo JPA estándar. Se mapea con un `UserType` propio o se exponen `inicio`/`fin` y el rango se construye en SQL nativo. Aquí se usa un `Range` custom.
- `GENERATED ALWAYS AS ... STORED` → `@Generated(event = {INSERT, UPDATE})` + `insertable=false, updatable=false`.
- Nunca `@ManyToOne` hacia una entidad de otro servicio: esas referencias son `UUID` planos.

## Alcance de estos archivos
Son **esqueletos de diseño**, no un módulo compilable: referencian clases que
aún no existen (`Categoria`, `VentaLinea`, `ComandaLinea`, `Resolucion`,
`PeriodoTstzRangeType`, `ModulosActivosService`) y viven fuera de una
estructura Maven. Su valor está en las decisiones que documentan: fronteras
de agregado, dónde hay FK real y dónde referencia lógica, qué invariante
protege el código y cuál protege la base de datos.
