-- =====================================================================
-- servicio-clientes . V4 . el indice trigram de nombre_display, usable
--
-- V1 creo `ix_clientes_busqueda` como
--     USING gin (nombre_display gin_trgm_ops)
-- sobre una columna VARCHAR. El planificador, ante `nombre_display ILIKE '%x%'`,
-- genera el predicado sobre `nombre_display::text` y no lo empareja con un
-- indice cuya clave es VARCHAR. Se rehace como indice de expresion sobre el
-- cast a text —que es lo que el planificador busca— y parcial sobre los
-- clientes vivos, que es como consulta el servicio.
--
-- NOTA (RLS): con `FORCE ROW LEVEL SECURITY` activa, la politica es una
-- barrera de seguridad y los operadores LIKE/ILIKE de PostgreSQL no son
-- `LEAKPROOF`, asi que el planificador no los baja al indice: la busqueda por
-- nombre se resuelve hoy por `ix_clientes_negocio` (indice, acotado al
-- negocio) + filtro ILIKE, que ya evita el scan completo y entra en el
-- presupuesto de 300 ms sobre 10.000 clientes. Para que ademas entre este
-- indice trigram hace falta, una sola vez y como superusuario del cluster:
--     ALTER FUNCTION pg_catalog.textlike(text,text)   LEAKPROOF;
--     ALTER FUNCTION pg_catalog.texticlike(text,text) LEAKPROOF;
-- El servicio no puede hacerlo (no es superusuario); queda como paso de
-- despliegue, comun a todo servicio que busca por trigram bajo RLS.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO crm, public;

DROP INDEX IF EXISTS ix_clientes_busqueda;
CREATE INDEX ix_clientes_busqueda
    ON clientes USING gin ((nombre_display::text) gin_trgm_ops)
    WHERE eliminado_en IS NULL;
