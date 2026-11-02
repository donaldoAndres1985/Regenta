-- =====================================================================
-- servicio-caja . V5 . configuración de caja por negocio (HU-061)
--
-- El retiro por encima de `retiro_max_sin_autorizacion` exige que quede
-- registrado quién lo autorizó (criterio 2). 0 = sin límite.
--
-- NO EDITAR después de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO caja, public;

CREATE TABLE config_caja (
    negocio_id                   UUID PRIMARY KEY,
    retiro_max_sin_autorizacion  NUMERIC(16,4) NOT NULL DEFAULT 0
                                 CHECK (retiro_max_sin_autorizacion >= 0),
    actualizado_en               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                      BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE config_caja ENABLE ROW LEVEL SECURITY;
ALTER TABLE config_caja FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON config_caja;
CREATE POLICY tenant_isolation ON config_caja
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
