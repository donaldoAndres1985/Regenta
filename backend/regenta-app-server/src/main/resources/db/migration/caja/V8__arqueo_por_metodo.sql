-- =====================================================================
-- caja . V8 . arqueo de cierre por método de pago (HU-140)
--
-- Una fila por método de pago de la sesión, escrita una sola vez al cerrar
-- (I12: la sesión cerrada es inmutable). Qué métodos eran cuadrables queda
-- congelado aquí (I8b): cambiar la configuración después no altera cierres.
--
-- Garantías en la base, no solo en la app:
--   * diferencia = contado - esperado, columna generada (I4b).
--   * un método no cuadrable no tiene diferencia: contado = esperado (I9).
--   * el efectivo siempre es cuadrable (I8).
--
-- NO EDITAR después de aplicada. Los cambios van en un V9.
-- =====================================================================

SET search_path TO caja, public;

CREATE TABLE arqueo_metodos (
    id           UUID PRIMARY KEY,
    negocio_id   UUID          NOT NULL,
    sesion_id    UUID          NOT NULL REFERENCES sesiones_caja(id),
    metodo_pago  VARCHAR(20)   NOT NULL
                 CHECK (metodo_pago IN ('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                                        'TRANSFERENCIA','QR','BONO','CREDITO','OTRO')),
    cuadrable    BOOLEAN       NOT NULL,
    esperado     NUMERIC(16,4) NOT NULL,
    contado      NUMERIC(16,4) NOT NULL,
    diferencia   NUMERIC(16,4) GENERATED ALWAYS AS (contado - esperado) STORED,
    movimientos  INT           NOT NULL DEFAULT 0 CHECK (movimientos >= 0),
    creado_en    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_arqueo_metodo UNIQUE (sesion_id, metodo_pago),
    CONSTRAINT ck_arqueo_no_cuadrable_sin_diferencia CHECK (cuadrable OR contado = esperado),
    CONSTRAINT ck_arqueo_efectivo_cuadrable CHECK (metodo_pago <> 'EFECTIVO' OR cuadrable)
);

CREATE INDEX ix_arqueo_metodos_sesion ON arqueo_metodos (negocio_id, sesion_id);

ALTER TABLE arqueo_metodos ENABLE ROW LEVEL SECURITY;
ALTER TABLE arqueo_metodos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON arqueo_metodos;
CREATE POLICY tenant_isolation ON arqueo_metodos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
