-- =====================================================================
-- servicio-facturacion . V4 . una sola contingencia abierta por negocio
--
-- HU-057: solo puede haber una contingencia sin cerrar a la vez. El índice
-- parcial lo garantiza en la base; el servicio comprueba antes, pero dos
-- transmisiones fallando a la vez podrían intentar abrir dos.
--
-- NO EDITAR después de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO facturacion, public;

CREATE UNIQUE INDEX uq_contingencia_abierta
    ON contingencias (negocio_id)
    WHERE fin_en IS NULL;
