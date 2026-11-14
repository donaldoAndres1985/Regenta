-- Base de pruebas: PostgreSQL 16 real, con un usuario de aplicacion que NO es superusuario.
--
-- Un superusuario se salta la Row-Level Security aunque la tabla tenga FORCE: si los tests
-- se conectaran con el usuario por defecto del contenedor, un test de aislamiento entre
-- negocios pasaria verde sin probar nada. El usuario de la app es dueno de la base y de los
-- schemas, igual que en produccion, y por eso FORCE ROW LEVEL SECURITY si lo alcanza.
CREATE ROLE regenta_app LOGIN PASSWORD 'regenta_app' NOSUPERUSER NOBYPASSRLS;
CREATE DATABASE regenta_prueba OWNER regenta_app;
