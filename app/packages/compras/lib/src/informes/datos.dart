import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-183 · Base, IVA y total.
class ImporteDeCompra {
  const ImporteDeCompra({this.base = 0, this.iva = 0, this.total = 0});

  final num base;
  final num iva;
  final num total;

  bool get esCero => base == 0 && iva == 0 && total == 0;

  factory ImporteDeCompra.desdeJson(Object? json) {
    final m = (json as Map?)?.cast<String, dynamic>() ?? const {};
    return ImporteDeCompra(
      base: (m['base'] as num?) ?? 0,
      iva: (m['iva'] as num?) ?? 0,
      total: (m['total'] as num?) ?? 0,
    );
  }
}

/// HU-183 C1 · Lo comprado a un proveedor en el periodo. `neto` = directas + con orden − devoluciones.
class CompraPorProveedorVista {
  const CompraPorProveedorVista({
    required this.proveedorId,
    required this.proveedor,
    this.recepciones = 0,
    this.directas = const ImporteDeCompra(),
    this.conOrden = const ImporteDeCompra(),
    this.devoluciones = const ImporteDeCompra(),
    this.neto = const ImporteDeCompra(),
  });

  final String proveedorId;
  final String proveedor;
  final int recepciones;
  final ImporteDeCompra directas;
  final ImporteDeCompra conOrden;
  final ImporteDeCompra devoluciones;
  final ImporteDeCompra neto;

  factory CompraPorProveedorVista.desdeJson(Map<String, dynamic> json) => CompraPorProveedorVista(
        proveedorId: json['proveedorId'] as String,
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        recepciones: (json['recepciones'] as num?)?.toInt() ?? 0,
        directas: ImporteDeCompra.desdeJson(json['directas']),
        conOrden: ImporteDeCompra.desdeJson(json['conOrden']),
        devoluciones: ImporteDeCompra.desdeJson(json['devoluciones']),
        neto: ImporteDeCompra.desdeJson(json['neto']),
      );
}

/// HU-183 C2 · Lo que entró de un producto en el periodo, neto de devoluciones.
class CompraPorProductoVista {
  const CompraPorProductoVista({
    required this.productoId,
    required this.producto,
    this.cantidad = 0,
    this.costoTotal = 0,
    this.costoPromedio,
  });

  final String productoId;
  final String producto;
  final num cantidad;
  final num costoTotal;
  final num? costoPromedio;

  factory CompraPorProductoVista.desdeJson(Map<String, dynamic> json) => CompraPorProductoVista(
        productoId: json['productoId'] as String,
        producto: (json['producto'] ?? 'Producto') as String,
        cantidad: (json['cantidad'] as num?) ?? 0,
        costoTotal: (json['costoTotal'] as num?) ?? 0,
        costoPromedio: json['costoPromedio'] as num?,
      );
}

/// HU-183 C3 · Una entrada del producto: cuándo, de quién, en qué recepción y a qué costo.
class CostoHistoricoVista {
  const CostoHistoricoVista({
    required this.fecha,
    required this.proveedor,
    required this.recepcionId,
    required this.recepcion,
    this.conOrden = false,
    this.cantidad = 0,
    this.costoEntrada = 0,
  });

  final DateTime fecha;
  final String proveedor;
  final String recepcionId;
  final String recepcion;
  final bool conOrden;
  final num cantidad;
  final num costoEntrada;

  factory CostoHistoricoVista.desdeJson(Map<String, dynamic> json) => CostoHistoricoVista(
        fecha: DateTime.parse(json['fecha'] as String).toLocal(),
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        recepcionId: json['recepcionId'] as String,
        recepcion: (json['recepcion'] ?? '') as String,
        conOrden: (json['conOrden'] as bool?) ?? false,
        cantidad: (json['cantidad'] as num?) ?? 0,
        costoEntrada: (json['costoEntrada'] as num?) ?? 0,
      );
}

/// Un periodo por días completos, los dos extremos incluidos.
class PeriodoDeInforme {
  const PeriodoDeInforme(this.desde, this.hasta);

  final DateTime desde;
  final DateTime hasta;

  /// Del primero del mes de [hoy] hasta [hoy].
  factory PeriodoDeInforme.esteMes(DateTime hoy) => PeriodoDeInforme(DateTime(hoy.year, hoy.month), _dia(hoy));

  /// El mes anterior al de [hoy], completo.
  factory PeriodoDeInforme.mesPasado(DateTime hoy) =>
      PeriodoDeInforme(DateTime(hoy.year, hoy.month - 1), DateTime(hoy.year, hoy.month, 0));

  static DateTime _dia(DateTime d) => DateTime(d.year, d.month, d.day);

  Map<String, String> get query => {'desde': fechaIso(desde), 'hasta': fechaIso(hasta)};

  @override
  bool operator ==(Object other) =>
      other is PeriodoDeInforme && _dia(other.desde) == _dia(desde) && _dia(other.hasta) == _dia(hasta);

  @override
  int get hashCode => Object.hash(_dia(desde), _dia(hasta));
}

/// «2026-11-05».
String fechaIso(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

/// «05/11/2026».
String fechaCorta(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

/// Los tres informes de la HU-183, con el nombre que usa la ruta del backend.
enum InformeDeCompras {
  porProveedor('por-proveedor', 'compras-por-proveedor'),
  porProducto('por-producto', 'compras-por-producto'),
  historialDeCostos('historial-de-costos', 'historial-de-costos');

  const InformeDeCompras(this.ruta, this.archivo);

  final String ruta;
  final String archivo;
}

/// Acceso a los informes de `compras` (HU-183). Todos exigen `COMPRAS_INFORME_VER`.
class RepositorioDeInformesDeCompras {
  RepositorioDeInformesDeCompras(this._http);

  static const _base = '/api/compras/informes';

  final ClienteHttp _http;

  Future<List<CompraPorProveedorVista>> porProveedor(PeriodoDeInforme periodo) async {
    final res = await _http.get<List<dynamic>>('$_base/por-proveedor', query: periodo.query);
    return [for (final e in res) CompraPorProveedorVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  Future<List<CompraPorProductoVista>> porProducto(PeriodoDeInforme periodo) async {
    final res = await _http.get<List<dynamic>>('$_base/por-producto', query: periodo.query);
    return [for (final e in res) CompraPorProductoVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  /// Todo el historial del producto, del más reciente al más antiguo.
  Future<List<CostoHistoricoVista>> historialDeCostos(String productoId) async {
    final res = await _http.get<List<dynamic>>('$_base/historial-de-costos', query: {'productoId': productoId});
    return [for (final e in res) CostoHistoricoVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  /// C4 · El informe en PDF o XLSX.
  Future<List<int>> archivo(InformeDeCompras informe, String formato,
      {PeriodoDeInforme? periodo, String? productoId}) {
    final query = Uri(queryParameters: {
      'formato': formato,
      if (periodo != null) ...periodo.query,
      'productoId': ?productoId,
    }).query;
    return _http.bytes('GET', '$_base/${informe.ruta}/archivo?$query');
  }
}

final repositorioDeInformesDeComprasProvider = Provider<RepositorioDeInformesDeCompras>(
  (ref) => throw UnimplementedError('repositorioDeInformesDeComprasProvider debe sobrescribirse en la app'),
);

/// HU-183 C5 · Si quien usa la app ve los informes de compras (`COMPRAS_INFORME_VER`).
final puedeVerInformesDeComprasProvider = Provider<bool>((ref) => false);

/// C4 · Guarda el archivo descargado: en web lo descarga; en el celular abre el diálogo de guardar.
abstract class GuardadorDeInformes {
  Future<void> guardar(String nombre, List<int> bytes);
}

class GuardadorConFilePicker implements GuardadorDeInformes {
  const GuardadorConFilePicker();

  @override
  Future<void> guardar(String nombre, List<int> bytes) async {
    await FilePicker.platform.saveFile(
      fileName: nombre,
      bytes: bytes is Uint8List ? bytes : Uint8List.fromList(bytes),
      type: FileType.custom,
      allowedExtensions: [nombre.split('.').last],
    );
  }
}

final guardadorDeInformesProvider = Provider<GuardadorDeInformes>((ref) => const GuardadorConFilePicker());
