-- =====================================================================
-- servicio-reportes . V8 . los archivos exportados van al almacen (HU-121)
--
-- Hasta ahora el archivo vivia en archivos_reporte.contenido (BYTEA): sirvio
-- para cerrar HU-100 pero no aguanta un XLSX de cien mil filas por negocio.
-- Desde aqui el archivo va al almacen de objetos y en la base solo queda su
-- URL (C1).
--
-- C4: los que ya estaban en la base los sube el barrido
-- 'archivos-a-almacen' (subir al almacen no se puede desde SQL): deja la URL
-- y vacia la columna. Por eso contenido pasa a ser opcional.
--
-- C5: cada archivo vence (expira_en). Al vencer, el barrido
-- 'archivos-expirados' lo borra del almacen y marca expirado_en: la
-- ejecucion sigue COMPLETADO, con el archivo expirado, no FALLIDO.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V9.
-- =====================================================================

SET search_path TO reportes, public;

ALTER TABLE archivos_reporte
    ALTER COLUMN contenido DROP NOT NULL,
    ADD COLUMN url         TEXT,
    ADD COLUMN expira_en   TIMESTAMPTZ,
    ADD COLUMN expirado_en TIMESTAMPTZ;

CREATE INDEX ix_archivos_por_subir ON archivos_reporte (negocio_id) WHERE contenido IS NOT NULL;
CREATE INDEX ix_archivos_por_vencer ON archivos_reporte (expira_en)
    WHERE url IS NOT NULL AND expirado_en IS NULL;

COMMENT ON TABLE archivos_reporte IS
 'El archivo exportado: en el almacen de objetos (url), no en la base. contenido solo queda en los de antes de HU-121 hasta que el barrido los sube.';
COMMENT ON COLUMN archivos_reporte.expirado_en IS
 'HU-121 C5: cuando se borro del almacen por retencion. La ejecucion no fallo: su archivo expiro.';
