-- =====================================================================
-- servicio-compras . V5 . índices para cuentas por pagar y pagos
--
-- HU-049: las tablas (cuentas_por_pagar, pagos_proveedor) ya están en V1 con
-- su RLS en V2. Faltan los índices de acceso:
--   - los pagos de una cuenta (al mostrarla y al recalcular su saldo)
--   - las cuentas de un proveedor (estado de cuenta del proveedor)
-- El listado por antigüedad (ix_cxp_vencimiento) ya viene de V1.
--
-- NO EDITAR después de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO compras, public;

CREATE INDEX ix_pagos_cuenta ON pagos_proveedor (cuenta_id);

CREATE INDEX ix_cxp_proveedor ON cuentas_por_pagar (negocio_id, proveedor_id);
