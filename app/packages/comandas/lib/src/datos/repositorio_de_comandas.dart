import 'package:regenta_core/regenta_core.dart';

import 'comanda_vista.dart';

/// El resultado de agregar una línea o enviar a cocina: confirmado por el
/// servidor, o encolado para subir cuando vuelva la señal (HU-091 criterio 4).
class ResultadoDeComanda {
  const ResultadoDeComanda.confirmado(this.comanda) : encolado = false;
  const ResultadoDeComanda.encolado()
      : comanda = null,
        encolado = true;

  final ComandaVista? comanda;
  final bool encolado;
}

/// Acceso a la comanda contra `servicio-comandas`, y a la carta / los
/// modificadores contra `servicio-menu` para el flujo de «Añadir» (HU-085).
/// Agregar una línea y enviar a cocina son encolables: sin señal, la toma
/// sigue local y sube sola al reconectar (HU-091 criterio 4).
class RepositorioDeComandas {
  RepositorioDeComandas(this._http);

  final ClienteHttp _http;

  Future<ComandaVista> comanda(String comandaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/comandas/$comandaId');
    return ComandaVista.desdeJson(cuerpo);
  }

  /// Los ítems de una carta del menú, para el selector de «Añadir» en escritorio.
  Future<List<ItemDeCarta>> itemsDeCarta(String cartaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/menu/cartas/$cartaId/menu');
    final categorias = (cuerpo['categorias'] as List<dynamic>? ?? const []);
    return [
      for (final c in categorias)
        for (final it in ((c as Map)['items'] as List<dynamic>? ?? const []))
          ItemDeCarta.desdeJson((it as Map).cast<String, dynamic>()),
    ];
  }

  /// La carta agrupada por categoría, para la rejilla de botones grandes del
  /// celular (HU-091 criterio 1).
  Future<List<CategoriaDeCarta>> categoriasDeCarta(String cartaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/menu/cartas/$cartaId/menu');
    final categorias = (cuerpo['categorias'] as List<dynamic>? ?? const []);
    return categorias
        .map((c) => CategoriaDeCarta.desdeJson((c as Map).cast<String, dynamic>()))
        .toList();
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
  /// obligatorios (HU-085 criterio 5). Sin señal, queda encolada (HU-091
  /// criterio 4): [ResultadoDeComanda.encolado].
  Future<ResultadoDeComanda> agregarLinea(
    String comandaId, {
    required String itemMenuId,
    required num cantidad,
    List<String> modificadorIds = const [],
    String? notas,
  }) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/comandas/$comandaId/lineas',
      encolable: true,
      datos: {
        'itemMenuId': itemMenuId,
        'cantidad': cantidad,
        'modificadorIds': modificadorIds,
        'notas': notas,
      },
    );
    if (respuesta is Encolado) {
      return const ResultadoDeComanda.encolado();
    }
    return ResultadoDeComanda.confirmado(
        ComandaVista.desdeJson((respuesta as Map).cast<String, dynamic>()));
  }

  /// Envía a cocina. Sin señal, queda encolado (HU-091 criterio 4).
  Future<ResultadoDeComanda> enviarACocina(String comandaId) async {
    final respuesta =
        await _http.post<Map<String, dynamic>>('/api/comandas/$comandaId/envio', encolable: true);
    if (respuesta is Encolado) {
      return const ResultadoDeComanda.encolado();
    }
    return ResultadoDeComanda.confirmado(
        ComandaVista.desdeJson((respuesta as Map).cast<String, dynamic>()));
  }

  /// Avanza el estado de una línea (HU-086 criterio 1).
  Future<ComandaVista> avanzarLinea(String comandaId, String lineaId) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
        '/api/comandas/$comandaId/lineas/$lineaId/avance') as Map<String, dynamic>;
    return ComandaVista.desdeJson(cuerpo);
  }
}
