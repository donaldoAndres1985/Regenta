-- =====================================================================
-- servicio-alertas . V5 . el certificado de firma por vencer (HU-116)
--
-- Sin certificado vigente no se puede facturar. Facturacion publica
-- certificado_por_vencer cuando faltan menos de 30 dias; aqui esta el tipo
-- de alerta con que el motor lo avisa. La regla por defecto (administrador,
-- en la app y por correo) la crea el motor la primera vez que llega el
-- aviso a un negocio que no tiene una.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO alertas, public;

INSERT INTO tipos_alerta (codigo, nombre, modulo, patron, severidad_default,
                          plantilla_titulo, plantilla_mensaje) VALUES
 ('CERTIFICADO_POR_VENCER', 'Certificado de firma por vencer', 'FACTURACION', NULL, 'CRITICA',
  'Certificado de firma por vencer: {alias}',
  'El certificado {alias} vence el {vigente_hasta} (en {dias_para_vencer} días). Sin un certificado vigente no se puede facturar: carga el nuevo antes de esa fecha.')
ON CONFLICT (codigo) DO NOTHING;
