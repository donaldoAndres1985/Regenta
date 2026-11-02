-- =====================================================================
-- servicio-ventas . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/04-servicio-ventas.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS ventas;
SET search_path TO ventas, public;

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
-- Parte 2 . Esquema propio de servicio-ventas
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-ventas  (esquema: ventas)
-- PATRON: Venta directa — modulo de TRANSACCION.
-- Publica `venta_completada`; Facturacion, Reportes, CRM y Caja lo
-- consumen sin que Ventas sepa que existen.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS ventas;
SET search_path TO ventas, public;

-- Consecutivos por negocio y por tipo de documento. NO se usa un
-- BIGSERIAL global: el numero de venta debe ser continuo POR NEGOCIO.
CREATE TABLE consecutivos (
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    tipo        VARCHAR(20) NOT NULL,   -- 'VENTA','DEVOLUCION','COTIZACION'
    prefijo     VARCHAR(10) NOT NULL DEFAULT '',
    siguiente   BIGINT      NOT NULL DEFAULT 1,
    sucursal_key UUID GENERATED ALWAYS AS (COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    PRIMARY KEY (negocio_id, sucursal_key, tipo)
);

CREATE TABLE ventas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    sucursal_id       UUID,                        -- [ref logica]
    bodega_id         UUID         NOT NULL,       -- [ref logica -> inventario]
    numero            VARCHAR(30)  NOT NULL,
    cliente_id        UUID,                        -- [ref logica -> crm]; NULL = consumidor final
    cliente_snapshot  JSONB,                       -- nombre/doc al momento de la venta
    usuario_id        UUID         NOT NULL,       -- vendedor [ref logica]
    caja_sesion_id    UUID,                        -- [ref logica -> caja]
    lista_precios_id  UUID,
    canal             VARCHAR(20)  NOT NULL DEFAULT 'MOSTRADOR'
                      CHECK (canal IN ('MOSTRADOR','APP','WEB','TELEFONO','DOMICILIO')),
    estado            VARCHAR(25)  NOT NULL DEFAULT 'BORRADOR'
                      CHECK (estado IN ('BORRADOR','PENDIENTE_STOCK','CONFIRMADA',
                                        'DEVUELTA_PARCIAL','DEVUELTA','ANULADA')),
    fecha             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Totales (todos calculados y persistidos, no derivados en la consulta)
    subtotal          NUMERIC(16,4) NOT NULL DEFAULT 0,
    descuento_total   NUMERIC(16,4) NOT NULL DEFAULT 0,
    base_gravable     NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto_total    NUMERIC(16,4) NOT NULL DEFAULT 0,
    total             NUMERIC(16,4) NOT NULL DEFAULT 0,
    costo_total       NUMERIC(16,4) NOT NULL DEFAULT 0,  -- para margen en Reportes
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    tasa_cambio       NUMERIC(14,6) NOT NULL DEFAULT 1,
    -- Credito
    forma_pago        VARCHAR(20)  NOT NULL DEFAULT 'CONTADO'
                      CHECK (forma_pago IN ('CONTADO','CREDITO','MIXTO')),
    saldo_pendiente   NUMERIC(16,4) NOT NULL DEFAULT 0,
    fecha_vencimiento DATE,
    -- Facturacion
    requiere_factura  BOOLEAN      NOT NULL DEFAULT false,
    factura_id        UUID,                        -- [ref logica -> facturacion]
    estado_factura    VARCHAR(20)  DEFAULT 'NO_APLICA'
                      CHECK (estado_factura IN ('NO_APLICA','PENDIENTE','EMITIDA','RECHAZADA')),
    -- Offline
    origen_offline_id UUID,
    dispositivo_id    VARCHAR(80),
    sincronizado_en   TIMESTAMPTZ,
    nota              TEXT,
    anulada_en        TIMESTAMPTZ,
    anulada_por       UUID,
    motivo_anulacion  TEXT,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_venta_numero UNIQUE (negocio_id, numero),
    CONSTRAINT ck_total CHECK (total >= 0),
    CONSTRAINT ck_anulacion CHECK (estado <> 'ANULADA' OR anulada_en IS NOT NULL)
);
CREATE UNIQUE INDEX uq_venta_offline
    ON ventas (negocio_id, origen_offline_id) WHERE origen_offline_id IS NOT NULL;
CREATE INDEX ix_ventas_fecha    ON ventas (negocio_id, fecha DESC);
CREATE INDEX ix_ventas_cliente  ON ventas (negocio_id, cliente_id, fecha DESC);
CREATE INDEX ix_ventas_usuario  ON ventas (negocio_id, usuario_id, fecha DESC);
CREATE INDEX ix_ventas_sucursal ON ventas (negocio_id, sucursal_id, fecha DESC);

CREATE TABLE venta_lineas (
    id                  UUID PRIMARY KEY,
    negocio_id          UUID         NOT NULL,
    venta_id            UUID         NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    linea               SMALLINT     NOT NULL,
    producto_id         UUID         NOT NULL,     -- [ref logica -> inventario]
    lote_id             UUID,
    serie_id            UUID,
    -- SNAPSHOTS: la linea no debe cambiar si mañana editan el producto.
    sku_snapshot        VARCHAR(60)  NOT NULL,
    nombre_snapshot     VARCHAR(180) NOT NULL,
    unidad_snapshot     VARCHAR(20),
    cantidad            NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    precio_unitario     NUMERIC(14,4) NOT NULL CHECK (precio_unitario >= 0),
    descuento_pct       NUMERIC(7,4)  NOT NULL DEFAULT 0,
    descuento_valor     NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_id         UUID,
    impuesto_codigo     VARCHAR(20),
    impuesto_pct        NUMERIC(7,4)  NOT NULL DEFAULT 0,
    impuesto_valor      NUMERIC(14,4) NOT NULL DEFAULT 0,
    base_gravable       NUMERIC(16,4) NOT NULL DEFAULT 0,
    subtotal            NUMERIC(16,4) NOT NULL,
    total               NUMERIC(16,4) NOT NULL,
    costo_unitario_snapshot NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad_devuelta   NUMERIC(18,6) NOT NULL DEFAULT 0,
    CONSTRAINT uq_venta_linea UNIQUE (venta_id, linea),
    CONSTRAINT ck_devuelta CHECK (cantidad_devuelta <= cantidad)
);
CREATE INDEX ix_venta_lineas_producto ON venta_lineas (negocio_id, producto_id);

CREATE TABLE pagos_venta (
    id             UUID PRIMARY KEY,
    negocio_id     UUID         NOT NULL,
    venta_id       UUID         NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    metodo         VARCHAR(20)  NOT NULL
                   CHECK (metodo IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                                     'TRANSFERENCIA','QR','CREDITO','BONO','OTRO')),
    monto          NUMERIC(16,4) NOT NULL CHECK (monto > 0),
    monto_recibido NUMERIC(16,4),            -- efectivo entregado
    cambio         NUMERIC(16,4) NOT NULL DEFAULT 0,
    referencia     VARCHAR(80),              -- voucher, ultimos 4 digitos
    franquicia     VARCHAR(30),
    caja_sesion_id UUID,
    recibido_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_pagos_venta ON pagos_venta (venta_id);

CREATE TABLE devoluciones (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    venta_id     UUID        NOT NULL REFERENCES ventas(id),
    numero       VARCHAR(30) NOT NULL,
    tipo         VARCHAR(20) NOT NULL DEFAULT 'TOTAL'
                 CHECK (tipo IN ('TOTAL','PARCIAL')),
    motivo       VARCHAR(40) NOT NULL
                 CHECK (motivo IN ('DEFECTUOSO','ERROR_DESPACHO','ARREPENTIMIENTO','GARANTIA','OTRO')),
    detalle      TEXT,
    reintegra_stock BOOLEAN  NOT NULL DEFAULT true,
    bodega_destino_id UUID,
    total        NUMERIC(16,4) NOT NULL,
    estado       VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA'
                 CHECK (estado IN ('REGISTRADA','APROBADA','ANULADA')),
    nota_credito_id UUID,                    -- [ref logica -> facturacion]
    fecha        TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id   UUID,
    CONSTRAINT uq_devolucion_numero UNIQUE (negocio_id, numero)
);

CREATE TABLE devolucion_lineas (
    id              UUID PRIMARY KEY,
    negocio_id      UUID NOT NULL,
    devolucion_id   UUID NOT NULL REFERENCES devoluciones(id) ON DELETE CASCADE,
    venta_linea_id  UUID NOT NULL REFERENCES venta_lineas(id),
    producto_id     UUID NOT NULL,
    cantidad        NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    monto           NUMERIC(16,4) NOT NULL
);

CREATE TABLE cotizaciones (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    numero      VARCHAR(30) NOT NULL,
    cliente_id  UUID,
    usuario_id  UUID,
    estado      VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                CHECK (estado IN ('ABIERTA','ENVIADA','ACEPTADA','RECHAZADA','VENCIDA','CONVERTIDA')),
    valida_hasta DATE,
    total       NUMERIC(16,4) NOT NULL DEFAULT 0,
    venta_id    UUID REFERENCES ventas(id),   -- si se convirtio
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cotizacion_numero UNIQUE (negocio_id, numero)
);

-- ---------------------------------------------------------------------
-- SAGA: Venta <-> Inventario
-- ---------------------------------------------------------------------
-- Sin @Transactional distribuido: si Ventas confirma y el descuento de
-- stock falla, hay que compensar. Maquina de estados persistida.
--   1. Venta CONFIRMADA (local)         -> publica solicitar_reserva_stock
--   2. Inventario responde stock_reservado / stock_reserva_fallida
--   3a. OK    -> venta.estado=CONFIRMADA, publica venta_completada
--   3b. FALLA -> venta.estado=BORRADOR + motivo, se libera lo reservado
CREATE TABLE sagas (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    tipo           VARCHAR(40) NOT NULL,     -- 'VENTA_DESCUENTA_STOCK'
    correlacion_id UUID        NOT NULL,
    agregado_id    UUID        NOT NULL,     -- venta_id
    estado         VARCHAR(30) NOT NULL DEFAULT 'INICIADA'
                   CHECK (estado IN ('INICIADA','ESPERANDO_STOCK','COMPLETADA',
                                     'COMPENSANDO','COMPENSADA','FALLIDA')),
    paso_actual    VARCHAR(40),
    intentos       INT         NOT NULL DEFAULT 0,
    payload        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ultimo_error   TEXT,
    timeout_en     TIMESTAMPTZ,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_saga_correlacion UNIQUE (correlacion_id)
);
CREATE INDEX ix_sagas_timeout ON sagas (timeout_en)
    WHERE estado IN ('INICIADA','ESPERANDO_STOCK','COMPENSANDO');

-- EVENTOS PUBLICADOS: venta_completada, venta_anulada, devolucion_registrada,
--                     solicitar_reserva_stock, venta_a_credito_registrada
-- EVENTOS CONSUMIDOS: stock_reservado, stock_reserva_fallida,
--                     producto_actualizado (cache local de precios),
--                     factura_emitida, factura_rechazada
