-- =====================================================================
-- inventario . V7 . Tarifa de IVA del producto (HU-147)
--
-- I1: todo producto tiene una tarifa de una lista cerrada. I2: precio_venta es
-- el precio de la etiqueta, con IVA incluido (precio_incluye_impuesto ya era
-- true por defecto); la base se calcula, no se guarda.
-- I6: los productos que ya existian (2.736 de Ferreteria Central, traidos de
-- Sara) quedan en IVA_19 y "por revisar", con el mismo precio.
--
-- Los valores para las filas existentes los pone el DEFAULT del ADD COLUMN,
-- no un UPDATE: con FORCE ROW LEVEL SECURITY un UPDATE sin app.negocio_id no
-- tocaria ninguna fila. Despues se quitan los defaults para que ninguna fila
-- nueva entre sin tarifa elegida.
-- =====================================================================

ALTER TABLE productos
    ADD COLUMN tarifa_iva VARCHAR(10) NOT NULL DEFAULT 'IVA_19'
        CONSTRAINT ck_productos_tarifa_iva
        CHECK (tarifa_iva IN ('IVA_19', 'IVA_5', 'EXENTO', 'EXCLUIDO')),
    ADD COLUMN tarifa_por_revisar BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE productos ALTER COLUMN tarifa_iva DROP DEFAULT;
ALTER TABLE productos ALTER COLUMN tarifa_por_revisar SET DEFAULT false;

COMMENT ON COLUMN productos.tarifa_iva IS
    'I1: IVA_19, IVA_5, EXENTO (gravado a 0 %) o EXCLUIDO (no causa IVA). precio_venta la incluye.';
COMMENT ON COLUMN productos.tarifa_por_revisar IS
    'I6: tarifa asignada por la migracion de HU-147 que nadie ha confirmado.';
