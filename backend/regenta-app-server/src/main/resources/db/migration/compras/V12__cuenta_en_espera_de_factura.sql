-- HU-170 · Recibir con remisión y registrar la factura después (I22 de docs/dominio/compras.md).
--
-- Una recepción a crédito se puede confirmar sin número de factura: la cuenta por pagar nace sin
-- número, «en espera de factura». Se puede abonar; al registrar el número y la fecha de la factura se
-- quita la marca y el vencimiento se recalcula desde esa fecha. La unicidad por proveedor sigue
-- igual: en PostgreSQL los NULL no chocan entre sí en uq_cxp.

SET search_path TO compras, public;

ALTER TABLE cuentas_por_pagar ALTER COLUMN numero_factura DROP NOT NULL;

CREATE INDEX ix_cxp_en_espera_factura ON cuentas_por_pagar (negocio_id, proveedor_id)
    WHERE numero_factura IS NULL;
