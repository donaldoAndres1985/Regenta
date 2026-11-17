import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-163 · lo que se puede abrir para conseguir un producto que falta: 1 [origen] da
/// [factor] [destino].
class OfertaDeDetalladoPos {
  const OfertaDeDetalladoPos({
    required this.origenId,
    required this.origen,
    required this.destino,
    required this.factor,
  });

  final String origenId;
  final String origen;
  final String destino;
  final num factor;

  factory OfertaDeDetalladoPos.desdeJson(Map<String, dynamic> j) => OfertaDeDetalladoPos(
        origenId: j['origenId'] as String,
        origen: (j['origen'] ?? '') as String,
        destino: (j['destino'] ?? '') as String,
        factor: (j['factor'] ?? 0) as num,
      );
}

/// HU-163 · el POS pregunta qué abrir y detalla (los endpoints de inventario, con
/// `INVENTARIO_DETALLAR`).
abstract class ConsultaDeDetallado {
  Future<OfertaDeDetalladoPos?> oferta(String productoId, String bodegaId);

  /// Devuelve cuántas unidades entraron al producto destino.
  Future<num> detallar(String origenId, String bodegaId, num cantidad);
}

class ConsultaDeDetalladoHttp implements ConsultaDeDetallado {
  ConsultaDeDetalladoHttp(this._http);

  final ClienteHttp _http;

  @override
  Future<OfertaDeDetalladoPos?> oferta(String productoId, String bodegaId) async {
    final cuerpo = await _http.get<dynamic>('/api/inventario/detallado/oferta',
        query: {'destinoId': productoId, 'bodegaId': bodegaId});
    return cuerpo is Map<String, dynamic> ? OfertaDeDetalladoPos.desdeJson(cuerpo) : null;
  }

  @override
  Future<num> detallar(String origenId, String bodegaId, num cantidad) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/detallado',
        datos: {'origenId': origenId, 'bodegaId': bodegaId, 'cantidad': cantidad}) as Map<String, dynamic>;
    return (cuerpo['entra'] ?? 0) as num;
  }
}

/// Sin servidor: nunca hay nada que abrir.
class ConsultaDeDetalladoVacia implements ConsultaDeDetallado {
  const ConsultaDeDetalladoVacia();

  @override
  Future<OfertaDeDetalladoPos?> oferta(String productoId, String bodegaId) async => null;

  @override
  Future<num> detallar(String origenId, String bodegaId, num cantidad) async => 0;
}

final consultaDeDetalladoProvider =
    Provider<ConsultaDeDetallado>((ref) => const ConsultaDeDetalladoVacia());

/// HU-163 C3 · si quien vende puede detallar (`INVENTARIO_DETALLAR`); la app lo cablea.
final puedeDetallarEnPosProvider = Provider<bool>((ref) => false);
