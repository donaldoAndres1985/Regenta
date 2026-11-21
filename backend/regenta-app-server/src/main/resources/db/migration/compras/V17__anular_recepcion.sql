-- HU-177 · Anular una recepción (I19 de docs/dominio/compras.md).
--
-- Una recepción mal registrada no se borra: se anula con motivo. Sigue visible, con quién y cuándo
-- la anuló; Inventario saca lo que entró, su cuenta por pagar queda ANULADA y la orden recupera el
-- faltante.

SET search_path TO compras, public;

ALTER TABLE recepciones
    ADD COLUMN anulada_por      UUID,
    ADD COLUMN anulada_en       TIMESTAMPTZ,
    ADD COLUMN motivo_anulacion VARCHAR(200);

-- Las recepciones ANULADA de antes (borradores descartados en HU-176) no tienen motivo: NOT VALID
-- no las revisa, y un descarte deja la recepción ANULADA sin anulada_en.
ALTER TABLE recepciones
    ADD CONSTRAINT ck_recepcion_motivo_anulacion
        CHECK (anulada_en IS NULL OR length(trim(coalesce(motivo_anulacion, ''))) > 0) NOT VALID;
