-- =====================================================================
-- servicio-mesas . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/09-servicio-mesas.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS mesas;
SET search_path TO mesas, public;

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
-- Parte 2 . Esquema propio de servicio-mesas
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-mesas  (esquema: mesas)
-- PATRON: Comanda. El PDF lo marca como "sin equivalente directo" en
-- Venta directa: es el mapa fisico del salon y el estado de ocupacion.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS mesas;
SET search_path TO mesas, public;

CREATE TABLE zonas (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    nombre      VARCHAR(60) NOT NULL,        -- 'Salon','Terraza','Barra','VIP'
    orden       INT         NOT NULL DEFAULT 0,
    color       CHAR(7),
    activa      BOOLEAN     NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_zona ON zonas
    (negocio_id, COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid), lower(nombre));

CREATE TABLE mesas (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    zona_id     UUID        REFERENCES zonas(id),
    codigo      VARCHAR(20) NOT NULL,        -- 'M1','T04'
    nombre      VARCHAR(60),
    capacidad   SMALLINT    NOT NULL DEFAULT 4 CHECK (capacidad > 0),
    forma       VARCHAR(20) NOT NULL DEFAULT 'CUADRADA'
                CHECK (forma IN ('CUADRADA','REDONDA','RECTANGULAR','BARRA')),
    estado      VARCHAR(20) NOT NULL DEFAULT 'LIBRE'
                CHECK (estado IN ('LIBRE','OCUPADA','RESERVADA','CUENTA_PEDIDA','SUCIA','BLOQUEADA')),
    -- Coordenadas para el plano del salon en la app
    pos_x       INT         NOT NULL DEFAULT 0,
    pos_y       INT         NOT NULL DEFAULT 0,
    ancho       INT         NOT NULL DEFAULT 80,
    alto        INT         NOT NULL DEFAULT 80,
    qr_token    VARCHAR(64) UNIQUE,          -- pedido desde el celular del comensal
    activa      BOOLEAN     NOT NULL DEFAULT true,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_mesa_codigo UNIQUE (negocio_id, codigo)
);
CREATE INDEX ix_mesas_zona ON mesas (negocio_id, zona_id) WHERE activa;
CREATE INDEX ix_mesas_estado ON mesas (negocio_id, estado);

-- Una sesion es "esta ocupacion de la mesa". Separarla de `mesas` permite
-- historial (cuantas veces roto la mesa 5 hoy) y union de mesas.
CREATE TABLE sesiones_mesa (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    sucursal_id    UUID,
    mesa_principal_id UUID     NOT NULL REFERENCES mesas(id),
    comanda_id     UUID,                       -- [ref logica -> comandas]
    mesero_usuario_id UUID,
    num_comensales SMALLINT    NOT NULL DEFAULT 1,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                   CHECK (estado IN ('ABIERTA','CUENTA_PEDIDA','CERRADA','ANULADA')),
    abierta_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrada_en     TIMESTAMPTZ,
    duracion_min   INT GENERATED ALWAYS AS
                   (CASE WHEN cerrada_en IS NULL THEN NULL
                    ELSE EXTRACT(EPOCH FROM (cerrada_en - abierta_en))::int / 60 END) STORED,
    version        BIGINT      NOT NULL DEFAULT 0
);
-- Una sola sesion abierta por mesa a la vez
CREATE UNIQUE INDEX uq_sesion_abierta
    ON sesiones_mesa (mesa_principal_id) WHERE estado IN ('ABIERTA','CUENTA_PEDIDA');
CREATE INDEX ix_sesiones_fecha ON sesiones_mesa (negocio_id, abierta_en DESC);

-- Union de mesas (grupo de 10 personas ocupa M1+M2)
CREATE TABLE sesion_mesas (
    sesion_id  UUID NOT NULL REFERENCES sesiones_mesa(id) ON DELETE CASCADE,
    mesa_id    UUID NOT NULL REFERENCES mesas(id),
    negocio_id UUID NOT NULL,
    PRIMARY KEY (sesion_id, mesa_id)
);

-- Reservas de mesa (restaurante que toma reservas SIN usar el patron Reserva
-- completo: aqui la transaccion sigue siendo la comanda, no la reserva).
CREATE TABLE reservas_mesa (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    mesa_id      UUID        REFERENCES mesas(id),
    zona_id      UUID        REFERENCES zonas(id),
    cliente_id   UUID,
    nombre_contacto VARCHAR(120) NOT NULL,
    telefono     VARCHAR(30),
    num_personas SMALLINT    NOT NULL,
    desde        TIMESTAMPTZ NOT NULL,
    hasta        TIMESTAMPTZ NOT NULL,
    estado       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                 CHECK (estado IN ('PENDIENTE','CONFIRMADA','SENTADA','CANCELADA','NO_SHOW')),
    notas        TEXT,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reserva_mesa_periodo CHECK (hasta > desde)
);
CREATE INDEX ix_reservas_mesa_fecha ON reservas_mesa (negocio_id, desde);

-- EVENTOS PUBLICADOS: mesa_ocupada, mesa_liberada, sesion_mesa_abierta,
--                     sesion_mesa_cerrada
-- EVENTOS CONSUMIDOS: comanda_cerrada (libera la mesa -> estado SUCIA)
