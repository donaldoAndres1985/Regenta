-- =====================================================================
-- servicio-facturacion . V3 . una nota crédito por documento de origen
--
-- El índice `uq_factura_origen` de V1 solo cubre FACTURA_VENTA / FACTURA_POS,
-- así que no impide dos notas crédito para la misma devolución si el evento
-- `devolucion_registrada` llega repetido. Este índice parcial lo cierra para
-- NOTA_CREDITO. El Inbox del consumidor es la primera barrera; esta es la de
-- la base.
--
-- NO EDITAR después de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO facturacion, public;

CREATE UNIQUE INDEX uq_nota_credito_origen
    ON facturas (negocio_id, origen_tipo, origen_id)
    WHERE tipo_documento = 'NOTA_CREDITO' AND origen_id IS NOT NULL AND estado <> 'ANULADA';
