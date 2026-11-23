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

/// Los informes de compras (HU-183 y HU-184), en el orden de las pestañas, con el nombre que usa la
/// ruta del backend y el del archivo descargado.
enum InformeDeCompras {
  porProveedor('por-proveedor', 'compras-por-proveedor', conPeriodo: true),
  porProducto('por-producto', 'compras-por-producto', conPeriodo: true),
  historialDeCostos('historial-de-costos', 'historial-de-costos'),
  antiguedad('antiguedad', 'cuentas-por-pagar-por-antiguedad'),
  pendientesPorRecibir('pendientes-por-recibir', 'pendientes-por-recibir'),
  faltantes('faltantes', 'faltantes-de-ordenes-cerradas'),
  pagos('pagos', 'pagos-a-proveedores', conPeriodo: true);

  const InformeDeCompras(this.ruta, this.archivo, {this.conPeriodo = false});

  final String ruta;
  final String archivo;

  /// Si se consulta por un periodo (los demás van a hoy, o completos).
  final bool conPeriodo;
}

num _n(Object? v) => (v as num?) ?? 0;

List<Map<String, dynamic>> _lista(Object? v) =>
    [for (final e in (v as List<dynamic>? ?? const [])) (e as Map).cast<String, dynamic>()];

/// HU-184 C1 · Un saldo repartido por días de mora.
class TramosDeMora {
  const TramosDeMora({this.alDia = 0, this.de1a30 = 0, this.de31a60 = 0, this.de61a90 = 0, this.masDe90 = 0, this.total = 0});

  final num alDia;
  final num de1a30;
  final num de31a60;
  final num de61a90;
  final num masDe90;
  final num total;

  /// Las cinco columnas, en orden, con su título.
  List<(String, num)> get columnas => [
        ('Al día', alDia),
        ('1-30', de1a30),
        ('31-60', de31a60),
        ('61-90', de61a90),
        ('+90', masDe90),
      ];

  factory TramosDeMora.desdeJson(Object? json) {
    final m = (json as Map?)?.cast<String, dynamic>() ?? const {};
    return TramosDeMora(
      alDia: _n(m['alDia']),
      de1a30: _n(m['de1a30']),
      de31a60: _n(m['de31a60']),
      de61a90: _n(m['de61a90']),
      masDe90: _n(m['masDe90']),
      total: _n(m['total']),
    );
  }
}

/// HU-184 C1 · Lo que se le debe a un proveedor, por antigüedad.
class AntiguedadDeProveedorVista {
  const AntiguedadDeProveedorVista({
    required this.proveedorId,
    required this.proveedor,
    this.cuentas = 0,
    this.moraMaxima = 0,
    this.saldo = const TramosDeMora(),
  });

  final String proveedorId;
  final String proveedor;
  final int cuentas;
  final int moraMaxima;
  final TramosDeMora saldo;

  factory AntiguedadDeProveedorVista.desdeJson(Map<String, dynamic> json) => AntiguedadDeProveedorVista(
        proveedorId: json['proveedorId'] as String,
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        cuentas: _n(json['cuentas']).toInt(),
        moraMaxima: _n(json['moraMaxima']).toInt(),
        saldo: TramosDeMora.desdeJson(json['saldo']),
      );
}

/// HU-184 C1 · Una fila por proveedor y el total de cada columna.
class InformeDeAntiguedadVista {
  const InformeDeAntiguedadVista({this.proveedores = const [], this.totales = const TramosDeMora()});

  final List<AntiguedadDeProveedorVista> proveedores;
  final TramosDeMora totales;

  factory InformeDeAntiguedadVista.desdeJson(Map<String, dynamic> json) => InformeDeAntiguedadVista(
        proveedores: [for (final p in _lista(json['proveedores'])) AntiguedadDeProveedorVista.desdeJson(p)],
        totales: TramosDeMora.desdeJson(json['totales']),
      );
}

/// HU-184 C2 / C3 · Una línea de orden con lo que falta; `valor` sin IVA.
class LineaPendienteVista {
  const LineaPendienteVista({
    required this.linea,
    required this.producto,
    this.pedida = 0,
    this.recibida = 0,
    this.faltante = 0,
    this.valor = 0,
  });

  final int linea;
  final String producto;
  final num pedida;
  final num recibida;
  final num faltante;
  final num valor;

  factory LineaPendienteVista.desdeJson(Map<String, dynamic> json) => LineaPendienteVista(
        linea: _n(json['linea']).toInt(),
        producto: (json['producto'] ?? 'Producto') as String,
        pedida: _n(json['pedida']),
        recibida: _n(json['recibida']),
        faltante: _n(json['faltante']),
        valor: _n(json['valor']),
      );
}

/// HU-184 C2 · Una orden con mercancía por llegar.
class PendientePorRecibirVista {
  const PendientePorRecibirVista({
    required this.ordenId,
    required this.numero,
    required this.proveedor,
    this.estado = '',
    this.fechaEsperada,
    this.atrasada = false,
    this.diasDeAtraso = 0,
    this.lineas = const [],
    this.valorPendiente = 0,
  });

  final String ordenId;
  final String numero;
  final String proveedor;
  final String estado;
  final DateTime? fechaEsperada;
  final bool atrasada;
  final int diasDeAtraso;
  final List<LineaPendienteVista> lineas;
  final num valorPendiente;

  factory PendientePorRecibirVista.desdeJson(Map<String, dynamic> json) => PendientePorRecibirVista(
        ordenId: json['ordenId'] as String,
        numero: (json['numero'] ?? '') as String,
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        estado: (json['estado'] ?? '') as String,
        fechaEsperada: json['fechaEsperada'] == null ? null : DateTime.parse(json['fechaEsperada'] as String),
        atrasada: (json['atrasada'] as bool?) ?? false,
        diasDeAtraso: _n(json['diasDeAtraso']).toInt(),
        lineas: [for (final l in _lista(json['lineas'])) LineaPendienteVista.desdeJson(l)],
        valorPendiente: _n(json['valorPendiente']),
      );
}

/// HU-184 C3 · Una orden cerrada con faltante y lo que nunca llegó.
class FaltanteDeOrdenVista {
  const FaltanteDeOrdenVista({
    required this.ordenId,
    required this.numero,
    required this.proveedor,
    this.cerradaEn,
    this.motivo,
    this.lineas = const [],
    this.valorFaltante = 0,
  });

  final String ordenId;
  final String numero;
  final String proveedor;
  final DateTime? cerradaEn;
  final String? motivo;
  final List<LineaPendienteVista> lineas;
  final num valorFaltante;

  factory FaltanteDeOrdenVista.desdeJson(Map<String, dynamic> json) => FaltanteDeOrdenVista(
        ordenId: json['ordenId'] as String,
        numero: (json['numero'] ?? '') as String,
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        cerradaEn: json['cerradaEn'] == null ? null : DateTime.parse(json['cerradaEn'] as String).toLocal(),
        motivo: json['motivo'] as String?,
        lineas: [for (final l in _lista(json['lineas'])) LineaPendienteVista.desdeJson(l)],
        valorFaltante: _n(json['valorFaltante']),
      );
}

/// HU-184 C4 · Lo pagado a un proveedor con un método.
class PagosPorProveedorYMetodoVista {
  const PagosPorProveedorYMetodoVista({
    required this.proveedorId,
    required this.proveedor,
    required this.metodo,
    this.pagos = 0,
    this.total = 0,
  });

  final String proveedorId;
  final String proveedor;
  final String metodo;
  final int pagos;
  final num total;

  factory PagosPorProveedorYMetodoVista.desdeJson(Map<String, dynamic> json) => PagosPorProveedorYMetodoVista(
        proveedorId: json['proveedorId'] as String,
        proveedor: (json['proveedor'] ?? 'Proveedor') as String,
        metodo: (json['metodo'] ?? 'OTRO') as String,
        pagos: _n(json['pagos']).toInt(),
        total: _n(json['total']),
      );
}

/// HU-184 C4 · Los pagos del periodo: por proveedor y método, totales por método y total.
class InformeDePagosVista {
  const InformeDePagosVista({this.filas = const [], this.porMetodo = const {}, this.total = 0});

  final List<PagosPorProveedorYMetodoVista> filas;
  final Map<String, num> porMetodo;
  final num total;

  factory InformeDePagosVista.desdeJson(Map<String, dynamic> json) => InformeDePagosVista(
        filas: [for (final f in _lista(json['filas'])) PagosPorProveedorYMetodoVista.desdeJson(f)],
        porMetodo: {
          for (final e in ((json['porMetodo'] as Map?) ?? const {}).entries) e.key as String: _n(e.value),
        },
        total: _n(json['total']),
      );
}

/// «Transferencia» para `TRANSFERENCIA`.
String nombreDelMetodo(String metodo) => switch (metodo) {
      'EFECTIVO' => 'Efectivo',
      'TRANSFERENCIA' => 'Transferencia',
      'CHEQUE' => 'Cheque',
      'TARJETA' => 'Tarjeta',
      _ => 'Otro',
    };

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

  /// HU-184 C1 · Lo que se debe a hoy, por días de mora.
  Future<InformeDeAntiguedadVista> antiguedad() async {
    final res = await _http.get<Map<String, dynamic>>('$_base/antiguedad');
    return InformeDeAntiguedadVista.desdeJson(res);
  }

  /// HU-184 C2 · Lo que falta por recibir de las órdenes abiertas; las atrasadas primero.
  Future<List<PendientePorRecibirVista>> pendientesPorRecibir() async {
    final res = await _http.get<List<dynamic>>('$_base/pendientes-por-recibir');
    return [for (final e in res) PendientePorRecibirVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  /// HU-184 C3 · Lo que nunca llegó de las órdenes cerradas con faltante.
  Future<List<FaltanteDeOrdenVista>> faltantes() async {
    final res = await _http.get<List<dynamic>>('$_base/faltantes');
    return [for (final e in res) FaltanteDeOrdenVista.desdeJson((e as Map).cast<String, dynamic>())];
  }

  /// HU-184 C4 · Los pagos del periodo por proveedor y método, sin los anulados.
  Future<InformeDePagosVista> pagos(PeriodoDeInforme periodo) async {
    final res = await _http.get<Map<String, dynamic>>('$_base/pagos', query: periodo.query);
    return InformeDePagosVista.desdeJson(res);
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
