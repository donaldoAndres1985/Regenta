import 'package:dio/dio.dart';

import 'cola_de_salida.dart';
import 'errores_http.dart';

/// El cliente HTTP de la app. Traduce todo lo que devuelve el backend a un
/// [ErrorDeApi] (HU-112) y, cuando no hay red y la operacion es encolable, la
/// manda a la [ColaDeSalida] en vez de fallar (criterio 5).
///
/// El refresco del token ante un 401 lo hace el `InterceptorDeRefresco` de
/// HU-109, que se instala en el mismo `Dio`. Si tras refrescar el 401 persiste,
/// aqui se traduce a [NoAutenticado].
class ClienteHttp {
  ClienteHttp(this._dio, {ColaDeSalida? cola}) : _cola = cola;

  final Dio _dio;
  final ColaDeSalida? _cola;

  Future<T> get<T>(String ruta, {Map<String, dynamic>? query}) async =>
      await _enviar<T>('GET', ruta, query: query) as T;

  /// Un POST. Si [encolable] y no hay red, la operacion se encola y el resultado
  /// es un [Encolado]; usa el tipo `dynamic` en ese caso.
  Future<dynamic> post<T>(
    String ruta, {
    Object? datos,
    Map<String, dynamic>? query,
    bool encolable = false,
  }) =>
      _enviar<T>('POST', ruta, datos: datos, query: query, encolable: encolable);

  Future<dynamic> put<T>(String ruta,
          {Object? datos, Map<String, dynamic>? query, bool encolable = false}) =>
      _enviar<T>('PUT', ruta, datos: datos, query: query, encolable: encolable);

  Future<dynamic> delete<T>(String ruta,
          {Object? datos, bool encolable = false}) =>
      _enviar<T>('DELETE', ruta, datos: datos, encolable: encolable);

  Future<dynamic> _enviar<T>(
    String metodo,
    String ruta, {
    Object? datos,
    Map<String, dynamic>? query,
    bool encolable = false,
  }) async {
    try {
      final respuesta = await _dio.request<T>(
        ruta,
        data: datos,
        queryParameters: query,
        options: Options(method: metodo),
      );
      return respuesta.data as T;
    } on DioException catch (fallo) {
      final error = mapearError(fallo);
      if (error is ErrorDeRed && encolable && _cola != null) {
        await _cola.encolar(OperacionEncolable(
          metodo: metodo,
          ruta: ruta,
          datos: datos,
          query: query,
        ));
        return const Encolado();
      }
      throw error;
    }
  }
}
