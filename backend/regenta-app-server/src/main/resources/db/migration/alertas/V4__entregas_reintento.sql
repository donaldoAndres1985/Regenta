-- =====================================================================
-- servicio-alertas . V4 . reintento con backoff y retención por "no molestar"
--
-- HU-094: una entrega fallida se reintenta con espera creciente (criterio 3) y
-- una que cae en la franja de "no molestar" se retiene hasta que termine, salvo
-- severidad CRÍTICA (criterio 4). Las dos cosas necesitan una marca de "cuándo
-- volver a intentarla".
--
-- NO EDITAR después de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO alertas, public;

ALTER TABLE entregas ADD COLUMN proximo_intento TIMESTAMPTZ;
ALTER TABLE entregas ADD COLUMN retenida_hasta  TIMESTAMPTZ;

-- La cola de reintentos: lo pendiente o fallido cuya hora de reintento ya llegó.
CREATE INDEX ix_entregas_reintento ON entregas (proximo_intento)
    WHERE estado IN ('PENDIENTE', 'FALLIDA');
