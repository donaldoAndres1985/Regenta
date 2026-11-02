-- =====================================================================
-- servicio-reportes . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/14-servicio-reportes.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS reportes;
SET search_path TO reportes, public;

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
-- Parte 2 . Esquema propio de servicio-reportes
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-reportes  (esquema: reportes)  [plan Profesional]
--
-- ESTE SERVICIO NO CONSULTA A LOS DEMAS. Es un read model (CQRS)
-- alimentado por eventos de RabbitMQ. Modelarlo como "vistas sobre las
-- tablas de Ventas" romperia la regla de "ningun servicio accede a la
-- base de otro" y, con microservicios, ni siquiera seria posible.
--
-- Esquema en estrella: tablas de hechos + dimensiones desnormalizadas.
-- Las dimensiones son SCD tipo 2: si cambia el nombre de un producto, el
-- reporte historico sigue mostrando el nombre que tenia entonces.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS reportes;
SET search_path TO reportes, public;

-- ---------------------------------------------------------------------
-- Dimensiones
-- ---------------------------------------------------------------------
CREATE TABLE dim_fecha (
    fecha_id     INT  PRIMARY KEY,          -- 20260903
    fecha        DATE NOT NULL UNIQUE,
    dia          SMALLINT NOT NULL,
    mes          SMALLINT NOT NULL,
    anio         SMALLINT NOT NULL,
    trimestre    SMALLINT NOT NULL,
    semana_iso   SMALLINT NOT NULL,
    dia_semana   SMALLINT NOT NULL,
    nombre_dia   VARCHAR(15) NOT NULL,
    nombre_mes   VARCHAR(15) NOT NULL,
    es_fin_semana BOOLEAN NOT NULL,
    es_festivo   BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE dim_producto (
    sk            BIGSERIAL PRIMARY KEY,     -- surrogate key (SCD2)
    negocio_id    UUID NOT NULL,
    producto_id   UUID NOT NULL,             -- clave natural
    sku           VARCHAR(60),
    nombre        VARCHAR(180) NOT NULL,
    categoria_id  UUID,
    categoria_nombre VARCHAR(100),
    marca         VARCHAR(80),
    unidad        VARCHAR(20),
    precio_venta  NUMERIC(14,4),
    costo         NUMERIC(14,4),
    vigente_desde TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigente_hasta TIMESTAMPTZ,               -- NULL = version actual
    es_actual     BOOLEAN NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_dim_producto_actual
    ON dim_producto (negocio_id, producto_id) WHERE es_actual;

CREATE TABLE dim_cliente (
    sk           BIGSERIAL PRIMARY KEY,
    negocio_id   UUID NOT NULL,
    cliente_id   UUID NOT NULL,
    nombre       VARCHAR(200),
    segmento     VARCHAR(40),
    ciudad       VARCHAR(80),
    vigente_desde TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigente_hasta TIMESTAMPTZ,
    es_actual    BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE dim_usuario (
    sk          BIGSERIAL PRIMARY KEY,
    negocio_id  UUID NOT NULL,
    usuario_id  UUID NOT NULL,
    nombre      VARCHAR(160),
    rol         VARCHAR(60),
    es_actual   BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE dim_sucursal (
    sk          BIGSERIAL PRIMARY KEY,
    negocio_id  UUID NOT NULL,
    sucursal_id UUID,
    nombre      VARCHAR(120),
    ciudad      VARCHAR(80),
    es_actual   BOOLEAN NOT NULL DEFAULT true
);

-- ---------------------------------------------------------------------
-- Hechos (grano: una linea de documento)
-- ---------------------------------------------------------------------
CREATE TABLE hechos_venta (
    id             BIGSERIAL,
    negocio_id     UUID NOT NULL,
    fecha_id       INT  NOT NULL REFERENCES dim_fecha(fecha_id),
    hora           SMALLINT NOT NULL,
    ocurrido_en    TIMESTAMPTZ NOT NULL,
    sucursal_sk    BIGINT REFERENCES dim_sucursal(sk),
    producto_sk    BIGINT REFERENCES dim_producto(sk),
    cliente_sk     BIGINT REFERENCES dim_cliente(sk),
    usuario_sk     BIGINT REFERENCES dim_usuario(sk),
    venta_id       UUID NOT NULL,
    canal          VARCHAR(20),
    -- Metricas aditivas
    cantidad       NUMERIC(18,6) NOT NULL,
    monto_bruto    NUMERIC(16,4) NOT NULL,
    descuento      NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto       NUMERIC(16,4) NOT NULL DEFAULT 0,
    monto_neto     NUMERIC(16,4) NOT NULL,
    costo          NUMERIC(16,4) NOT NULL DEFAULT 0,
    margen         NUMERIC(16,4) GENERATED ALWAYS AS (monto_neto - costo) STORED,
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE hechos_venta_default PARTITION OF hechos_venta DEFAULT;
CREATE INDEX ix_hv_negocio_fecha ON hechos_venta (negocio_id, fecha_id);
CREATE INDEX ix_hv_producto ON hechos_venta (negocio_id, producto_sk, fecha_id);

CREATE TABLE hechos_reserva (
    id             BIGSERIAL,
    negocio_id     UUID NOT NULL,
    fecha_id       INT  NOT NULL REFERENCES dim_fecha(fecha_id),
    ocurrido_en    TIMESTAMPTZ NOT NULL,
    sucursal_sk    BIGINT,
    cliente_sk     BIGINT,
    reserva_id     UUID NOT NULL,
    tipo_recurso_id UUID,
    tipo_recurso_nombre VARCHAR(100),
    recurso_id     UUID,
    canal          VARCHAR(20),
    estado_final   VARCHAR(20),
    noches         INT NOT NULL DEFAULT 1,
    num_personas   SMALLINT,
    monto_neto     NUMERIC(16,4) NOT NULL,
    impuesto       NUMERIC(16,4) NOT NULL DEFAULT 0,
    consumos       NUMERIC(16,4) NOT NULL DEFAULT 0,
    penalizacion   NUMERIC(16,4) NOT NULL DEFAULT 0,
    -- Metricas hoteleras clasicas
    adr            NUMERIC(14,4),             -- average daily rate
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE hechos_reserva_default PARTITION OF hechos_reserva DEFAULT;

CREATE TABLE hechos_comanda (
    id             BIGSERIAL,
    negocio_id     UUID NOT NULL,
    fecha_id       INT  NOT NULL REFERENCES dim_fecha(fecha_id),
    ocurrido_en    TIMESTAMPTZ NOT NULL,
    sucursal_sk    BIGINT,
    usuario_sk     BIGINT,                    -- mesero
    comanda_id     UUID NOT NULL,
    item_menu_id   UUID,
    item_nombre    VARCHAR(150),
    categoria_menu VARCHAR(80),
    estacion       VARCHAR(60),
    mesa_codigo    VARCHAR(20),
    tipo           VARCHAR(20),
    cantidad       NUMERIC(12,4) NOT NULL,
    monto_neto     NUMERIC(16,4) NOT NULL,
    costo          NUMERIC(16,4) NOT NULL DEFAULT 0,
    propina        NUMERIC(16,4) NOT NULL DEFAULT 0,
    tiempo_preparacion_min INT,
    tiempo_mesa_min INT,
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE hechos_comanda_default PARTITION OF hechos_comanda DEFAULT;

CREATE TABLE hechos_inventario (
    id            BIGSERIAL,
    negocio_id    UUID NOT NULL,
    fecha_id      INT  NOT NULL REFERENCES dim_fecha(fecha_id),
    ocurrido_en   TIMESTAMPTZ NOT NULL,
    producto_sk   BIGINT,
    bodega_id     UUID,
    tipo_movimiento VARCHAR(25),
    cantidad      NUMERIC(18,6) NOT NULL,
    valor         NUMERIC(16,4) NOT NULL DEFAULT 0,
    saldo_posterior NUMERIC(18,6),
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
CREATE TABLE hechos_inventario_default PARTITION OF hechos_inventario DEFAULT;

-- ---------------------------------------------------------------------
-- Agregados pre-calculados (lo que consume el dashboard de la app)
-- ---------------------------------------------------------------------
CREATE TABLE agregados_diarios (
    negocio_id      UUID NOT NULL,
    sucursal_id     UUID,
    fecha           DATE NOT NULL,
    patron          VARCHAR(30) NOT NULL,
    num_documentos  INT  NOT NULL DEFAULT 0,
    unidades        NUMERIC(18,6) NOT NULL DEFAULT 0,
    monto_bruto     NUMERIC(18,4) NOT NULL DEFAULT 0,
    descuentos      NUMERIC(18,4) NOT NULL DEFAULT 0,
    impuestos       NUMERIC(18,4) NOT NULL DEFAULT 0,
    monto_neto      NUMERIC(18,4) NOT NULL DEFAULT 0,
    costo           NUMERIC(18,4) NOT NULL DEFAULT 0,
    margen          NUMERIC(18,4) NOT NULL DEFAULT 0,
    ticket_promedio NUMERIC(14,4) NOT NULL DEFAULT 0,
    clientes_unicos INT NOT NULL DEFAULT 0,
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sucursal_key    UUID GENERATED ALWAYS AS (COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    PRIMARY KEY (negocio_id, sucursal_key, fecha, patron)
);

CREATE TABLE ranking_productos (
    negocio_id   UUID NOT NULL,
    periodo      VARCHAR(10) NOT NULL,        -- '2026-09' o '2026-W36'
    producto_id  UUID NOT NULL,
    nombre       VARCHAR(180),
    unidades     NUMERIC(18,6) NOT NULL,
    monto_neto   NUMERIC(18,4) NOT NULL,
    margen       NUMERIC(18,4) NOT NULL,
    posicion     INT NOT NULL,
    PRIMARY KEY (negocio_id, periodo, producto_id)
);

CREATE TABLE ocupacion_diaria (          -- patron Reserva
    negocio_id      UUID NOT NULL,
    sucursal_id     UUID,
    fecha           DATE NOT NULL,
    tipo_recurso_id UUID NOT NULL,
    recursos_totales INT NOT NULL,
    recursos_ocupados INT NOT NULL,
    ocupacion_pct   NUMERIC(7,4) GENERATED ALWAYS AS
                    (CASE WHEN recursos_totales = 0 THEN 0
                     ELSE recursos_ocupados::numeric * 100 / recursos_totales END) STORED,
    adr             NUMERIC(14,4),
    revpar          NUMERIC(14,4),
    PRIMARY KEY (negocio_id, fecha, tipo_recurso_id)
);

-- ---------------------------------------------------------------------
-- Definicion y programacion de reportes
-- ---------------------------------------------------------------------
CREATE TABLE definiciones_reporte (
    id          UUID PRIMARY KEY,
    negocio_id  UUID,                          -- NULL = reporte de sistema
    codigo      VARCHAR(60) NOT NULL,
    nombre      VARCHAR(120) NOT NULL,
    modulo      VARCHAR(40),
    patron      VARCHAR(30),
    consulta    JSONB NOT NULL,               -- especificacion declarativa
    columnas    JSONB NOT NULL DEFAULT '[]'::jsonb,
    activo      BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE reportes_programados (
    id            UUID PRIMARY KEY,
    negocio_id    UUID NOT NULL,
    definicion_id UUID NOT NULL REFERENCES definiciones_reporte(id),
    nombre        VARCHAR(120) NOT NULL,
    cron          VARCHAR(40) NOT NULL,
    parametros    JSONB NOT NULL DEFAULT '{}'::jsonb,
    formato       VARCHAR(10) NOT NULL DEFAULT 'PDF' CHECK (formato IN ('PDF','XLSX','CSV')),
    destinatarios TEXT[] NOT NULL DEFAULT '{}',
    activo        BOOLEAN NOT NULL DEFAULT true,
    proxima_ejecucion TIMESTAMPTZ
);

CREATE TABLE ejecuciones_reporte (
    id            UUID PRIMARY KEY,
    negocio_id    UUID NOT NULL,
    definicion_id UUID,
    programado_id UUID REFERENCES reportes_programados(id),
    parametros    JSONB,
    estado        VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO'
                  CHECK (estado IN ('EN_CURSO','COMPLETADO','FALLIDO')),
    filas         INT,
    archivo_url   TEXT,
    duracion_ms   INT,
    error         TEXT,
    iniciado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_en TIMESTAMPTZ,
    usuario_id    UUID
);

-- Control de consumo de eventos (evita reprocesar y permite rebuild)
CREATE TABLE posicion_consumo (
    consumidor   VARCHAR(60) PRIMARY KEY,
    ultimo_evento_en TIMESTAMPTZ,
    eventos_procesados BIGINT NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- EVENTOS CONSUMIDOS: venta_completada, venta_anulada, pedido_completado,
--                     reserva_confirmada, estancia_finalizada,
--                     stock_actualizado, producto_actualizado,
--                     cliente_actualizado, caja_cerrada
