-- =====================================================================
-- compras . V21 . recepción armada en el celular sin conexión (HU-182 · I32)
--
-- El celular le pone a la recepción un identificador propio al guardarla sin
-- señal. Con él, reenviarla las veces que sea deja una sola recepción: si ya
-- llegó, el backend la devuelve en vez de crear otra.
--
-- No mueve datos: las recepciones de antes no traen identificador.
--
-- NO EDITAR después de aplicada. Los cambios van en un V22.
-- =====================================================================

SET search_path TO compras, public;

ALTER TABLE recepciones ADD COLUMN id_dispositivo UUID;

CREATE UNIQUE INDEX uq_recepcion_id_dispositivo
    ON recepciones (negocio_id, id_dispositivo)
 WHERE id_dispositivo IS NOT NULL;
