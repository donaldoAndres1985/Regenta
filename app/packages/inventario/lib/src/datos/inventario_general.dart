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
    this.soloBajoMinimo = false,
  });

  final String? q;
  final String? categoriaId;
  final String estado;
  final bool tarifaPorRevisar;
  final bool existenciaNegativa;
  final String? bodegaId;
  final int pagina;
  final int tamano;

  /// Solo los que están en su mínimo o por debajo (R8).
  final bool soloBajoMinimo;

  ConsultaDeInventario copiar({
    Object? q = _sinCambio,
    Object? categoriaId = _sinCambio,
    String? estado,
    bool? tarifaPorRevisar,
    bool? existenciaNegativa,
    int? pagina,
    bool? soloBajoMinimo,
  }) =>
      ConsultaDeInventario(
        q: q == _sinCambio ? this.q : q as String?,
        categoriaId: categoriaId == _sinCambio ? this.categoriaId : categoriaId as String?,
        estado: estado ?? this.estado,
        tarifaPorRevisar: tarifaPorRevisar ?? this.tarifaPorRevisar,
        existenciaNegativa: existenciaNegativa ?? this.existenciaNegativa,
        bodegaId: bodegaId,
        pagina: pagina ?? this.pagina,
        tamano: tamano,
        soloBajoMinimo: soloBajoMinimo ?? this.soloBajoMinimo,
      );

  /// La misma consulta con otro texto de búsqueda (nulo = sin texto), desde la página 0.
  ConsultaDeInventario conTexto(String? texto) => copiar(q: texto, pagina: 0);

  static const _sinCambio = Object();

  Map<String, dynamic> comoQuery() => {
        if (q != null && q!.trim().isNotEmpty) 'q': q!.trim(),
        if (categoriaId != null) 'categoriaId': categoriaId,
        'estado': estado,
        if (tarifaPorRevisar) 'tarifaPorRevisar': true,
        if (existenciaNegativa) 'existenciaNegativa': true,
        if (bodegaId != null) 'bodegaId': bodegaId,
        'pagina': pagina,
        'tamano': tamano,
        if (soloBajoMinimo) 'soloBajoMinimo': true,
      };
}

/// HU-153 C1 · un producto del inventario general. `precio` incluye IVA. `costo` y `valorCosto`
/// llegan nulos sin INVENTARIO_EXISTENCIA_VER (R11). `nivel`: NORMAL, BAJO o CERO (R7).
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
    this.stockMinimo = 0,
    this.nivel = 'NORMAL',
  });

  final String id;
  final String sku;
  final String nombre;
  final String categoria;
  final String tarifaIva;
  final bool tarifaPorRevisar;
  final num existencia;
  final num? costo;
  final num precio;
  final num? valorCosto;
  final num valorPrecio;
  final bool activo;
  final num stockMinimo;
  final String nivel;

  factory FilaDeInventario.desdeJson(Map<String, dynamic> j) => FilaDeInventario(
        id: j['id'] as String,
        sku: (j['sku'] ?? '') as String,
        nombre: (j['nombre'] ?? '') as String,
        categoria: (j['categoria'] ?? '') as String,
        tarifaIva: (j['tarifaIva'] ?? '') as String,
        tarifaPorRevisar: (j['tarifaPorRevisar'] ?? false) as bool,
        existencia: (j['existencia'] ?? 0) as num,
        costo: j['costo'] as num?,
        precio: (j['precio'] ?? 0) as num,
        valorCosto: j['valorCosto'] as num?,
        valorPrecio: (j['valorPrecio'] ?? 0) as num,
        activo: (j['activo'] ?? true) as bool,
        stockMinimo: (j['stockMinimo'] ?? 0) as num,
        nivel: (j['nivel'] ?? 'NORMAL') as String,
      );
}

/// Una página del inventario general y los totales de todo lo filtrado.
class ResultadoDeInventario {
  const ResultadoDeInventario({
    required this.filas,
    required this.totalFilas,
    required this.valorCosto,
    required this.valorPrecio,
    this.bajoMinimo = 0,
  });

  final List<FilaDeInventario> filas;
  final int totalFilas;
  final num? valorCosto;
  final num valorPrecio;

  /// Cuántos de lo filtrado están en su mínimo o por debajo (chip «Bajo mínimo · n»).
  final int bajoMinimo;

  factory ResultadoDeInventario.desdeJson(Map<String, dynamic> j) => ResultadoDeInventario(
        filas: [
          for (final f in ((j['filas'] ?? const []) as List).cast<Map<String, dynamic>>())
            FilaDeInventario.desdeJson(f),
        ],
        totalFilas: ((j['totalFilas'] ?? 0) as num).toInt(),
        valorCosto: j['valorCosto'] as num?,
        valorPrecio: (j['valorPrecio'] ?? 0) as num,
        bajoMinimo: ((j['bajoMinimo'] ?? 0) as num).toInt(),
      );
}
