import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_recepciones.dart';
import 'factura_de_recepcion.dart';

/// HU-182 · Una recepción armada en el celular que todavía no entró: «por enviar» (esperando
/// señal) o «con error» (el backend la rechazó y hay que corregirla, I32).
class RecepcionPorEnviar {
  const RecepcionPorEnviar({
    required this.id,
    required this.ordenId,
    required this.guardadaEn,
    required this.datos,
    this.ordenNumero,
    this.lineas = 0,
    this.conError = false,
    this.error,
  });

  /// El identificador que le puso el celular.
  final String id;
  final String ordenId;
  final String? ordenNumero;
  final int lineas;
  final DateTime guardadaEn;
  final bool conError;
  final String? error;

  /// Lo que se mandará: `{recepcion, preciosNuevos?, enElCelular?}`.
  final Map<String, dynamic> datos;

  Map<String, dynamic> get recepcion => ((datos['recepcion'] as Map?) ?? const {}).cast<String, dynamic>();

  /// Lo recibido por línea de la orden (ordenLineaId → la línea como se guardó).
  Map<String, Map<String, dynamic>> get lineasRecibidas => {
        for (final l in (recepcion['lineas'] as List<dynamic>? ?? const []))
          ((l as Map)['ordenLineaId'] as String): l.cast<String, dynamic>(),
      };

  FacturaDeRecepcion get factura => FacturaDeRecepcion.desdeJson(recepcion);

  Map<String, num> get preciosNuevos => {
        for (final p in (datos['preciosNuevos'] as List<dynamic>? ?? const []))
          ((p as Map)['productoId'] as String): p['precioVenta'] as num,
      };

  static RecepcionPorEnviar? deOperacion(OperacionEnCola op) {
    final datos = op.datos;
    if (datos is! Map) return null;
    final mapa = datos.cast<String, dynamic>();
    final recepcion = (mapa['recepcion'] as Map?)?.cast<String, dynamic>();
    final ordenId = recepcion?['ordenId'] as String?;
    if (ordenId == null) return null;
    final enElCelular = (mapa['enElCelular'] as Map?)?.cast<String, dynamic>() ?? const {};
    return RecepcionPorEnviar(
      id: op.id,
      ordenId: ordenId,
      ordenNumero: enElCelular['ordenNumero'] as String?,
      lineas: (enElCelular['lineas'] as num?)?.toInt() ?? (recepcion!['lineas'] as List? ?? const []).length,
      guardadaEn: op.creadoEn.toLocal(),
      datos: mapa,
      conError: op.conError,
      error: op.ultimoError,
    );
  }
}

/// HU-182 · Las recepciones del celular que esperan en la cola, con lo que se puede hacer con ellas.
class BandejaDeRecepciones {
  BandejaDeRecepciones({required ClienteHttp http, required ColaDeSalidaLocal cola})
      : _http = http,
        _cola = cola;

  final ClienteHttp _http;
  final ColaDeSalidaLocal _cola;

  Stream<List<RecepcionPorEnviar>> observar() => _cola
      .observar(prefijo: rutaDeRecepcionDesdeElCelular)
      .map((ops) => ops.map(RecepcionPorEnviar.deOperacion).whereType<RecepcionPorEnviar>().toList());

  /// La manda ya. Devuelve `null` si entró, o por qué no: sin señal sigue «por enviar»; si el
  /// backend la rechaza, queda «con error» con su motivo (C3).
  Future<String?> enviarAhora(RecepcionPorEnviar r) async {
    try {
      await _http.put<dynamic>('$rutaDeRecepcionDesdeElCelular${r.id}', datos: r.datos);
    } on ErrorDeRed {
      return 'Todavía no hay señal: sigue guardada y subirá sola.';
    } on ErrorDeApi catch (fallo) {
      await _cola.marcarRechazada(r.id, fallo);
      return motivoDelRechazo(fallo);
    }
    await _cola.quitar(r.id);
    return null;
  }

  Future<void> descartar(String id) => _cola.quitar(id);
}

/// La app lo sobrescribe con la bandeja de verdad (sobre la cola local). Nulo: no hay cola, y la
/// pantalla de Compras no muestra el aviso.
final bandejaDeRecepcionesProvider = Provider<BandejaDeRecepciones?>((ref) => null);

final recepcionesPorEnviarProvider = StreamProvider.autoDispose<List<RecepcionPorEnviar>>((ref) {
  final bandeja = ref.watch(bandejaDeRecepcionesProvider);
  return bandeja == null ? Stream.value(const []) : bandeja.observar();
});
