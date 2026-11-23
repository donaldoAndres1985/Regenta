import 'dart:math';

import 'package:regenta_core/regenta_core.dart';

import 'orden_recibible.dart';
import 'orden_resumen_vista.dart';
import '../recepcion/factura_de_recepcion.dart';
import '../recepcion/precio_actual.dart';

/// Una línea que se va a mandar al backend al registrar la recepción.
class LineaParaRecibir {
  const LineaParaRecibir({
    required this.ordenLineaId,
    required this.cantidad,
    required this.exigeLote,
    this.costoUnitario,
    this.codigoLote,
    this.fechaVencimiento,
  });

  final String ordenLineaId;
  final num cantidad;
  final bool exigeLote;
  final num? costoUnitario;
  final String? codigoLote;
  final DateTime? fechaVencimiento;

  Map<String, dynamic> aJson() => {
        'ordenLineaId': ordenLineaId,
        'cantidad': cantidad,
        'exigeLote': exigeLote,
        if (costoUnitario != null) 'costoUnitario': costoUnitario,
        if (codigoLote != null && codigoLote!.isNotEmpty) 'codigoLote': codigoLote,
        if (fechaVencimiento != null)
          'fechaVencimiento':
              fechaVencimiento!.toIso8601String().substring(0, 10),
      };
}

/// El resultado de registrar la recepción: confirmada en el servidor, o
/// encolada para subir cuando vuelva la señal (criterio 4).
class ResultadoDeRecepcion {
  const ResultadoDeRecepcion.confirmada(this.numero)
      : encolada = false,
        idDispositivo = null;
  const ResultadoDeRecepcion.encolada({this.idDispositivo})
      : numero = null,
        encolada = true;

  final String? numero;
  final bool encolada;

  /// HU-182 · Con qué identificador quedó en el celular, «por enviar».
  final String? idDispositivo;
}

/// La ruta con que sube una recepción armada en el celular (HU-182 · I32).
const rutaDeRecepcionDesdeElCelular = '/api/compras/recepciones/dispositivo/';

/// Acceso a `servicio-compras` para la recepción móvil. Se apoya en el
/// [ClienteHttp] del núcleo. HU-182: la recepción sube de una vez (se crea y se
/// confirma) con un identificador del celular; sin red queda en la cola con ese
/// identificador, «por enviar», y reenviarla no la duplica.
class RepositorioDeRecepciones {
  RepositorioDeRecepciones(this._http, {ColaDeSalidaLocal? cola}) : _cola = cola;

  final ClienteHttp _http;

  /// Para sacar de la cola una recepción corregida que por fin entró.
  final ColaDeSalidaLocal? _cola;

  Future<List<OrdenResumenVista>> listarOrdenes() async {
    final res = await _http.get<List<dynamic>>('/api/compras/ordenes', conRespaldo: true);
    return res
        .map((e) => OrdenResumenVista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// HU-179 · El costo promedio y el precio de los productos en la bodega, para mostrar el margen.
  Future<Map<String, PrecioActual>> preciosActuales(String bodegaId, Iterable<String> productoIds) async {
    final ids = productoIds.toSet();
    if (ids.isEmpty) return const {};
    final consulta = [for (final id in ids) 'productoId=${Uri.encodeComponent(id)}'].join('&');
    final res = await _http.get<List<dynamic>>(
        '/api/compras/recepciones/precios-actuales?bodegaId=${Uri.encodeComponent(bodegaId)}&$consulta');
    return {
      for (final e in res)
        (e as Map)['productoId'] as String: PrecioActual.desdeJson(e.cast<String, dynamic>()),
    };
  }

  Future<OrdenRecibible> verOrden(String ordenId) async {
    // HU-182 · Sin señal en la bodega, la orden como se vio la última vez.
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/compras/ordenes/${Uri.encodeComponent(ordenId)}',
      conRespaldo: true,
    );
    return OrdenRecibible.desdeJson(cuerpo);
  }

  /// HU-182 · Crea y confirma la recepción de una vez, con el identificador del celular
  /// ([idDispositivo]). Sin red queda en la cola con ese identificador y devuelve
  /// [ResultadoDeRecepcion.encolada]; el backend no la duplica aunque llegue dos veces.
  Future<ResultadoDeRecepcion> registrar({
    required String ordenId,
    required String bodegaId,
    String? facturaProveedor,
    FacturaDeRecepcion? factura,
    required List<LineaParaRecibir> lineas,
    Map<String, num> preciosNuevos = const {},
    String? idDispositivo,
    String? ordenNumero,
  }) async {
    final id = idDispositivo ?? nuevoIdDeDispositivo();
    final ruta = '$rutaDeRecepcionDesdeElCelular$id';
    final respuesta = await _http.put<dynamic>(
      ruta,
      encolable: true,
      claveDeCola: id,
      datos: {
        'recepcion': {
          'ordenId': ordenId,
          'bodegaId': bodegaId,
          if (facturaProveedor != null && facturaProveedor.isNotEmpty) 'facturaProveedor': facturaProveedor,
          ...?factura?.aJson(),
          'lineas': [for (final l in lineas) l.aJson()],
        },
        // HU-179 · el precio de venta nuevo (IVA incluido) de lo que llegó; sin él, no cambia.
        if (preciosNuevos.isNotEmpty)
          'preciosNuevos': [
            for (final e in preciosNuevos.entries) {'productoId': e.key, 'precioVenta': e.value},
          ],
        // Solo para la lista «por enviar» del celular; el backend lo ignora.
        if (ordenNumero != null) 'enElCelular': {'ordenNumero': ordenNumero, 'lineas': lineas.length},
      },
    );
    if (respuesta is Encolado) {
      return ResultadoDeRecepcion.encolada(idDispositivo: id);
    }
    // Si era una corregida que estaba en la cola con error, ya entró.
    await _cola?.quitar(id);
    final numero = ((respuesta as Map)['numero'] ?? '') as String;
    return ResultadoDeRecepcion.confirmada(numero);
  }
}

/// Un identificador nuevo para una recepción armada en el celular (UUID v4).
String nuevoIdDeDispositivo() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
