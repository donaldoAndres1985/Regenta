import 'package:regenta_core/regenta_core.dart';

import 'cuenta_vista.dart';

/// Dividir la cuenta entre comensales (HU-089), contra `servicio-comandas`.
class RepositorioDeCuentas {
  RepositorioDeCuentas(this._http);

  final ClienteHttp _http;

  Future<List<CuentaVista>> listar(String comandaId) async {
    final cuerpo = await _http.get<List<dynamic>>('/api/comandas/$comandaId/cuentas');
    return _lista(cuerpo);
  }

  Future<CuentaVista> crear(String comandaId, {String? etiqueta}) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/comandas/$comandaId/cuentas',
      datos: {'etiqueta': etiqueta, 'modoDivision': 'POR_ITEM'},
    ) as Map<String, dynamic>;
    return CuentaVista.desdeJson(cuerpo);
  }

  /// Marca [lineaId] en [cuentaId]. Si ya estaba en otras, el reparto se
  /// recalcula igual entre todas (HU-089 criterio 2).
  Future<List<CuentaVista>> marcarLinea(String comandaId, String cuentaId, String lineaId) async {
    final cuerpo =
        await _http.put<List<dynamic>>('/api/comandas/$comandaId/cuentas/$cuentaId/lineas/$lineaId');
    return _lista(cuerpo);
  }

  Future<List<CuentaVista>> desmarcarLinea(String comandaId, String cuentaId, String lineaId) async {
    final cuerpo =
        await _http.delete<List<dynamic>>('/api/comandas/$comandaId/cuentas/$cuentaId/lineas/$lineaId');
    return _lista(cuerpo);
  }

  /// HU-089 criterio 3: reparte el total en [numeroPartes] cuentas iguales.
  Future<List<CuentaVista>> dividirEnPartesIguales(String comandaId, int numeroPartes) async {
    final cuerpo = await _http.post<List<dynamic>>(
      '/api/comandas/$comandaId/cuentas/division-igual',
      datos: {'numeroPartes': numeroPartes},
    );
    return _lista(cuerpo);
  }

  /// Cobra la cuenta entera; si era la última abierta, la comanda se cierra sola.
  Future<CuentaVista> marcarPagada(String comandaId, String cuentaId) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/comandas/$comandaId/cuentas/$cuentaId/pago')
        as Map<String, dynamic>;
    return CuentaVista.desdeJson(cuerpo);
  }

  List<CuentaVista> _lista(List<dynamic> cuerpo) =>
      cuerpo.map((e) => CuentaVista.desdeJson((e as Map).cast<String, dynamic>())).toList();
}
