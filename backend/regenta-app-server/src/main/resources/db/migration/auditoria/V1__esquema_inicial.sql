-- =====================================================================
-- servicio-auditoria . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/15-servicio-auditoria.sql (el esquema del servicio) y de
-- 00-convenciones.sql (lo comun a todos). Cada servicio vive en su propia
-- base, asi que lo comun se repite en cada una: no hay base compartida de
-- donde tomarlo.
--
-- NO EDITAR despues de aplicada: Flyway guarda el checksum y una migracion
-- modificada falla al arrancar en vez de aplicarse en silencio. Los cambios
-- van en un V2.
--
-- Generado desde el DDL de modelo-datos/. Si el modelo cambia, el cambio
-- entra como migracion nueva, nunca reescribiendo esta.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS auditoria;
SET search_path TO auditoria, public;

-- ---------------------------------------------------------------------
-- Parte 1 . Convenciones comunes a todos los servicios
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid() en las tablas de outbox/inbox

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
-- Parte 2 . Esquema propio de servicio-auditoria
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-auditoria  (esquema: auditoria)  [plan Empresarial]
-- Cubre los dos modulos que el PDF junta: "Sincronizacion / auditoria".
-- Son dos problemas distintos y se modelan por separado:
--   A) AUDITORIA: quien cambio que y cuando (append-only, particionado).
--   B) SINCRONIZACION: la cola de operaciones que sube el cliente
--      offline-first y la resolucion de conflictos.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS auditoria;
SET search_path TO auditoria, public;

-- ---------------------------------------------------------------------
-- A) Auditoria
-- ---------------------------------------------------------------------
CREATE TABLE eventos_auditoria (
    id            UUID        NOT NULL,
    negocio_id    UUID        NOT NULL,
    sucursal_id   UUID,
    usuario_id    UUID,
    usuario_email VARCHAR(150),               -- snapshot: el usuario puede borrarse
    servicio      VARCHAR(40) NOT NULL,       -- 'servicio-ventas'
    modulo        VARCHAR(40),
    entidad_tipo  VARCHAR(60) NOT NULL,       -- 'Producto','Venta'
    entidad_id    UUID,
    accion        VARCHAR(20) NOT NULL
                  CHECK (accion IN ('CREAR','ACTUALIZAR','ELIMINAR','ANULAR','APROBAR',
                                    'LOGIN','LOGOUT','LOGIN_FALLIDO','EXPORTAR',
                                    'IMPRIMIR','CAMBIO_PERMISO','CAMBIO_PLAN')),
    -- Solo los campos que cambiaron, no la entidad completa
    cambios       JSONB,                      -- {"precio":{"antes":100,"despues":120}}
    datos_antes   JSONB,
    datos_despues JSONB,
    ip            INET,
    user_agent    TEXT,
    plataforma    VARCHAR(20),
    dispositivo_id VARCHAR(80),
    trace_id      VARCHAR(64),                -- correlaciona el request entre servicios
    resultado     VARCHAR(10) NOT NULL DEFAULT 'OK' CHECK (resultado IN ('OK','ERROR','DENEGADO')),
    ocurrido_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE eventos_auditoria_default PARTITION OF eventos_auditoria DEFAULT;
CREATE INDEX ix_audit_entidad ON eventos_auditoria (negocio_id, entidad_tipo, entidad_id, ocurrido_en DESC);
CREATE INDEX ix_audit_usuario ON eventos_auditoria (negocio_id, usuario_id, ocurrido_en DESC);
CREATE INDEX ix_audit_trace   ON eventos_auditoria (trace_id);

-- ---------------------------------------------------------------------
-- B) Sincronizacion offline-first
-- ---------------------------------------------------------------------
CREATE TABLE dispositivos (
    id             UUID PRIMARY KEY,
    negocio_id     UUID         NOT NULL,
    usuario_id     UUID         NOT NULL,
    identificador  VARCHAR(120) NOT NULL,     -- android_id / fingerprint web
    nombre         VARCHAR(80),
    plataforma     VARCHAR(20)  NOT NULL CHECK (plataforma IN ('ANDROID','WEB')),
    version_app    VARCHAR(20),
    version_esquema_local INT   NOT NULL DEFAULT 1,
    ultimo_sync_en TIMESTAMPTZ,
    cursor_sync    VARCHAR(80),               -- watermark de la ultima bajada
    activo         BOOLEAN      NOT NULL DEFAULT true,
    registrado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_dispositivo UNIQUE (negocio_id, identificador)
);

-- Cola de subida. El cliente (workmanager) envia lotes; el servidor los
-- aplica en orden por dispositivo y responde el resultado de cada uno.
CREATE TABLE operaciones_sync (
    id              UUID PRIMARY KEY,          -- generado en el cliente
    negocio_id      UUID         NOT NULL,
    dispositivo_id  UUID         NOT NULL REFERENCES dispositivos(id),
    usuario_id      UUID         NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    secuencia_local BIGINT       NOT NULL,     -- orden en que ocurrio en el cliente
    entidad_tipo    VARCHAR(60)  NOT NULL,
    entidad_id      UUID         NOT NULL,
    operacion       VARCHAR(10)  NOT NULL CHECK (operacion IN ('CREAR','ACTUALIZAR','ELIMINAR')),
    payload         JSONB        NOT NULL,
    version_base    BIGINT,                    -- version que el cliente creia tener
    creado_cliente_en TIMESTAMPTZ NOT NULL,    -- reloj del dispositivo
    recibido_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    aplicado_en     TIMESTAMPTZ,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'RECIBIDA'
                    CHECK (estado IN ('RECIBIDA','APLICADA','CONFLICTO','RECHAZADA','DESCARTADA')),
    intentos        SMALLINT     NOT NULL DEFAULT 0,
    error           TEXT,
    CONSTRAINT uq_operacion_idem UNIQUE (negocio_id, idempotency_key)
);
CREATE INDEX ix_sync_pendientes ON operaciones_sync (dispositivo_id, secuencia_local)
    WHERE estado = 'RECIBIDA';

CREATE TABLE conflictos_sync (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    operacion_id    UUID        NOT NULL REFERENCES operaciones_sync(id) ON DELETE CASCADE,
    entidad_tipo    VARCHAR(60) NOT NULL,
    entidad_id      UUID        NOT NULL,
    tipo            VARCHAR(30) NOT NULL
                    CHECK (tipo IN ('VERSION_DESACTUALIZADA','ELIMINADO_EN_SERVIDOR',
                                    'DUPLICADO','STOCK_INSUFICIENTE','REGLA_NEGOCIO')),
    version_servidor BIGINT,
    version_cliente  BIGINT,
    datos_servidor  JSONB,
    datos_cliente   JSONB,
    resolucion      VARCHAR(25)
                    CHECK (resolucion IN ('SERVIDOR_GANA','CLIENTE_GANA','FUSION','MANUAL','DESCARTADO')),
    resuelto_por    UUID,
    resuelto_en     TIMESTAMPTZ,
    detectado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_conflictos_pendientes ON conflictos_sync (negocio_id, detectado_en DESC)
    WHERE resolucion IS NULL;

-- Watermark de bajada: que cambio en el servidor desde el ultimo sync.
-- Se alimenta de los eventos de todos los servicios.
CREATE TABLE cambios_servidor (
    id           BIGSERIAL,
    negocio_id   UUID        NOT NULL,
    entidad_tipo VARCHAR(60) NOT NULL,
    entidad_id   UUID        NOT NULL,
    operacion    VARCHAR(10) NOT NULL,
    version      BIGINT      NOT NULL,
    payload      JSONB,
    ocurrido_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE cambios_servidor_default PARTITION OF cambios_servidor DEFAULT;
CREATE INDEX ix_cambios_negocio ON cambios_servidor (negocio_id, ocurrido_en);

-- Politica de retencion (el plan define cuanto se conserva)
CREATE TABLE politicas_retencion (
    negocio_id   UUID PRIMARY KEY,
    dias_auditoria INT NOT NULL DEFAULT 365,
    dias_sync      INT NOT NULL DEFAULT 30,
    dias_cambios   INT NOT NULL DEFAULT 90
);
