-- =====================================================================
-- servicio-compras . V3 . consecutivo por negocio para las órdenes de compra
--
-- HU-047: la orden lleva un `numero` único por negocio (`uq_oc_numero`). Igual
-- que en ventas, no puede ser un BIGSERIAL —deja huecos ante cualquier
-- rollback—: se toma con un INSERT ... ON CONFLICT ... RETURNING sobre esta
-- tabla, que bloquea la fila hasta el commit.
--
-- NO EDITAR después de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO compras, public;

CREATE TABLE consecutivos (
    negocio_id   UUID        NOT NULL,
    sucursal_id  UUID,
    tipo         VARCHAR(20) NOT NULL,   -- 'ORDEN_COMPRA','RECEPCION'
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
