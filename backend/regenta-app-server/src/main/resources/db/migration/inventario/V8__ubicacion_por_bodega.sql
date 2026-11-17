-- =====================================================================
-- inventario . V8 . Ubicacion del producto en cada bodega (HU-152)
--
-- I9: texto libre opcional ("Pasillo 3 - E2"), por producto y bodega, de
-- hasta 60 caracteres. Vive en existencias porque esa fila ya es la de
-- "este producto en esta bodega" y ya tiene negocio_id con RLS.
-- =====================================================================

ALTER TABLE existencias ADD COLUMN ubicacion VARCHAR(60);

COMMENT ON COLUMN existencias.ubicacion IS
    'I9: donde esta el producto dentro de la bodega. Opcional.';
