/// Una orden de compra tal como la necesita la recepción: la cabecera y sus
/// líneas con lo pedido y lo ya recibido (HU-051).
class OrdenRecibible {
  const OrdenRecibible({
    required this.id,
    required this.numero,
    required this.proveedorId,
    required this.bodegaId,
    required this.estado,
    required this.lineas,
  });

  final String id;
  final String numero;
  final String proveedorId;
  final String bodegaId;
  final String estado;
  final List<LineaRecibible> lineas;

  factory OrdenRecibible.desdeJson(Map<String, dynamic> json) => OrdenRecibible(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        proveedorId: (json['proveedorId'] ?? '') as String,
        bodegaId: (json['bodegaDestinoId'] ?? json['bodegaId'] ?? '') as String,
        estado: (json['estado'] ?? '') as String,
        lineas: ((json['lineas'] ?? const []) as List<dynamic>)
            .map((e) => LineaRecibible.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
      );
}

/// Una línea de la orden en la pantalla de recepción.
class LineaRecibible {
  const LineaRecibible({
    required this.id,
    required this.linea,
    required this.productoId,
    required this.nombre,
    required this.cantidadPedida,
    required this.cantidadRecibida,
    required this.costoUnitario,
    this.codigo,
    this.exigeLote = false,
  });

  final String id;
  final int linea;
  final String productoId;
  final String nombre;
  final num cantidadPedida;
  final num cantidadRecibida;
  final num costoUnitario;

  /// El código/SKU del producto, si el backend lo trae. Se usa para saltar a la
  /// línea al escanear (criterio 3).
  final String? codigo;

  /// Si la categoría del producto exige lote y vencimiento al recibir (HU-031).
  /// Llega de la config de inventario; hasta que el backend lo incluya en la
  /// orden, viaja en el JSON como `exige_lote`.
  final bool exigeLote;

  /// Lo que todavía falta por recibir de esta línea.
  num get faltante {
    final f = cantidadPedida - cantidadRecibida;
    return f < 0 ? 0 : f;
  }

  factory LineaRecibible.desdeJson(Map<String, dynamic> json) => LineaRecibible(
        id: json['id'] as String,
        linea: (json['linea'] ?? 0) as int,
        productoId: (json['productoId'] ?? '') as String,
        nombre: (json['nombre'] ?? '') as String,
        cantidadPedida: (json['cantidadPedida'] ?? 0) as num,
        cantidadRecibida: (json['cantidadRecibida'] ?? 0) as num,
        costoUnitario: (json['costoUnitario'] ?? 0) as num,
        codigo: json['codigo'] as String?,
        exigeLote: (json['exigeLote'] ?? json['exige_lote'] ?? false) as bool,
      );
}
