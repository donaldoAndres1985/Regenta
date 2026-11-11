/// Una bodega del negocio (HU-029). El POS necesita el `id` real para vender:
/// la saga de confirmación (HU-038) reserva stock contra `(producto, bodega)`
/// exacto, y nunca encuentra nada si se le manda cualquier otro id.
class BodegaDelNegocio {
  const BodegaDelNegocio({
    required this.id,
    required this.codigo,
    required this.nombre,
    required this.esDefault,
    required this.activa,
  });

  final String id;
  final String codigo;
  final String nombre;
  final bool esDefault;
  final bool activa;

  factory BodegaDelNegocio.desdeJson(Map<String, dynamic> json) => BodegaDelNegocio(
        id: json['id'] as String,
        codigo: json['codigo'] as String,
        nombre: json['nombre'] as String,
        esDefault: json['esDefault'] as bool? ?? false,
        activa: json['activa'] as bool? ?? true,
      );
}
