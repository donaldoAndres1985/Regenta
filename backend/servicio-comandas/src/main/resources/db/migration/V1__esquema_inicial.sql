-- =====================================================================
-- servicio-comandas . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/10-servicio-comandas.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS comandas;
SET search_path TO comandas, public;

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
-- Parte 2 . Esquema propio de servicio-comandas
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-comandas  (esquema: comandas)
-- PATRON: Comanda — modulo de TRANSACCION (equivale a Ventas).
--
-- LO QUE HACE DISTINTO A ESTE PATRON: la transaccion queda ABIERTA y
-- acumula lineas en el tiempo. Una venta se cierra en un instante; una
-- comanda vive 90 minutos. Consecuencias en el modelo:
--   1. Cada LINEA tiene su propio ciclo de vida (pedida -> en preparacion
--      -> lista -> entregada), no solo la cabecera.
--   2. Los totales se recalculan en cada adicion; no se persiste solo al
--      final.
--   3. La cuenta se puede DIVIDIR entre comensales -> tabla `cuentas`.
--   4. Anular una linea ya enviada a cocina es un hecho auditable
--      (merma), distinto de borrarla antes de enviarla.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS comandas;
SET search_path TO comandas, public;

CREATE TABLE consecutivos (
    negocio_id UUID        NOT NULL,
    sucursal_id UUID,
    tipo       VARCHAR(20) NOT NULL,
    prefijo    VARCHAR(10) NOT NULL DEFAULT '',
    siguiente  BIGINT      NOT NULL DEFAULT 1,
    sucursal_key UUID GENERATED ALWAYS AS (COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    PRIMARY KEY (negocio_id, sucursal_key, tipo)
);

CREATE TABLE comandas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    sucursal_id       UUID,
    numero            VARCHAR(30)  NOT NULL,
    tipo              VARCHAR(20)  NOT NULL DEFAULT 'MESA'
                      CHECK (tipo IN ('MESA','PARA_LLEVAR','DOMICILIO','BARRA','HABITACION')),
    -- Origen
    mesa_id           UUID,                        -- [ref logica -> mesas]
    sesion_mesa_id    UUID,                        -- [ref logica -> mesas]
    estancia_id       UUID,                        -- [ref logica -> reservas] room service
    cliente_id        UUID,                        -- [ref logica -> crm]
    cliente_snapshot  JSONB,
    direccion_entrega JSONB,                       -- domicilio
    -- Personal
    mesero_usuario_id UUID,
    cajero_usuario_id UUID,
    num_comensales    SMALLINT     NOT NULL DEFAULT 1,
    -- Ciclo de vida de la CABECERA
    estado            VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA'
                      CHECK (estado IN ('ABIERTA','EN_COCINA','SERVIDA','CUENTA_PEDIDA',
                                        'CERRADA','ANULADA')),
    -- Totales
    subtotal          NUMERIC(16,4) NOT NULL DEFAULT 0,
    descuento_total   NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto_total    NUMERIC(16,4) NOT NULL DEFAULT 0,
    propina_sugerida  NUMERIC(16,4) NOT NULL DEFAULT 0,   -- 10% voluntario (CO)
    propina           NUMERIC(16,4) NOT NULL DEFAULT 0,
    total             NUMERIC(16,4) NOT NULL DEFAULT 0,
    costo_total       NUMERIC(16,4) NOT NULL DEFAULT 0,
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    -- Facturacion
    factura_id        UUID,
    estado_factura    VARCHAR(20)  DEFAULT 'NO_APLICA',
    -- Tiempos
    abierta_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    enviada_cocina_en TIMESTAMPTZ,
    servida_en        TIMESTAMPTZ,
    cerrada_en        TIMESTAMPTZ,
    anulada_en        TIMESTAMPTZ,
    motivo_anulacion  TEXT,
    origen_offline_id UUID,
    notas             TEXT,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_comanda_numero UNIQUE (negocio_id, numero),
    CONSTRAINT ck_comanda_origen CHECK (
        (tipo = 'MESA' AND mesa_id IS NOT NULL) OR tipo <> 'MESA')
);
CREATE UNIQUE INDEX uq_comanda_offline
    ON comandas (negocio_id, origen_offline_id) WHERE origen_offline_id IS NOT NULL;
-- Una comanda abierta por sesion de mesa
CREATE UNIQUE INDEX uq_comanda_sesion_abierta
    ON comandas (sesion_mesa_id)
    WHERE sesion_mesa_id IS NOT NULL AND estado NOT IN ('CERRADA','ANULADA');
CREATE INDEX ix_comandas_abiertas ON comandas (negocio_id, sucursal_id, abierta_en)
    WHERE estado NOT IN ('CERRADA','ANULADA');
CREATE INDEX ix_comandas_fecha ON comandas (negocio_id, abierta_en DESC);

CREATE TABLE comanda_lineas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    comanda_id        UUID         NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    linea             SMALLINT     NOT NULL,
    item_menu_id      UUID         NOT NULL,      -- [ref logica -> menu]
    nombre_snapshot   VARCHAR(150) NOT NULL,
    estacion_id       UUID,
    cantidad          NUMERIC(12,4) NOT NULL CHECK (cantidad > 0),
    precio_unitario   NUMERIC(14,4) NOT NULL,
    descuento_valor   NUMERIC(14,4) NOT NULL DEFAULT 0,
    impuesto_pct      NUMERIC(7,4)  NOT NULL DEFAULT 0,
    impuesto_valor    NUMERIC(14,4) NOT NULL DEFAULT 0,
    modificadores_valor NUMERIC(14,4) NOT NULL DEFAULT 0,
    subtotal          NUMERIC(16,4) NOT NULL,
    total             NUMERIC(16,4) NOT NULL,
    costo_snapshot    NUMERIC(14,4) NOT NULL DEFAULT 0,
    -- Ciclo de vida PROPIO de la linea (esto es lo que no existe en Ventas)
    estado            VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE','ENVIADA','EN_PREPARACION',
                                        'LISTA','ENTREGADA','ANULADA')),
    curso             VARCHAR(20)  NOT NULL DEFAULT 'FUERTE'
                      CHECK (curso IN ('ENTRADA','FUERTE','POSTRE','BEBIDA','ACOMPANAMIENTO')),
    secuencia_envio   SMALLINT     NOT NULL DEFAULT 1,   -- "sacar los postres despues"
    notas             VARCHAR(200),                      -- 'sin cebolla'
    comensal_numero   SMALLINT,                          -- para dividir la cuenta
    pedida_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    enviada_en        TIMESTAMPTZ,
    lista_en          TIMESTAMPTZ,
    entregada_en      TIMESTAMPTZ,
    anulada_en        TIMESTAMPTZ,
    anulada_por       UUID,
    motivo_anulacion  VARCHAR(120),
    genera_merma      BOOLEAN      NOT NULL DEFAULT false, -- anulada despues de cocinar
    CONSTRAINT uq_comanda_linea UNIQUE (comanda_id, linea)
);
CREATE INDEX ix_lineas_estacion ON comanda_lineas (negocio_id, estacion_id, estado)
    WHERE estado IN ('ENVIADA','EN_PREPARACION');
CREATE INDEX ix_lineas_comanda ON comanda_lineas (comanda_id);

CREATE TABLE comanda_linea_modificadores (
    id              UUID PRIMARY KEY,
    negocio_id      UUID NOT NULL,
    linea_id        UUID NOT NULL REFERENCES comanda_lineas(id) ON DELETE CASCADE,
    modificador_id  UUID NOT NULL,               -- [ref logica -> menu]
    nombre_snapshot VARCHAR(80) NOT NULL,
    precio_extra    NUMERIC(14,4) NOT NULL DEFAULT 0,
    cantidad        SMALLINT NOT NULL DEFAULT 1
);

-- Division de cuenta. Sin esta tabla, "pagamos por separado" obliga a
-- anular la comanda y rehacerla.
CREATE TABLE cuentas (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    comanda_id     UUID        NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    numero_division SMALLINT   NOT NULL DEFAULT 1,
    etiqueta       VARCHAR(40),                  -- 'Comensal 1'
    modo_division  VARCHAR(20) NOT NULL DEFAULT 'POR_ITEM'
                   CHECK (modo_division IN ('UNICA','POR_ITEM','PARTES_IGUALES','MONTO_FIJO')),
    subtotal       NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto_total NUMERIC(16,4) NOT NULL DEFAULT 0,
    propina        NUMERIC(16,4) NOT NULL DEFAULT 0,
    total          NUMERIC(16,4) NOT NULL DEFAULT 0,
    pagado         NUMERIC(16,4) NOT NULL DEFAULT 0,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                   CHECK (estado IN ('ABIERTA','PAGADA','ANULADA')),
    factura_id     UUID,
    CONSTRAINT uq_cuenta UNIQUE (comanda_id, numero_division)
);

CREATE TABLE cuenta_lineas (
    cuenta_id       UUID NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    comanda_linea_id UUID NOT NULL REFERENCES comanda_lineas(id) ON DELETE CASCADE,
    negocio_id      UUID NOT NULL,
    proporcion      NUMERIC(7,6) NOT NULL DEFAULT 1,  -- 0.5 = la mitad del plato
    monto           NUMERIC(16,4) NOT NULL,
    PRIMARY KEY (cuenta_id, comanda_linea_id),
    CONSTRAINT ck_proporcion CHECK (proporcion > 0 AND proporcion <= 1)
);

CREATE TABLE pagos_comanda (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    comanda_id     UUID        NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    cuenta_id      UUID        REFERENCES cuentas(id),
    metodo         VARCHAR(20) NOT NULL
                   CHECK (metodo IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                                     'TRANSFERENCIA','QR','BONO','CARGO_HABITACION','OTRO')),
    monto          NUMERIC(16,4) NOT NULL CHECK (monto > 0),
    propina        NUMERIC(16,4) NOT NULL DEFAULT 0,
    monto_recibido NUMERIC(16,4),
    cambio         NUMERIC(16,4) NOT NULL DEFAULT 0,
    referencia     VARCHAR(80),
    caja_sesion_id UUID,
    recibido_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id     UUID
);

-- KDS: cola de trabajo de cocina. El PDF lo equipara a "Alertas".
CREATE TABLE tickets_cocina (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    sucursal_id    UUID,
    comanda_id     UUID        NOT NULL REFERENCES comandas(id) ON DELETE CASCADE,
    estacion_id    UUID        NOT NULL,
    secuencia      INT         NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'NUEVO'
                   CHECK (estado IN ('NUEVO','EN_PREPARACION','LISTO','ENTREGADO','ANULADO')),
    prioridad      SMALLINT    NOT NULL DEFAULT 0,
    tiempo_objetivo_min SMALLINT,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    iniciado_en    TIMESTAMPTZ,
    listo_en       TIMESTAMPTZ,
    entregado_en   TIMESTAMPTZ,
    demora_min     INT GENERATED ALWAYS AS
                   (CASE WHEN listo_en IS NULL THEN NULL
                    ELSE EXTRACT(EPOCH FROM (listo_en - creado_en))::int / 60 END) STORED,
    impreso_en     TIMESTAMPTZ,
    usuario_cocina_id UUID
);
CREATE INDEX ix_kds_cola ON tickets_cocina (negocio_id, estacion_id, prioridad DESC, creado_en)
    WHERE estado IN ('NUEVO','EN_PREPARACION');

CREATE TABLE ticket_cocina_lineas (
    ticket_id UUID NOT NULL REFERENCES tickets_cocina(id) ON DELETE CASCADE,
    linea_id  UUID NOT NULL REFERENCES comanda_lineas(id) ON DELETE CASCADE,
    negocio_id UUID NOT NULL,
    PRIMARY KEY (ticket_id, linea_id)
);

-- EVENTOS PUBLICADOS: comanda_abierta, linea_enviada_cocina, linea_lista,
--                     pedido_completado (equivalente a venta_completada),
--                     comanda_anulada, insumos_consumidos (explosion de receta),
--                     merma_registrada
-- EVENTOS CONSUMIDOS: item_menu_actualizado, item_agotado, factura_emitida,
--                     sesion_mesa_cerrada
