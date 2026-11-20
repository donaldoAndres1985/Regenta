-- HU-166 · I3 de docs/dominio/compras.md: cada producto tiene a lo sumo un proveedor preferido
-- en el negocio. La aplicación desmarca a los demás antes de marcar uno; el índice lo garantiza
-- también ante dos peticiones a la vez.

SET search_path TO compras, public;

-- Si hoy hay varios preferidos para el mismo producto, queda uno solo (el primero por proveedor_id):
-- no hay forma de saber cuál quiso el usuario, y dejar dos impediría crear el índice.
UPDATE proveedor_productos pp
   SET preferido = false
 WHERE pp.preferido
   AND EXISTS (
         SELECT 1
           FROM proveedor_productos otro
          WHERE otro.negocio_id = pp.negocio_id
            AND otro.producto_id = pp.producto_id
            AND otro.preferido
            AND otro.proveedor_id < pp.proveedor_id);

CREATE UNIQUE INDEX uq_un_preferido_por_producto
    ON proveedor_productos (negocio_id, producto_id)
 WHERE preferido;
