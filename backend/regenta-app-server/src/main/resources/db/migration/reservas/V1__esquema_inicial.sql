-- =====================================================================
-- servicio-reservas . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/07-servicio-reservas.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS reservas;
SET search_path TO reservas, public;

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
-- Parte 2 . Esquema propio de servicio-reservas
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-reservas  (esquema: reservas)
-- PATRON: Reserva — modulo de TRANSACCION (equivale a Ventas).
--
-- EL PUNTO TECNICO CENTRAL DE ESTE PATRON: el overbooking NO se evita
-- con un "SELECT ... WHERE solapa" en el servicio. Dos requests
-- concurrentes leen ambos "libre" y ambos insertan. Se evita con un
-- constraint de exclusion GiST, que es atomico a nivel de base de datos.
-- Esto es lo que hace que `periodo TSTZRANGE` sea la representacion
-- correcta y no dos columnas inicio/fin sueltas.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS reservas;
SET search_path TO reservas, public;

CREATE TABLE consecutivos (
    negocio_id UUID        NOT NULL,
    tipo       VARCHAR(20) NOT NULL,
    prefijo    VARCHAR(10) NOT NULL DEFAULT '',
    siguiente  BIGINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (negocio_id, tipo)
);

CREATE TABLE reservas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    sucursal_id       UUID,
    numero            VARCHAR(30)  NOT NULL,
    -- Quien reserva
    cliente_id        UUID,                        -- [ref logica -> crm]
    cliente_snapshot  JSONB,
    -- Que se reserva
    tipo_recurso_id   UUID         NOT NULL,       -- [ref logica -> recursos]
    recurso_id        UUID,                        -- NULL mientras no se asigne habitacion concreta
    recurso_snapshot  JSONB,
    -- Cuando
    periodo           TSTZRANGE    NOT NULL,
    -- No puede ser GENERATED: el cast tstz->date depende de la zona horaria
    -- de la sesion y Postgres exige expresiones inmutables. Lo calcula el
    -- servicio con la zona horaria del negocio y lo persiste aqui.
    noches            INT          NOT NULL DEFAULT 1 CHECK (noches >= 1),
    num_personas      SMALLINT     NOT NULL DEFAULT 1 CHECK (num_personas > 0),
    num_adultos       SMALLINT     NOT NULL DEFAULT 1,
    num_ninos         SMALLINT     NOT NULL DEFAULT 0,
    -- Ciclo de vida
    estado            VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE','CONFIRMADA','CHECK_IN','CHECK_OUT',
                                        'CANCELADA','NO_SHOW','EXPIRADA')),
    canal             VARCHAR(20)  NOT NULL DEFAULT 'MOSTRADOR'
                      CHECK (canal IN ('MOSTRADOR','APP','WEB','TELEFONO','OTA')),
    referencia_externa VARCHAR(60),                -- codigo de Booking/Airbnb
    -- Dinero
    tarifa_id         UUID,
    politica_cancelacion_id UUID,
    subtotal          NUMERIC(16,4) NOT NULL DEFAULT 0,
    descuento_total   NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto_total    NUMERIC(16,4) NOT NULL DEFAULT 0,
    total             NUMERIC(16,4) NOT NULL DEFAULT 0,
    anticipo          NUMERIC(16,4) NOT NULL DEFAULT 0,
    saldo             NUMERIC(16,4) NOT NULL DEFAULT 0,
    penalizacion      NUMERIC(16,4) NOT NULL DEFAULT 0,
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    -- Facturacion
    factura_id        UUID,
    estado_factura    VARCHAR(20)  DEFAULT 'NO_APLICA',
    -- Trazabilidad
    usuario_id        UUID,
    notas             TEXT,
    notas_internas    TEXT,
    confirmada_en     TIMESTAMPTZ,
    cancelada_en      TIMESTAMPTZ,
    motivo_cancelacion TEXT,
    origen_offline_id UUID,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_reserva_numero UNIQUE (negocio_id, numero),
    CONSTRAINT ck_periodo CHECK (NOT isempty(periodo) AND lower(periodo) < upper(periodo)),
    -- ***** ANTI-OVERBOOKING *****
    -- Un mismo recurso no puede tener dos periodos solapados en estados
    -- que ocupan fisicamente el recurso. Las canceladas/no-show no cuentan.
    CONSTRAINT ex_reserva_solape EXCLUDE USING gist (
        negocio_id  WITH =,
        recurso_id  WITH =,
        periodo     WITH &&
    ) WHERE (recurso_id IS NOT NULL
             AND estado IN ('PENDIENTE','CONFIRMADA','CHECK_IN'))
);
CREATE UNIQUE INDEX uq_reserva_offline
    ON reservas (negocio_id, origen_offline_id) WHERE origen_offline_id IS NOT NULL;
CREATE INDEX ix_reservas_periodo  ON reservas USING gist (periodo);
-- Llegadas del dia. Se indexa lower(periodo) como TIMESTAMPTZ, no casteado
-- a DATE: el cast depende de la zona horaria de la sesion y no es inmutable,
-- asi que Postgres no lo admite en un indice. El servicio consulta con un
-- rango [inicio_dia, fin_dia) construido en la zona horaria del negocio.
CREATE INDEX ix_reservas_llegadas ON reservas (negocio_id, lower(periodo))
    WHERE estado IN ('PENDIENTE','CONFIRMADA');
CREATE INDEX ix_reservas_cliente  ON reservas (negocio_id, cliente_id, creado_en DESC);
CREATE INDEX ix_reservas_estado   ON reservas (negocio_id, estado);

-- Si el negocio permite overbooking controlado (tipos_recurso.permite_overbooking),
-- el EXCLUDE se relaja: se aplica solo sobre recurso_id ya asignado y se
-- gestiona un cupo por tipo_recurso con un contador transaccional aparte.
CREATE TABLE cupos_tipo_recurso (
    negocio_id      UUID NOT NULL,
    tipo_recurso_id UUID NOT NULL,
    fecha           DATE NOT NULL,
    capacidad       INT  NOT NULL,
    reservado       INT  NOT NULL DEFAULT 0,
    disponible      INT  GENERATED ALWAYS AS (capacidad - reservado) STORED,
    PRIMARY KEY (negocio_id, tipo_recurso_id, fecha),
    CONSTRAINT ck_cupo CHECK (reservado >= 0)
);

CREATE TABLE reserva_servicios (
    id            UUID PRIMARY KEY,
    negocio_id    UUID NOT NULL,
    reserva_id    UUID NOT NULL REFERENCES reservas(id) ON DELETE CASCADE,
    servicio_id   UUID NOT NULL,                -- [ref logica -> recursos.servicios_adicionales]
    nombre_snapshot VARCHAR(120) NOT NULL,
    cantidad      NUMERIC(12,4) NOT NULL DEFAULT 1,
    precio_unitario NUMERIC(14,4) NOT NULL,
    impuesto_pct  NUMERIC(7,4)  NOT NULL DEFAULT 0,
    total         NUMERIC(16,4) NOT NULL
);

CREATE TABLE ocupantes (
    id           UUID PRIMARY KEY,
    negocio_id   UUID NOT NULL,
    reserva_id   UUID NOT NULL REFERENCES reservas(id) ON DELETE CASCADE,
    es_titular   BOOLEAN NOT NULL DEFAULT false,
    nombres      VARCHAR(120) NOT NULL,
    apellidos    VARCHAR(120),
    tipo_documento VARCHAR(10),
    numero_documento VARCHAR(30),
    nacionalidad CHAR(2),
    fecha_nacimiento DATE,
    telefono     VARCHAR(30),
    email        VARCHAR(150)
);
CREATE INDEX ix_ocupantes_reserva ON ocupantes (reserva_id);

-- Check-in / Check-out: modulo "Extension de Ventas" del patron Reserva.
CREATE TABLE estancias (
    id                  UUID PRIMARY KEY,
    negocio_id          UUID        NOT NULL,
    reserva_id          UUID        NOT NULL UNIQUE REFERENCES reservas(id),
    recurso_asignado_id UUID        NOT NULL,     -- [ref logica]
    check_in_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    check_in_usuario_id UUID,
    check_out_en        TIMESTAMPTZ,
    check_out_usuario_id UUID,
    check_out_previsto  TIMESTAMPTZ NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO'
                        CHECK (estado IN ('EN_CURSO','FINALIZADA','EXTENDIDA')),
    consumo_total       NUMERIC(16,4) NOT NULL DEFAULT 0,
    deposito            NUMERIC(16,4) NOT NULL DEFAULT 0,
    observaciones_entrada TEXT,
    observaciones_salida  TEXT,
    version             BIGINT      NOT NULL DEFAULT 0
);

-- Consumos cargados a la habitacion (minibar, restaurante, spa).
-- Si producto_id no es NULL, al cerrar la estancia se publica un evento
-- que Inventario consume para descontar stock.
CREATE TABLE consumos_estancia (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    estancia_id  UUID        NOT NULL REFERENCES estancias(id) ON DELETE CASCADE,
    origen       VARCHAR(20) NOT NULL DEFAULT 'MINIBAR'
                 CHECK (origen IN ('MINIBAR','RESTAURANTE','SPA','LAVANDERIA','TELEFONO','OTRO')),
    producto_id  UUID,                          -- [ref logica -> inventario]
    comanda_id   UUID,                          -- [ref logica -> comandas] hotel con restaurante
    descripcion  VARCHAR(180) NOT NULL,
    cantidad     NUMERIC(12,4) NOT NULL DEFAULT 1,
    precio_unitario NUMERIC(14,4) NOT NULL,
    impuesto_pct NUMERIC(7,4)  NOT NULL DEFAULT 0,
    total        NUMERIC(16,4) NOT NULL,
    cargado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id   UUID
);
CREATE INDEX ix_consumos_estancia ON consumos_estancia (estancia_id);

CREATE TABLE pagos_reserva (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    reserva_id  UUID        NOT NULL REFERENCES reservas(id) ON DELETE CASCADE,
    tipo        VARCHAR(20) NOT NULL
                CHECK (tipo IN ('ANTICIPO','SALDO','DEPOSITO','PENALIZACION','REEMBOLSO','CONSUMO')),
    metodo      VARCHAR(20) NOT NULL
                CHECK (metodo IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                                  'TRANSFERENCIA','QR','OTA','OTRO')),
    monto       NUMERIC(16,4) NOT NULL,
    referencia  VARCHAR(80),
    caja_sesion_id UUID,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id  UUID
);

-- Historial de estados para auditoria del ciclo de la reserva
CREATE TABLE reserva_eventos (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    reserva_id  UUID        NOT NULL REFERENCES reservas(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20) NOT NULL,
    usuario_id  UUID,
    detalle     TEXT,
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- EVENTOS PUBLICADOS: reserva_creada, reserva_confirmada, reserva_cancelada,
--                     check_in_registrado, check_out_registrado,
--                     estancia_finalizada (dispara facturacion + descuento
--                     de insumos consumidos)
-- EVENTOS CONSUMIDOS: recurso_bloqueado, tarifa_actualizada, factura_emitida
