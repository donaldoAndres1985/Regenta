-- =====================================================================
-- REGENTA — servicio-usuarios  (esquema: core_identidad)
-- Contexto: Tenant + Planes + Identidad + Autorizacion + Configuracion
-- Es el UNICO servicio que conoce la tabla `negocios`. El resto de los
-- servicios recibe negocio_id/plan/patron como claim del JWT y como
-- replica por evento (negocio_creado, plan_cambiado), nunca por consulta
-- sincrona en cada request.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS core_identidad;
SET search_path TO core_identidad, public;

-- ---------------------------------------------------------------------
-- CATALOGO GLOBAL (sin negocio_id: es el catalogo del SaaS, no del tenant)
-- ---------------------------------------------------------------------

CREATE TABLE patrones_operativos (
    codigo        VARCHAR(30) PRIMARY KEY
                  CHECK (codigo IN ('VENTA_DIRECTA','RESERVA','COMANDA')),
    nombre        VARCHAR(80)  NOT NULL,
    descripcion   TEXT,
    modulo_catalogo    VARCHAR(40) NOT NULL, -- INVENTARIO | RECURSOS | MENU
    modulo_transaccion VARCHAR(40) NOT NULL, -- VENTAS | RESERVAS | COMANDAS
    evento_cierre VARCHAR(60)  NOT NULL      -- venta_completada | reserva_confirmada | pedido_completado
);

CREATE TABLE modulos (
    codigo        VARCHAR(40) PRIMARY KEY,   -- 'INVENTARIO', 'FACTURACION'
    nombre        VARCHAR(80) NOT NULL,
    descripcion   TEXT,
    patron        VARCHAR(30) REFERENCES patrones_operativos(codigo), -- NULL = Core
    tipo          VARCHAR(20) NOT NULL
                  CHECK (tipo IN ('CORE','CATALOGO','TRANSACCION','EXTENSION')),
    servicio      VARCHAR(60) NOT NULL,      -- microservicio que lo implementa
    obligatorio   BOOLEAN     NOT NULL DEFAULT false,
    orden         INT         NOT NULL DEFAULT 0
);

-- Grafo de dependencias entre modulos.
-- Impide activar FACTURACION sin VENTAS, o RESERVAS sin RECURSOS.
CREATE TABLE modulo_dependencias (
    modulo_codigo    VARCHAR(40) NOT NULL REFERENCES modulos(codigo) ON DELETE CASCADE,
    depende_de       VARCHAR(40) NOT NULL REFERENCES modulos(codigo) ON DELETE CASCADE,
    obligatoria      BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (modulo_codigo, depende_de),
    CHECK (modulo_codigo <> depende_de)
);

CREATE TABLE planes (
    id              UUID PRIMARY KEY,
    codigo          VARCHAR(30) NOT NULL UNIQUE
                    CHECK (codigo IN ('BASICO','PROFESIONAL','EMPRESARIAL')),
    nombre          VARCHAR(60)  NOT NULL,
    descripcion     TEXT,
    max_usuarios    INT,                       -- NULL = ilimitado (Empresarial)
    max_sucursales  INT         NOT NULL DEFAULT 1,
    max_dispositivos INT,
    precio_mensual  NUMERIC(14,2) NOT NULL DEFAULT 0,
    moneda          CHAR(3)     NOT NULL DEFAULT 'COP',
    nivel_soporte   VARCHAR(20) NOT NULL
                    CHECK (nivel_soporte IN ('EMAIL','EMAIL_CHAT','PRIORITARIO')),
    orden           INT         NOT NULL,      -- 1,2,3 -> acumulatividad
    activo          BOOLEAN     NOT NULL DEFAULT true
);

-- Que modulos incluye cada plan. Los planes son ACUMULATIVOS: se materializa
-- la lista completa (no solo el delta) para que la validacion sea una sola
-- lectura y no un recorrido del grafo en cada request.
CREATE TABLE plan_modulos (
    plan_id       UUID        NOT NULL REFERENCES planes(id) ON DELETE CASCADE,
    modulo_codigo VARCHAR(40) NOT NULL REFERENCES modulos(codigo) ON DELETE CASCADE,
    limites       JSONB       NOT NULL DEFAULT '{}'::jsonb, -- {"max_productos":500}
    PRIMARY KEY (plan_id, modulo_codigo)
);

CREATE TABLE permisos (
    codigo        VARCHAR(80) PRIMARY KEY,   -- 'INVENTARIO_PRODUCTO_EDITAR'
    modulo_codigo VARCHAR(40) NOT NULL REFERENCES modulos(codigo),
    recurso       VARCHAR(40) NOT NULL,      -- 'PRODUCTO'
    accion        VARCHAR(20) NOT NULL       -- 'VER','CREAR','EDITAR','ELIMINAR','APROBAR','EXPORTAR'
                  CHECK (accion IN ('VER','CREAR','EDITAR','ELIMINAR','APROBAR','EXPORTAR','ANULAR')),
    descripcion   TEXT
);

-- Plantillas de rol por patron. El motor de permisos NO cambia entre
-- patrones; solo cambian las plantillas que cada negocio instancia.
CREATE TABLE plantillas_rol (
    id            UUID PRIMARY KEY,
    codigo        VARCHAR(40) NOT NULL UNIQUE, -- 'ADMINISTRADOR','MESERO'
    nombre        VARCHAR(60) NOT NULL,
    patron        VARCHAR(30) REFERENCES patrones_operativos(codigo), -- NULL = comun a todos
    descripcion   TEXT
);
CREATE TABLE plantilla_rol_permisos (
    plantilla_id   UUID        NOT NULL REFERENCES plantillas_rol(id) ON DELETE CASCADE,
    permiso_codigo VARCHAR(80) NOT NULL REFERENCES permisos(codigo) ON DELETE CASCADE,
    PRIMARY KEY (plantilla_id, permiso_codigo)
);

-- ---------------------------------------------------------------------
-- TENANT
-- ---------------------------------------------------------------------

CREATE TABLE negocios (
    id                UUID PRIMARY KEY,
    nombre_comercial  VARCHAR(150) NOT NULL,
    razon_social      VARCHAR(200),
    tipo_documento    VARCHAR(10)  NOT NULL DEFAULT 'NIT'
                      CHECK (tipo_documento IN ('NIT','CC','CE','RUT','OTRO')),
    numero_documento  VARCHAR(30)  NOT NULL,
    digito_verificacion CHAR(1),
    patron_operativo  VARCHAR(30)  NOT NULL REFERENCES patrones_operativos(codigo),
    plan_id           UUID         NOT NULL REFERENCES planes(id),
    estado            VARCHAR(20)  NOT NULL DEFAULT 'TRIAL'
                      CHECK (estado IN ('TRIAL','ACTIVO','SUSPENDIDO','CANCELADO')),
    pais              CHAR(2)      NOT NULL DEFAULT 'CO',
    zona_horaria      VARCHAR(50)  NOT NULL DEFAULT 'America/Bogota',
    moneda            CHAR(3)      NOT NULL DEFAULT 'COP',
    idioma            VARCHAR(5)   NOT NULL DEFAULT 'es-CO',
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_negocio_documento UNIQUE (pais, tipo_documento, numero_documento)
);
COMMENT ON COLUMN negocios.patron_operativo IS
 'Inmutable en la practica: cambiarlo despues de operar exige migracion de datos entre patrones. Validar en el servicio.';

-- Historial de suscripcion. El PDF trata el plan como un campo; guardarlo
-- como historial permite facturar el SaaS, auditar upgrades y responder
-- "que modulos tenia activos este negocio el 12 de marzo".
CREATE TABLE suscripciones (
    id              UUID PRIMARY KEY,
    negocio_id      UUID        NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    plan_id         UUID        NOT NULL REFERENCES planes(id),
    fecha_inicio    DATE        NOT NULL,
    fecha_fin       DATE,                        -- NULL = vigente
    periodicidad    VARCHAR(20) NOT NULL DEFAULT 'MENSUAL'
                    CHECK (periodicidad IN ('MENSUAL','ANUAL')),
    precio_pactado  NUMERIC(14,2) NOT NULL,
    moneda          CHAR(3)     NOT NULL DEFAULT 'COP',
    estado          VARCHAR(20) NOT NULL DEFAULT 'VIGENTE'
                    CHECK (estado IN ('VIGENTE','VENCIDA','CANCELADA')),
    motivo_cambio   TEXT,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_suscripciones_negocio ON suscripciones (negocio_id, fecha_inicio DESC);
-- Una sola suscripcion vigente por negocio:
CREATE UNIQUE INDEX uq_suscripcion_vigente
    ON suscripciones (negocio_id) WHERE fecha_fin IS NULL;

-- Activacion efectiva por modulo. Permite add-ons (el PDF marca POS como
-- "Empresarial / add-on") y desactivaciones puntuales sin cambiar de plan.
CREATE TABLE negocio_modulos (
    negocio_id    UUID        NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    modulo_codigo VARCHAR(40) NOT NULL REFERENCES modulos(codigo),
    activo        BOOLEAN     NOT NULL DEFAULT true,
    origen        VARCHAR(20) NOT NULL DEFAULT 'PLAN'
                  CHECK (origen IN ('PLAN','ADDON','CORTESIA')),
    activado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    desactivado_en TIMESTAMPTZ,
    PRIMARY KEY (negocio_id, modulo_codigo)
);

CREATE TABLE configuracion_negocio (
    negocio_id          UUID PRIMARY KEY REFERENCES negocios(id) ON DELETE CASCADE,
    logo_url            TEXT,
    direccion           VARCHAR(200),
    ciudad              VARCHAR(80),
    departamento        VARCHAR(80),
    telefono            VARCHAR(30),
    email               VARCHAR(150),
    sitio_web           VARCHAR(150),
    regimen_fiscal      VARCHAR(40),        -- 'RESPONSABLE_IVA','NO_RESPONSABLE'
    responsabilidades_fiscales JSONB NOT NULL DEFAULT '[]'::jsonb, -- codigos DIAN O-13, O-15...
    codigo_postal       VARCHAR(15),
    decimales_moneda    SMALLINT    NOT NULL DEFAULT 2,
    formato_fecha       VARCHAR(20) NOT NULL DEFAULT 'dd/MM/yyyy',
    precios_incluyen_impuesto BOOLEAN NOT NULL DEFAULT true,
    impuesto_default_id UUID,               -- -> impuestos.id
    politica_stock_negativo BOOLEAN NOT NULL DEFAULT false,
    preferencias        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE impuestos (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    codigo        VARCHAR(20) NOT NULL,      -- '01' IVA, '04' INC (codigos DIAN)
    nombre        VARCHAR(60) NOT NULL,      -- 'IVA 19%'
    tipo          VARCHAR(20) NOT NULL
                  CHECK (tipo IN ('IVA','INC','ICA','RETEFUENTE','RETEIVA','EXENTO','EXCLUIDO')),
    porcentaje    NUMERIC(7,4) NOT NULL DEFAULT 0,
    aplica_sobre  VARCHAR(20) NOT NULL DEFAULT 'BASE'
                  CHECK (aplica_sobre IN ('BASE','TOTAL')),
    activo        BOOLEAN     NOT NULL DEFAULT true,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_impuesto UNIQUE (negocio_id, codigo, porcentaje)
);
CREATE INDEX ix_impuestos_negocio ON impuestos (negocio_id) WHERE activo;

-- Multi-sucursal se vende en plan Empresarial, pero la columna sucursal_id
-- debe existir en inventario/ventas/caja DESDE EL DIA 1. Agregarla despues
-- obliga a reescribir todos los indices y todas las queries.
CREATE TABLE sucursales (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    codigo        VARCHAR(20) NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    direccion     VARCHAR(200),
    ciudad        VARCHAR(80),
    telefono      VARCHAR(30),
    es_principal  BOOLEAN     NOT NULL DEFAULT false,
    activa        BOOLEAN     NOT NULL DEFAULT true,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_sucursal_codigo UNIQUE (negocio_id, codigo)
);
CREATE UNIQUE INDEX uq_sucursal_principal
    ON sucursales (negocio_id) WHERE es_principal;

-- ---------------------------------------------------------------------
-- IDENTIDAD Y AUTORIZACION (siempre por negocio)
-- ---------------------------------------------------------------------

CREATE TABLE usuarios (
    id                UUID PRIMARY KEY,
    negocio_id        UUID         NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    email             VARCHAR(150) NOT NULL,
    password_hash     VARCHAR(120) NOT NULL,     -- BCrypt
    nombre            VARCHAR(80)  NOT NULL,
    apellido          VARCHAR(80),
    documento         VARCHAR(30),
    telefono          VARCHAR(30),
    avatar_url        TEXT,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
                      CHECK (estado IN ('INVITADO','ACTIVO','INACTIVO','BLOQUEADO')),
    mfa_habilitado    BOOLEAN      NOT NULL DEFAULT false,
    mfa_secreto       VARCHAR(120),
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMPTZ,
    password_cambiado_en TIMESTAMPTZ,
    ultimo_acceso_en  TIMESTAMPTZ,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    eliminado_en      TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,
    -- El email es unico POR NEGOCIO, no globalmente: la misma persona puede
    -- trabajar en dos negocios clientes de Regenta con el mismo correo.
    CONSTRAINT uq_usuario_email UNIQUE (negocio_id, email)
);
CREATE INDEX ix_usuarios_negocio ON usuarios (negocio_id) WHERE eliminado_en IS NULL;

CREATE TABLE roles (
    id            UUID PRIMARY KEY,
    negocio_id    UUID        NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    plantilla_id  UUID        REFERENCES plantillas_rol(id),  -- de que plantilla salio
    nombre        VARCHAR(60) NOT NULL,
    descripcion   TEXT,
    es_sistema    BOOLEAN     NOT NULL DEFAULT false,  -- no se puede borrar (Administrador)
    activo        BOOLEAN     NOT NULL DEFAULT true,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_rol_nombre UNIQUE (negocio_id, nombre)
);

CREATE TABLE rol_permisos (
    rol_id         UUID        NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_codigo VARCHAR(80) NOT NULL REFERENCES permisos(codigo),
    PRIMARY KEY (rol_id, permiso_codigo)
);

CREATE TABLE usuario_roles (
    usuario_id   UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    sucursal_id  UUID REFERENCES sucursales(id) ON DELETE CASCADE, -- NULL = todas
    -- Columna generada: Postgres no admite expresiones en una PRIMARY KEY,
    -- asi que se materializa el COALESCE para poder incluirlo en la clave.
    sucursal_key UUID GENERATED ALWAYS AS (COALESCE(sucursal_id, '00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    asignado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    asignado_por UUID,
    PRIMARY KEY (usuario_id, rol_id, sucursal_key)
);

CREATE TABLE usuario_sucursales (
    usuario_id  UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    sucursal_id UUID NOT NULL REFERENCES sucursales(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, sucursal_id)
);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY,
    negocio_id   UUID        NOT NULL,
    usuario_id   UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash   VARCHAR(128) NOT NULL UNIQUE,   -- SHA-256, nunca el token en claro
    dispositivo_id VARCHAR(80),
    plataforma   VARCHAR(20) CHECK (plataforma IN ('ANDROID','WEB')),
    ip           INET,
    user_agent   TEXT,
    emitido_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en    TIMESTAMPTZ NOT NULL,
    revocado_en  TIMESTAMPTZ,
    reemplazado_por UUID REFERENCES refresh_tokens(id)  -- rotacion
);
CREATE INDEX ix_refresh_usuario ON refresh_tokens (usuario_id) WHERE revocado_en IS NULL;

CREATE TABLE invitaciones (
    id          UUID PRIMARY KEY,
    negocio_id  UUID         NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    email       VARCHAR(150) NOT NULL,
    rol_id      UUID         NOT NULL REFERENCES roles(id),
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                CHECK (estado IN ('PENDIENTE','ACEPTADA','EXPIRADA','REVOCADA')),
    invitado_por UUID        REFERENCES usuarios(id),
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_en   TIMESTAMPTZ  NOT NULL,
    aceptada_en TIMESTAMPTZ
);

-- ---------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY['impuestos','sucursales','usuarios','roles','invitaciones','suscripciones','negocio_modulos']
  LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE %I FORCE  ROW LEVEL SECURITY', t);
    EXECUTE format($f$CREATE POLICY tenant_isolation ON %I
        USING (negocio_id = app_negocio_actual())
        WITH CHECK (negocio_id = app_negocio_actual())$f$, t);
  END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- CLAIMS DEL JWT que emite este servicio
-- ---------------------------------------------------------------------
--  sub          usuario_id
--  negocio_id   tenant
--  patron       VENTA_DIRECTA | RESERVA | COMANDA
--  plan         BASICO | PROFESIONAL | EMPRESARIAL
--  modulos      ["INVENTARIO","VENTAS",...]   <- activos efectivos
--  roles        ["ADMINISTRADOR"]
--  permisos     opcional; si crece mucho, cada servicio lo resuelve por
--               introspeccion contra este servicio y lo cachea.
--  sucursales   [uuid,...]  (o "*" para todas)
--  exp / iat / jti
