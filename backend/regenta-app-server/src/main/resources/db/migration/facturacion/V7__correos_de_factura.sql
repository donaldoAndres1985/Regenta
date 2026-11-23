-- =====================================================================
-- servicio-facturacion . V7 . la factura por correo, con reintento (HU-122)
--
-- Mandar la factura al cliente nunca bloquea ni tumba nada (C4): se anota
-- aqui, se intenta en el momento y, si el proveedor de correo no responde
-- o rechaza, queda PENDIENTE con el motivo y un barrido la reintenta con
-- espera creciente. Al quinto fallo queda FALLIDO, con su motivo a la vista.
-- C1: lo enviado guarda el identificador que dio el proveedor.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V8.
-- =====================================================================

SET search_path TO facturacion, public;

CREATE TABLE correos_de_factura (
    id                 UUID         PRIMARY KEY,
    negocio_id         UUID         NOT NULL,
    factura_id         UUID         NOT NULL REFERENCES facturas(id),
    destinatario       VARCHAR(200) NOT NULL,
    estado             VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE'
                       CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    intentos           INT          NOT NULL DEFAULT 0,
    proximo_intento_en TIMESTAMPTZ,
    id_proveedor       VARCHAR(250),
    error              TEXT,
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    enviado_en         TIMESTAMPTZ
);

CREATE INDEX ix_correos_de_factura_pendientes ON correos_de_factura (proximo_intento_en)
    WHERE estado = 'PENDIENTE';

ALTER TABLE correos_de_factura ENABLE ROW LEVEL SECURITY;
ALTER TABLE correos_de_factura FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON correos_de_factura
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
