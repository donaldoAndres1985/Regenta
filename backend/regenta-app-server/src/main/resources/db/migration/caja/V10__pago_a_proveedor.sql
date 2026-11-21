-- HU-145 · Pago a proveedor en efectivo desde la caja (docs/dominio/caja.md, I2, I3, I13).
-- Compras registra el pago y la caja recibe la salida por evento: un tipo de movimiento nuevo.

SET search_path TO caja, public;

ALTER TABLE movimientos_caja DROP CONSTRAINT IF EXISTS movimientos_caja_tipo_check;
ALTER TABLE movimientos_caja ADD CONSTRAINT movimientos_caja_tipo_check
    CHECK (tipo IN ('VENTA','COMANDA','RESERVA','DEVOLUCION','RECAUDO',
                    'INGRESO','RETIRO','GASTO','AJUSTE','APERTURA','ANULACION',
                    'PAGO_PROVEEDOR'));
