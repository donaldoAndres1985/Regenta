import '../datos/cliente_de_la_venta.dart';

/// El estado del selector de cliente (HU-113 y HU-114).
class EstadoDeCliente {
  const EstadoDeCliente({
    this.termino = '',
    this.resultados = const [],
    this.buscando = false,
    this.trabajando = false,
    this.asignado,
    this.mensaje,
    this.creando = false,
    this.duplicado = false,
  });

  final String termino;
  final List<ClienteDeLaVenta> resultados;
  final bool buscando;

  /// Se está asignando, quitando o creando: los toques se ignoran mientras.
  final bool trabajando;

  /// `null` es consumidor final, que es el estado normal de una venta.
  final ClienteDeLaVenta? asignado;

  final String? mensaje;

  /// El formulario de alta está abierto.
  final bool creando;

  /// El último intento de crear chocó con un documento que ya existe
  /// (HU-114 criterio 2): la pantalla ofrece asignar el que hay.
  final bool duplicado;

  bool get esConsumidorFinal => asignado == null;

  EstadoDeCliente copiar({
    String? termino,
    List<ClienteDeLaVenta>? resultados,
    bool? buscando,
    bool? trabajando,
    Object? asignado = _sinCambio,
    Object? mensaje = _sinCambio,
    bool? creando,
    bool? duplicado,
  }) =>
      EstadoDeCliente(
        termino: termino ?? this.termino,
        resultados: resultados ?? this.resultados,
        buscando: buscando ?? this.buscando,
        trabajando: trabajando ?? this.trabajando,
        asignado: asignado == _sinCambio ? this.asignado : asignado as ClienteDeLaVenta?,
        mensaje: mensaje == _sinCambio ? this.mensaje : mensaje as String?,
        creando: creando ?? this.creando,
        duplicado: duplicado ?? this.duplicado,
      );

  static const _sinCambio = Object();
}
