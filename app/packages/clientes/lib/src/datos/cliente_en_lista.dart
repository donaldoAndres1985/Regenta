/// Cómo está la cartera de un cliente, para el chip de la fila.
enum EstadoDeCartera { alDia, excedido, sinSaldo }

/// Un cliente como se ve en el listado móvil (HU-025): lo justo para la fila y
/// para filtrar sin volver a pedir nada al servidor.
class ClienteEnLista {
  const ClienteEnLista({
    required this.id,
    required this.nombre,
    required this.tipoDocumento,
    required this.numeroDocumento,
    this.saldo = 0,
    this.cupo = 0,
    this.segmento,
    this.vencido = false,
  });

  final String id;
  final String nombre;
  final String tipoDocumento;
  final String? numeroDocumento;
  final num saldo;
  final num cupo;
  final String? segmento;

  /// Tiene cuentas por cobrar vencidas. Lo llena el backend cuando HU-022 sume
  /// el estado de cartera al listado; hasta entonces es `false`.
  final bool vencido;

  bool get tieneSaldo => saldo > 0;

  bool get esMayorista => (segmento ?? '').toUpperCase() == 'MAYORISTA';

  EstadoDeCartera get estadoDeCartera {
    if (saldo <= 0) return EstadoDeCartera.sinSaldo;
    if (cupo > 0 && saldo > cupo) return EstadoDeCartera.excedido;
    return EstadoDeCartera.alDia;
  }

  /// "NIT 900.412.883-1", "CC 71.884.203" o, sin documento, "Sin identificar".
  String get documentoLabel {
    final numero = numeroDocumento;
    if (numero == null || numero.isEmpty || tipoDocumento == 'SIN_IDENTIFICAR') {
      return 'Sin identificar';
    }
    return '$tipoDocumento $numero';
  }

  /// Une nombre y documento para el filtro por texto (criterio 1).
  bool coincideCon(String termino) {
    final q = termino.trim().toLowerCase();
    if (q.isEmpty) return true;
    return nombre.toLowerCase().contains(q) ||
        (numeroDocumento ?? '').toLowerCase().contains(q);
  }

  factory ClienteEnLista.desdeJson(Map<String, dynamic> json) => ClienteEnLista(
        id: json['id'] as String,
        nombre: (json['nombreDisplay'] ?? json['nombre'] ?? '') as String,
        tipoDocumento: (json['tipoDocumento'] ?? 'SIN_IDENTIFICAR') as String,
        numeroDocumento: json['numeroDocumento'] as String?,
        saldo: (json['saldoPendiente'] ?? 0) as num,
        cupo: (json['cupoCredito'] ?? 0) as num,
        segmento: json['segmento'] as String?,
        vencido: (json['carteraVencida'] ?? false) as bool,
      );

  Map<String, dynamic> aJson() => {
        'id': id,
        'nombreDisplay': nombre,
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': numeroDocumento,
        'saldoPendiente': saldo,
        'cupoCredito': cupo,
        'segmento': segmento,
        'carteraVencida': vencido,
      };
}
