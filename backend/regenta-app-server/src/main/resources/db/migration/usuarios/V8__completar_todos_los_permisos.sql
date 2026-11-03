-- =====================================================================
-- V8 . Completar todos los permisos del catálogo y asignarlos al Administrador
-- =====================================================================
SET search_path TO core_identidad, public;
SELECT set_config('app.negocio_id', 'c1000000-0000-4000-8000-000000000001', false);

ALTER TABLE permisos NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

-- Eliminar la restricción restrictiva de acciones para permitir acciones operativas
ALTER TABLE permisos DROP CONSTRAINT IF EXISTS permisos_accion_check;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('INVENTARIO_AJUSTE_APLICAR', 'INVENTARIO', 'AJUSTE', 'APROBAR', 'Aplicar ajuste de inventario'),
 ('INVENTARIO_AJUSTE_CREAR', 'INVENTARIO', 'AJUSTE', 'CREAR', 'Crear ajuste de inventario'),
 ('INVENTARIO_AJUSTE_VER', 'INVENTARIO', 'AJUSTE', 'VER', 'Ver ajuste de inventario'),
 ('INVENTARIO_ATRIBUTO_CREAR', 'INVENTARIO', 'ATRIBUTO', 'CREAR', 'Crear atributo de categoria'),
 ('INVENTARIO_ATRIBUTO_EDITAR', 'INVENTARIO', 'ATRIBUTO', 'EDITAR', 'Editar atributo de categoria'),
 ('INVENTARIO_ATRIBUTO_VER', 'INVENTARIO', 'ATRIBUTO', 'VER', 'Ver atributo de categoria'),
 ('INVENTARIO_BODEGA_CREAR', 'INVENTARIO', 'BODEGA', 'CREAR', 'Crear bodega'),
 ('INVENTARIO_BODEGA_ELIMINAR', 'INVENTARIO', 'BODEGA', 'ELIMINAR', 'Eliminar bodega'),
 ('INVENTARIO_BODEGA_VER', 'INVENTARIO', 'BODEGA', 'VER', 'Ver bodega'),
 ('INVENTARIO_CATEGORIA_CREAR', 'INVENTARIO', 'CATEGORIA', 'CREAR', 'Crear categoria'),
 ('INVENTARIO_CATEGORIA_EDITAR', 'INVENTARIO', 'CATEGORIA', 'EDITAR', 'Editar categoria'),
 ('INVENTARIO_CATEGORIA_ELIMINAR', 'INVENTARIO', 'CATEGORIA', 'ELIMINAR', 'Eliminar categoria'),
 ('INVENTARIO_CATEGORIA_VER', 'INVENTARIO', 'CATEGORIA', 'VER', 'Ver categoria'),
 ('INVENTARIO_EXISTENCIA_VER', 'INVENTARIO', 'EXISTENCIA', 'VER', 'Ver existencias'),
 ('INVENTARIO_LISTA_PRECIOS_GESTIONAR', 'INVENTARIO', 'LISTA_PRECIOS', 'EDITAR', 'Gestionar listas de precios'),
 ('INVENTARIO_LISTA_PRECIOS_VER', 'INVENTARIO', 'LISTA_PRECIOS', 'VER', 'Ver listas de precios'),
 ('INVENTARIO_LOTE_REGISTRAR', 'INVENTARIO', 'LOTE', 'CREAR', 'Registrar lote'),
 ('INVENTARIO_LOTE_VER', 'INVENTARIO', 'LOTE', 'VER', 'Ver lote'),
 ('INVENTARIO_MOVIMIENTO_VER', 'INVENTARIO', 'MOVIMIENTO', 'VER', 'Ver movimientos de inventario'),
 ('INVENTARIO_RESERVA_GESTIONAR', 'INVENTARIO', 'RESERVA', 'EDITAR', 'Gestionar reservas de stock'),
 ('INVENTARIO_TRASLADO_CREAR', 'INVENTARIO', 'TRASLADO', 'CREAR', 'Crear traslado'),
 ('INVENTARIO_TRASLADO_ENVIAR', 'INVENTARIO', 'TRASLADO', 'EDITAR', 'Enviar traslado'),
 ('INVENTARIO_TRASLADO_RECIBIR', 'INVENTARIO', 'TRASLADO', 'EDITAR', 'Recibir traslado'),
 ('INVENTARIO_TRASLADO_VER', 'INVENTARIO', 'TRASLADO', 'VER', 'Ver traslado'),
 ('VENTAS_COTIZACION_CONVERTIR', 'VENTAS', 'COTIZACION', 'EDITAR', 'Convertir cotizacion'),
 ('VENTAS_COTIZACION_CREAR', 'VENTAS', 'COTIZACION', 'CREAR', 'Crear cotizacion'),
 ('VENTAS_COTIZACION_VER', 'VENTAS', 'COTIZACION', 'VER', 'Ver cotizacion'),
 ('VENTAS_DEVOLUCION_REGISTRAR', 'VENTAS', 'DEVOLUCION', 'CREAR', 'Registrar devolucion'),
 ('VENTAS_PAGO_REGISTRAR', 'VENTAS', 'PAGO', 'CREAR', 'Registrar pago'),
 ('VENTAS_VENTA_CONFIRMAR', 'VENTAS', 'VENTA', 'APROBAR', 'Confirmar venta')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar TODOS los permisos al rol Administrador de Ferretería Central
INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT 'c1000000-0000-4000-8000-000000000200'::uuid, codigo
FROM permisos
ON CONFLICT (rol_id, permiso_codigo) DO NOTHING;

-- Asignar TODOS los permisos a la plantilla Administrador
INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT 'b0000000-0000-4000-8000-000000000001'::uuid, codigo
FROM permisos
ON CONFLICT (rol_id, permiso_codigo) DO NOTHING;

ALTER TABLE permisos FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
