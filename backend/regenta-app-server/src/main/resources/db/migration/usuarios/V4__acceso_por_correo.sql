-- ---------------------------------------------------------------------
-- V4 . El directorio con el que empieza el login
-- ---------------------------------------------------------------------
-- Problema: la tabla usuarios tiene FORCE ROW LEVEL SECURITY filtrada por
-- negocio_id, y en el login todavia no se sabe cual es el negocio. Es el huevo
-- y la gallina: para leer al usuario hay que fijar el negocio, y el negocio
-- sale del usuario.
--
-- Las salidas posibles eran tres:
--   1. Abrir la politica cuando no hay negocio fijado. Se descarto: convierte
--      "fallar cerrado" en "fallar abierto" en todas las tablas a la vez.
--   2. Una funcion SECURITY DEFINER. No sirve: con FORCE, la politica aplica
--      tambien al dueno de la tabla, que es justo con quien se conecta el
--      servicio.
--   3. Esta tabla: el minimo indispensable para resolver a que negocio entrar,
--      sin RLS y sin un solo dato sensible. No hay hash de contraseña, ni
--      estado, ni roles. Solo la correspondencia correo -> negocio.
--
-- Se escribe en la misma transaccion que el usuario. Si alguna vez discrepan,
-- manda usuarios: esto es un indice, no la verdad.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

CREATE TABLE acceso_por_correo (
    email            VARCHAR(150) NOT NULL,
    negocio_id       UUID         NOT NULL REFERENCES negocios(id) ON DELETE CASCADE,
    usuario_id       UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre_comercial VARCHAR(150) NOT NULL,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (email, negocio_id)
);

CREATE INDEX ix_acceso_por_correo_email ON acceso_por_correo (email);

COMMENT ON TABLE acceso_por_correo IS
 'Directorio correo -> negocio para el login. A proposito SIN RLS: se consulta antes de saber el tenant. No guarda ningun dato sensible.';
