-- HU-172 · Anular un pago a proveedor (I28 de docs/dominio/compras.md).
--
-- Un pago no se edita ni se borra: se anula con motivo. Devuelve los saldos a sus cuentas y, si salió
-- de la caja, la caja recibe el reverso en sesion_reverso_id: la misma sesión si sigue abierta, o la
-- que indique el usuario si ya cerró.

SET search_path TO compras, public;

ALTER TABLE pagos_proveedor
    ADD COLUMN anulado_en        TIMESTAMPTZ,
    ADD COLUMN anulado_por       UUID,
    ADD COLUMN motivo_anulacion  VARCHAR(200),
    ADD COLUMN sesion_reverso_id UUID,   -- [ref lógica -> caja.sesiones_caja]
    ADD CONSTRAINT ck_pago_anulacion_completa
        CHECK (anulado_en IS NULL
               OR (motivo_anulacion IS NOT NULL AND length(trim(motivo_anulacion)) > 0));
