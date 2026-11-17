/// HU-153 · lo que se pide al inventario general: `GET /api/inventario/general`.
class ConsultaDeInventario {
  const ConsultaDeInventario({
    this.q,
    this.categoriaId,
    this.estado = 'ACTIVOS',
    this.tarifaPorRevisar = false,
    this.existenciaNegativa = false,
    this.bodegaId,
    this.pagina = 0,
    this.tamano = 50,
  });

  final String? q;
  final String? categoriaId;
  final String estado;
  final bool tarifaPorRevisar;
  final bool existenciaNegativa;
  final String? bodegaId;
  final int pagina;
  final int tamano;

  ConsultaDeInventario copiar({
    String? q,
    String? estado,
    bool? tarifaPorRevisar,
    bool? existenciaNegativa,
    int? pagina,
  }) =>
      ConsultaDeInventario(
        q: q ?? this.q,
        categoriaId: categoriaId,
        estado: estado ?? this.estado,
        tarifaPorRevisar: tarifaPorRevisar ?? this.tarifaPorRevisar,
        existenciaNegativa: existenciaNegativa ?? this.existenciaNegativa,
        bodegaId: bodegaId,
        pagina: pagina ?? this.pagina,
        tamano: tamano,
      );

  Map<String, dynamic> comoQuery() => {
        if (q != null && q!.trim().isNotEmpty) 'q': q!.trim(),
        if (categoriaId != null) 'categoriaId': categoriaId,
        'estado': estado,
        if (tarifaPorRevisar) 'tarifaPorRevisar': true,
        if (existenciaNegativa) 'existenciaNegativa': true,
        if (bodegaId != null) 'bodegaId': bodegaId,
        'pagina': pagina,
        'tamano': tamano,
      };
}

/// HU-153 C1 · un producto del inventario general. `precio` incluye IVA.
class FilaDeInventario {
  const FilaDeInventario({
    required this.id,
    required this.sku,
    required this.nombre,
    required this.categoria,
    required this.tarifaIva,
    required this.tarifaPorRevisar,
    required this.existencia,
    required this.costo,
    required this.precio,
    required this.valorCosto,
    required this.valorPrecio,
    required this.activo,
  });

  final String id;
  final String sku;
  final String nombre;
  final String categoria;
  final String tarifaIva;
  final bool tarifaPorRevisar;
  final num existencia;
  final num costo;
  final num precio;
  final num valorCosto;
  final num valorPrecio;
  final bool activo;

  factory FilaDeInventario.desdeJson(Map<String, dynamic> j) => FilaDeInventario(
        id: j['id'] as String,
        sku: (j['sku'] ?? '') as String,
        nombre: (j['nombre'] ?? '') as String,
        categoria: (j['categoria'] ?? '') as String,
        tarifaIva: (j['tarifaIva'] ?? '') as String,
        tarifaPorRevisar: (j['tarifaPorRevisar'] ?? false) as bool,
        existencia: (j['existencia'] ?? 0) as num,
        costo: (j['costo'] ?? 0) as num,
        precio: (j['precio'] ?? 0) as num,
        valorCosto: (j['valorCosto'] ?? 0) as num,
        valorPrecio: (j['valorPrecio'] ?? 0) as num,
        activo: (j['activo'] ?? true) as bool,
      );
}

/// Una página del inventario general y los totales de todo lo filtrado.
class ResultadoDeInventario {
  const ResultadoDeInventario({
    required this.filas,
    required this.totalFilas,
    required this.valorCosto,
    required this.valorPrecio,
  });

  final List<FilaDeInventario> filas;
  final int totalFilas;
  final num valorCosto;
  final num valorPrecio;

  factory ResultadoDeInventario.desdeJson(Map<String, dynamic> j) => ResultadoDeInventario(
        filas: [
          for (final f in ((j['filas'] ?? const []) as List).cast<Map<String, dynamic>>())
            FilaDeInventario.desdeJson(f),
        ],
        totalFilas: ((j['totalFilas'] ?? 0) as num).toInt(),
        valorCosto: (j['valorCosto'] ?? 0) as num,
        valorPrecio: (j['valorPrecio'] ?? 0) as num,
      );
}
