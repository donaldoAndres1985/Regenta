# Modelo de datos de Regenta

Traducción del PDF de arquitectura y del `CLAUDE.md` a un modelo entidad-relación
ejecutable. **147 tablas en 15 esquemas**, uno por microservicio.

Los 16 scripts de `sql/` se ejecutaron completos contra PostgreSQL 16 sin errores.

## Contenido

- `Regenta-modelo-de-datos.pdf` — **el análisis completo en PDF**, 28 páginas A4 con portada,
  índice, numeración y todos los diagramas incrustados como vectores (se puede hacer zoom sin
  pixelarse). Es la versión para archivar, imprimir o mandar por correo.

- `sql/00-convenciones.sql` — extensiones, tipos, RLS, Outbox/Inbox, idempotencia offline
- `sql/01..15-servicio-*.sql` — un archivo por microservicio, en orden de dependencia
- `jpa/` — entidades Java de los agregados raíz + la base multi-tenant y `@RequiereModulo`
- `drawio/` — los diagramas en formato draw.io: un archivo maestro de 22 páginas
  (mapa de contextos, patrones, saga, eventos, referencias lógicas y el ER de cada
  microservicio) más un archivo suelto por servicio. Generados desde el catálogo de
  PostgreSQL, no dibujados a mano. Ver `drawio/LEEME.md`.

## Levantarlo en local

```bash
createdb regenta
psql -d regenta -f sql/00-convenciones.sql
for f in sql/0[1-9]*.sql sql/1*.sql; do psql -d regenta -f "$f"; done
```

Todos los esquemas caben en una base para revisar el modelo entero. **En producción
cada esquema va a su propia base**, una por microservicio: no hay ni una FK que cruce
de un esquema a otro, así que separarlos no rompe nada.

## Verificaciones que pasa el modelo

| Comprobación | Resultado |
|---|---|
| FKs que cruzan esquemas de servicios distintos | 0 |
| Tablas de negocio sin `negocio_id` | 0 (las 15 sin él son catálogos globales o tablas puente) |
| Constraints de exclusión anti-solape | 2 (`reservas`, `bloqueos_recurso`) |
| Tablas particionadas por fecha | 8 |
| Anti-overbooking probado con solapes reales | rechaza solape · permite cancelada · permite check-in = check-out |

## Los 14 hallazgos

Los más importantes, resueltos en el DDL:

1. `stock` como columna de `productos` no aguanta multi-sucursal → `existencias(producto, bodega)` + libro mayor
2. Clientes, Alertas, POS/Caja y Auditoría están en la tabla de planes pero no tienen servicio en el scaffold
3. `negocios` no tiene servicio declarado → vive en `servicio-usuarios`, los demás lo reciben por JWT y evento
4. El patrón Comanda no tenía forma de descontar inventario → tabla `recetas`
5. El consecutivo DIAN no puede ser `BIGSERIAL` → rango + `SELECT FOR UPDATE`
6. Facturación acoplada a Ventas rompe con 3 patrones → origen polimórfico
7. El overbooking no se evita en Java → `EXCLUDE USING gist`
8. Offline-first con PK secuenciales es imposible → UUID v7 + `idempotency_key`
9. RLS con `SET` en vez de `SET LOCAL` es una fuga entre tenants
10. Sin Outbox el evento se pierde en silencio

El análisis completo, con los diagramas E-R por contexto y el diccionario de datos,
está en el documento publicado del modelo.
