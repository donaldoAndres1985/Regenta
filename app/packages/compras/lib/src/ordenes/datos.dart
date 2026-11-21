import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:printing/printing.dart';
import 'package:regenta_core/regenta_core.dart';

/// Los estados de una orden de compra (I7) con el nombre que ve el usuario.
const estadosDeOrden = <String, String>{
  'BORRADOR': 'Borrador',
  'APROBADA': 'Aprobada',
  'ENVIADA': 'Enviada',
  'PARCIAL': 'Parcial',
  'RECIBIDA': 'Recibida',
  'CERRADA': 'Cerrada',
  'ANULADA': 'Anulada',
};

/// Una línea de la orden: lo pedido, lo recibido y lo que falta (HU-174 C5).
class LineaDeOrdenVista {
  const LineaDeOrdenVista({
    required this.productoId,
    required this.nombre,
    required this.cantidadPedida,
    required this.cantidadRecibida,
    required this.faltante,
    required this.costoUnitario,
    this.codigoProveedor,
    this.descuentoPct = 0,
    this.impuestoPct = 0,
    this.total = 0,
  });

  final String productoId;
  final String nombre;
  final String? codigoProveedor;
  final num cantidadPedida;
  final num cantidadRecibida;
  final num faltante;
  final num costoUnitario;
  final num descuentoPct;
  final num impuestoPct;
  final num total;

  factory LineaDeOrdenVista.desdeJson(Map<String, dynamic> j) => LineaDeOrdenVista(
        productoId: j['productoId'] as String,
        nombre: (j['nombre'] ?? '') as String,
        codigoProveedor: j['codigoProveedor'] as String?,
        cantidadPedida: (j['cantidadPedida'] as num?) ?? 0,
        cantidadRecibida: (j['cantidadRecibida'] as num?) ?? 0,
        faltante: (j['faltante'] as num?) ?? 0,
        costoUnitario: (j['costoUnitario'] as num?) ?? 0,
        descuentoPct: (j['descuentoPct'] as num?) ?? 0,
        impuestoPct: (j['impuestoPct'] as num?) ?? 0,
        total: (j['total'] as num?) ?? 0,
      );
}

/// Una orden de compra como la devuelve `GET /api/compras/ordenes` (HU-174).
class OrdenDeCompraVista {
  const OrdenDeCompraVista({
    required this.id,
    required this.numero,
    required this.proveedorId,
    required this.estado,
    required this.total,
    this.proveedorNombre,
    this.bodegaId,
    this.fechaEmision,
    this.fechaEsperada,
    this.subtotal = 0,
    this.impuestoTotal = 0,
    this.flete = 0,
    this.observaciones,
    this.aprobacionAutomatica = false,
    this.esperaAprobacion = false,
    this.motivoAnulacion,
    this.anuladaEn,
    this.motivoCierre,
    this.cerradaEn,
    this.lineas = const [],
  });

  final String id;
  final String numero;
  final String proveedorId;
  final String? proveedorNombre;
  final String? bodegaId;
  final String estado;
  final DateTime? fechaEmision;
  final DateTime? fechaEsperada;
  final num subtotal;
  final num impuestoTotal;
  final num flete;
  final num total;
  final String? observaciones;
  final bool aprobacionAutomatica;
  final bool esperaAprobacion;

  /// HU-176 · Por qué y cuándo se anuló o se cerró con faltante.
  final String? motivoAnulacion;
  final DateTime? anuladaEn;
  final String? motivoCierre;
  final DateTime? cerradaEn;
  final List<LineaDeOrdenVista> lineas;

  String get estadoVisible => esperaAprobacion ? 'Por aprobar' : (estadosDeOrden[estado] ?? estado);

  bool get editable => estado == 'BORRADOR';

  /// Se le puede mandar al proveedor y recibir: ya está aprobada y no se cerró.
  bool get enCurso => estado == 'APROBADA' || estado == 'ENVIADA' || estado == 'PARCIAL';

  /// HU-176 C1 · Se anula mientras no haya recibido nada.
  bool get anulable =>
      (estado == 'BORRADOR' || estado == 'APROBADA' || estado == 'ENVIADA') &&
      lineas.every((l) => l.cantidadRecibida == 0);

  /// HU-176 C3 · Llegó en parte: se cierra con lo que faltó.
  bool get cerrable => estado == 'PARCIAL';

  static DateTime? _fecha(Object? v) => v == null ? null : DateTime.tryParse(v as String);

  factory OrdenDeCompraVista.desdeJson(Map<String, dynamic> j) => OrdenDeCompraVista(
        id: j['id'] as String,
        numero: (j['numero'] ?? '') as String,
        proveedorId: (j['proveedorId'] ?? '') as String,
        proveedorNombre: j['proveedorNombre'] as String?,
        bodegaId: j['bodegaDestinoId'] as String?,
        estado: (j['estado'] ?? 'BORRADOR') as String,
        fechaEmision: _fecha(j['fechaEmision']),
        fechaEsperada: _fecha(j['fechaEsperada']),
        subtotal: (j['subtotal'] as num?) ?? 0,
        impuestoTotal: (j['impuestoTotal'] as num?) ?? 0,
        flete: (j['flete'] as num?) ?? 0,
        total: (j['total'] as num?) ?? 0,
        observaciones: j['observaciones'] as String?,
        aprobacionAutomatica: (j['aprobacionAutomatica'] as bool?) ?? false,
        esperaAprobacion: (j['esperaAprobacion'] as bool?) ?? false,
        motivoAnulacion: j['motivoAnulacion'] as String?,
        anuladaEn: _fecha(j['anuladaEn']),
        motivoCierre: j['motivoCierre'] as String?,
        cerradaEn: _fecha(j['cerradaEn']),
        lineas: [
          for (final l in (j['lineas'] as List<dynamic>? ?? const []))
            LineaDeOrdenVista.desdeJson((l as Map).cast<String, dynamic>()),
        ],
      );
}

/// Acceso a las órdenes de compra de `compras` (HU-047, HU-173, HU-174).
class RepositorioDeOrdenes {
  RepositorioDeOrdenes(this._http);

  static const _base = '/api/compras/ordenes';

  final ClienteHttp _http;

  OrdenDeCompraVista _leer(dynamic res) => OrdenDeCompraVista.desdeJson((res as Map).cast<String, dynamic>());

  Future<List<OrdenDeCompraVista>> listar() async {
    final res = await _http.get<List<dynamic>>(_base);
    return [for (final e in res) _leer(e)];
  }

  Future<OrdenDeCompraVista> ver(String id) async => _leer(await _http.get<dynamic>('$_base/$id'));

  /// Crea la orden en borrador. Las líneas llevan producto, cantidad y, si se cambió, el costo.
  Future<OrdenDeCompraVista> crear(Map<String, dynamic> orden) async =>
      _leer(await _http.post<dynamic>(_base, datos: orden));

  Future<OrdenDeCompraVista> editarLineas(String id, List<Map<String, dynamic>> lineas) async =>
      _leer(await _http.put<dynamic>('$_base/$id/lineas', datos: lineas));

  /// HU-173 · Se aprueba sola si el negocio lo permite; si no, queda esperando aprobación.
  Future<OrdenDeCompraVista> confirmar(String id) async => _leer(await _http.post<dynamic>('$_base/$id/confirmacion'));

  Future<OrdenDeCompraVista> aprobar(String id) async => _leer(await _http.post<dynamic>('$_base/$id/aprobacion'));

  /// HU-174 C4 · Marca la orden como enviada al proveedor.
  Future<OrdenDeCompraVista> enviar(String id) async => _leer(await _http.post<dynamic>('$_base/$id/envio'));

  /// HU-176 · Anular una orden que no va (sin mercancía recibida).
  Future<OrdenDeCompraVista> anular(String id, String motivo) async =>
      _leer(await _http.post<dynamic>('$_base/$id/anulacion', datos: {'motivo': motivo}));

  /// HU-176 · Cerrar con faltante una orden recibida en parte.
  Future<OrdenDeCompraVista> cerrar(String id, String motivo) async =>
      _leer(await _http.post<dynamic>('$_base/$id/cierre', datos: {'motivo': motivo}));

  /// HU-174 C4 · El PDF de la orden para el proveedor.
  Future<List<int>> pdf(String id) => _http.bytes('GET', '$_base/$id/pdf');
}

/// Cómo sale el PDF del teléfono o del navegador: en el celular abre «Compartir» (WhatsApp, correo…);
/// en la web lo descarga. Los tests lo reemplazan.
abstract class CompartidorDePdf {
  Future<void> compartir(List<int> bytes, String nombreDeArchivo);
}

class CompartidorConPrinting implements CompartidorDePdf {
  const CompartidorConPrinting();

  @override
  Future<void> compartir(List<int> bytes, String nombreDeArchivo) =>
      Printing.sharePdf(bytes: Uint8ListDe(bytes).valor, filename: nombreDeArchivo);
}

/// Pequeño adaptador para no exigir `Uint8List` a quien llama.
class Uint8ListDe {
  Uint8ListDe(List<int> bytes) : valor = bytes is Uint8List ? bytes : Uint8List.fromList(bytes);

  final Uint8List valor;
}

final repositorioDeOrdenesProvider = Provider<RepositorioDeOrdenes>((ref) {
  throw UnimplementedError('Sobrescribe repositorioDeOrdenesProvider al montar el ProviderScope.');
});

final compartidorDePdfProvider = Provider<CompartidorDePdf>((ref) => const CompartidorConPrinting());
