/// HU-150 · la ficha de un producto, como la devuelve `GET/PUT /api/inventario/productos/{id}`.
/// `precioVenta` es el de la etiqueta, con IVA incluido (I2 de docs/dominio/inventario.md).
class FichaDeProducto {
  const FichaDeProducto({
    required this.id,
    required this.sku,
    required this.nombre,
    required this.categoriaId,
    required this.unidadMedidaId,
    required this.tarifaIva,
    required this.precioVenta,
    required this.version,
    this.codigoBarras,
    this.costo = 0,
    this.tarifaPorRevisar = false,
    this.activo = true,
  });

  final String id;
  final String sku;
  final String? codigoBarras;
  final String nombre;
  final String categoriaId;
  final String unidadMedidaId;
  final String tarifaIva;
  final bool tarifaPorRevisar;
  final num precioVenta;
  final num costo;
  final bool activo;
  final int version;

  factory FichaDeProducto.desdeJson(Map<String, dynamic> j) => FichaDeProducto(
        id: j['id'] as String,
        sku: (j['sku'] ?? '') as String,
        codigoBarras: j['codigoBarras'] as String?,
        nombre: (j['nombre'] ?? '') as String,
        categoriaId: (j['categoriaId'] ?? '') as String,
        unidadMedidaId: (j['unidadMedidaId'] ?? '') as String,
        tarifaIva: (j['tarifaIva'] ?? 'IVA_19') as String,
        tarifaPorRevisar: (j['tarifaPorRevisar'] ?? false) as bool,
        precioVenta: (j['precioVenta'] ?? 0) as num,
        costo: (j['costo'] ?? 0) as num,
        activo: (j['activo'] ?? true) as bool,
        version: ((j['version'] ?? 0) as num).toInt(),
      );
}

/// Lo que se manda al guardar la ficha.
class EdicionDeProducto {
  const EdicionDeProducto({
    required this.sku,
    required this.nombre,
    required this.categoriaId,
    required this.unidadMedidaId,
    required this.tarifaIva,
    required this.precioVenta,
    required this.costo,
    required this.version,
    this.codigoBarras,
  });

  final String sku;
  final String? codigoBarras;
  final String nombre;
  final String categoriaId;
  final String unidadMedidaId;
  final String tarifaIva;
  final num precioVenta;
  final num costo;
  final int version;

  Map<String, dynamic> aJson() => {
        'sku': sku,
        if (codigoBarras != null && codigoBarras!.isNotEmpty) 'codigoBarras': codigoBarras,
        'nombre': nombre,
        'categoriaId': categoriaId,
        'unidadMedidaId': unidadMedidaId,
        'tarifaIva': tarifaIva,
        'precioVenta': precioVenta,
        'costo': costo,
        'version': version,
      };
}

/// HU-152 · la ubicación del producto en una bodega (vacía si no se ha dicho).
class UbicacionEnBodega {
  const UbicacionEnBodega({required this.bodegaId, required this.bodegaNombre, this.ubicacion});

  final String bodegaId;
  final String bodegaNombre;
  final String? ubicacion;
}

/// HU-159 · un ajuste de existencia desde la app: SUMAR, RESTAR o CONTEO (fijar por conteo
/// físico), con su tipo (CONTEO_FISICO, MERMA, AVERIA, VENCIMIENTO) y motivo.
class AjusteDeExistencia {
  const AjusteDeExistencia({
    required this.productoId,
    required this.bodegaId,
    required this.operacion,
    required this.cantidad,
    required this.tipo,
    required this.motivo,
  });

  final String productoId;
  final String bodegaId;
  final String operacion;
  final num cantidad;
  final String tipo;
  final String motivo;

  Map<String, dynamic> aJson() => {
        'productoId': productoId,
        'bodegaId': bodegaId,
        'operacion': operacion,
        'cantidad': cantidad,
        'tipo': tipo,
        'motivo': motivo,
      };
}

/// HU-159 · los tipos de ajuste que se ofrecen, con su nombre para mostrar.
const tiposDeAjuste = <(String, String)>[
  ('CONTEO_FISICO', 'Conteo físico'),
  ('MERMA', 'Merma'),
  ('AVERIA', 'Avería'),
  ('VENCIMIENTO', 'Vencimiento'),
];

/// HU-155 · una unidad de medida del negocio.
class UnidadDeMedida {
  const UnidadDeMedida({
    required this.id,
    required this.codigo,
    required this.nombre,
    this.permiteDecimales = false,
    this.activa = true,
  });

  final String id;
  final String codigo;
  final String nombre;
  final bool permiteDecimales;
  final bool activa;

  factory UnidadDeMedida.desdeJson(Map<String, dynamic> j) => UnidadDeMedida(
        id: j['id'] as String,
        codigo: (j['codigo'] ?? '') as String,
        nombre: (j['nombre'] ?? '') as String,
        permiteDecimales: (j['permiteDecimales'] ?? false) as bool,
        activa: (j['activa'] ?? true) as bool,
      );
}

/// Una opción para elegir en la ficha (categoría o unidad).
class OpcionDeFicha {
  const OpcionDeFicha({required this.id, required this.nombre});

  final String id;
  final String nombre;
}

/// Las tarifas de IVA del catálogo, con su nombre para mostrar y su porcentaje.
const tarifasDeIva = <(String codigo, String nombre, num porcentaje)>[
  ('IVA_19', 'IVA 19 %', 19),
  ('IVA_5', 'IVA 5 %', 5),
  ('EXENTO', 'Exento (0 %)', 0),
  ('EXCLUIDO', 'Excluido', 0),
];

num porcentajeDeTarifa(String codigo) =>
    tarifasDeIva.firstWhere((t) => t.$1 == codigo, orElse: () => ('', '', 0)).$3;

/// HU-150 C1 · base, IVA, margen y beneficio de un precio con IVA incluido, igual que el
/// backend: base = precio / (1 + t) a 2 decimales; margen = (base − costo) / costo;
/// beneficio = (base − costo) / base. Margen y beneficio son nulos sin costo.
class CalculoDeFicha {
  CalculoDeFicha({required num precio, required String tarifa, required num costo}) {
    final t = porcentajeDeTarifa(tarifa);
    base = t == 0 ? precio : _r2(precio / (1 + t / 100));
    iva = _r2(precio - base);
    final ganancia = base - costo;
    margenPct = costo > 0 ? _r2(ganancia / costo * 100) : null;
    beneficioPct = costo > 0 && base > 0 ? _r2(ganancia / base * 100) : null;
  }

  late final num base;
  late final num iva;
  late final num? margenPct;
  late final num? beneficioPct;

  static num _r2(num v) => (v * 100).round() / 100;
}

/// R12 · lo que pide «Nuevo producto»: `POST /api/inventario/productos`. `precioVenta` incluye IVA.
class NuevoProducto {
  const NuevoProducto({
    required this.sku,
    this.codigoBarras,
    required this.nombre,
    required this.categoriaId,
    required this.unidadMedidaId,
    required this.tarifaIva,
    required this.precioVenta,
    this.stockMinimo = 0,
  });

  final String sku;
  final String? codigoBarras;
  final String nombre;
  final String categoriaId;
  final String unidadMedidaId;
  final String tarifaIva;
  final num precioVenta;
  final num stockMinimo;

  Map<String, dynamic> aJson() => {
        'sku': sku,
        if (codigoBarras != null && codigoBarras!.isNotEmpty) 'codigoBarras': codigoBarras,
        'nombre': nombre,
        'categoriaId': categoriaId,
        'unidadMedidaId': unidadMedidaId,
        'tarifaIva': tarifaIva,
        'precioVenta': precioVenta,
        'controlaStock': true,
        'stockMinimo': stockMinimo,
      };
}
