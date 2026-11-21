-- HU-171 · Un pago a proveedor puede abonar a varias facturas del mismo proveedor (I26 de
-- docs/dominio/compras.md).
--
-- pagos_proveedor pasa a ser el pago (proveedor, monto, método, sesión de caja) y la nueva tabla
-- pago_aplicaciones dice cuánto de ese pago abonó a cada cuenta. Los pagos de antes abonaban a una
-- sola cuenta: cada uno queda con una aplicación por su monto completo. cuenta_id se conserva para
-- los pagos de una sola cuenta.
--
-- El relleno de datos corre con FORCE ROW LEVEL SECURITY apagado y se vuelve a encender al final:
-- con FORCE, una migración que no fija app.negocio_id vería cero filas y no rellenaría nada, en
-- silencio, si el usuario de la migración no es superusuario.

SET search_path TO compras, public;

ALTER TABLE pagos_proveedor NO FORCE ROW LEVEL SECURITY;
ALTER TABLE cuentas_por_pagar NO FORCE ROW LEVEL SECURITY;

ALTER TABLE pagos_proveedor ADD COLUMN proveedor_id UUID REFERENCES proveedores(id);
UPDATE pagos_proveedor p
   SET proveedor_id = c.proveedor_id
  FROM cuentas_por_pagar c
 WHERE c.id = p.cuenta_id;
ALTER TABLE pagos_proveedor ALTER COLUMN proveedor_id SET NOT NULL;
ALTER TABLE pagos_proveedor ALTER COLUMN cuenta_id DROP NOT NULL;
CREATE INDEX ix_pagos_proveedor ON pagos_proveedor (negocio_id, proveedor_id, fecha DESC);

CREATE TABLE pago_aplicaciones (
    id         UUID PRIMARY KEY,
    negocio_id UUID          NOT NULL,
    pago_id    UUID          NOT NULL REFERENCES pagos_proveedor(id) ON DELETE CASCADE,
    cuenta_id  UUID          NOT NULL REFERENCES cuentas_por_pagar(id),
    monto      NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    CONSTRAINT uq_pago_aplicacion UNIQUE (pago_id, cuenta_id)
);
CREATE INDEX ix_pago_aplicaciones_cuenta ON pago_aplicaciones (cuenta_id);

INSERT INTO pago_aplicaciones (id, negocio_id, pago_id, cuenta_id, monto)
SELECT gen_random_uuid(), p.negocio_id, p.id, p.cuenta_id, p.monto
  FROM pagos_proveedor p
 WHERE p.cuenta_id IS NOT NULL;

ALTER TABLE pagos_proveedor FORCE ROW LEVEL SECURITY;
ALTER TABLE cuentas_por_pagar FORCE ROW LEVEL SECURITY;

ALTER TABLE pago_aplicaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE pago_aplicaciones FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON pago_aplicaciones;
CREATE POLICY tenant_isolation ON pago_aplicaciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
