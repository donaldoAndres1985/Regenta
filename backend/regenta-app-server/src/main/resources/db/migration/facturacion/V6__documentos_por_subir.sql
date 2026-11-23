-- =====================================================================
-- servicio-facturacion . V6 . documentos que esperan el almacen (HU-121)
--
-- El XML firmado vive en el almacen de objetos y en la factura solo su
-- URL (C1). Si al firmar el almacen no responde, la emision no se pierde
-- (C3): el XML espera aqui, la factura queda con la URL provisional
-- 'pendiente://<id>' (la transmision lo lee de esta tabla) y un barrido lo
-- sube despues. Al subirlo se reemplaza la URL y se borra la fila: esta
-- tabla solo guarda lo que todavia no llego al almacen.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V7.
-- =====================================================================

SET search_path TO facturacion, public;

CREATE TABLE documentos_por_subir (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID         NOT NULL,
    nombre             VARCHAR(200) NOT NULL,
    tipo_mime          VARCHAR(100) NOT NULL,
    contenido          BYTEA        NOT NULL,
    intentos           INT          NOT NULL DEFAULT 0,
    ultimo_error       TEXT,
    proximo_intento_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_documentos_por_subir ON documentos_por_subir (proximo_intento_en);

ALTER TABLE documentos_por_subir ENABLE ROW LEVEL SECURITY;
ALTER TABLE documentos_por_subir FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON documentos_por_subir
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

COMMENT ON TABLE documentos_por_subir IS
 'HU-121 C3: lo firmado mientras el almacen de objetos no respondia. Se sube y se borra.';
