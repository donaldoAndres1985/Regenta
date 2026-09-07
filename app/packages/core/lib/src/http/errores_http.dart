import 'package:dio/dio.dart';

/// El error del backend, ya traducido a algo que la app sabe pintar. Un tipo
/// por forma de reaccionar, no un codigo suelto.
sealed class ErrorDeApi implements Exception {
  const ErrorDeApi(this.mensaje);

  final String mensaje;

  @override
  String toString() => '$runtimeType: $mensaje';
}

/// 401: el token no vale. El interceptor de refresco ya intento renovarlo una
/// vez; si llega aqui, es que ni con eso.
class NoAutenticado extends ErrorDeApi {
  const NoAutenticado([super.mensaje = 'Tu sesion no es valida']);
}

/// 402: el modulo no esta en el plan del negocio. Se ofrece contactar a ventas.
class ModuloFueraDelPlan extends ErrorDeApi {
  const ModuloFueraDelPlan([super.mensaje = 'Este modulo no esta en tu plan']);

  bool get contactarVentas => true;
}

/// 403: falta un permiso. Ojo: no es lo mismo que falta plan.
class SinPermiso extends ErrorDeApi {
  const SinPermiso([super.mensaje = 'No tienes permiso para esta accion']);
}

/// 409: choca con algo que ya existe.
class RecursoDuplicado extends ErrorDeApi {
  const RecursoDuplicado([super.mensaje = 'Ya existe algo asi']);
}

/// 422: hay campos que no cumplen. Se pintan campo por campo en el formulario.
class ErroresDeValidacion extends ErrorDeApi {
  const ErroresDeValidacion(this.porCampo)
      : super('Hay campos que no cumplen el contrato');

  final Map<String, List<String>> porCampo;

  /// El primer mensaje de un campo, o null si ese campo esta bien.
  String? deCampo(String campo) {
    final lista = porCampo[campo];
    return (lista == null || lista.isEmpty) ? null : lista.first;
  }
}

/// Sin respuesta: no hubo red. Si la operacion es encolable, se encola en vez
/// de mostrar el error.
class ErrorDeRed extends ErrorDeApi {
  const ErrorDeRed([super.mensaje = 'Sin conexion']);
}

/// 5xx: el problema esta del otro lado.
class ErrorDelServidor extends ErrorDeApi {
  const ErrorDelServidor(this.codigo)
      : super('El servidor tuvo un problema. Intenta de nuevo en un momento');

  final int codigo;
}

/// Cualquier otro codigo que no tiene un trato propio.
class ErrorDesconocido extends ErrorDeApi {
  const ErrorDesconocido(this.codigo, [String? mensaje])
      : super(mensaje ?? 'Algo salio mal');

  final int codigo;
}

/// Traduce el fallo de dio a un [ErrorDeApi]. Una sola tabla para los quince
/// servicios, porque todos responden `application/problem+json` igual.
ErrorDeApi mapearError(DioException fallo) {
  final respuesta = fallo.response;
  final codigo = respuesta?.statusCode;

  if (codigo == null) return const ErrorDeRed();

  final cuerpo = respuesta?.data is Map
      ? (respuesta!.data as Map).cast<String, dynamic>()
      : const <String, dynamic>{};
  final detalle = ((cuerpo['detail'] ?? cuerpo['title'] ?? '') as Object).toString();

  return switch (codigo) {
    401 => NoAutenticado(detalle.isEmpty ? 'Tu sesion no es valida' : detalle),
    402 => ModuloFueraDelPlan(
        detalle.isEmpty ? 'Este modulo no esta en tu plan' : detalle),
    403 => SinPermiso(detalle.isEmpty
        ? 'No tienes permiso para esta accion'
        : 'No tienes permiso para esta accion: $detalle'),
    409 => RecursoDuplicado(detalle.isEmpty ? 'Ya existe algo asi' : detalle),
    422 => ErroresDeValidacion(_camposDe(cuerpo)),
    >= 500 => ErrorDelServidor(codigo),
    _ => ErrorDesconocido(codigo, detalle.isEmpty ? null : detalle),
  };
}

Map<String, List<String>> _camposDe(Map<String, dynamic> cuerpo) {
  final crudo = cuerpo['campos'];
  if (crudo is! Map) return const {};
  return {
    for (final entrada in crudo.entries)
      entrada.key.toString(): switch (entrada.value) {
        final List<dynamic> lista => lista.map((m) => m.toString()).toList(),
        final Object v => [v.toString()],
        _ => const <String>['invalido'],
      },
  };
}
