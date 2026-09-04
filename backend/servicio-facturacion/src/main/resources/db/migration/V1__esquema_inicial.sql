-- =====================================================================
-- servicio-facturacion . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/11-servicio-facturacion.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS facturacion;
SET search_path TO facturacion, public;

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
-- Parte 2 . Esquema propio de servicio-facturacion
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-facturacion  (esquema: facturacion)
-- CORE (plan Profesional). Facturacion electronica DIAN (Colombia).
--
-- HALLAZGO DE DISENIO: el PDF define "Facturacion depende de Ventas".
-- Con tres patrones operativos eso deja de ser cierto: tambien debe
-- facturar Reservas y Comandas. Por eso `facturas` referencia el origen
-- de forma POLIMORFICA (origen_tipo + origen_id) y consume tres eventos
-- distintos, en vez de acoplarse a la tabla `ventas`.
--
-- SEGUNDO HALLAZGO: el consecutivo de facturacion NO puede ser un
-- BIGSERIAL. La DIAN autoriza un RANGO por resolucion; el numero debe ser
-- continuo, sin huecos y dentro del rango vigente. Se modela como una
-- tabla con bloqueo (`resoluciones.consecutivo_actual` + SELECT FOR UPDATE).
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS facturacion;
SET search_path TO facturacion, public;

CREATE TABLE resoluciones (
    id                UUID PRIMARY KEY,
    negocio_id        UUID        NOT NULL,
    sucursal_id       UUID,
    tipo_documento    VARCHAR(20) NOT NULL
                      CHECK (tipo_documento IN ('FACTURA_VENTA','FACTURA_POS','NOTA_CREDITO',
                                                'NOTA_DEBITO','DOCUMENTO_SOPORTE')),
    numero_resolucion VARCHAR(40) NOT NULL,
    prefijo           VARCHAR(10) NOT NULL DEFAULT '',
    rango_desde       BIGINT      NOT NULL,
    rango_hasta       BIGINT      NOT NULL,
    consecutivo_actual BIGINT     NOT NULL,
    clave_tecnica     VARCHAR(120),                -- requerida para el CUFE
    vigente_desde     DATE        NOT NULL,
    vigente_hasta     DATE        NOT NULL,
    ambiente          VARCHAR(20) NOT NULL DEFAULT 'PRUEBAS'
                      CHECK (ambiente IN ('PRUEBAS','PRODUCCION')),
    estado            VARCHAR(20) NOT NULL DEFAULT 'VIGENTE'
                      CHECK (estado IN ('VIGENTE','AGOTADA','VENCIDA','ANULADA')),
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_resolucion UNIQUE (negocio_id, numero_resolucion),
    CONSTRAINT ck_rango CHECK (rango_hasta >= rango_desde),
    CONSTRAINT ck_consecutivo CHECK (consecutivo_actual BETWEEN rango_desde AND rango_hasta + 1),
    CONSTRAINT ck_vigencia CHECK (vigente_hasta >= vigente_desde)
);
-- Solo una resolucion vigente por tipo y sucursal
CREATE UNIQUE INDEX uq_resolucion_vigente
    ON resoluciones (negocio_id, COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid), tipo_documento)
    WHERE estado = 'VIGENTE';

CREATE TABLE facturas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    sucursal_id       UUID,
    resolucion_id     UUID         NOT NULL REFERENCES resoluciones(id),
    tipo_documento    VARCHAR(20)  NOT NULL
                      CHECK (tipo_documento IN ('FACTURA_VENTA','FACTURA_POS','NOTA_CREDITO',
                                                'NOTA_DEBITO','DOCUMENTO_SOPORTE')),
    prefijo           VARCHAR(10)  NOT NULL DEFAULT '',
    numero            BIGINT       NOT NULL,
    numero_completo   VARCHAR(30)  GENERATED ALWAYS AS (prefijo || numero::text) STORED,
    -- ORIGEN POLIMORFICO: sirve a los tres patrones operativos
    origen_tipo       VARCHAR(20)  NOT NULL
                      CHECK (origen_tipo IN ('VENTA','RESERVA','COMANDA','MANUAL','DEVOLUCION')),
    origen_id         UUID,                        -- [ref logica]
    factura_origen_id UUID         REFERENCES facturas(id),  -- para NC/ND
    -- SNAPSHOTS INMUTABLES: una factura emitida no cambia si mañana editan
    -- el cliente o la configuracion del negocio.
    emisor_snapshot   JSONB        NOT NULL,
    cliente_snapshot  JSONB        NOT NULL,
    cliente_id        UUID,
    -- Fechas
    fecha_emision     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_vencimiento DATE,
    -- Importes
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    tasa_cambio       NUMERIC(14,6) NOT NULL DEFAULT 1,
    subtotal          NUMERIC(16,4) NOT NULL DEFAULT 0,
    descuento_total   NUMERIC(16,4) NOT NULL DEFAULT 0,
    base_gravable     NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuestos_total   NUMERIC(16,4) NOT NULL DEFAULT 0,
    retenciones_total NUMERIC(16,4) NOT NULL DEFAULT 0,
    propina           NUMERIC(16,4) NOT NULL DEFAULT 0,
    total             NUMERIC(16,4) NOT NULL DEFAULT 0,
    forma_pago        VARCHAR(20)  NOT NULL DEFAULT 'CONTADO'
                      CHECK (forma_pago IN ('CONTADO','CREDITO')),
    medio_pago_codigo VARCHAR(10),                 -- catalogo DIAN
    -- Estado del documento electronico
    estado            VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR'
                      CHECK (estado IN ('BORRADOR','GENERADA','FIRMADA','ENVIADA',
                                        'ACEPTADA','RECHAZADA','ANULADA','CONTINGENCIA')),
    cufe              VARCHAR(96),                 -- hash unico DIAN
    qr_data           TEXT,
    uuid_proveedor    VARCHAR(64),
    xml_url           TEXT,                        -- objeto en storage, no en BD
    pdf_url           TEXT,
    respuesta_dian    JSONB,
    intentos_envio    SMALLINT     NOT NULL DEFAULT 0,
    enviada_en        TIMESTAMPTZ,
    aceptada_en       TIMESTAMPTZ,
    -- Anulacion
    anulada_en        TIMESTAMPTZ,
    motivo_anulacion  TEXT,
    codigo_nota       VARCHAR(10),                 -- motivo DIAN de la NC
    observaciones     TEXT,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_factura_numero UNIQUE (negocio_id, prefijo, numero, tipo_documento),
    -- Una factura por documento de origen (idempotencia frente a
    -- venta_completada entregado dos veces por RabbitMQ)
    CONSTRAINT ck_nota_referencia CHECK (
        tipo_documento NOT IN ('NOTA_CREDITO','NOTA_DEBITO') OR factura_origen_id IS NOT NULL)
);
CREATE UNIQUE INDEX uq_factura_origen
    ON facturas (negocio_id, origen_tipo, origen_id)
    WHERE origen_id IS NOT NULL AND tipo_documento IN ('FACTURA_VENTA','FACTURA_POS')
      AND estado <> 'ANULADA';
CREATE INDEX ix_facturas_fecha   ON facturas (negocio_id, fecha_emision DESC);
CREATE INDEX ix_facturas_cliente ON facturas (negocio_id, cliente_id, fecha_emision DESC);
CREATE INDEX ix_facturas_pendientes ON facturas (negocio_id, estado)
    WHERE estado IN ('GENERADA','FIRMADA','ENVIADA','RECHAZADA');

CREATE TABLE factura_lineas (
    id             UUID PRIMARY KEY,
    negocio_id     UUID NOT NULL,
    factura_id     UUID NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
    linea          SMALLINT NOT NULL,
    codigo         VARCHAR(60),
    descripcion    VARCHAR(300) NOT NULL,
    cantidad       NUMERIC(18,6) NOT NULL,
    unidad_codigo  VARCHAR(10) NOT NULL DEFAULT '94',   -- catalogo UNECE
    precio_unitario NUMERIC(14,4) NOT NULL,
    descuento_pct  NUMERIC(7,4)  NOT NULL DEFAULT 0,
    descuento_valor NUMERIC(14,4) NOT NULL DEFAULT 0,
    base_gravable  NUMERIC(16,4) NOT NULL,
    total          NUMERIC(16,4) NOT NULL,
    CONSTRAINT uq_factura_linea UNIQUE (factura_id, linea)
);

CREATE TABLE factura_impuestos (
    id              UUID PRIMARY KEY,
    negocio_id      UUID NOT NULL,
    factura_id      UUID NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
    factura_linea_id UUID REFERENCES factura_lineas(id) ON DELETE CASCADE,
    codigo          VARCHAR(10) NOT NULL,      -- '01'=IVA, '04'=INC
    nombre          VARCHAR(40) NOT NULL,
    porcentaje      NUMERIC(7,4) NOT NULL,
    base            NUMERIC(16,4) NOT NULL,
    valor           NUMERIC(16,4) NOT NULL,
    es_retencion    BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX ix_factura_impuestos ON factura_impuestos (factura_id);

-- Log de cada intercambio con la DIAN / proveedor tecnologico.
-- Es evidencia legal: se conserva request y response completos.
CREATE TABLE transmisiones (
    id           UUID        NOT NULL,
    negocio_id   UUID        NOT NULL,
    factura_id   UUID        NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
    evento       VARCHAR(30) NOT NULL
                 CHECK (evento IN ('ENVIO','CONSULTA','ANULACION','ACUSE','EMAIL_CLIENTE')),
    endpoint     TEXT,
    request      TEXT,
    response     TEXT,
    http_status  SMALLINT,
    codigo_error VARCHAR(20),
    mensaje      TEXT,
    duracion_ms  INT,
    ocurrido_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE transmisiones_default PARTITION OF transmisiones DEFAULT;

-- Metadatos del certificado. El .p12 NUNCA se guarda en la base de datos:
-- va a un secret manager / KMS y aqui solo queda la referencia.
CREATE TABLE certificados (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    alias          VARCHAR(80) NOT NULL,
    emisor         VARCHAR(150),
    numero_serie   VARCHAR(80),
    vigente_desde  DATE        NOT NULL,
    vigente_hasta  DATE        NOT NULL,
    referencia_kms TEXT        NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
                   CHECK (estado IN ('ACTIVO','VENCIDO','REVOCADO')),
    CONSTRAINT uq_certificado UNIQUE (negocio_id, alias)
);

-- Numeracion de contingencia cuando la DIAN no responde
CREATE TABLE contingencias (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    inicio_en   TIMESTAMPTZ NOT NULL,
    fin_en      TIMESTAMPTZ,
    motivo      TEXT,
    facturas_afectadas INT NOT NULL DEFAULT 0,
    regularizada BOOLEAN   NOT NULL DEFAULT false
);

-- EVENTOS PUBLICADOS: factura_emitida, factura_aceptada, factura_rechazada,
--                     nota_credito_emitida
-- EVENTOS CONSUMIDOS: venta_completada, reserva_confirmada / estancia_finalizada,
--                     pedido_completado, devolucion_registrada,
--                     configuracion_negocio_actualizada
