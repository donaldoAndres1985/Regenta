-- =====================================================================
-- servicio-auditoria . V4 . la bitacora vieja se archiva (HU-131)
--
-- Las particiones de eventos_auditoria son compartidas entre negocios: no
-- se puede soltar una particion porque a un negocio se le vencio la
-- retencion mientras otro la sigue necesitando. Asi que se archiva por
-- negocio (C1, C2): los eventos vencidos de ESE negocio se copian a un
-- archivo en el almacen de objetos (HU-121), se anota aqui el archivo y
-- se borran de la tabla viva. Los de otros negocios no se tocan.
--
-- La tabla sigue siendo append-only (HU-101 C2): un UPDATE se rechaza
-- siempre, y un DELETE solo pasa si ya existe un archivo de ese negocio
-- que cubre la fecha del evento. Borrar sin archivar antes es imposible,
-- aunque alguien entre directo a la base.
--
-- C4: politicas_retencion anota de donde salio la retencion: el negocio
-- la configuro, o se aplico la de su plan (y cual).
--
-- NO EDITAR despues de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO auditoria, public;

CREATE TABLE archivos_auditoria (
    id         UUID        PRIMARY KEY,
    negocio_id UUID        NOT NULL,
    desde      TIMESTAMPTZ NOT NULL,       -- el evento mas viejo del archivo
    hasta      TIMESTAMPTZ NOT NULL,       -- el mas nuevo (inclusive)
    eventos    INT         NOT NULL CHECK (eventos > 0),
    url        TEXT        NOT NULL,       -- objeto en el almacen, nunca el contenido
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_archivo_rango CHECK (hasta >= desde)
);
CREATE INDEX ix_archivos_auditoria ON archivos_auditoria (negocio_id, desde, hasta);

ALTER TABLE archivos_auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE archivos_auditoria FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON archivos_auditoria
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

ALTER TABLE politicas_retencion
    ADD COLUMN origen         VARCHAR(10) NOT NULL DEFAULT 'NEGOCIO'
                              CHECK (origen IN ('NEGOCIO', 'PLAN')),
    ADD COLUMN plan           VARCHAR(30),
    ADD COLUMN actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now();

COMMENT ON TABLE archivos_auditoria IS
 'HU-131: eventos de un negocio que pasaron su retencion, en un archivo del almacen. Se buscan ahi (C3).';
COMMENT ON COLUMN politicas_retencion.origen IS
 'HU-131 C4: NEGOCIO si la configuro el negocio; PLAN si se aplico la del plan (anotado en plan).';

CREATE OR REPLACE FUNCTION auditoria.rechazar_cambio() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND EXISTS (
        SELECT 1 FROM auditoria.archivos_auditoria a
         WHERE a.negocio_id = OLD.negocio_id
           AND OLD.ocurrido_en BETWEEN a.desde AND a.hasta) THEN
        RETURN OLD;   -- ya esta archivado: sale de la tabla viva (HU-131)
    END IF;
    RAISE EXCEPTION 'eventos_auditoria es append-only: no se puede % un evento de auditoria', TG_OP;
END $$;
