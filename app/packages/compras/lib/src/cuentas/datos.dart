import 'package:regenta_core/regenta_core.dart';

/// Una cuenta por pagar como la devuelve `GET /api/compras/cuentas-por-pagar` (HU-170).
class CuentaPorPagarVista {
  const CuentaPorPagarVista({
    required this.id,
    required this.monto,
    required this.saldo,
    required this.fechaVencimiento,
    this.proveedorId,
    this.proveedorNombre,
    this.recepcionNumero,
    this.numeroFactura,
    this.enEsperaDeFactura = false,
    this.estado = 'PENDIENTE',
    this.vencida = false,
    this.diasDeMora = 0,
  });

  final String id;
  final String? proveedorId;
  final String? proveedorNombre;
  final String? recepcionNumero;
  final String? numeroFactura;

  /// Llegó con remisión: todavía no tiene número de factura (I22).
  final bool enEsperaDeFactura;
  final num monto;
  final num saldo;
  final DateTime fechaVencimiento;
  final String estado;
  final bool vencida;
  final int diasDeMora;

  bool get pagada => estado == 'PAGADA';

  /// Se le puede abonar: tiene saldo y no está pagada ni anulada.
  bool get abierta => saldo > 0 && !pagada && estado != 'ANULADA';

  /// Cómo se nombra en un reparto: su factura, o su remisión si aún no tiene.
  String get documento =>
      enEsperaDeFactura ? 'Remisión ${recepcionNumero ?? ''}'.trim() : 'Factura ${numeroFactura ?? ''}'.trim();

  factory CuentaPorPagarVista.desdeJson(Map<String, dynamic> json) => CuentaPorPagarVista(
        id: json['id'] as String,
        proveedorId: json['proveedorId'] as String?,
        proveedorNombre: json['proveedorNombre'] as String?,
        recepcionNumero: json['recepcionNumero'] as String?,
        numeroFactura: json['numeroFactura'] as String?,
        enEsperaDeFactura: (json['enEsperaDeFactura'] as bool?) ?? false,
        monto: (json['monto'] as num?) ?? 0,
        saldo: (json['saldo'] as num?) ?? 0,
        fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
        estado: (json['estado'] ?? 'PENDIENTE') as String,
        vencida: (json['vencida'] as bool?) ?? false,
        diasDeMora: (json['diasDeMora'] as num?)?.toInt() ?? 0,
      );
}

/// Acceso a las cuentas por pagar de `compras`.
class RepositorioDeCuentasPorPagar {
  RepositorioDeCuentasPorPagar(this._http);

  static const _base = '/api/compras/cuentas-por-pagar';

  final ClienteHttp _http;

  Future<List<CuentaPorPagarVista>> listar() async {
    final res = await _http.get<List<dynamic>>(_base);
    return res.map((e) => CuentaPorPagarVista.desdeJson((e as Map).cast<String, dynamic>())).toList();
  }

  /// HU-171 · Un pago a un proveedor que abona a varias de sus cuentas. Sin [aplicaciones], el
  /// backend reparte de la más antigua a la más nueva.
  Future<void> pagar({
    required String proveedorId,
    required num monto,
    required String metodo,
    String? referencia,
    bool desdeLaCaja = false,
    Map<String, num>? aplicaciones,
  }) async {
    await _http.post<dynamic>('$_base/pagos', datos: {
      'proveedorId': proveedorId,
      'monto': monto,
      'metodo': metodo,
      if (referencia != null && referencia.trim().isNotEmpty) 'referencia': referencia.trim(),
      'pagarDesdeLaCaja': desdeLaCaja,
      if (aplicaciones != null)
        'aplicaciones': [
          for (final e in aplicaciones.entries)
            if (e.value > 0) {'cuentaId': e.key, 'monto': e.value},
        ],
    });
  }

  /// Si el usuario tiene una caja abierta (para «Pagar con dinero de la caja»).
  Future<bool> tengoCajaAbierta() async {
    try {
      await _http.get<dynamic>('/api/caja/sesiones/mia');
      return true;
    } on ErrorDeApi {
      return false;
    }
  }

  /// HU-170 C3 · Llegó la factura de una cuenta en espera.
  Future<CuentaPorPagarVista> registrarFactura(String cuentaId, String numero, DateTime fecha) async {
    final dynamic res = await _http.put<dynamic>('$_base/$cuentaId/factura', datos: {
      'numeroFactura': numero.trim(),
      'fechaFactura': fecha.toIso8601String().substring(0, 10),
    });
    return CuentaPorPagarVista.desdeJson((res as Map).cast<String, dynamic>());
  }
}
