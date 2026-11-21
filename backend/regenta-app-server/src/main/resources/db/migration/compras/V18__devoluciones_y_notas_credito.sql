-- HU-178 · Devolución a proveedor con nota crédito (I5, I27 de docs/dominio/compras.md).
--
-- Una devolución saca de la bodega mercancía de una recepción confirmada, a su costo de entrada, y
-- genera una nota crédito por el valor facturado de lo devuelto (base con sus descuentos + IVA; el
-- flete no se devuelve). La nota baja el saldo de la cuenta de esa recepción; lo que sobra queda
-- como saldo a favor del negocio con el proveedor, que se usa en el siguiente pago (HU-171 C5).

SET search_path TO compras, public;

CREATE TABLE devoluciones_proveedor (
    id            UUID PRIMARY KEY,
    negocio_id    UUID          NOT NULL,
    numero        VARCHAR(30)   NOT NULL,
    recepcion_id  UUID          NOT NULL REFERENCES recepciones(id),
    proveedor_id  UUID          NOT NULL REFERENCES proveedores(id),
    bodega_id     UUID          NOT NULL,
    motivo        VARCHAR(200)  NOT NULL CHECK (length(trim(motivo)) > 0),
    valor_nota    NUMERIC(14,2) NOT NULL CHECK (valor_nota > 0),
    usuario_id    UUID,
    fecha         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    creado_en     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_devolucion_numero UNIQUE (negocio_id, numero)
);
CREATE INDEX ix_devoluciones_recepcion ON devoluciones_proveedor (negocio_id, recepcion_id);

CREATE TABLE devolucion_lineas (
    id                 UUID PRIMARY KEY,
    negocio_id         UUID          NOT NULL,
    devolucion_id      UUID          NOT NULL REFERENCES devoluciones_proveedor(id) ON DELETE CASCADE,
    recepcion_linea_id UUID          NOT NULL REFERENCES recepcion_lineas(id),
    producto_id        UUID          NOT NULL,
    nombre_snapshot    VARCHAR(200)  NOT NULL,
    cantidad           NUMERIC(14,4) NOT NULL CHECK (cantidad > 0),
    codigo_lote        VARCHAR(60),
    costo_entrada      NUMERIC(14,4) NOT NULL,
    base               NUMERIC(14,2) NOT NULL,
    impuesto           NUMERIC(14,2) NOT NULL
);
CREATE INDEX ix_devolucion_lineas_recepcion ON devolucion_lineas (recepcion_linea_id);

-- aplicado_a_cuenta + excedente = monto. saldo_a_favor es lo que queda del excedente sin usar.
CREATE TABLE notas_credito_proveedor (
    id                UUID PRIMARY KEY,
    negocio_id        UUID          NOT NULL,
    proveedor_id      UUID          NOT NULL REFERENCES proveedores(id),
    devolucion_id     UUID          NOT NULL REFERENCES devoluciones_proveedor(id),
    cuenta_id         UUID          REFERENCES cuentas_por_pagar(id),
    monto             NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    aplicado_a_cuenta NUMERIC(14,2) NOT NULL CHECK (aplicado_a_cuenta >= 0),
    excedente         NUMERIC(14,2) NOT NULL CHECK (excedente >= 0),
    saldo_a_favor     NUMERIC(14,2) NOT NULL,
    creado_en         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_nota_reparto CHECK (aplicado_a_cuenta + excedente = monto),
    CONSTRAINT ck_nota_saldo CHECK (saldo_a_favor >= 0 AND saldo_a_favor <= excedente)
);
CREATE INDEX ix_notas_credito_proveedor ON notas_credito_proveedor (negocio_id, proveedor_id, creado_en);

ALTER TABLE devoluciones_proveedor  ENABLE ROW LEVEL SECURITY;
ALTER TABLE devoluciones_proveedor  FORCE  ROW LEVEL SECURITY;
ALTER TABLE devolucion_lineas       ENABLE ROW LEVEL SECURITY;
ALTER TABLE devolucion_lineas       FORCE  ROW LEVEL SECURITY;
ALTER TABLE notas_credito_proveedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE notas_credito_proveedor FORCE  ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON devoluciones_proveedor;
CREATE POLICY tenant_isolation ON devoluciones_proveedor
    USING (negocio_id = app_negocio_actual()) WITH CHECK (negocio_id = app_negocio_actual());
DROP POLICY IF EXISTS tenant_isolation ON devolucion_lineas;
CREATE POLICY tenant_isolation ON devolucion_lineas
    USING (negocio_id = app_negocio_actual()) WITH CHECK (negocio_id = app_negocio_actual());
DROP POLICY IF EXISTS tenant_isolation ON notas_credito_proveedor;
CREATE POLICY tenant_isolation ON notas_credito_proveedor
    USING (negocio_id = app_negocio_actual()) WITH CHECK (negocio_id = app_negocio_actual());

-- HU-177 · Una recepción con devoluciones ya no se anula: se marca al devolver.
ALTER TABLE recepciones ADD COLUMN con_devoluciones BOOLEAN NOT NULL DEFAULT false;

-- HU-171 C5 · Cuánto saldo a favor usó un pago, además de su monto.
ALTER TABLE pagos_proveedor
    ADD COLUMN saldo_a_favor_usado NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (saldo_a_favor_usado >= 0);
