-- =====================================================================
-- servicio-clientes . V5 . marca de "recordatorio de seguimiento enviado"
--
-- HU-024 criterio 3: una interaccion con `seguimiento_en` genera una alerta al
-- responsable cuando llega la fecha. El barrido necesita saber cuales ya
-- avisaron para no repetir el aviso cada corrida: esa marca es
-- `seguimiento_notificado_en`.
--
-- El indice parcial cubre exactamente lo que consulta el barrido: seguimientos
-- con fecha y sin avisar todavia.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO crm, public;

ALTER TABLE interacciones ADD COLUMN seguimiento_notificado_en TIMESTAMPTZ;

CREATE INDEX ix_interacciones_seguimiento
    ON interacciones (negocio_id, seguimiento_en)
    WHERE seguimiento_en IS NOT NULL AND seguimiento_notificado_en IS NULL;
