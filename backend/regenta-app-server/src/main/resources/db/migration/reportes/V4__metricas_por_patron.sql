-- =====================================================================
-- servicio-reportes . V4 . soporte para HU-099 (metricas de Reserva y de
-- Comanda)
--
--   dim_recurso
--     Cuantas habitaciones, canchas o consultorios existe y cuales estan
--     fuera de servicio. Es el denominador de la ocupacion y del RevPAR, y
--     hasta ahora no habia forma de saberlo: ningun evento del patron
--     Reserva lo traia. Llega por recurso_creado / recurso_actualizado /
--     recurso_eliminado.
--
--     Sin SCD tipo 2, igual que dim_cliente y dim_usuario: la ocupacion se
--     calcula contra el inventario de hoy, no contra el que habia el dia de
--     la estancia. Versionarla cuando la historia lo pida.
--
--   ocupacion_diaria.ingreso_alojamiento
--     ADR y RevPAR son divisiones, y las divisiones no se pueden acumular:
--     hay que guardar el numerador. El ingreso de alojamiento (sin
--     consumos ni servicios) se suma noche a noche y de ahi salen los dos.
--
--   hechos_comanda.mesa_id / num_comensales
--     La rotacion de mesas necesita saber que mesa fue, y el ticket por
--     comensal necesita el divisor. Los dos venian en pedido_completado
--     -mesa_id desde siempre, num_comensales desde HU-099- y se estaban
--     tirando a la basura. La columna mesa_codigo de V1 se queda en NULL:
--     servicio-mesas no publica su catalogo, asi que no hay de donde sacar
--     el codigo legible.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO reportes, public;

CREATE TABLE dim_recurso (
    sk                  BIGSERIAL PRIMARY KEY,
    negocio_id          UUID NOT NULL,
    recurso_id          UUID NOT NULL,
    sucursal_id         UUID,
    tipo_recurso_id     UUID NOT NULL,
    tipo_recurso_nombre VARCHAR(100),
    codigo              VARCHAR(40),
    nombre              VARCHAR(150),
    capacidad           SMALLINT,
    estado              VARCHAR(20),
    -- activo: existe y cuenta para la ocupacion.
    -- disponible: ademas se puede vender (no esta en MANTENIMIENTO).
    activo              BOOLEAN NOT NULL DEFAULT true,
    disponible          BOOLEAN NOT NULL DEFAULT true,
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dim_recurso UNIQUE (negocio_id, recurso_id)
);
CREATE INDEX ix_dim_recurso_tipo ON dim_recurso (negocio_id, tipo_recurso_id) WHERE activo;

ALTER TABLE dim_recurso ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_recurso FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON dim_recurso
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

ALTER TABLE ocupacion_diaria
    ADD COLUMN ingreso_alojamiento NUMERIC(18,4) NOT NULL DEFAULT 0;

ALTER TABLE hechos_comanda
    ADD COLUMN mesa_id        UUID,
    ADD COLUMN num_comensales SMALLINT;

COMMENT ON TABLE dim_recurso IS
 'Inventario de recursos por negocio. Denominador de la ocupacion y del RevPAR; se mantiene con los eventos del catalogo de servicio-recursos.';
COMMENT ON COLUMN ocupacion_diaria.ingreso_alojamiento IS
 'Solo alojamiento, sin consumos ni servicios: es el numerador correcto de ADR y RevPAR.';

-- EVENTOS CONSUMIDOS (ademas de los de V1 y V3): recurso_creado,
-- recurso_actualizado, recurso_eliminado
