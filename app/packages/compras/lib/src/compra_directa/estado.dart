import '../proveedores/datos/producto_del_proveedor.dart';
import '../proveedores/datos/proveedor_del_negocio.dart';
import 'datos.dart';

/// Una línea de la factura mientras se escribe.
class LineaEnEdicion {
  const LineaEnEdicion({
    required this.producto,
    this.cantidad = 1,
    this.costo,
    this.descuentoPct = 0,
    this.codigoProveedor,
    this.codigoLote,
    this.vencimiento,
  });

  final ProductoParaComprar producto;
  final num cantidad;
  final num? costo;
  final num descuentoPct;
  final String? codigoProveedor;
  final String? codigoLote;
  final DateTime? vencimiento;

  /// Costo con el descuento de la línea, sin IVA (como `RecepcionLinea.costoNeto`).
  num get costoNeto => (costo ?? 0) * (1 - descuentoPct / 100);

  /// La base de la línea: cantidad × costo neto.
  num get base => cantidad * costoNeto;

  num get iva => base * producto.impuestoPct / 100;

  /// Lo que le falta a la línea para poder registrarse, o `null` si está completa.
  String? get falta {
    if (cantidad <= 0) return 'La cantidad de «${producto.nombre}» debe ser mayor que cero.';
    if (costo == null) return 'Falta el costo de «${producto.nombre}».';
    if (descuentoPct < 0 || descuentoPct >= 100) {
      return 'El descuento de «${producto.nombre}» va de 0 a menos de 100 %.';
    }
    if (producto.exigeLote && (codigoLote == null || codigoLote!.trim().isEmpty)) {
      return '«${producto.nombre}» maneja lotes: escribe el código del lote.';
    }
    if (producto.perecedero && vencimiento == null) {
      return '«${producto.nombre}» es perecedero: indica el vencimiento del lote.';
    }
    return null;
  }

  LineaEnEdicion copiar({
    num? cantidad,
    Object? costo = _sin,
    num? descuentoPct,
    Object? codigoProveedor = _sin,
    Object? codigoLote = _sin,
    Object? vencimiento = _sin,
  }) =>
      LineaEnEdicion(
        producto: producto,
        cantidad: cantidad ?? this.cantidad,
        costo: identical(costo, _sin) ? this.costo : costo as num?,
        descuentoPct: descuentoPct ?? this.descuentoPct,
        codigoProveedor:
            identical(codigoProveedor, _sin) ? this.codigoProveedor : codigoProveedor as String?,
        codigoLote: identical(codigoLote, _sin) ? this.codigoLote : codigoLote as String?,
        vencimiento: identical(vencimiento, _sin) ? this.vencimiento : vencimiento as DateTime?,
      );

  Map<String, dynamic> aJson() => {
        'productoId': producto.productoId,
        'cantidad': cantidad,
        'costoUnitario': costo,
        'descuentoPct': descuentoPct,
        if (codigoProveedor != null && codigoProveedor!.trim().isNotEmpty)
          'codigoProveedor': codigoProveedor!.trim(),
        if (producto.exigeLote && codigoLote != null) 'codigoLote': codigoLote!.trim(),
        if (producto.exigeLote && vencimiento != null)
          'fechaVencimiento': _fecha(vencimiento!),
      };

  static const _sin = Object();
}

String _fecha(DateTime d) => d.toIso8601String().substring(0, 10);

/// El estado de la pantalla de compra directa (HU-167). Inmutable.
class EstadoDeCompraDirecta {
  const EstadoDeCompraDirecta({
    this.proveedores = const [],
    this.bodegas = const [],
    this.proveedorId,
    this.bodegaId,
    this.factura = '',
    this.fechaFactura,
    this.flete = 0,
    this.descuentoGlobal = 0,
    this.lineas = const [],
    this.resultados = const [],
    this.cargando = false,
    this.buscando = false,
    this.guardando = false,
    this.error,
    this.borradorId,
  });

  final List<ProveedorDelNegocio> proveedores;
  final List<BodegaParaComprar> bodegas;
  final String? proveedorId;
  final String? bodegaId;
  final String factura;
  final DateTime? fechaFactura;
  final num flete;

  /// HU-168 · Descuento a pie de factura; se reparte entre las líneas por su valor.
  final num descuentoGlobal;
  final List<LineaEnEdicion> lineas;
  final List<ProductoEncontrado> resultados;
  final bool cargando;
  final bool buscando;
  final bool guardando;
  final String? error;

  /// Si la confirmación falló, el borrador que quedó en el servidor: el reintento lo corrige.
  final String? borradorId;

  ProveedorDelNegocio? get proveedor =>
      proveedores.where((p) => p.id == proveedorId).firstOrNull;

  /// La suma de los netos de las líneas, antes del descuento global.
  num get base => lineas.fold<num>(0, (s, l) => s + l.base);

  /// HU-168 · El IVA se calcula sobre la base de cada línea ya rebajada con su parte del descuento
  /// global (repartido por valor), como en el backend.
  num get iva {
    final total = base;
    if (total <= 0 || descuentoGlobal == 0) return lineas.fold<num>(0, (s, l) => s + l.iva);
    return lineas.fold<num>(0, (s, l) {
      final rebajada = l.base - descuentoGlobal * l.base / total;
      return s + rebajada * l.producto.impuestoPct / 100;
    });
  }

  /// HU-167 C1 / I22: lo que se le debe al proveedor = base − descuento global + IVA + flete.
  num get total => base - descuentoGlobal + iva + flete;

  /// Lo que falta para poder registrar la compra, o `null` si está lista.
  String? get falta {
    if (proveedorId == null) return 'Elige el proveedor.';
    if (bodegaId == null) return 'Elige la bodega a la que entra la mercancía.';
    if (factura.trim().isEmpty) return 'Escribe el número de la factura del proveedor.';
    if (lineas.isEmpty) return 'Agrega al menos un producto.';
    if (descuentoGlobal > base) return 'El descuento global no puede ser mayor que el valor de la factura.';
    for (final l in lineas) {
      final f = l.falta;
      if (f != null) return f;
    }
    return null;
  }

  Map<String, dynamic> aJson() => {
        'proveedorId': proveedorId,
        'bodegaId': bodegaId,
        'facturaProveedor': factura.trim(),
        if (fechaFactura != null) 'fechaFactura': _fecha(fechaFactura!),
        'flete': flete,
        'descuentoGlobal': descuentoGlobal,
        'lineas': [for (final l in lineas) l.aJson()],
      };

  EstadoDeCompraDirecta copiar({
    List<ProveedorDelNegocio>? proveedores,
    List<BodegaParaComprar>? bodegas,
    Object? proveedorId = _sin,
    Object? bodegaId = _sin,
    String? factura,
    Object? fechaFactura = _sin,
    num? flete,
    num? descuentoGlobal,
    List<LineaEnEdicion>? lineas,
    List<ProductoEncontrado>? resultados,
    bool? cargando,
    bool? buscando,
    bool? guardando,
    Object? error = _sin,
    Object? borradorId = _sin,
  }) =>
      EstadoDeCompraDirecta(
        proveedores: proveedores ?? this.proveedores,
        bodegas: bodegas ?? this.bodegas,
        proveedorId: identical(proveedorId, _sin) ? this.proveedorId : proveedorId as String?,
        bodegaId: identical(bodegaId, _sin) ? this.bodegaId : bodegaId as String?,
        factura: factura ?? this.factura,
        fechaFactura: identical(fechaFactura, _sin) ? this.fechaFactura : fechaFactura as DateTime?,
        flete: flete ?? this.flete,
        descuentoGlobal: descuentoGlobal ?? this.descuentoGlobal,
        lineas: lineas ?? this.lineas,
        resultados: resultados ?? this.resultados,
        cargando: cargando ?? this.cargando,
        buscando: buscando ?? this.buscando,
        guardando: guardando ?? this.guardando,
        error: identical(error, _sin) ? this.error : error as String?,
        borradorId: identical(borradorId, _sin) ? this.borradorId : borradorId as String?,
      );

  static const _sin = Object();
}
