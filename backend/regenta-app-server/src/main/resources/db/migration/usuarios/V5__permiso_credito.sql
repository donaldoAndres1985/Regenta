-- ---------------------------------------------------------------------
-- V5 . Autorizar una venta a credito con cartera vencida (HU-040)
-- ---------------------------------------------------------------------
-- Vender a plazo ya lo cubre VENTAS_VENTA_CREAR. Lo que no cubria ningun
-- permiso es lo otro: seguir adelante cuando el cliente ya debe plata vencida.
-- Eso no es "vender", es decidir asumir un riesgo que el sistema acaba de
-- senalar, y no es del vendedor que esta en el mostrador con el cliente
-- enfrente. Por eso es permiso propio y no un atributo del de crear.
--
-- Se concede a Administrador y a Gerente. Vendedor y Cajero se quedan sin el
-- a proposito: pueden vender a credito dentro del cupo, y cuando hay mora
-- tienen que pedirselo a alguien.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

-- La accion es APROBAR y no AUTORIZAR porque el CHECK de permisos.accion tiene
-- un vocabulario cerrado a proposito. Agregarle un verbo nuevo para un solo
-- permiso rompe la simetria del catalogo; APROBAR ya es el verbo de
-- VENTAS_DESCUENTO_APROBAR, que es exactamente la misma clase de decision.
INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('VENTAS_CREDITO_APROBAR','VENTAS','CREDITO','APROBAR',
  'Autorizar una venta a credito a un cliente con cartera vencida');

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','VENTAS_CREDITO_APROBAR'),
 ('b0000000-0000-4000-8000-000000000002','VENTAS_CREDITO_APROBAR');

-- Vender a plazo obliga a mirar el cupo, y el cupo lo tiene servicio-clientes:
-- servicio-ventas se lo pregunta con las cabeceras de quien esta vendiendo, no
-- con un superusuario interno. Sin CLIENTES_CARTERA_VER, el vendedor y el
-- cajero no podrian hacer una venta a credito ni dentro del cupo, porque la
-- consulta previa les daria 403. Es lectura, filtrada por negocio como todo lo
-- demas, y es exactamente el dato que necesitan para atender al cliente que
-- tienen enfrente.
INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000003','CLIENTES_CARTERA_VER'),
 ('b0000000-0000-4000-8000-000000000004','CLIENTES_CARTERA_VER')
ON CONFLICT DO NOTHING;

-- Los negocios que ya existen no vuelven a instanciar sus plantillas: el alta
-- copia los permisos una sola vez, al crear el negocio. Sin este relleno, un
-- permiso nuevo solo lo tendrian los negocios creados despues del despliegue,
-- y los de antes se quedarian sin poder autorizar nunca.
--
-- roles y rol_permisos van con FORCE ROW LEVEL SECURITY, asi que tambien
-- filtran al dueno de la tabla, que es con quien corre Flyway. Sin negocio
-- fijado app_negocio_actual() da NULL y el UPDATE no veria una sola fila: el
-- relleno no fallaria, simplemente no haria nada, que es peor. Se levanta el
-- FORCE lo justo para este INSERT y se vuelve a poner en la misma transaccion;
-- la politica sigue activa todo el tiempo para cualquier otro rol.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo IN ('VENTAS_CREDITO_APROBAR','CLIENTES_CARTERA_VER')
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
