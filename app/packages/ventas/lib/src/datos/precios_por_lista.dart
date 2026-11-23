import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

num _n(Object? v) => (v as num?) ?? 0;

List<Map<String, dynamic>> _lista(Object? v) =>
    [for (final e in (v as List<dynamic>? ?? const [])) (e as Map).cast<String, dynamic>()];

/// HU-132 · Una línea del carrito que cambiaría de precio con la lista del cliente.
class CambioDePrecioVista {
  const CambioDePrecioVista({
    required this.linea,
    required this.producto,
    this.cantidad = 0,
    this.precioActual = 0,
    this.precioNuevo = 0,
  });

  /// La posición de la línea en el carrito.
  final int linea;
  final String producto;
  final num cantidad;
  final num precioActual;
  final num precioNuevo;

  factory CambioDePrecioVista.desdeJson(Map<String, dynamic> json) => CambioDePrecioVista(
        linea: _n(json['linea']).toInt(),
        producto: (json['producto'] ?? 'Producto') as String,
        cantidad: _n(json['cantidad']),
        precioActual: _n(json['precioActual']),
        precioNuevo: _n(json['precioNuevo']),
      );
}

/// HU-132 · Lo que pasaría con los precios del carrito al asignar un cliente (C2: se avisa antes).
class PrevisionDePreciosVista {
  const PrevisionDePreciosVista({
    this.listaActualId,
    this.listaNuevaId,
    this.listaNuevaNombre,
    this.cambios = const [],
    this.conDescuentoAMano = 0,
    this.conPrecioEspecial = 0,
    this.totalActual = 0,
    this.totalNuevo = 0,
  });

  final String? listaActualId;

  /// `null`: los precios del negocio (catálogo).
  final String? listaNuevaId;
  final String? listaNuevaNombre;
  final List<CambioDePrecioVista> cambios;

  /// C3 · Líneas que no se tocan.
  final int conDescuentoAMano;
  final int conPrecioEspecial;
  final num totalActual;
  final num totalNuevo;

  bool get hayCambios => cambios.isNotEmpty;

  /// Si el carrito pasaría a otra lista (aunque no cambie ningún precio, p. ej. vacío).
  bool get cambiaLaLista => listaActualId != listaNuevaId;

  factory PrevisionDePreciosVista.desdeJson(Map<String, dynamic> json) {
    final actual = (json['listaActual'] as Map?)?.cast<String, dynamic>() ?? const {};
    final nueva = (json['listaNueva'] as Map?)?.cast<String, dynamic>() ?? const {};
    final sinCambio = _lista(json['sinCambio']);
    return PrevisionDePreciosVista(
      listaActualId: actual['id'] as String?,
      listaNuevaId: nueva['id'] as String?,
      listaNuevaNombre: nueva['nombre'] as String?,
      cambios: [for (final c in _lista(json['cambios'])) CambioDePrecioVista.desdeJson(c)],
      conDescuentoAMano: sinCambio.where((s) => s['motivo'] == 'DESCUENTO_A_MANO').length,
      conPrecioEspecial: sinCambio.where((s) => s['motivo'] == 'PRECIO_ESPECIAL').length,
      totalActual: _n(json['totalActual']),
      totalNuevo: _n(json['totalNuevo']),
    );
  }
}

/// Una línea del carrito tal como se manda a prever: el precio que muestra hoy y su descuento a mano.
class LineaAPreciar {
  const LineaAPreciar({
    required this.linea,
    required this.productoId,
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    this.descuentoPct = 0,
  });

  final int linea;
  final String productoId;
  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num descuentoPct;

  Map<String, dynamic> aJson() => {
        'linea': linea,
        'productoId': productoId,
        'nombre': nombre,
        'cantidad': cantidad,
        'precioUnitario': precioUnitario,
        'descuentoPct': descuentoPct,
      };
}

/// HU-132 · Los precios del carrito según la lista del cliente (`/api/ventas/precios`). El backend
/// sigue decidiendo el precio (HU-148): aquí solo se pregunta.
class RepositorioDePreciosDeVenta {
  RepositorioDePreciosDeVenta(this._http);

  final ClienteHttp _http;

  /// `clienteId` nulo: volver a consumidor final. `listaPreciosId`: con la que está el carrito.
  Future<PrevisionDePreciosVista> prever({
    String? clienteId,
    String? listaPreciosId,
    required List<LineaAPreciar> lineas,
  }) async {
    final res = await _http.post<Map<String, dynamic>>('/api/ventas/precios/prevision', datos: {
      'clienteId': clienteId,
      'listaPreciosId': listaPreciosId,
      'lineas': [for (final l in lineas) l.aJson()],
    });
    return PrevisionDePreciosVista.desdeJson(res as Map<String, dynamic>);
  }

  /// El precio de un producto con la lista del carrito, para esa cantidad (tramo por volumen).
  Future<num> precio({required String listaPreciosId, required String productoId, required num cantidad}) async {
    final res = await _http.post<List<dynamic>>('/api/ventas/precios', datos: {
      'listaPreciosId': listaPreciosId,
      'productos': [
        {'productoId': productoId, 'cantidad': cantidad},
      ],
    });
    final lista = res as List<dynamic>;
    return _n((lista.first as Map)['precio']);
  }
}

/// La conecta la app. Nula (pruebas, módulo sin backend): el POS no recalcula precios por cliente.
final repositorioDePreciosDeVentaProvider = Provider<RepositorioDePreciosDeVenta?>((ref) => null);
