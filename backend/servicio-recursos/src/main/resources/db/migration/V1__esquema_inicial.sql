-- =====================================================================
-- servicio-recursos . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/06-servicio-recursos.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS recursos;
SET search_path TO recursos, public;

-- ---------------------------------------------------------------------
-- Parte 1 . Convenciones comunes a todos los servicios
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid() en las tablas de outbox/inbox
CREATE EXTENSION IF NOT EXISTS "btree_gist";   -- constraints EXCLUDE mixtos

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
-- Parte 2 . Esquema propio de servicio-recursos
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-recursos  (esquema: recursos)
-- PATRON: Reserva — modulo de CATALOGO (equivale a Inventario).
-- La diferencia con Inventario: un recurso no se consume, se OCUPA
-- durante un intervalo de tiempo. Por eso no hay "stock" sino
-- disponibilidad sobre una linea de tiempo.
-- Misma tecnica de generalizacion: tipos_recurso + atributos JSONB
-- (hotel, spa, canchas, consultorios = configuracion, no codigo).
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS recursos;
SET search_path TO recursos, public;

CREATE TABLE tipos_recurso (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID        NOT NULL,
    nombre             VARCHAR(100) NOT NULL,   -- 'Habitacion Doble','Cancha F5','Consultorio'
    descripcion        TEXT,
    unidad_tiempo      VARCHAR(20) NOT NULL DEFAULT 'NOCHE'
                       CHECK (unidad_tiempo IN ('MINUTO','HORA','NOCHE','DIA','SESION')),
    duracion_minima_min INT        NOT NULL DEFAULT 60,
    incremento_min     INT         NOT NULL DEFAULT 30,   -- granularidad de reserva
    capacidad_default  SMALLINT    NOT NULL DEFAULT 1,
    permite_overbooking BOOLEAN    NOT NULL DEFAULT false,
    buffer_antes_min   INT         NOT NULL DEFAULT 0,    -- limpieza / preparacion
    buffer_despues_min INT         NOT NULL DEFAULT 0,
    activo             BOOLEAN     NOT NULL DEFAULT true,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_tipo_recurso ON tipos_recurso (negocio_id, lower(nombre));

-- Espejo exacto de inventario.atributos_categoria: misma idea de
-- configuracion sin codigo nuevo, aplicada al patron Reserva.
CREATE TABLE atributos_tipo_recurso (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    tipo_recurso_id UUID        NOT NULL REFERENCES tipos_recurso(id) ON DELETE CASCADE,
    nombre_campo    VARCHAR(50) NOT NULL,
    etiqueta        VARCHAR(80) NOT NULL,
    tipo            VARCHAR(20) NOT NULL
                    CHECK (tipo IN ('TEXTO','NUMERO','DECIMAL','FECHA','BOOLEANO','LISTA','MULTILISTA')),
    obligatorio     BOOLEAN     NOT NULL DEFAULT false,
    opciones        JSONB,
    orden           INT         NOT NULL DEFAULT 0,
    activo          BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_atr_tipo_recurso UNIQUE (tipo_recurso_id, nombre_campo)
);

CREATE TABLE recursos (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    sucursal_id     UUID,
    tipo_recurso_id UUID        NOT NULL REFERENCES tipos_recurso(id),
    codigo          VARCHAR(30) NOT NULL,       -- '101','Cancha 2'
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    capacidad       SMALLINT    NOT NULL DEFAULT 1,
    piso            VARCHAR(20),
    zona            VARCHAR(60),
    estado          VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                    CHECK (estado IN ('DISPONIBLE','OCUPADO','LIMPIEZA','MANTENIMIENTO','FUERA_SERVICIO')),
    -- Campos variables por tipo: vista al mar, aire acondicionado,
    -- superficie sintetica, equipamiento medico...
    atributos       JSONB       NOT NULL DEFAULT '{}'::jsonb,
    imagen_url      TEXT,
    activo          BOOLEAN     NOT NULL DEFAULT true,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    eliminado_en    TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_recurso_codigo UNIQUE (negocio_id, codigo)
);
CREATE INDEX ix_recursos_tipo ON recursos (negocio_id, tipo_recurso_id) WHERE activo;
CREATE INDEX ix_recursos_atributos ON recursos USING gin (atributos jsonb_path_ops);

-- Tarifas por temporada / dia de la semana / franja horaria.
-- `prioridad` resuelve solapamientos: gana la de mayor prioridad.
CREATE TABLE tarifas (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    tipo_recurso_id UUID        REFERENCES tipos_recurso(id) ON DELETE CASCADE,
    recurso_id      UUID        REFERENCES recursos(id) ON DELETE CASCADE, -- tarifa puntual
    nombre          VARCHAR(80) NOT NULL,       -- 'Temporada alta','Fin de semana'
    unidad_tiempo   VARCHAR(20) NOT NULL
                    CHECK (unidad_tiempo IN ('MINUTO','HORA','NOCHE','DIA','SESION')),
    precio_base     NUMERIC(14,4) NOT NULL CHECK (precio_base >= 0),
    precio_persona_adicional NUMERIC(14,4) NOT NULL DEFAULT 0,
    moneda          CHAR(3)     NOT NULL DEFAULT 'COP',
    impuesto_id     UUID,
    vigente_desde   DATE,
    vigente_hasta   DATE,
    dias_semana     SMALLINT[]  NOT NULL DEFAULT '{1,2,3,4,5,6,7}',
    hora_desde      TIME,
    hora_hasta      TIME,
    estancia_minima INT         NOT NULL DEFAULT 1,
    prioridad       SMALLINT    NOT NULL DEFAULT 0,
    activa          BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT ck_tarifa_destino CHECK (tipo_recurso_id IS NOT NULL OR recurso_id IS NOT NULL)
);
CREATE INDEX ix_tarifas_vigencia ON tarifas (negocio_id, tipo_recurso_id, vigente_desde, vigente_hasta)
    WHERE activa;

-- Horario en que el recurso puede reservarse (canchas 6am-11pm, hotel 24h)
CREATE TABLE reglas_disponibilidad (
    id              UUID PRIMARY KEY,
    negocio_id      UUID     NOT NULL,
    tipo_recurso_id UUID     REFERENCES tipos_recurso(id) ON DELETE CASCADE,
    recurso_id      UUID     REFERENCES recursos(id) ON DELETE CASCADE,
    dia_semana      SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_apertura   TIME     NOT NULL,
    hora_cierre     TIME     NOT NULL,
    activa          BOOLEAN  NOT NULL DEFAULT true,
    CONSTRAINT ck_horario CHECK (hora_cierre > hora_apertura)
);

-- Bloqueos (mantenimiento, evento privado, feriado).
-- El EXCLUDE evita que se solapen dos bloqueos del mismo recurso.
CREATE TABLE bloqueos_recurso (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    recurso_id  UUID        NOT NULL REFERENCES recursos(id) ON DELETE CASCADE,
    periodo     TSTZRANGE   NOT NULL,
    motivo      VARCHAR(30) NOT NULL
                CHECK (motivo IN ('MANTENIMIENTO','LIMPIEZA','EVENTO','FERIADO','OTRO')),
    detalle     TEXT,
    usuario_id  UUID,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&)
);
CREATE INDEX ix_bloqueos_periodo ON bloqueos_recurso USING gist (periodo);

CREATE TABLE servicios_adicionales (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    codigo      VARCHAR(30) NOT NULL,
    nombre      VARCHAR(120) NOT NULL,          -- 'Desayuno','Alquiler de raqueta'
    descripcion TEXT,
    precio      NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_id UUID,
    modo_cobro  VARCHAR(20) NOT NULL DEFAULT 'POR_ESTANCIA'
                CHECK (modo_cobro IN ('POR_ESTANCIA','POR_NOCHE','POR_PERSONA','POR_PERSONA_NOCHE','POR_UNIDAD')),
    producto_id UUID,                           -- [ref logica] si descuenta inventario
    activo      BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_servicio_adicional UNIQUE (negocio_id, codigo)
);

CREATE TABLE politicas_cancelacion (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    nombre         VARCHAR(80) NOT NULL,
    horas_antes    INT         NOT NULL,     -- >= N horas antes: sin penalizacion
    penalizacion_pct NUMERIC(7,4) NOT NULL DEFAULT 0,
    anticipo_requerido_pct NUMERIC(7,4) NOT NULL DEFAULT 0,
    es_default     BOOLEAN     NOT NULL DEFAULT false,
    activa         BOOLEAN     NOT NULL DEFAULT true
);

-- EVENTOS PUBLICADOS: recurso_creado, recurso_bloqueado, tarifa_actualizada
