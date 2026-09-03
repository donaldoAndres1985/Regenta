-- =====================================================================
-- REGENTA — Convenciones transversales del modelo de datos
-- Se aplican a TODOS los microservicios. Cada servicio tiene su propia
-- base/esquema: NUNCA hay FK entre esquemas de servicios distintos.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Extensiones requeridas (ejecutar en cada base de servicio)
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";     -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "btree_gist";   -- constraints EXCLUDE mixtos (reservas)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- busqueda por nombre/SKU
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- ---------------------------------------------------------------------
-- 2. Identificadores
-- ---------------------------------------------------------------------
-- REGLA: toda PK es UUID, nunca BIGSERIAL.
-- Motivo: la app es offline-first (Drift + workmanager). Un vendedor sin
-- senal crea una venta en el dispositivo; si la PK la asignara el servidor
-- no habria forma de referenciar esa venta localmente ni de deduplicar al
-- sincronizar. El cliente genera el UUID, el servidor lo respeta.
-- Preferir UUID v7 (ordenable en el tiempo -> indices B-tree sin
-- fragmentacion). Si no hay v7 disponible, gen_random_uuid() (v4).

-- ---------------------------------------------------------------------
-- 3. Columnas obligatorias en TODA tabla de negocio
-- ---------------------------------------------------------------------
--   negocio_id     UUID        NOT NULL   -- discriminador de tenant
--   creado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
--   creado_por     UUID                     -- usuario_id (ref logica)
--   actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
--   actualizado_por UUID
--   version        BIGINT      NOT NULL DEFAULT 0  -- @Version, optimistic locking
--   eliminado_en   TIMESTAMPTZ              -- soft delete (NULL = vigente)
--
-- REGLA: todo indice de tabla de negocio lleva negocio_id como PRIMER
-- campo del indice compuesto. Un indice que no empiece por negocio_id es
-- un bug de rendimiento en multi-tenant.

-- ---------------------------------------------------------------------
-- 4. Tipos
-- ---------------------------------------------------------------------
--   Dinero      -> NUMERIC(18,4) para calculo interno, NUMERIC(14,2) para
--                  totales presentables. NUNCA float/double.
--   Cantidades  -> NUMERIC(18,6) (permite kg, metros, fracciones de receta)
--   Porcentajes -> NUMERIC(7,4)  (19.0000 = 19%)
--   Fecha/hora  -> TIMESTAMPTZ siempre (negocios en distintas zonas).
--                  DATE solo para fechas de calendario puro (vencimiento).
--   Enums       -> VARCHAR + CHECK, no tipo ENUM de Postgres.
--                  Motivo: agregar un valor a un ENUM requiere DDL con
--                  lock; un CHECK se cambia sin bloquear escrituras largas.
--   Texto libre variable por categoria -> JSONB + indice GIN.

-- ---------------------------------------------------------------------
-- 5. Aislamiento multi-tenant: Row-Level Security
-- ---------------------------------------------------------------------
-- El filtro por negocio_id en la capa de aplicacion (Hibernate @Filter o
-- specification) es la primera linea. RLS es la red de seguridad: si un
-- developer olvida el WHERE, la base igual no devuelve filas de otro
-- negocio.
--
-- Plantilla a aplicar a cada tabla de negocio:

-- ALTER TABLE <tabla> ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE <tabla> FORCE ROW LEVEL SECURITY;  -- aplica tambien al owner
-- CREATE POLICY tenant_isolation ON <tabla>
--   USING      (negocio_id = current_setting('app.negocio_id', true)::uuid)
--   WITH CHECK (negocio_id = current_setting('app.negocio_id', true)::uuid);
--
-- El backend debe ejecutar, al inicio de CADA transaccion:
--   SET LOCAL app.negocio_id = '<claim negocio_id del JWT>';
-- Con HikariCP esto va en un interceptor/AOP o en un
-- AbstractRoutingDataSource; NUNCA en el pool de conexiones global
-- (las conexiones se reutilizan entre requests de negocios distintos).
--
-- Funcion helper reutilizable:
CREATE OR REPLACE FUNCTION app_negocio_actual() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT NULLIF(current_setting('app.negocio_id', true), '')::uuid
$$;

-- Trigger generico de actualizado_en
CREATE OR REPLACE FUNCTION trg_actualizado_en() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  NEW.actualizado_en := now();
  NEW.version := COALESCE(OLD.version, 0) + 1;
  RETURN NEW;
END $$;

-- ---------------------------------------------------------------------
-- 6. Referencias entre servicios
-- ---------------------------------------------------------------------
-- PROHIBIDO: FK fisica hacia una tabla de otro microservicio.
-- En su lugar:
--   a) "Referencia logica": se guarda el UUID (ej. ventas.cliente_id) SIN
--      REFERENCES. La integridad la garantiza el servicio duenio del dato.
--   b) "Snapshot desnormalizado": se copia lo que se necesita mostrar o
--      lo que debe quedar inmutable (ej. venta_lineas.descripcion_snapshot,
--      facturas.cliente_snapshot JSONB). Una factura emitida no puede
--      cambiar porque alguien editó el nombre del cliente despues.
--   c) "Replica por evento": el servicio mantiene una copia de solo lectura
--      alimentada por RabbitMQ (ej. reportes.dim_producto).
-- En el DDL estas columnas se marcan con el comentario -- [ref logica].

-- ---------------------------------------------------------------------
-- 7. Outbox / Inbox (obligatorio en todo servicio que publique o consuma)
-- ---------------------------------------------------------------------
-- Sin Outbox no hay atomicidad entre "guardar la venta" y "publicar
-- venta_completada": si RabbitMQ falla despues del commit, el evento se
-- pierde para siempre y Facturacion nunca emite la factura.
-- Sin Inbox no hay idempotencia: RabbitMQ garantiza at-least-once, asi que
-- el mismo evento puede llegar dos veces y facturar dos veces.

CREATE TABLE IF NOT EXISTS outbox_eventos (
    id               UUID PRIMARY KEY,
    negocio_id       UUID        NOT NULL,
    agregado_tipo    VARCHAR(60) NOT NULL,           -- 'Venta', 'Reserva'
    agregado_id      UUID        NOT NULL,
    tipo_evento      VARCHAR(80) NOT NULL,           -- 'venta_completada'
    payload          JSONB       NOT NULL,
    trace_id         VARCHAR(64),
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                     CHECK (estado IN ('PENDIENTE','PUBLICADO','FALLIDO')),
    intentos         INT         NOT NULL DEFAULT 0,
    ultimo_error     TEXT,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    publicado_en     TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_outbox_pendiente
    ON outbox_eventos (creado_en) WHERE estado = 'PENDIENTE';

CREATE TABLE IF NOT EXISTS inbox_eventos (
    mensaje_id       UUID PRIMARY KEY,               -- message-id de AMQP
    negocio_id       UUID        NOT NULL,
    tipo_evento      VARCHAR(80) NOT NULL,
    payload          JSONB,
    recibido_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    procesado_en     TIMESTAMPTZ,
    estado           VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO'
                     CHECK (estado IN ('RECIBIDO','PROCESADO','DESCARTADO','ERROR')),
    intentos         INT         NOT NULL DEFAULT 0,
    ultimo_error     TEXT
);

-- ---------------------------------------------------------------------
-- 8. Idempotencia de escrituras desde el cliente offline
-- ---------------------------------------------------------------------
-- Toda tabla que reciba operaciones creadas offline lleva:
--   origen_offline_id UUID UNIQUE (negocio_id, origen_offline_id)
-- Si workmanager reintenta la subida, el segundo INSERT choca contra el
-- UNIQUE y el servicio devuelve el recurso ya creado en vez de duplicarlo.
