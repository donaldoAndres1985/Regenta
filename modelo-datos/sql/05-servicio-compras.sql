-- =====================================================================
-- REGENTA — servicio-compras  (esquema: compras)
-- PATRON: Venta directa — modulo de EXTENSION (plan Profesional).
-- Es lo que "llena" el inventario: sin el, el stock solo baja.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS compras;
SET search_path TO compras, public;

CREATE TABLE proveedores (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL,
    tipo_documento    VARCHAR(10)  NOT NULL DEFAULT 'NIT',
    numero_documento  VARCHAR(30)  NOT NULL,
    razon_social      VARCHAR(200) NOT NULL,
    nombre_comercial  VARCHAR(150),
    contacto_nombre   VARCHAR(120),
    email             VARCHAR(150),
    telefono          VARCHAR(30),
    direccion         VARCHAR(200),
    ciudad            VARCHAR(80),
    pais              CHAR(2)      NOT NULL DEFAULT 'CO',
    dias_credito      SMALLINT     NOT NULL DEFAULT 0,
    cupo_credito      NUMERIC(14,2) NOT NULL DEFAULT 0,
    saldo_pendiente   NUMERIC(14,2) NOT NULL DEFAULT 0,
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    calificacion      SMALLINT     CHECK (calificacion BETWEEN 1 AND 5),
    notas             TEXT,
    activo            BOOLEAN      NOT NULL DEFAULT true,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    eliminado_en      TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_proveedor_doc UNIQUE (negocio_id, tipo_documento, numero_documento)
);
CREATE INDEX ix_proveedores_negocio ON proveedores (negocio_id) WHERE activo;

-- Que productos vende cada proveedor y a que costo (evita retipear costos)
CREATE TABLE proveedor_productos (
    negocio_id       UUID NOT NULL,
    proveedor_id     UUID NOT NULL REFERENCES proveedores(id) ON DELETE CASCADE,
    producto_id      UUID NOT NULL,             -- [ref logica -> inventario]
    codigo_proveedor VARCHAR(60),
    costo_ultimo     NUMERIC(14,4),
    dias_entrega     SMALLINT,
    cantidad_minima  NUMERIC(18,6) NOT NULL DEFAULT 1,
    preferido        BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (proveedor_id, producto_id)
);

CREATE TABLE ordenes_compra (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL,
    sucursal_id     UUID,
    numero          VARCHAR(30) NOT NULL,
    proveedor_id    UUID        NOT NULL REFERENCES proveedores(id),
    bodega_destino_id UUID      NOT NULL,       -- [ref logica -> inventario.bodegas]
    estado          VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR','APROBADA','ENVIADA','PARCIAL',
                                      'RECIBIDA','CERRADA','ANULADA')),
    fecha_emision   DATE        NOT NULL DEFAULT CURRENT_DATE,
    fecha_esperada  DATE,
    subtotal        NUMERIC(16,4) NOT NULL DEFAULT 0,
    descuento_total NUMERIC(16,4) NOT NULL DEFAULT 0,
    impuesto_total  NUMERIC(16,4) NOT NULL DEFAULT 0,
    flete           NUMERIC(14,4) NOT NULL DEFAULT 0,
    total           NUMERIC(16,4) NOT NULL DEFAULT 0,
    moneda          CHAR(3)     NOT NULL DEFAULT 'COP',
    tasa_cambio     NUMERIC(14,6) NOT NULL DEFAULT 1,
    usuario_id      UUID,
    aprobado_por    UUID,
    aprobado_en     TIMESTAMPTZ,
    observaciones   TEXT,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_oc_numero UNIQUE (negocio_id, numero)
);
CREATE INDEX ix_oc_proveedor ON ordenes_compra (negocio_id, proveedor_id, fecha_emision DESC);
CREATE INDEX ix_oc_pendientes ON ordenes_compra (negocio_id, fecha_esperada)
    WHERE estado IN ('APROBADA','ENVIADA','PARCIAL');

CREATE TABLE orden_compra_lineas (
    id                UUID PRIMARY KEY,
    negocio_id        UUID NOT NULL,
    orden_id          UUID NOT NULL REFERENCES ordenes_compra(id) ON DELETE CASCADE,
    linea             SMALLINT NOT NULL,
    producto_id       UUID NOT NULL,
    nombre_snapshot   VARCHAR(180) NOT NULL,
    cantidad_pedida   NUMERIC(18,6) NOT NULL CHECK (cantidad_pedida > 0),
    cantidad_recibida NUMERIC(18,6) NOT NULL DEFAULT 0,
    costo_unitario    NUMERIC(14,4) NOT NULL CHECK (costo_unitario >= 0),
    descuento_pct     NUMERIC(7,4)  NOT NULL DEFAULT 0,
    impuesto_pct      NUMERIC(7,4)  NOT NULL DEFAULT 0,
    subtotal          NUMERIC(16,4) NOT NULL,
    total             NUMERIC(16,4) NOT NULL,
    CONSTRAINT uq_ocl UNIQUE (orden_id, linea),
    CONSTRAINT ck_recibida_oc CHECK (cantidad_recibida <= cantidad_pedida * 1.05) -- tolerancia 5%
);

CREATE TABLE recepciones (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    orden_id      UUID        REFERENCES ordenes_compra(id),  -- NULL = compra directa
    proveedor_id  UUID        NOT NULL REFERENCES proveedores(id),
    bodega_id     UUID        NOT NULL,
    numero        VARCHAR(30) NOT NULL,
    factura_proveedor VARCHAR(40),
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado        VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                  CHECK (estado IN ('BORRADOR','CONFIRMADA','ANULADA')),
    total         NUMERIC(16,4) NOT NULL DEFAULT 0,
    usuario_id    UUID,
    observaciones TEXT,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_recepcion_numero UNIQUE (negocio_id, numero)
);

CREATE TABLE recepcion_lineas (
    id               UUID PRIMARY KEY,
    negocio_id       UUID NOT NULL,
    recepcion_id     UUID NOT NULL REFERENCES recepciones(id) ON DELETE CASCADE,
    orden_linea_id   UUID REFERENCES orden_compra_lineas(id),
    producto_id      UUID NOT NULL,
    cantidad         NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    costo_unitario   NUMERIC(14,4) NOT NULL,
    -- Datos de lote capturados en la recepcion (drogueria, alimentos)
    codigo_lote      VARCHAR(60),
    fecha_vencimiento DATE,
    registro_sanitario VARCHAR(60),
    series           TEXT[]
);

CREATE TABLE cuentas_por_pagar (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    proveedor_id   UUID        NOT NULL REFERENCES proveedores(id),
    recepcion_id   UUID        REFERENCES recepciones(id),
    numero_factura VARCHAR(40) NOT NULL,
    monto          NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    saldo          NUMERIC(14,2) NOT NULL,
    fecha_emision  DATE        NOT NULL,
    fecha_vencimiento DATE     NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE','PARCIAL','PAGADA','VENCIDA','ANULADA')),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_cxp UNIQUE (negocio_id, proveedor_id, numero_factura),
    CONSTRAINT ck_saldo_cxp CHECK (saldo >= 0 AND saldo <= monto)
);
CREATE INDEX ix_cxp_vencimiento ON cuentas_por_pagar (negocio_id, fecha_vencimiento)
    WHERE estado IN ('PENDIENTE','PARCIAL','VENCIDA');

CREATE TABLE pagos_proveedor (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    cuenta_id   UUID        NOT NULL REFERENCES cuentas_por_pagar(id) ON DELETE CASCADE,
    monto       NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    metodo      VARCHAR(20) NOT NULL
                CHECK (metodo IN ('EFECTIVO','TRANSFERENCIA','CHEQUE','TARJETA','OTRO')),
    referencia  VARCHAR(60),
    fecha       DATE        NOT NULL DEFAULT CURRENT_DATE,
    usuario_id  UUID,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- EVENTOS PUBLICADOS: recepcion_registrada (Inventario da entrada),
--                     orden_compra_aprobada, cuenta_por_pagar_creada
-- EVENTOS CONSUMIDOS: stock_bajo_minimo (sugerencia de reposicion)
