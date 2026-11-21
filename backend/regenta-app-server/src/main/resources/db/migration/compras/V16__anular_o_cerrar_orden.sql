-- HU-176 · Anular una orden o cerrarla con faltante (I7, I11, I12 de docs/dominio/compras.md).
--
-- Una orden que no va se anula con motivo, si todavía no recibió mercancía. Una que llegó incompleta
-- se cierra con motivo: no admite más recepciones y lo que faltó sigue en sus líneas. De las dos
-- queda quién, cuándo y por qué.

SET search_path TO compras, public;

ALTER TABLE ordenes_compra
    ADD COLUMN anulada_por      UUID,
    ADD COLUMN anulada_en       TIMESTAMPTZ,
    ADD COLUMN motivo_anulacion VARCHAR(200),
    ADD COLUMN cerrada_por      UUID,
    ADD COLUMN cerrada_en       TIMESTAMPTZ,
    ADD COLUMN motivo_cierre    VARCHAR(200);

-- Las anuladas o cerradas de antes de esta historia no tienen motivo: NOT VALID no las revisa.
ALTER TABLE ordenes_compra
    ADD CONSTRAINT ck_oc_motivo_anulacion
        CHECK (estado <> 'ANULADA' OR length(trim(coalesce(motivo_anulacion, ''))) > 0) NOT VALID,
    ADD CONSTRAINT ck_oc_motivo_cierre
        CHECK (estado <> 'CERRADA' OR length(trim(coalesce(motivo_cierre, ''))) > 0) NOT VALID;
