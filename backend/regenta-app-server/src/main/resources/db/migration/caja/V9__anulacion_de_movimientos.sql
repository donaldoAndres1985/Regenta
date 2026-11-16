-- =====================================================================
-- caja . V9 . anular un movimiento manual (HU-141)
--
-- Invariantes I5 e I6 de docs/dominio/caja.md: la anulación es un
-- movimiento nuevo, de signo contrario, que apunta al original con
-- `anula_a`. El original no se modifica ni se borra.
--
-- Garantías en la base, no solo en la app:
--   * un movimiento se anula una sola vez (índice único parcial);
--   * una ANULACION siempre trae el original y un motivo.
--
-- NO EDITAR después de aplicada. Los cambios van en un V10.
-- =====================================================================

SET search_path TO caja, public;

ALTER TABLE movimientos_caja DROP CONSTRAINT IF EXISTS movimientos_caja_tipo_check;
ALTER TABLE movimientos_caja ADD CONSTRAINT movimientos_caja_tipo_check
    CHECK (tipo IN ('VENTA','COMANDA','RESERVA','DEVOLUCION','RECAUDO',
                    'INGRESO','RETIRO','GASTO','AJUSTE','APERTURA','ANULACION'));

ALTER TABLE movimientos_caja
    ADD COLUMN anula_a          UUID REFERENCES movimientos_caja(id),
    ADD COLUMN motivo_anulacion VARCHAR(200),
    ADD CONSTRAINT ck_mov_caja_anulacion_completa
        CHECK (tipo <> 'ANULACION'
               OR (anula_a IS NOT NULL AND motivo_anulacion IS NOT NULL
                   AND length(trim(motivo_anulacion)) > 0));

CREATE UNIQUE INDEX uq_mov_caja_una_anulacion
    ON movimientos_caja (anula_a) WHERE anula_a IS NOT NULL;
