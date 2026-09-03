-- =====================================================================
-- REGENTA — servicio-alertas  (esquema: alertas)  [plan Profesional]
-- Motor generico: una regla es una condicion + un canal + destinatarios.
-- Agregar "alertar vencimientos en drogueria" NO es codigo nuevo: es una
-- fila en `reglas_alerta` con tipo VENCIMIENTO_LOTE.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS alertas;
SET search_path TO alertas, public;

CREATE TABLE tipos_alerta (
    codigo        VARCHAR(40) PRIMARY KEY,
    nombre        VARCHAR(80) NOT NULL,
    modulo        VARCHAR(40) NOT NULL,
    patron        VARCHAR(30),                 -- NULL = aplica a todos
    severidad_default VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    plantilla_titulo  TEXT NOT NULL,
    plantilla_mensaje TEXT NOT NULL
);
-- Semilla: STOCK_MINIMO, STOCK_AGOTADO, VENCIMIENTO_LOTE, PRODUCTO_SIN_ROTACION,
--          CXC_VENCIDA, CXP_POR_VENCER, CAJA_DESCUADRADA, RESERVA_PROXIMA,
--          CHECK_OUT_PENDIENTE, NO_SHOW, COMANDA_DEMORADA, ITEM_AGOTADO,
--          FACTURA_RECHAZADA, RESOLUCION_POR_AGOTARSE, SYNC_CONFLICTO

CREATE TABLE reglas_alerta (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    sucursal_id   UUID,
    tipo_codigo   VARCHAR(40) NOT NULL REFERENCES tipos_alerta(codigo),
    nombre        VARCHAR(80) NOT NULL,
    -- Condicion declarativa evaluada por el servicio:
    -- {"campo":"dias_para_vencer","op":"<=","valor":30}
    condicion     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    severidad     VARCHAR(10) NOT NULL DEFAULT 'MEDIA'
                  CHECK (severidad IN ('BAJA','MEDIA','ALTA','CRITICA')),
    canales       TEXT[]      NOT NULL DEFAULT '{IN_APP}',   -- IN_APP, PUSH, EMAIL
    destinatarios_roles TEXT[] NOT NULL DEFAULT '{}',
    destinatarios_usuarios UUID[] NOT NULL DEFAULT '{}',
    frecuencia    VARCHAR(20) NOT NULL DEFAULT 'INMEDIATA'
                  CHECK (frecuencia IN ('INMEDIATA','HORARIA','DIARIA','SEMANAL')),
    hora_envio    TIME,
    silenciar_horas SMALLINT  NOT NULL DEFAULT 24,   -- anti-spam de la misma alerta
    activa        BOOLEAN     NOT NULL DEFAULT true,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_regla UNIQUE (negocio_id, tipo_codigo, nombre)
);

CREATE TABLE alertas (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL,
    sucursal_id   UUID,
    regla_id      UUID        REFERENCES reglas_alerta(id) ON DELETE SET NULL,
    tipo_codigo   VARCHAR(40) NOT NULL REFERENCES tipos_alerta(codigo),
    severidad     VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    titulo        VARCHAR(150) NOT NULL,
    mensaje       TEXT        NOT NULL,
    datos         JSONB       NOT NULL DEFAULT '{}'::jsonb,
    -- A que entidad apunta (para navegar desde la notificacion)
    entidad_tipo  VARCHAR(40),
    entidad_id    UUID,
    ruta_app      VARCHAR(200),               -- deep link go_router
    estado        VARCHAR(20) NOT NULL DEFAULT 'NUEVA'
                  CHECK (estado IN ('NUEVA','VISTA','EN_CURSO','RESUELTA','DESCARTADA')),
    -- Deduplicacion: la misma alerta no se repite dentro de la ventana
    huella        VARCHAR(120) NOT NULL,
    generada_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    vista_en      TIMESTAMPTZ,
    resuelta_en   TIMESTAMPTZ,
    resuelta_por  UUID,
    expira_en     TIMESTAMPTZ,
    CONSTRAINT uq_alerta_huella UNIQUE (negocio_id, huella)
);
CREATE INDEX ix_alertas_pendientes ON alertas (negocio_id, estado, severidad, generada_en DESC)
    WHERE estado IN ('NUEVA','VISTA');
CREATE INDEX ix_alertas_entidad ON alertas (negocio_id, entidad_tipo, entidad_id);

CREATE TABLE dispositivos_push (
    id           UUID PRIMARY KEY,
    negocio_id   UUID         NOT NULL,
    usuario_id   UUID         NOT NULL,
    token_fcm    VARCHAR(255) NOT NULL UNIQUE,
    plataforma   VARCHAR(20)  NOT NULL CHECK (plataforma IN ('ANDROID','WEB','IOS')),
    modelo       VARCHAR(80),
    version_app  VARCHAR(20),
    activo       BOOLEAN      NOT NULL DEFAULT true,
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultimo_uso_en TIMESTAMPTZ
);
CREATE INDEX ix_push_usuario ON dispositivos_push (negocio_id, usuario_id) WHERE activo;

CREATE TABLE entregas (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    alerta_id    UUID        NOT NULL REFERENCES alertas(id) ON DELETE CASCADE,
    usuario_id   UUID        NOT NULL,
    canal        VARCHAR(20) NOT NULL CHECK (canal IN ('IN_APP','PUSH','EMAIL','SMS','WEBHOOK')),
    destino      VARCHAR(255),
    estado       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                 CHECK (estado IN ('PENDIENTE','ENVIADA','ENTREGADA','FALLIDA','LEIDA')),
    proveedor_id VARCHAR(120),               -- message id de FCM/SES
    intentos     SMALLINT    NOT NULL DEFAULT 0,
    error        TEXT,
    enviada_en   TIMESTAMPTZ,
    leida_en     TIMESTAMPTZ
);
CREATE INDEX ix_entregas_pendientes ON entregas (estado, enviada_en)
    WHERE estado IN ('PENDIENTE','FALLIDA');

-- Preferencias por usuario (no molestar, canales que acepta)
CREATE TABLE preferencias_notificacion (
    negocio_id   UUID NOT NULL,
    usuario_id   UUID NOT NULL,
    tipo_codigo  VARCHAR(40) NOT NULL REFERENCES tipos_alerta(codigo),
    canales      TEXT[] NOT NULL DEFAULT '{IN_APP}',
    habilitada   BOOLEAN NOT NULL DEFAULT true,
    no_molestar_desde TIME,
    no_molestar_hasta TIME,
    PRIMARY KEY (usuario_id, tipo_codigo)
);

-- EVENTOS CONSUMIDOS: stock_bajo_minimo, lote_por_vencer, caja_descuadrada,
--                     factura_rechazada, comanda_demorada, cxc_vencida,
--                     sync_conflicto, reserva_proxima
