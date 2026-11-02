-- =====================================================================
-- servicio-reportes . V7 . soporte para HU-134 (el salon en los reportes)
--
--   dim_mesa
--     Que codigo y que zona tiene cada mesa. hechos_comanda.mesa_codigo
--     existe desde V1 y siempre quedo en NULL: servicio-mesas no publicaba
--     su catalogo, igual que le pasaba a servicio-recursos antes de HU-099.
--     La rotacion cuenta bien porque agrupa por mesa_id, pero un listado
--     por mesa mostraba UUIDs.
--
--     Sin SCD tipo 2, igual que dim_recurso: un reporte por mesa se arma
--     contra la zona de hoy, no la del dia del pedido. El codigo si viaja
--     como snapshot en cada fila de hechos_comanda -eso es lo que sobrevive
--     a que la mesa se elimine (criterio 5)-, dim_mesa solo resuelve la
--     zona vigente.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V8.
-- =====================================================================

SET search_path TO reportes, public;

CREATE TABLE dim_mesa (
    sk             BIGSERIAL PRIMARY KEY,
    negocio_id     UUID NOT NULL,
    mesa_id        UUID NOT NULL,
    zona_id        UUID,
    zona_nombre    VARCHAR(60),
    codigo         VARCHAR(20),
    nombre         VARCHAR(60),
    capacidad      SMALLINT,
    activa         BOOLEAN NOT NULL DEFAULT true,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dim_mesa UNIQUE (negocio_id, mesa_id)
);
CREATE INDEX ix_dim_mesa_negocio ON dim_mesa (negocio_id) WHERE activa;

ALTER TABLE dim_mesa ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_mesa FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON dim_mesa
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

COMMENT ON TABLE dim_mesa IS
 'Catalogo de mesas por negocio: codigo y zona vigentes. Se mantiene con mesa_creada / mesa_actualizada / mesa_eliminada de servicio-mesas.';

-- EVENTOS CONSUMIDOS (ademas de los de V1, V3 y V4): mesa_creada,
-- mesa_actualizada, mesa_eliminada
