-- =====================================================================
-- servicio-inventario . V4 . Lotes y fechas de vencimiento (HU-031)
--
-- Las tablas `lotes` y `existencias_lote` ya vienen de V1. Aqui se agrega lo
-- que HU-031 necesita encima:
--
--  * autorizaciones_lote_vencido: cuando alguien da salida a un lote vencido
--    con autorizacion explicita, esa autorizacion queda registrada y no se
--    puede editar ni borrar (criterio 4). Es append-only, igual que el libro
--    mayor.
--  * un indice parcial sobre existencias_lote para que la sugerencia FEFO
--    (lote de vencimiento mas proximo con saldo) no escanee toda la tabla.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO inventario, public;

-- Trigger generico append-only: sirve para cualquier tabla que no se corrige
-- editando, sino con una fila nueva. Reusa la idea del libro mayor (V3) pero
-- con un mensaje que nombra la tabla real.
CREATE OR REPLACE FUNCTION inventario.trg_tabla_append_only()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION '% es append-only: no se actualiza ni se borra (operacion %)',
    TG_TABLE_NAME, TG_OP
    USING ERRCODE = 'restrict_violation';
END $$;

-- Sugerencia FEFO: de un producto, en una bodega, los lotes con saldo. El
-- WHERE parcial deja fuera los lotes ya agotados, que son la mayoria con el
-- tiempo.
CREATE INDEX ix_existencias_lote_con_saldo
    ON existencias_lote (negocio_id, bodega_id) WHERE cantidad > 0;

CREATE TABLE autorizaciones_lote_vencido (
    id                UUID PRIMARY KEY,
    negocio_id        UUID        NOT NULL,
    lote_id           UUID        NOT NULL REFERENCES lotes(id),
    producto_id       UUID        NOT NULL REFERENCES productos(id),
    bodega_id         UUID        NOT NULL REFERENCES bodegas(id),
    cantidad          NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    usuario_id        UUID,
    motivo            TEXT        NOT NULL,
    fecha_vencimiento DATE        NOT NULL,
    autorizado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_autoriz_lote_vencido ON autorizaciones_lote_vencido (negocio_id, lote_id);

CREATE TRIGGER autoriz_lote_vencido_append_only
    BEFORE UPDATE OR DELETE ON autorizaciones_lote_vencido
    FOR EACH ROW
    EXECUTE FUNCTION inventario.trg_tabla_append_only();

-- RLS: como toda tabla de negocio. ENABLE + FORCE + politica tenant_isolation,
-- si no el dueno de la tabla (el rol del servicio) se la salta.
ALTER TABLE autorizaciones_lote_vencido ENABLE ROW LEVEL SECURITY;
ALTER TABLE autorizaciones_lote_vencido FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON autorizaciones_lote_vencido;
CREATE POLICY tenant_isolation ON autorizaciones_lote_vencido
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
