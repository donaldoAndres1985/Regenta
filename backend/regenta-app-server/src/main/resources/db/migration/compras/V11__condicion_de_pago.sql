-- HU-169 · Contado o crédito al registrar la factura (I22, I23, I24 de docs/dominio/compras.md).
--
-- La recepción guarda cómo se paga la factura. A crédito nace la cuenta por pagar con el plazo del
-- proveedor; de contado, la cuenta vence el mismo día y se paga en la misma transacción, con el
-- método indicado y —si es efectivo de la caja— desde la sesión abierta del usuario (HU-145).

SET search_path TO compras, public;

ALTER TABLE recepciones
    ADD COLUMN condicion_pago     VARCHAR(10) NOT NULL DEFAULT 'CREDITO'
        CHECK (condicion_pago IN ('CONTADO','CREDITO')),
    ADD COLUMN metodo_pago        VARCHAR(20)
        CHECK (metodo_pago IN ('EFECTIVO','TRANSFERENCIA','CHEQUE','TARJETA','OTRO')),
    ADD COLUMN pagar_desde_caja   BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN referencia_pago    VARCHAR(60);
