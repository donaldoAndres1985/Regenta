import 'package:regenta_core/regenta_core.dart';

import 'comanda_vista.dart';

/// Acceso a la comanda contra `servicio-comandas`, y a la carta / los
/// modificadores contra `servicio-menu` para el flujo de «Añadir» (HU-085).
class RepositorioDeComandas {
  RepositorioDeComandas(this._http);

  final ClienteHttp _http;

  Future<ComandaVista> comanda(String comandaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/comandas/$comandaId');
    return ComandaVista.desdeJson(cuerpo);
  }

  /// Los ítems de una carta del menú, para el buscador de «Añadir».
  Future<List<ItemDeCarta>> itemsDeCarta(String cartaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/menu/cartas/$cartaId/menu');
    final categorias = (cuerpo['categorias'] as List<dynamic>? ?? const []);
    return [
      for (final c in categorias)
        for (final it in ((c as Map)['items'] as List<dynamic>? ?? const []))
          ItemDeCarta.desdeJson((it as Map).cast<String, dynamic>()),
    ];
  }

  /// Los grupos de modificadores de un ítem (HU-078), para el criterio 5.
  Future<List<GrupoModificadores>> gruposDeItem(String itemId) async {
    try {
      final cuerpo = await _http.get<List<dynamic>>('/api/menu/items/$itemId/grupos-modificadores');
      return cuerpo
          .map((e) => GrupoModificadores.desdeJson((e as Map).cast<String, dynamic>()))
          .toList();
    } on ErrorDeApi {
      return const [];
    }
  }

  /// Agrega una línea. Propaga [ErroresDeValidacion] si faltan modificadores
  /// obligatorios (HU-085 criterio 5).
  Future<ComandaVista> agregarLinea(
    String comandaId, {
    required String itemMenuId,
    required num cantidad,
    List<String> modificadorIds = const [],
    String? notas,
  }) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/comandas/$comandaId/lineas',
      datos: {
        'itemMenuId': itemMenuId,
        'cantidad': cantidad,
        'modificadorIds': modificadorIds,
        'notas': notas,
      },
    ) as Map<String, dynamic>;
    return ComandaVista.desdeJson(cuerpo);
  }

  Future<ComandaVista> enviarACocina(String comandaId) async {
    final cuerpo = await _http
        .post<Map<String, dynamic>>('/api/comandas/$comandaId/envio') as Map<String, dynamic>;
    return ComandaVista.desdeJson(cuerpo);
  }
}
