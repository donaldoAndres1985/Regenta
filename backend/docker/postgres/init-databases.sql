-- =====================================================================
-- REGENTA — una base y un usuario por microservicio.
--
-- Ningun servicio puede conectarse a la base de otro: se revoca todo a
-- PUBLIC y solo se concede CONNECT al rol dueno. Que un servicio no lea las
-- tablas de otro deja de depender de la disciplina del equipo y pasa a
-- depender de PostgreSQL.
--
-- IDEMPOTENTE: se puede ejecutar dos veces sin fallar. Los CREATE van con
-- \gexec sobre un SELECT condicional, porque CREATE DATABASE no admite
-- IF NOT EXISTS ni puede ir dentro de un bloque DO.
--
-- Lo invoca init-databases.sh, que le pasa la clave:
--   psql -v clave="$REGENTA_DB_PASSWORD" -f init-databases.sql
-- =====================================================================
\set ON_ERROR_STOP on

-- ---------------------------------------------------------------- usuarios
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_usuarios', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_usuarios')\gexec
ALTER ROLE reg_usuarios WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_usuarios', 'reg_usuarios')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_usuarios')\gexec

REVOKE ALL ON DATABASE regenta_usuarios FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_usuarios TO reg_usuarios;

-- ---------------------------------------------------------------- clientes
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_clientes', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_clientes')\gexec
ALTER ROLE reg_clientes WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_clientes', 'reg_clientes')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_clientes')\gexec

REVOKE ALL ON DATABASE regenta_clientes FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_clientes TO reg_clientes;

-- ---------------------------------------------------------------- inventario
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_inventario', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_inventario')\gexec
ALTER ROLE reg_inventario WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_inventario', 'reg_inventario')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_inventario')\gexec

REVOKE ALL ON DATABASE regenta_inventario FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_inventario TO reg_inventario;

-- ---------------------------------------------------------------- ventas
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_ventas', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_ventas')\gexec
ALTER ROLE reg_ventas WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_ventas', 'reg_ventas')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_ventas')\gexec

REVOKE ALL ON DATABASE regenta_ventas FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_ventas TO reg_ventas;

-- ---------------------------------------------------------------- compras
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_compras', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_compras')\gexec
ALTER ROLE reg_compras WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_compras', 'reg_compras')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_compras')\gexec

REVOKE ALL ON DATABASE regenta_compras FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_compras TO reg_compras;

-- ---------------------------------------------------------------- recursos
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_recursos', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_recursos')\gexec
ALTER ROLE reg_recursos WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_recursos', 'reg_recursos')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_recursos')\gexec

REVOKE ALL ON DATABASE regenta_recursos FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_recursos TO reg_recursos;

-- ---------------------------------------------------------------- reservas
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_reservas', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_reservas')\gexec
ALTER ROLE reg_reservas WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_reservas', 'reg_reservas')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_reservas')\gexec

REVOKE ALL ON DATABASE regenta_reservas FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_reservas TO reg_reservas;

-- ---------------------------------------------------------------- menu
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_menu', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_menu')\gexec
ALTER ROLE reg_menu WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_menu', 'reg_menu')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_menu')\gexec

REVOKE ALL ON DATABASE regenta_menu FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_menu TO reg_menu;

-- ---------------------------------------------------------------- mesas
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_mesas', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_mesas')\gexec
ALTER ROLE reg_mesas WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_mesas', 'reg_mesas')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_mesas')\gexec

REVOKE ALL ON DATABASE regenta_mesas FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_mesas TO reg_mesas;

-- ---------------------------------------------------------------- comandas
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_comandas', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_comandas')\gexec
ALTER ROLE reg_comandas WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_comandas', 'reg_comandas')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_comandas')\gexec

REVOKE ALL ON DATABASE regenta_comandas FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_comandas TO reg_comandas;

-- ---------------------------------------------------------------- facturacion
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_facturacion', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_facturacion')\gexec
ALTER ROLE reg_facturacion WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_facturacion', 'reg_facturacion')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_facturacion')\gexec

REVOKE ALL ON DATABASE regenta_facturacion FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_facturacion TO reg_facturacion;

-- ---------------------------------------------------------------- caja
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_caja', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_caja')\gexec
ALTER ROLE reg_caja WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_caja', 'reg_caja')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_caja')\gexec

REVOKE ALL ON DATABASE regenta_caja FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_caja TO reg_caja;

-- ---------------------------------------------------------------- alertas
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_alertas', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_alertas')\gexec
ALTER ROLE reg_alertas WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_alertas', 'reg_alertas')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_alertas')\gexec

REVOKE ALL ON DATABASE regenta_alertas FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_alertas TO reg_alertas;

-- ---------------------------------------------------------------- reportes
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_reportes', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_reportes')\gexec
ALTER ROLE reg_reportes WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_reportes', 'reg_reportes')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_reportes')\gexec

REVOKE ALL ON DATABASE regenta_reportes FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_reportes TO reg_reportes;

-- ---------------------------------------------------------------- auditoria
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'reg_auditoria', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_auditoria')\gexec
ALTER ROLE reg_auditoria WITH LOGIN PASSWORD :'clave';

SELECT format('CREATE DATABASE %I OWNER %I', 'regenta_auditoria', 'reg_auditoria')
  WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'regenta_auditoria')\gexec

REVOKE ALL ON DATABASE regenta_auditoria FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE regenta_auditoria TO reg_auditoria;

-- ------------------------------------------------- roles de los barridos (HU-124)
-- Con FORCE ROW LEVEL SECURITY, una consulta sin negocio fijado no ve una sola
-- fila: un @Scheduled que recorra todos los negocios no tiene como saber cuales
-- tienen trabajo pendiente. Cada servicio con barridos tiene un segundo rol,
-- reg_<servicio>_barridos, que:
--   * se salta la RLS (BYPASSRLS) para DESCUBRIR que negocios tienen pendientes;
--   * solo lee: SELECT, nunca INSERT/UPDATE/DELETE. El trabajo en si lo hace el
--     servicio con su usuario normal, negocio por negocio y bajo su RLS;
--   * solo entra a la base de su servicio;
--   * no es el usuario con el que corre el servicio: la aplicacion lo usa en un
--     pool aparte que no se puede inyectar (comun/barridos).
-- Los privilegios por defecto cubren las tablas que Flyway cree despues; el
-- bloque DO cubre las que ya existan si el script se vuelve a correr.

SELECT format('CREATE ROLE %I LOGIN BYPASSRLS PASSWORD %L', 'reg_ventas_barridos', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_ventas_barridos')\gexec
ALTER ROLE reg_ventas_barridos WITH LOGIN BYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'clave';
GRANT CONNECT ON DATABASE regenta_ventas TO reg_ventas_barridos;
\c regenta_ventas
ALTER DEFAULT PRIVILEGES FOR ROLE reg_ventas GRANT USAGE ON SCHEMAS TO reg_ventas_barridos;
ALTER DEFAULT PRIVILEGES FOR ROLE reg_ventas GRANT SELECT ON TABLES TO reg_ventas_barridos;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'ventas') THEN
        GRANT USAGE ON SCHEMA ventas TO reg_ventas_barridos;
        GRANT SELECT ON ALL TABLES IN SCHEMA ventas TO reg_ventas_barridos;
    END IF;
END $$;
\c postgres

SELECT format('CREATE ROLE %I LOGIN BYPASSRLS PASSWORD %L', 'reg_alertas_barridos', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_alertas_barridos')\gexec
ALTER ROLE reg_alertas_barridos WITH LOGIN BYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'clave';
GRANT CONNECT ON DATABASE regenta_alertas TO reg_alertas_barridos;
\c regenta_alertas
ALTER DEFAULT PRIVILEGES FOR ROLE reg_alertas GRANT USAGE ON SCHEMAS TO reg_alertas_barridos;
ALTER DEFAULT PRIVILEGES FOR ROLE reg_alertas GRANT SELECT ON TABLES TO reg_alertas_barridos;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'alertas') THEN
        GRANT USAGE ON SCHEMA alertas TO reg_alertas_barridos;
        GRANT SELECT ON ALL TABLES IN SCHEMA alertas TO reg_alertas_barridos;
    END IF;
END $$;
\c postgres

SELECT format('CREATE ROLE %I LOGIN BYPASSRLS PASSWORD %L', 'reg_clientes_barridos', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_clientes_barridos')\gexec
ALTER ROLE reg_clientes_barridos WITH LOGIN BYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'clave';
GRANT CONNECT ON DATABASE regenta_clientes TO reg_clientes_barridos;
\c regenta_clientes
ALTER DEFAULT PRIVILEGES FOR ROLE reg_clientes GRANT USAGE ON SCHEMAS TO reg_clientes_barridos;
ALTER DEFAULT PRIVILEGES FOR ROLE reg_clientes GRANT SELECT ON TABLES TO reg_clientes_barridos;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'crm') THEN
        GRANT USAGE ON SCHEMA crm TO reg_clientes_barridos;
        GRANT SELECT ON ALL TABLES IN SCHEMA crm TO reg_clientes_barridos;
    END IF;
END $$;
\c postgres

SELECT format('CREATE ROLE %I LOGIN BYPASSRLS PASSWORD %L', 'reg_reportes_barridos', :'clave')
  WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reg_reportes_barridos')\gexec
ALTER ROLE reg_reportes_barridos WITH LOGIN BYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'clave';
GRANT CONNECT ON DATABASE regenta_reportes TO reg_reportes_barridos;
\c regenta_reportes
ALTER DEFAULT PRIVILEGES FOR ROLE reg_reportes GRANT USAGE ON SCHEMAS TO reg_reportes_barridos;
ALTER DEFAULT PRIVILEGES FOR ROLE reg_reportes GRANT SELECT ON TABLES TO reg_reportes_barridos;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'reportes') THEN
        GRANT USAGE ON SCHEMA reportes TO reg_reportes_barridos;
        GRANT SELECT ON ALL TABLES IN SCHEMA reportes TO reg_reportes_barridos;
    END IF;
END $$;
\c postgres

-- ------------------------------------------------------- indices trigram (HU-126)
-- Bajo FORCE ROW LEVEL SECURITY, los operadores LIKE/ILIKE (textlike/texticlike)
-- no son LEAKPROOF, asi que el planificador nunca los baja a un indice GIN
-- trigram: la busqueda por nombre parcial se resuelve por el indice de
-- negocio_id + filtro, sub-lineal pero no tan rapido como el trigram. Marcarlos
-- LEAKPROOF es responsabilidad del cluster, no de un servicio (ningun reg_*
-- es superusuario), y es por base porque pg_proc es un catalogo por base de
-- datos: hay que repetirlo en cada una que busca por nombre.
--
-- Es una propiedad, no una migracion: ALTER FUNCTION ... LEAKPROOF no falla si
-- ya estaba aplicada, asi que este bloque es idempotente igual que el resto
-- del script.
\c regenta_clientes
ALTER FUNCTION pg_catalog.textlike(text, text)   LEAKPROOF;
ALTER FUNCTION pg_catalog.texticlike(text, text) LEAKPROOF;

\c regenta_inventario
ALTER FUNCTION pg_catalog.textlike(text, text)   LEAKPROOF;
ALTER FUNCTION pg_catalog.texticlike(text, text) LEAKPROOF;

\c postgres
