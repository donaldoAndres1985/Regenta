import 'package:regenta_core/regenta_core.dart';

import 'orden_recibible.dart';

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
  const ResultadoDeRecepcion.confirmada(this.numero) : encolada = false;
  const ResultadoDeRecepcion.encolada()
      : numero = null,
        encolada = true;

  final String? numero;
  final bool encolada;
}

/// Acceso a `servicio-compras` para la recepción móvil. Se apoya en el
/// [ClienteHttp] del núcleo; los POST son encolables, así que sin red la
/// recepción se guarda y sube después.
class RepositorioDeRecepciones {
  RepositorioDeRecepciones(this._http);

  final ClienteHttp _http;

  Future<OrdenRecibible> verOrden(String ordenId) async {
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/compras/ordenes/${Uri.encodeComponent(ordenId)}',
    );
    return OrdenRecibible.desdeJson(cuerpo);
  }

  /// Crea la recepción y la confirma. Si el primer POST se encola por falta de
  /// red, devuelve [ResultadoDeRecepcion.encolada]; la confirmación queda
  /// pendiente hasta que la cola suba.
  Future<ResultadoDeRecepcion> registrar({
    required String ordenId,
    required String bodegaId,
    String? facturaProveedor,
    required List<LineaParaRecibir> lineas,
  }) async {
    final creada = await _http.post<Map<String, dynamic>>(
      '/api/compras/recepciones',
      encolable: true,
      datos: {
        'ordenId': ordenId,
        'bodegaId': bodegaId,
        if (facturaProveedor != null && facturaProveedor.isNotEmpty)
          'facturaProveedor': facturaProveedor,
        'lineas': [for (final l in lineas) l.aJson()],
      },
    );
    if (creada is Encolado) {
      return const ResultadoDeRecepcion.encolada();
    }
    final mapa = (creada as Map).cast<String, dynamic>();
    final id = mapa['id'] as String;
    final confirmada = await _http.post<Map<String, dynamic>>(
      '/api/compras/recepciones/$id/confirmacion',
      encolable: true,
    );
    if (confirmada is Encolado) {
      return const ResultadoDeRecepcion.encolada();
    }
    final numero =
        ((confirmada as Map)['numero'] ?? mapa['numero'] ?? '') as String;
    return ResultadoDeRecepcion.confirmada(numero);
  }
}
