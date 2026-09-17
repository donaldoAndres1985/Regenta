import 'package:regenta_core/regenta_core.dart';

import 'ticket_de_cocina.dart';

/// Acceso al KDS (HU-088): las estaciones contra `servicio-menu` (HU-077, ya
/// existe) y los tickets de cocina contra `servicio-comandas`.
class RepositorioDeCocina {
  RepositorioDeCocina(this._http);

  final ClienteHttp _http;

  Future<List<EstacionDeCocina>> estaciones() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/menu/estaciones');
    return cuerpo
        .map((e) => EstacionDeCocina.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  Future<List<TicketDeCocina>> ticketsDeEstacion(String estacionId) async {
    final cuerpo = await _http.get<List<dynamic>>('/api/cocina/estaciones/$estacionId/tickets');
    return cuerpo
        .map((e) => TicketDeCocina.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// Avanza el ticket un paso (HU-088 criterio 4).
  Future<TicketDeCocina> avanzarTicket(String ticketId) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/cocina/tickets/$ticketId/avance')
        as Map<String, dynamic>;
    return TicketDeCocina.desdeJson(cuerpo);
  }
}
