-- =====================================================================
-- servicio-caja . V3 . consecutivo por negocio para las sesiones de caja
--
-- HU-059: la sesión lleva un `numero` único por negocio
-- (`uq_sesion_caja_numero`). Igual que en ventas/compras: no un BIGSERIAL
-- —dejaría huecos ante un rollback—, sino un
-- INSERT ... ON CONFLICT ... RETURNING que bloquea la fila hasta el commit.
--
-- NO EDITAR después de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO caja, public;

CREATE TABLE consecutivos (
    negocio_id   UUID        NOT NULL,
    sucursal_id  UUID,
    tipo         VARCHAR(20) NOT NULL,   -- 'SESION_CAJA'
    prefijo      VARCHAR(10) NOT NULL DEFAULT '',
    siguiente    BIGINT      NOT NULL DEFAULT 1,
    sucursal_key UUID GENERATED ALWAYS AS
                 (COALESCE(sucursal_id, '00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    PRIMARY KEY (negocio_id, sucursal_key, tipo)
);

ALTER TABLE consecutivos ENABLE ROW LEVEL SECURITY;
ALTER TABLE consecutivos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON consecutivos;
CREATE POLICY tenant_isolation ON consecutivos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
