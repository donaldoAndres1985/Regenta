import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

DateTime? _fecha(Object? v) => v == null ? null : DateTime.tryParse(v as String);
num _num(Object? v) => (v as num?) ?? 0;

/// Una línea de una recepción confirmada: lo que entró, de qué lote y a qué costo.
class LineaRecibidaVista {
  const LineaRecibidaVista({
    required this.id,
    required this.productoId,
    required this.nombre,
    required this.cantidad,
    this.codigoLote,
    this.costoEntrada = 0,
  });

  final String id;
  final String productoId;
  final String nombre;
  final num cantidad;
  final String? codigoLote;
  final num costoEntrada;

  factory LineaRecibidaVista.desdeJson(Map<String, dynamic> j) => LineaRecibidaVista(
        id: j['id'] as String,
        productoId: (j['productoId'] ?? '') as String,
        nombre: (j['nombre'] ?? 'Producto') as String,
        cantidad: _num(j['cantidad']),
        codigoLote: j['codigoLote'] as String?,
        costoEntrada: _num(j['costoEntrada']),
      );
}

/// Una recepción (contra orden o compra directa), para devolver o anular.
class RecepcionVista {
  const RecepcionVista({
    required this.id,
    required this.numero,
    required this.estado,
    required this.total,
    this.proveedorNombre,
    this.facturaProveedor,
    this.fecha,
    this.compraDirecta = false,
    this.motivoAnulacion,
    this.lineas = const [],
  });

  final String id;
  final String numero;
  final String estado;
  final num total;
  final String? proveedorNombre;
  final String? facturaProveedor;
  final DateTime? fecha;
  final bool compraDirecta;
  final String? motivoAnulacion;
  final List<LineaRecibidaVista> lineas;

  bool get confirmada => estado == 'CONFIRMADA';

  factory RecepcionVista.desdeJson(Map<String, dynamic> j) => RecepcionVista(
        id: j['id'] as String,
        numero: (j['numero'] ?? '') as String,
        estado: (j['estado'] ?? 'BORRADOR') as String,
        total: _num(j['total']),
        proveedorNombre: j['proveedorNombre'] as String?,
        facturaProveedor: j['facturaProveedor'] as String?,
        fecha: _fecha(j['fecha']),
        compraDirecta: (j['compraDirecta'] as bool?) ?? false,
        motivoAnulacion: j['motivoAnulacion'] as String?,
        lineas: [
          for (final l in (j['lineas'] as List<dynamic>? ?? const []))
            LineaRecibidaVista.desdeJson((l as Map).cast<String, dynamic>()),
        ],
      );
}

/// HU-178 · Una devolución hecha, con su nota crédito.
class DevolucionVista {
  const DevolucionVista({
    required this.id,
    required this.numero,
    required this.motivo,
    required this.valorNota,
    this.aplicadoACuenta = 0,
    this.saldoAFavor = 0,
    this.fecha,
    this.devueltoPorLinea = const {},
  });

  final String id;
  final String numero;
  final String motivo;
  final num valorNota;
  final num aplicadoACuenta;
  final num saldoAFavor;
  final DateTime? fecha;

  /// recepcionLineaId → cantidad devuelta.
  final Map<String, num> devueltoPorLinea;

  factory DevolucionVista.desdeJson(Map<String, dynamic> j) {
    final devuelto = <String, num>{};
    for (final l in (j['lineas'] as List<dynamic>? ?? const [])) {
      final m = (l as Map).cast<String, dynamic>();
      final id = m['recepcionLineaId'] as String;
      devuelto[id] = (devuelto[id] ?? 0) + _num(m['cantidad']);
    }
    return DevolucionVista(
      id: j['id'] as String,
      numero: (j['numero'] ?? '') as String,
      motivo: (j['motivo'] ?? '') as String,
      valorNota: _num(j['valorNota']),
      aplicadoACuenta: _num(j['aplicadoACuenta']),
      saldoAFavor: _num(j['saldoAFavor']),
      fecha: _fecha(j['fecha']),
      devueltoPorLinea: devuelto,
    );
  }
}

/// Recepciones, devoluciones al proveedor (HU-178) y anulación de recepciones (HU-177).
class RepositorioDeDevoluciones {
  RepositorioDeDevoluciones(this._http);

  final ClienteHttp _http;

  static const _recepciones = '/api/compras/recepciones';
  static const _devoluciones = '/api/compras/devoluciones';

  Future<List<RecepcionVista>> recepciones() async {
    final res = await _http.get<List<dynamic>>(_recepciones);
    return res.map((e) => RecepcionVista.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  Future<RecepcionVista> recepcion(String id) async =>
      RecepcionVista.desdeJson(await _http.get<Map<String, dynamic>>('$_recepciones/$id'));

  Future<List<DevolucionVista>> devolucionesDe(String recepcionId) async {
    final res = await _http.get<List<dynamic>>('$_devoluciones?recepcionId=$recepcionId');
    return res.map((e) => DevolucionVista.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  /// HU-178 · [cantidades]: recepcionLineaId → cuánto se devuelve.
  Future<DevolucionVista> devolver(String recepcionId, String motivo, Map<String, num> cantidades) async {
    final dynamic res = await _http.post<dynamic>(_devoluciones, datos: {
      'recepcionId': recepcionId,
      'motivo': motivo.trim(),
      'lineas': [
        for (final e in cantidades.entries)
          if (e.value > 0) {'recepcionLineaId': e.key, 'cantidad': e.value},
      ],
    });
    return DevolucionVista.desdeJson((res as Map).cast<String, dynamic>());
  }

  /// HU-177 · Anula una recepción mal registrada.
  Future<RecepcionVista> anular(String recepcionId, String motivo) async {
    final dynamic res =
        await _http.post<dynamic>('$_recepciones/$recepcionId/anulacion', datos: {'motivo': motivo.trim()});
    return RecepcionVista.desdeJson((res as Map).cast<String, dynamic>());
  }
}

final repositorioDeDevolucionesProvider = Provider<RepositorioDeDevoluciones>((ref) {
  throw UnimplementedError('Sobrescribe repositorioDeDevolucionesProvider al montar el ProviderScope.');
});
