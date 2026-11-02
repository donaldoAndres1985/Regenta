-- =====================================================================
-- servicio-menu . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/08-servicio-menu.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS menu;
SET search_path TO menu, public;

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
-- Parte 2 . Esquema propio de servicio-menu
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-menu  (esquema: menu)
-- PATRON: Comanda — modulo de CATALOGO (equivale a Inventario).
--
-- HALLAZGO IMPORTANTE: el documento de arquitectura no menciona como se
-- descuenta el inventario en un restaurante. Un plato no es un producto
-- con stock; consume INSUMOS. Sin la tabla `recetas` el patron Comanda
-- queda desconectado de Inventario y el negocio no puede saber cuanta
-- carne le queda. Esta es la pieza que une Comanda con Venta directa.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS menu;
SET search_path TO menu, public;

CREATE TABLE cartas (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    sucursal_id   UUID,
    nombre        VARCHAR(80) NOT NULL,       -- 'Carta principal','Desayunos','Happy hour'
    descripcion   TEXT,
    hora_desde    TIME,
    hora_hasta    TIME,
    dias_semana   SMALLINT[]  NOT NULL DEFAULT '{1,2,3,4,5,6,7}',
    vigente_desde DATE,
    vigente_hasta DATE,
    es_default    BOOLEAN     NOT NULL DEFAULT false,
    activa        BOOLEAN     NOT NULL DEFAULT true,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_carta ON cartas (negocio_id, lower(nombre));

CREATE TABLE categorias_menu (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    carta_id    UUID        NOT NULL REFERENCES cartas(id) ON DELETE CASCADE,
    nombre      VARCHAR(80) NOT NULL,          -- 'Entradas','Fuertes','Bebidas'
    descripcion TEXT,
    orden       INT         NOT NULL DEFAULT 0,
    icono       VARCHAR(40),
    activa      BOOLEAN     NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_categoria_menu ON categorias_menu (carta_id, lower(nombre));

CREATE TABLE estaciones_cocina (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    codigo      VARCHAR(20) NOT NULL,          -- 'PARRILLA','FRIA','BAR','POSTRES'
    nombre      VARCHAR(60) NOT NULL,
    impresora   VARCHAR(80),
    orden       INT         NOT NULL DEFAULT 0,
    activa      BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_estacion UNIQUE (negocio_id, codigo)
);

CREATE TABLE items_menu (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID         NOT NULL,
    categoria_menu_id  UUID         NOT NULL REFERENCES categorias_menu(id),
    estacion_id        UUID         REFERENCES estaciones_cocina(id),
    codigo             VARCHAR(30)  NOT NULL,
    nombre             VARCHAR(150) NOT NULL,
    descripcion        TEXT,
    tipo               VARCHAR(20)  NOT NULL DEFAULT 'PLATO'
                       CHECK (tipo IN ('PLATO','BEBIDA','POSTRE','COMBO','ADICIONAL')),
    precio             NUMERIC(14,4) NOT NULL CHECK (precio >= 0),
    impuesto_id        UUID,
    precio_incluye_impuesto BOOLEAN NOT NULL DEFAULT true,
    costo_estimado     NUMERIC(14,4) NOT NULL DEFAULT 0,   -- calculado desde la receta
    tiempo_preparacion_min SMALLINT,
    curso              VARCHAR(20)  DEFAULT 'FUERTE'
                       CHECK (curso IN ('ENTRADA','FUERTE','POSTRE','BEBIDA','ACOMPANAMIENTO')),
    disponible         BOOLEAN      NOT NULL DEFAULT true,  -- "se acabo" del dia
    -- Alergenos, apto vegano, nivel de picante, calorias... configuracion,
    -- no columnas nuevas.
    atributos          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    imagen_url         TEXT,
    orden              INT          NOT NULL DEFAULT 0,
    activo             BOOLEAN      NOT NULL DEFAULT true,
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    eliminado_en       TIMESTAMPTZ,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_item_menu UNIQUE (negocio_id, codigo)
);
CREATE INDEX ix_items_categoria ON items_menu (negocio_id, categoria_menu_id) WHERE activo;
CREATE INDEX ix_items_atributos ON items_menu USING gin (atributos jsonb_path_ops);

-- Modificadores: 'Termino de coccion' (min 1, max 1), 'Adiciones' (min 0, max 5)
CREATE TABLE grupos_modificadores (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    nombre          VARCHAR(80) NOT NULL,
    min_selecciones SMALLINT    NOT NULL DEFAULT 0,
    max_selecciones SMALLINT    NOT NULL DEFAULT 1,
    obligatorio     BOOLEAN     GENERATED ALWAYS AS (min_selecciones > 0) STORED,
    activo          BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT ck_selecciones CHECK (max_selecciones >= min_selecciones)
);
CREATE UNIQUE INDEX uq_grupo_mod ON grupos_modificadores (negocio_id, lower(nombre));

CREATE TABLE modificadores (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    grupo_id      UUID        NOT NULL REFERENCES grupos_modificadores(id) ON DELETE CASCADE,
    nombre        VARCHAR(80) NOT NULL,        -- 'Termino medio','Extra queso'
    precio_extra  NUMERIC(14,4) NOT NULL DEFAULT 0,
    producto_id   UUID,                        -- [ref logica] si descuenta insumo
    cantidad_insumo NUMERIC(18,6),
    orden         INT         NOT NULL DEFAULT 0,
    activo        BOOLEAN     NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_modificador ON modificadores (grupo_id, lower(nombre));

CREATE TABLE item_grupos_modificadores (
    item_id  UUID     NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    grupo_id UUID     NOT NULL REFERENCES grupos_modificadores(id) ON DELETE CASCADE,
    negocio_id UUID   NOT NULL,
    orden    INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id, grupo_id)
);

-- ***** RECETA: el puente Comanda -> Inventario *****
-- Al cerrar una comanda, el servicio explota cada linea contra su receta y
-- publica `insumos_consumidos`; Inventario descuenta los productos.
CREATE TABLE recetas (
    id              UUID PRIMARY KEY,
    negocio_id      UUID NOT NULL,
    item_menu_id    UUID NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    producto_id     UUID NOT NULL,             -- [ref logica -> inventario.productos]
    nombre_snapshot VARCHAR(180),
    cantidad        NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    unidad          VARCHAR(20),
    merma_pct       NUMERIC(7,4) NOT NULL DEFAULT 0,
    opcional        BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_receta UNIQUE (item_menu_id, producto_id)
);
CREATE INDEX ix_recetas_producto ON recetas (negocio_id, producto_id);

CREATE TABLE combo_items (
    combo_id   UUID NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    item_id    UUID NOT NULL REFERENCES items_menu(id),
    negocio_id UUID NOT NULL,
    cantidad   SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (combo_id, item_id),
    CONSTRAINT ck_combo_no_autoref CHECK (combo_id <> item_id)
);

-- Disponibilidad del dia (86'd items). Se resetea por turno.
CREATE TABLE disponibilidad_diaria (
    negocio_id   UUID NOT NULL,
    sucursal_id  UUID,
    item_id      UUID NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    fecha        DATE NOT NULL,
    cantidad_disponible INT,          -- NULL = ilimitado
    cantidad_vendida    INT NOT NULL DEFAULT 0,
    agotado      BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (item_id, fecha)
);

-- EVENTOS PUBLICADOS: item_menu_actualizado, item_agotado
-- EVENTOS CONSUMIDOS: stock_actualizado (marcar agotado si falta el insumo)
