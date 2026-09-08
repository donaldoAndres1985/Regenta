-- =====================================================================
-- servicio-compras . V4 . índices para la recepción de mercancía
--
-- HU-048: las tablas (recepciones, recepcion_lineas, cuentas_por_pagar) ya
-- están en V1 con su RLS en V2. Lo que falta son los índices de acceso:
--   - las recepciones de una orden (para saber qué se recibió y qué falta)
--   - las líneas de una recepción (al confirmarla y al publicá el evento)
--   - la cuenta por pagar que nació de una recepción (criterio 6)
--
-- NO EDITAR después de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO compras, public;

CREATE INDEX ix_recepciones_orden ON recepciones (negocio_id, orden_id)
    WHERE orden_id IS NOT NULL;

CREATE INDEX ix_recepciones_negocio_fecha ON recepciones (negocio_id, fecha DESC);

CREATE INDEX ix_recepcion_lineas_recepcion ON recepcion_lineas (recepcion_id);

CREATE INDEX ix_recepcion_lineas_orden_linea ON recepcion_lineas (orden_linea_id)
    WHERE orden_linea_id IS NOT NULL;

CREATE INDEX ix_cxp_recepcion ON cuentas_por_pagar (recepcion_id)
    WHERE recepcion_id IS NOT NULL;
