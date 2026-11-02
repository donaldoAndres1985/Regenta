-- =====================================================================
-- servicio-inventario . V1 . esquema inicial
--
-- Portado de modelo-datos/sql/03-servicio-inventario.sql (el esquema del servicio) y de
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

CREATE SCHEMA IF NOT EXISTS inventario;
SET search_path TO inventario, public;

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
-- Parte 2 . Esquema propio de servicio-inventario
-- ---------------------------------------------------------------------
-- =====================================================================
-- REGENTA — servicio-inventario  (esquema: inventario)
-- PATRON: Venta directa — modulo de CATALOGO.
-- Es el modulo donde vive la "generalizacion por configuracion":
-- ferreteria, papeleria, drogueria, veterinaria... usan estas mismas
-- tablas; lo unico que cambia son las filas de `categorias` y
-- `atributos_categoria`, y el contenido del JSONB `atributos`.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS inventario;
SET search_path TO inventario, public;

-- ---------------------------------------------------------------------
-- Configuracion por negocio (lo que reemplaza al "modulo por vertical")
-- ---------------------------------------------------------------------

CREATE TABLE categorias (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID        NOT NULL,
    categoria_padre_id UUID        REFERENCES categorias(id) ON DELETE RESTRICT,
    nombre             VARCHAR(100) NOT NULL,
    descripcion        TEXT,
    ruta               VARCHAR(500),        -- materialized path: 'Herramientas/Manuales'
    nivel              SMALLINT    NOT NULL DEFAULT 0,
    icono              VARCHAR(40),
    color              CHAR(7),
    orden              INT         NOT NULL DEFAULT 0,
    activa             BOOLEAN     NOT NULL DEFAULT true,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_categoria_no_autopadre CHECK (id <> categoria_padre_id)
);
CREATE UNIQUE INDEX uq_categoria_nombre
    ON categorias (negocio_id, COALESCE(categoria_padre_id,'00000000-0000-0000-0000-000000000000'::uuid), lower(nombre));
CREATE INDEX ix_categorias_negocio ON categorias (negocio_id) WHERE activa;

-- Define QUE campos extra exige cada categoria. Es el contrato que valida
-- el JSONB `productos.atributos`. Ej.: "Medicamentos" exige lote +
-- vencimiento + registro sanitario; "Tornilleria" no exige nada.
CREATE TABLE atributos_categoria (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    categoria_id   UUID        NOT NULL REFERENCES categorias(id) ON DELETE CASCADE,
    nombre_campo   VARCHAR(50) NOT NULL,        -- slug: 'fecha_vencimiento'
    etiqueta       VARCHAR(80) NOT NULL,        -- 'Fecha de vencimiento'
    tipo           VARCHAR(20) NOT NULL
                   CHECK (tipo IN ('TEXTO','NUMERO','DECIMAL','FECHA','BOOLEANO','LISTA','MULTILISTA')),
    obligatorio    BOOLEAN     NOT NULL DEFAULT false,
    unidad         VARCHAR(20),                 -- 'metro','kilo'
    opciones       JSONB,                       -- ['Rojo','Azul'] para LISTA
    valor_default  TEXT,
    validacion_regex TEXT,
    valor_min      NUMERIC(18,6),
    valor_max      NUMERIC(18,6),
    heredable      BOOLEAN     NOT NULL DEFAULT true,  -- lo heredan subcategorias
    orden          INT         NOT NULL DEFAULT 0,
    activo         BOOLEAN     NOT NULL DEFAULT true,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_atributo UNIQUE (categoria_id, nombre_campo),
    CONSTRAINT ck_opciones_lista CHECK (tipo NOT IN ('LISTA','MULTILISTA') OR opciones IS NOT NULL)
);

CREATE TABLE unidades_medida (
    id                UUID PRIMARY KEY,
    negocio_id        UUID        NOT NULL,
    codigo            VARCHAR(10) NOT NULL,   -- 'UND','KG','MT','LT'
    nombre            VARCHAR(50) NOT NULL,
    permite_decimales BOOLEAN     NOT NULL DEFAULT false,
    factor_base       NUMERIC(18,6) NOT NULL DEFAULT 1,
    unidad_base_id    UUID        REFERENCES unidades_medida(id),
    activa            BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_unidad UNIQUE (negocio_id, codigo)
);

CREATE TABLE marcas (
    id         UUID PRIMARY KEY,
    negocio_id UUID        NOT NULL,
    nombre     VARCHAR(80) NOT NULL,
    activa     BOOLEAN     NOT NULL DEFAULT true
);
-- UNIQUE funcional: va como indice, no como constraint inline.
CREATE UNIQUE INDEX uq_marca ON marcas (negocio_id, lower(nombre));

-- ---------------------------------------------------------------------
-- Catalogo
-- ---------------------------------------------------------------------

CREATE TABLE productos (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    sku               VARCHAR(60)  NOT NULL,
    codigo_barras     VARCHAR(60),
    nombre            VARCHAR(180) NOT NULL,
    descripcion       TEXT,
    categoria_id      UUID         NOT NULL REFERENCES categorias(id),
    marca_id          UUID         REFERENCES marcas(id),
    unidad_medida_id  UUID         NOT NULL REFERENCES unidades_medida(id),
    tipo              VARCHAR(20)  NOT NULL DEFAULT 'BIEN'
                      CHECK (tipo IN ('BIEN','SERVICIO','COMBO','INSUMO')),
    -- Precios
    precio_venta      NUMERIC(14,4) NOT NULL DEFAULT 0 CHECK (precio_venta >= 0),
    costo_ultimo      NUMERIC(14,4) NOT NULL DEFAULT 0,
    costo_promedio    NUMERIC(14,4) NOT NULL DEFAULT 0,  -- costeo promedio ponderado
    margen_objetivo   NUMERIC(7,4),
    impuesto_id       UUID,                              -- [ref logica -> core_identidad.impuestos]
    precio_incluye_impuesto BOOLEAN NOT NULL DEFAULT true,
    -- Control de stock
    controla_stock    BOOLEAN      NOT NULL DEFAULT true, -- false para SERVICIO
    stock_minimo      NUMERIC(18,6) NOT NULL DEFAULT 0,
    stock_maximo      NUMERIC(18,6),
    punto_reorden     NUMERIC(18,6),
    maneja_lotes      BOOLEAN      NOT NULL DEFAULT false,
    maneja_series     BOOLEAN      NOT NULL DEFAULT false,
    perecedero        BOOLEAN      NOT NULL DEFAULT false,
    dias_alerta_vencimiento SMALLINT,
    permite_venta_sin_stock BOOLEAN NOT NULL DEFAULT false,
    -- Campos variables por categoria. VALIDADOS contra atributos_categoria
    -- en el servicio antes de persistir; Postgres no valida el shape.
    atributos         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    imagen_principal  TEXT,
    activo            BOOLEAN      NOT NULL DEFAULT true,
    origen_offline_id UUID,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_por        UUID,
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_por   UUID,
    eliminado_en      TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_producto_sku UNIQUE (negocio_id, sku),
    CONSTRAINT ck_lotes_perecedero CHECK (NOT perecedero OR maneja_lotes),
    CONSTRAINT ck_stock_min_max CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo)
);
CREATE UNIQUE INDEX uq_producto_barras
    ON productos (negocio_id, codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE UNIQUE INDEX uq_producto_offline
    ON productos (negocio_id, origen_offline_id) WHERE origen_offline_id IS NOT NULL;
CREATE INDEX ix_productos_categoria ON productos (negocio_id, categoria_id) WHERE activo;
CREATE INDEX ix_productos_nombre    ON productos USING gin (nombre gin_trgm_ops);
-- Indice GIN sobre el JSONB: permite filtrar por atributos sin columnas nuevas.
-- ADVERTENCIA: si un atributo se filtra en TODA consulta (ej. vencimiento),
-- promoverlo a columna real o a indice de expresion; el GIN generico no
-- rinde igual que un B-tree dedicado.
CREATE INDEX ix_productos_atributos ON productos USING gin (atributos jsonb_path_ops);

CREATE TABLE producto_imagenes (
    id          UUID PRIMARY KEY,
    negocio_id  UUID NOT NULL,
    producto_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    orden       INT  NOT NULL DEFAULT 0,
    principal   BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE producto_codigos (          -- codigos de barras alternos (caja, docena)
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    producto_id  UUID        NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    codigo       VARCHAR(60) NOT NULL,
    factor       NUMERIC(18,6) NOT NULL DEFAULT 1,  -- 1 caja = 12 unidades
    descripcion  VARCHAR(60),
    CONSTRAINT uq_producto_codigo UNIQUE (negocio_id, codigo)
);

CREATE TABLE listas_precios (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    nombre         VARCHAR(80) NOT NULL,
    moneda         CHAR(3)     NOT NULL DEFAULT 'COP',
    es_default     BOOLEAN     NOT NULL DEFAULT false,
    vigente_desde  DATE,
    vigente_hasta  DATE,
    activa         BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_lista_precio UNIQUE (negocio_id, nombre)
);

CREATE TABLE precios_producto (
    lista_id        UUID NOT NULL REFERENCES listas_precios(id) ON DELETE CASCADE,
    producto_id     UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    negocio_id      UUID NOT NULL,
    precio          NUMERIC(14,4) NOT NULL CHECK (precio >= 0),
    descuento_max_pct NUMERIC(7,4) NOT NULL DEFAULT 0,
    cantidad_minima NUMERIC(18,6) NOT NULL DEFAULT 1,   -- precio por volumen
    PRIMARY KEY (lista_id, producto_id, cantidad_minima)
);

-- ---------------------------------------------------------------------
-- Stock: bodegas, existencias, lotes, series
-- ---------------------------------------------------------------------
-- DECISION DE DISENIO (correccion sobre el PDF): el PDF define
--   productos (id, negocio_id, nombre, categoria_id, precio, stock, ...)
-- con `stock` como columna del producto. Eso funciona solo mientras haya
-- una sola bodega. El plan Empresarial vende Multi-sucursal, y ademas
-- POS/Caja necesita saber de que bodega salio la mercancia. Mover el stock
-- a `existencias` DESPUES obliga a reescribir todas las queries de venta.
-- Por eso el stock se modela desde el dia 1 como (producto, bodega).

CREATE TABLE bodegas (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    sucursal_id  UUID,                          -- [ref logica -> sucursales]
    codigo       VARCHAR(20) NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    tipo         VARCHAR(20) NOT NULL DEFAULT 'PRINCIPAL'
                 CHECK (tipo IN ('PRINCIPAL','SECUNDARIA','TRANSITO','DEVOLUCIONES','AVERIAS')),
    es_default   BOOLEAN     NOT NULL DEFAULT false,
    activa       BOOLEAN     NOT NULL DEFAULT true,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bodega_codigo UNIQUE (negocio_id, codigo)
);

-- Proyeccion del saldo actual. La VERDAD esta en movimientos_inventario;
-- esta tabla es un cache transaccional para no sumar el libro entero.
CREATE TABLE existencias (
    negocio_id          UUID NOT NULL,
    producto_id         UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    bodega_id           UUID NOT NULL REFERENCES bodegas(id)   ON DELETE CASCADE,
    cantidad            NUMERIC(18,6) NOT NULL DEFAULT 0,
    cantidad_reservada  NUMERIC(18,6) NOT NULL DEFAULT 0,  -- saga: stock apartado
    cantidad_disponible NUMERIC(18,6) GENERATED ALWAYS AS (cantidad - cantidad_reservada) STORED,
    costo_promedio      NUMERIC(14,4) NOT NULL DEFAULT 0,
    ultima_entrada_en   TIMESTAMPTZ,
    ultima_salida_en    TIMESTAMPTZ,
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (producto_id, bodega_id),
    CONSTRAINT ck_reservada CHECK (cantidad_reservada >= 0)
);
CREATE INDEX ix_existencias_negocio ON existencias (negocio_id, bodega_id);
-- Productos bajo minimo (alimenta al servicio-alertas)
CREATE INDEX ix_existencias_bajo_minimo ON existencias (negocio_id, producto_id)
    WHERE cantidad <= 0;

CREATE TABLE lotes (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID        NOT NULL,
    producto_id        UUID        NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    codigo_lote        VARCHAR(60) NOT NULL,
    fecha_fabricacion  DATE,
    fecha_vencimiento  DATE,
    registro_sanitario VARCHAR(60),         -- INVIMA (drogueria)
    proveedor_id       UUID,                -- [ref logica -> compras]
    costo_unitario     NUMERIC(14,4),
    atributos          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lote UNIQUE (negocio_id, producto_id, codigo_lote)
);
CREATE INDEX ix_lotes_vencimiento ON lotes (negocio_id, fecha_vencimiento)
    WHERE fecha_vencimiento IS NOT NULL;

CREATE TABLE existencias_lote (
    negocio_id  UUID NOT NULL,
    lote_id     UUID NOT NULL REFERENCES lotes(id)   ON DELETE CASCADE,
    bodega_id   UUID NOT NULL REFERENCES bodegas(id) ON DELETE CASCADE,
    cantidad    NUMERIC(18,6) NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (lote_id, bodega_id)
);

CREATE TABLE series (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    producto_id   UUID        NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    numero_serie  VARCHAR(80) NOT NULL,
    bodega_id     UUID        REFERENCES bodegas(id),
    lote_id       UUID        REFERENCES lotes(id),
    estado        VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
                  CHECK (estado IN ('DISPONIBLE','RESERVADA','VENDIDA','DEVUELTA','BAJA')),
    venta_id      UUID,                     -- [ref logica]
    garantia_hasta DATE,
    CONSTRAINT uq_serie UNIQUE (negocio_id, producto_id, numero_serie)
);

-- ---------------------------------------------------------------------
-- Libro mayor de inventario (append-only). FUENTE DE VERDAD.
-- ---------------------------------------------------------------------
CREATE TABLE movimientos_inventario (
    id               UUID        NOT NULL,
    negocio_id       UUID        NOT NULL,
    sucursal_id      UUID,
    producto_id      UUID        NOT NULL REFERENCES productos(id),
    bodega_id        UUID        NOT NULL REFERENCES bodegas(id),
    lote_id          UUID        REFERENCES lotes(id),
    serie_id         UUID        REFERENCES series(id),
    tipo             VARCHAR(25) NOT NULL
                     CHECK (tipo IN ('ENTRADA_COMPRA','ENTRADA_AJUSTE','ENTRADA_DEVOLUCION',
                                     'ENTRADA_TRASLADO','SALIDA_VENTA','SALIDA_AJUSTE',
                                     'SALIDA_TRASLADO','SALIDA_MERMA','SALIDA_CONSUMO',
                                     'RESERVA','LIBERACION_RESERVA')),
    signo            SMALLINT    NOT NULL CHECK (signo IN (-1,0,1)),
    cantidad         NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    costo_unitario   NUMERIC(14,4) NOT NULL DEFAULT 0,
    saldo_posterior  NUMERIC(18,6) NOT NULL,   -- snapshot para auditoria
    costo_promedio_posterior NUMERIC(14,4),
    origen_tipo      VARCHAR(25) NOT NULL
                     CHECK (origen_tipo IN ('VENTA','COMPRA','RECEPCION','AJUSTE','TRASLADO',
                                            'DEVOLUCION','COMANDA','RESERVA','CARGA_INICIAL','SAGA')),
    origen_id        UUID,                     -- [ref logica]
    usuario_id       UUID,
    motivo           TEXT,
    idempotency_key  VARCHAR(120) NOT NULL,    -- evita doble descuento por reintento
    ocurrido_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- En una tabla particionada toda clave unica debe incluir la columna de
    -- particion; por eso la PK y el UNIQUE de idempotencia llevan ocurrido_en.
    PRIMARY KEY (id, ocurrido_en),
    CONSTRAINT uq_movimiento_idem UNIQUE (negocio_id, idempotency_key, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);
-- Particionado mensual: es la tabla que mas crece del sistema.
CREATE TABLE movimientos_inventario_default PARTITION OF movimientos_inventario DEFAULT;
CREATE INDEX ix_mov_producto ON movimientos_inventario (negocio_id, producto_id, ocurrido_en DESC);
CREATE INDEX ix_mov_origen   ON movimientos_inventario (negocio_id, origen_tipo, origen_id);

-- ---------------------------------------------------------------------
-- Traslados y ajustes
-- ---------------------------------------------------------------------
CREATE TABLE traslados (
    id               UUID PRIMARY KEY,
    negocio_id       UUID        NOT NULL,
    numero           VARCHAR(30) NOT NULL,
    bodega_origen_id UUID        NOT NULL REFERENCES bodegas(id),
    bodega_destino_id UUID       NOT NULL REFERENCES bodegas(id),
    estado           VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                     CHECK (estado IN ('BORRADOR','EN_TRANSITO','RECIBIDO','ANULADO')),
    fecha_envio      TIMESTAMPTZ,
    fecha_recepcion  TIMESTAMPTZ,
    usuario_envio_id UUID,
    usuario_recepcion_id UUID,
    observaciones    TEXT,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_traslado_numero UNIQUE (negocio_id, numero),
    CONSTRAINT ck_bodegas_distintas CHECK (bodega_origen_id <> bodega_destino_id)
);

CREATE TABLE traslado_lineas (
    id               UUID PRIMARY KEY,
    negocio_id       UUID NOT NULL,
    traslado_id      UUID NOT NULL REFERENCES traslados(id) ON DELETE CASCADE,
    producto_id      UUID NOT NULL REFERENCES productos(id),
    lote_id          UUID REFERENCES lotes(id),
    cantidad_enviada NUMERIC(18,6) NOT NULL CHECK (cantidad_enviada > 0),
    cantidad_recibida NUMERIC(18,6) NOT NULL DEFAULT 0,
    CONSTRAINT ck_recibida CHECK (cantidad_recibida <= cantidad_enviada)
);

CREATE TABLE ajustes_inventario (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    numero      VARCHAR(30) NOT NULL,
    bodega_id   UUID        NOT NULL REFERENCES bodegas(id),
    tipo        VARCHAR(20) NOT NULL
                CHECK (tipo IN ('CONTEO_FISICO','MERMA','AVERIA','VENCIMIENTO','CARGA_INICIAL','OTRO')),
    estado      VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                CHECK (estado IN ('BORRADOR','APLICADO','ANULADO')),
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id  UUID,
    aprobado_por UUID,
    motivo      TEXT,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_ajuste_numero UNIQUE (negocio_id, numero)
);

CREATE TABLE ajuste_lineas (
    id               UUID PRIMARY KEY,
    negocio_id       UUID NOT NULL,
    ajuste_id        UUID NOT NULL REFERENCES ajustes_inventario(id) ON DELETE CASCADE,
    producto_id      UUID NOT NULL REFERENCES productos(id),
    lote_id          UUID REFERENCES lotes(id),
    cantidad_sistema NUMERIC(18,6) NOT NULL,
    cantidad_fisica  NUMERIC(18,6) NOT NULL,
    diferencia       NUMERIC(18,6) GENERATED ALWAYS AS (cantidad_fisica - cantidad_sistema) STORED,
    costo_unitario   NUMERIC(14,4)
);

-- Reservas de stock del patron saga (Ventas aparta antes de confirmar)
CREATE TABLE reservas_stock (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    producto_id    UUID        NOT NULL REFERENCES productos(id),
    bodega_id      UUID        NOT NULL REFERENCES bodegas(id),
    cantidad       NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    origen_tipo    VARCHAR(20) NOT NULL,      -- 'VENTA','COMANDA'
    origen_id      UUID        NOT NULL,
    correlacion_id UUID        NOT NULL,      -- saga id
    estado         VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
                   CHECK (estado IN ('ACTIVA','CONFIRMADA','LIBERADA','EXPIRADA')),
    expira_en      TIMESTAMPTZ NOT NULL,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reserva_origen UNIQUE (negocio_id, origen_tipo, origen_id, producto_id)
);
CREATE INDEX ix_reservas_expiran ON reservas_stock (expira_en) WHERE estado = 'ACTIVA';

-- Outbox / Inbox (ver 00-convenciones.sql)
-- EVENTOS PUBLICADOS:  stock_actualizado, producto_creado, producto_actualizado,
--                      stock_bajo_minimo, lote_por_vencer, stock_reservado,
--                      stock_reserva_fallida
-- EVENTOS CONSUMIDOS:  venta_completada, venta_anulada, recepcion_registrada,
--                      pedido_completado (descuento de insumos por receta)
