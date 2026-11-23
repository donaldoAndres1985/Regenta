import 'package:meta/meta.dart';

/// Una operacion que no pudo salir por falta de red y espera turno. La cola de
/// verdad (Drift + workmanager) llega en HU-110 y HU-111; aqui solo el
/// contrato.
@immutable
class OperacionEncolable {
  const OperacionEncolable({
    required this.metodo,
    required this.ruta,
    this.datos,
    this.query,
    this.clave,
  });

  final String metodo;
  final String ruta;
  final Object? datos;
  final Map<String, dynamic>? query;

  /// HU-182 · I32: el identificador que le puso quien la armó (p. ej. una recepción hecha en el
  /// celular). Encolarla otra vez con la misma clave la reemplaza en vez de duplicarla.
  final String? clave;
}

/// Donde van a parar las operaciones que se registraron sin conexion.
abstract interface class ColaDeSalida {
  Future<void> encolar(OperacionEncolable operacion);
}

/// Lo que devuelve el cliente cuando, en vez de fallar, encolo la operacion.
class Encolado {
  const Encolado();
}
