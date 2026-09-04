-- =====================================================================
-- servicio-caja . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/12-servicio-caja.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS caja;
SET search_path TO caja, public;

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
-- Parte 2 . Esquema propio de servicio-caja
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-caja  (esquema: caja)  [POS / Caja, plan Empresarial]
-- HALLAZGO: el modulo POS/Caja aparece en la tabla de planes pero no
-- tiene servicio en el scaffold. No puede vivir dentro de Ventas: en el
-- patron Comanda la caja tambien recibe pagos de comandas, y en Reserva
-- recibe anticipos. Es transversal a los tres patrones.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS caja;
SET search_path TO caja, public;

CREATE TABLE cajas (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    codigo      VARCHAR(20) NOT NULL,
    nombre      VARCHAR(60) NOT NULL,
    terminal_id VARCHAR(80),                  -- identificador del dispositivo
    activa      BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_caja UNIQUE (negocio_id, codigo)
);

CREATE TABLE sesiones_caja (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID        NOT NULL,
    sucursal_id        UUID,
    caja_id            UUID        NOT NULL REFERENCES cajas(id),
    numero             VARCHAR(30) NOT NULL,
    usuario_apertura_id UUID       NOT NULL,
    abierta_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    monto_apertura     NUMERIC(16,4) NOT NULL DEFAULT 0,
    -- Cierre / arqueo
    usuario_cierre_id  UUID,
    cerrada_en         TIMESTAMPTZ,
    monto_esperado     NUMERIC(16,4),          -- calculado por el sistema
    monto_declarado    NUMERIC(16,4),          -- contado por el cajero
    diferencia         NUMERIC(16,4) GENERATED ALWAYS AS
                       (COALESCE(monto_declarado,0) - COALESCE(monto_esperado,0)) STORED,
    -- Totales del turno por metodo (se persisten al cerrar)
    total_efectivo     NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_tarjetas     NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_transferencias NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_otros        NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_propinas     NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_ingresos     NUMERIC(16,4) NOT NULL DEFAULT 0,
    total_retiros      NUMERIC(16,4) NOT NULL DEFAULT 0,
    num_transacciones  INT         NOT NULL DEFAULT 0,
    estado             VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                       CHECK (estado IN ('ABIERTA','CERRADA','CUADRADA','DESCUADRADA')),
    observaciones      TEXT,
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_sesion_caja_numero UNIQUE (negocio_id, numero)
);
-- Una sola sesion abierta por caja
CREATE UNIQUE INDEX uq_sesion_caja_abierta
    ON sesiones_caja (caja_id) WHERE estado = 'ABIERTA';
CREATE INDEX ix_sesiones_caja_fecha ON sesiones_caja (negocio_id, abierta_en DESC);

CREATE TABLE movimientos_caja (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    sesion_id     UUID        NOT NULL REFERENCES sesiones_caja(id) ON DELETE CASCADE,
    tipo          VARCHAR(25) NOT NULL
                  CHECK (tipo IN ('VENTA','COMANDA','RESERVA','DEVOLUCION','RECAUDO',
                                  'INGRESO','RETIRO','GASTO','AJUSTE','APERTURA')),
    signo         SMALLINT    NOT NULL CHECK (signo IN (-1,1)),
    metodo_pago   VARCHAR(20) NOT NULL
                  CHECK (metodo_pago IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                                         'TRANSFERENCIA','QR','BONO','CREDITO','OTRO')),
    monto         NUMERIC(16,4) NOT NULL CHECK (monto > 0),
    propina       NUMERIC(16,4) NOT NULL DEFAULT 0,
    -- Origen polimorfico: sirve a los tres patrones
    origen_tipo   VARCHAR(20),
    origen_id     UUID,                        -- [ref logica]
    documento_ref VARCHAR(40),
    concepto      VARCHAR(200),
    usuario_id    UUID        NOT NULL,
    autorizado_por UUID,                       -- retiros exigen autorizacion
    idempotency_key VARCHAR(120) NOT NULL,
    ocurrido_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mov_caja_idem UNIQUE (negocio_id, idempotency_key)
);
CREATE INDEX ix_mov_caja_sesion ON movimientos_caja (sesion_id, ocurrido_en);

-- Conteo de billetes/monedas al cerrar
CREATE TABLE arqueo_denominaciones (
    id           UUID PRIMARY KEY,
    negocio_id   UUID NOT NULL,
    sesion_id    UUID NOT NULL REFERENCES sesiones_caja(id) ON DELETE CASCADE,
    denominacion NUMERIC(12,2) NOT NULL,
    tipo         VARCHAR(10) NOT NULL CHECK (tipo IN ('BILLETE','MONEDA')),
    cantidad     INT NOT NULL CHECK (cantidad >= 0),
    subtotal     NUMERIC(16,4) GENERATED ALWAYS AS (denominacion * cantidad) STORED,
    CONSTRAINT uq_denominacion UNIQUE (sesion_id, denominacion)
);

CREATE TABLE turnos (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sesion_id   UUID        NOT NULL REFERENCES sesiones_caja(id) ON DELETE CASCADE,
    usuario_id  UUID        NOT NULL,
    inicio_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    fin_en      TIMESTAMPTZ,
    monto_inicio NUMERIC(16,4) NOT NULL DEFAULT 0,
    monto_fin   NUMERIC(16,4)
);

-- EVENTOS PUBLICADOS: caja_abierta, caja_cerrada, caja_descuadrada,
--                     retiro_registrado
-- EVENTOS CONSUMIDOS: venta_completada, pedido_completado,
--                     reserva_confirmada, devolucion_registrada
