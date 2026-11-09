import 'package:regenta_core/regenta_core.dart';

import 'factura_vista.dart';

/// Acceso a `servicio-facturacion` desde la app: ver una factura, su
/// trazabilidad y enviarla al cliente (HU-058).
class RepositorioDeFacturas {
  RepositorioDeFacturas(this._http);

  final ClienteHttp _http;

  Future<FacturaVista> ver(String facturaId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/facturacion/facturas/$facturaId');
    final factura = FacturaVista.desdeJson(cuerpo);
    final pasos = await _trazabilidad(facturaId);
    return factura.conTrazabilidad(pasos);
  }

  Future<List<PasoDeTrazabilidad>> _trazabilidad(String facturaId) async {
    try {
      final lista = await _http
          .get<List<dynamic>>('/api/facturacion/facturas/$facturaId/transmisiones');
      return lista
          .map((e) => PasoDeTrazabilidad.desdeJson((e as Map).cast<String, dynamic>()))
          .toList();
    } on ErrorDeApi {
      return const [];
    }
  }

  /// Manda la factura aceptada al cliente. Sin [correo], el backend usa el del
  /// snapshot del cliente. Devuelve el destinatario.
  Future<String> enviarPorCorreo(String facturaId, {String? correo}) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/facturacion/facturas/$facturaId/envio-cliente',
      datos: correo == null ? null : {'correo': correo},
    );
    return ((respuesta as Map)['destinatario'] ?? '') as String;
  }
}
