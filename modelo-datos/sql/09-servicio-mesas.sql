-- =====================================================================
-- REGENTA — servicio-mesas  (esquema: mesas)
-- PATRON: Comanda. El PDF lo marca como "sin equivalente directo" en
-- Venta directa: es el mapa fisico del salon y el estado de ocupacion.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS mesas;
SET search_path TO mesas, public;

CREATE TABLE zonas (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    nombre      VARCHAR(60) NOT NULL,        -- 'Salon','Terraza','Barra','VIP'
    orden       INT         NOT NULL DEFAULT 0,
    color       CHAR(7),
    activa      BOOLEAN     NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX uq_zona ON zonas
    (negocio_id, COALESCE(sucursal_id,'00000000-0000-0000-0000-000000000000'::uuid), lower(nombre));

CREATE TABLE mesas (
    id          UUID PRIMARY KEY,
    negocio_id  UUID        NOT NULL,
    sucursal_id UUID,
    zona_id     UUID        REFERENCES zonas(id),
    codigo      VARCHAR(20) NOT NULL,        -- 'M1','T04'
    nombre      VARCHAR(60),
    capacidad   SMALLINT    NOT NULL DEFAULT 4 CHECK (capacidad > 0),
    forma       VARCHAR(20) NOT NULL DEFAULT 'CUADRADA'
                CHECK (forma IN ('CUADRADA','REDONDA','RECTANGULAR','BARRA')),
    estado      VARCHAR(20) NOT NULL DEFAULT 'LIBRE'
                CHECK (estado IN ('LIBRE','OCUPADA','RESERVADA','CUENTA_PEDIDA','SUCIA','BLOQUEADA')),
    -- Coordenadas para el plano del salon en la app
    pos_x       INT         NOT NULL DEFAULT 0,
    pos_y       INT         NOT NULL DEFAULT 0,
    ancho       INT         NOT NULL DEFAULT 80,
    alto        INT         NOT NULL DEFAULT 80,
    qr_token    VARCHAR(64) UNIQUE,          -- pedido desde el celular del comensal
    activa      BOOLEAN     NOT NULL DEFAULT true,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_mesa_codigo UNIQUE (negocio_id, codigo)
);
CREATE INDEX ix_mesas_zona ON mesas (negocio_id, zona_id) WHERE activa;
CREATE INDEX ix_mesas_estado ON mesas (negocio_id, estado);

-- Una sesion es "esta ocupacion de la mesa". Separarla de `mesas` permite
-- historial (cuantas veces roto la mesa 5 hoy) y union de mesas.
CREATE TABLE sesiones_mesa (
    id             UUID PRIMARY KEY,
    negocio_id     UUID        NOT NULL,
    sucursal_id    UUID,
    mesa_principal_id UUID     NOT NULL REFERENCES mesas(id),
    comanda_id     UUID,                       -- [ref logica -> comandas]
    mesero_usuario_id UUID,
    num_comensales SMALLINT    NOT NULL DEFAULT 1,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                   CHECK (estado IN ('ABIERTA','CUENTA_PEDIDA','CERRADA','ANULADA')),
    abierta_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrada_en     TIMESTAMPTZ,
    duracion_min   INT GENERATED ALWAYS AS
                   (CASE WHEN cerrada_en IS NULL THEN NULL
                    ELSE EXTRACT(EPOCH FROM (cerrada_en - abierta_en))::int / 60 END) STORED,
    version        BIGINT      NOT NULL DEFAULT 0
);
-- Una sola sesion abierta por mesa a la vez
CREATE UNIQUE INDEX uq_sesion_abierta
    ON sesiones_mesa (mesa_principal_id) WHERE estado IN ('ABIERTA','CUENTA_PEDIDA');
CREATE INDEX ix_sesiones_fecha ON sesiones_mesa (negocio_id, abierta_en DESC);

-- Union de mesas (grupo de 10 personas ocupa M1+M2)
CREATE TABLE sesion_mesas (
    sesion_id  UUID NOT NULL REFERENCES sesiones_mesa(id) ON DELETE CASCADE,
    mesa_id    UUID NOT NULL REFERENCES mesas(id),
    negocio_id UUID NOT NULL,
    PRIMARY KEY (sesion_id, mesa_id)
);

-- Reservas de mesa (restaurante que toma reservas SIN usar el patron Reserva
-- completo: aqui la transaccion sigue siendo la comanda, no la reserva).
CREATE TABLE reservas_mesa (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    mesa_id      UUID        REFERENCES mesas(id),
    zona_id      UUID        REFERENCES zonas(id),
    cliente_id   UUID,
    nombre_contacto VARCHAR(120) NOT NULL,
    telefono     VARCHAR(30),
    num_personas SMALLINT    NOT NULL,
    desde        TIMESTAMPTZ NOT NULL,
    hasta        TIMESTAMPTZ NOT NULL,
    estado       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                 CHECK (estado IN ('PENDIENTE','CONFIRMADA','SENTADA','CANCELADA','NO_SHOW')),
    notas        TEXT,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reserva_mesa_periodo CHECK (hasta > desde)
);
CREATE INDEX ix_reservas_mesa_fecha ON reservas_mesa (negocio_id, desde);

-- EVENTOS PUBLICADOS: mesa_ocupada, mesa_liberada, sesion_mesa_abierta,
--                     sesion_mesa_cerrada
-- EVENTOS CONSUMIDOS: comanda_cerrada (libera la mesa -> estado SUCIA)
