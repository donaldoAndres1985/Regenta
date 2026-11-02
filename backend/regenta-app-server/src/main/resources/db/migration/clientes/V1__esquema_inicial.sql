-- =====================================================================
-- servicio-clientes . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/02-servicio-clientes.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS crm;
SET search_path TO crm, public;

-- ---------------------------------------------------------------------
-- Parte 1 . Convenciones comunes a todos los servicios
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid() en las tablas de outbox/inbox
CREATE EXTENSION IF NOT EXISTS "pg_trgm";   -- busqueda por nombre o codigo

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
-- Parte 2 . Esquema propio de servicio-clientes
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-clientes  (esquema: crm)
-- HALLAZGO: el modulo "Clientes / CRM" aparece en la tabla de planes del
-- documento de arquitectura, pero NO existe un `servicio-clientes` en el
-- scaffold de repos. Se propone como servicio propio: es consumido por
-- Ventas, Reservas, Comandas y Facturacion (los cuatro), asi que meterlo
-- dentro de Ventas lo volveria una dependencia sincrona de todos.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS crm;
SET search_path TO crm, public;

CREATE TABLE clientes (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,          -- [ref logica]
    tipo_persona      VARCHAR(20)  NOT NULL DEFAULT 'NATURAL'
                      CHECK (tipo_persona IN ('NATURAL','JURIDICA')),
    tipo_documento    VARCHAR(10)  NOT NULL DEFAULT 'CC'
                      CHECK (tipo_documento IN ('CC','CE','NIT','PP','TI','NIT_EXT','SIN_IDENTIFICAR')),
    numero_documento  VARCHAR(30),
    digito_verificacion CHAR(1),
    nombres           VARCHAR(120),
    apellidos         VARCHAR(120),
    razon_social      VARCHAR(200),
    nombre_display    VARCHAR(200) GENERATED ALWAYS AS
                      (COALESCE(razon_social, TRIM(COALESCE(nombres,'')||' '||COALESCE(apellidos,'')))) STORED,
    email             VARCHAR(150),
    telefono          VARCHAR(30),
    telefono_alterno  VARCHAR(30),
    fecha_nacimiento  DATE,
    segmento          VARCHAR(40),                    -- 'MAYORISTA','VIP'
    lista_precios_id  UUID,                           -- [ref logica -> inventario]
    -- Credito
    credito_habilitado BOOLEAN     NOT NULL DEFAULT false,
    cupo_credito      NUMERIC(14,2) NOT NULL DEFAULT 0,
    dias_credito      SMALLINT     NOT NULL DEFAULT 0,
    saldo_pendiente   NUMERIC(14,2) NOT NULL DEFAULT 0, -- proyeccion, se recalcula por evento
    -- Fiscal (para Facturacion)
    responsabilidades_fiscales JSONB NOT NULL DEFAULT '[]'::jsonb,
    regimen_fiscal    VARCHAR(40),
    notas             TEXT,
    etiquetas         TEXT[]       NOT NULL DEFAULT '{}',
    activo            BOOLEAN      NOT NULL DEFAULT true,
    origen_offline_id UUID,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por        UUID,
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_por   UUID,
    eliminado_en      TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_cliente_nombre CHECK (
        (tipo_persona = 'NATURAL'  AND nombres IS NOT NULL) OR
        (tipo_persona = 'JURIDICA' AND razon_social IS NOT NULL))
);
-- Documento unico por negocio, pero permitiendo "consumidor final" repetido
CREATE UNIQUE INDEX uq_cliente_documento
    ON clientes (negocio_id, tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL AND tipo_documento <> 'SIN_IDENTIFICAR';
CREATE UNIQUE INDEX uq_cliente_offline
    ON clientes (negocio_id, origen_offline_id) WHERE origen_offline_id IS NOT NULL;
CREATE INDEX ix_clientes_busqueda
    ON clientes USING gin (nombre_display gin_trgm_ops);
CREATE INDEX ix_clientes_negocio ON clientes (negocio_id) WHERE eliminado_en IS NULL;

CREATE TABLE direcciones_cliente (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    cliente_id  UUID        NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    etiqueta    VARCHAR(40) NOT NULL DEFAULT 'PRINCIPAL',
    direccion   VARCHAR(200) NOT NULL,
    ciudad      VARCHAR(80),
    departamento VARCHAR(80),
    pais        CHAR(2)     NOT NULL DEFAULT 'CO',
    codigo_postal VARCHAR(15),
    latitud     NUMERIC(10,7),
    longitud    NUMERIC(10,7),
    es_facturacion BOOLEAN  NOT NULL DEFAULT false,
    es_envio    BOOLEAN     NOT NULL DEFAULT false,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_direcciones_cliente ON direcciones_cliente (cliente_id);

CREATE TABLE contactos_cliente (
    id         UUID PRIMARY KEY,
    negocio_id UUID        NOT NULL,
    cliente_id UUID        NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    nombre     VARCHAR(120) NOT NULL,
    cargo      VARCHAR(80),
    email      VARCHAR(150),
    telefono   VARCHAR(30),
    principal  BOOLEAN     NOT NULL DEFAULT false,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE interacciones (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    cliente_id   UUID        NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    usuario_id   UUID,                              -- [ref logica]
    tipo         VARCHAR(20) NOT NULL
                 CHECK (tipo IN ('LLAMADA','EMAIL','VISITA','WHATSAPP','NOTA','RECLAMO')),
    asunto       VARCHAR(150),
    detalle      TEXT,
    ocurrido_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    seguimiento_en DATE,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_interacciones_cliente ON interacciones (negocio_id, cliente_id, ocurrido_en DESC);

-- Proyeccion alimentada por eventos venta_completada / reserva_confirmada /
-- pedido_completado. NO se calcula con un JOIN a Ventas: son servicios
-- distintos con bases distintas.
CREATE TABLE cliente_metricas (
    cliente_id        UUID PRIMARY KEY REFERENCES clientes(id) ON DELETE CASCADE,
    negocio_id        UUID          NOT NULL,
    total_documentos  INT           NOT NULL DEFAULT 0,
    monto_total       NUMERIC(16,2) NOT NULL DEFAULT 0,
    ticket_promedio   NUMERIC(14,2) NOT NULL DEFAULT 0,
    primera_compra_en TIMESTAMPTZ,
    ultima_compra_en  TIMESTAMPTZ,
    dias_sin_comprar  INT,
    actualizado_en    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Cuentas por cobrar de clientes con credito (ventas a plazo).
CREATE TABLE cuentas_por_cobrar (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    cliente_id     UUID        NOT NULL REFERENCES clientes(id),
    origen_tipo    VARCHAR(20) NOT NULL CHECK (origen_tipo IN ('VENTA','RESERVA','COMANDA','FACTURA')),
    origen_id      UUID        NOT NULL,           -- [ref logica]
    documento_ref  VARCHAR(40),
    monto          NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    saldo          NUMERIC(14,2) NOT NULL,
    fecha_emision  DATE        NOT NULL,
    fecha_vencimiento DATE     NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','VENCIDA','INCOBRABLE')),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_saldo CHECK (saldo >= 0 AND saldo <= monto)
);
CREATE INDEX ix_cxc_cliente ON cuentas_por_cobrar (negocio_id, cliente_id, estado);
CREATE INDEX ix_cxc_vencimiento ON cuentas_por_cobrar (negocio_id, fecha_vencimiento)
    WHERE estado IN ('PENDIENTE','PARCIAL','VENCIDA');

CREATE TABLE recaudos (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    cuenta_id   UUID        NOT NULL REFERENCES cuentas_por_cobrar(id) ON DELETE CASCADE,
    monto       NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    metodo      VARCHAR(20) NOT NULL
                CHECK (metodo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','CHEQUE','OTRO')),
    referencia  VARCHAR(60),
    recibido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id  UUID,
    caja_sesion_id UUID                            -- [ref logica -> servicio-caja]
);
