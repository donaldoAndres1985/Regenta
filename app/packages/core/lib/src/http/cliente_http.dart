import 'package:dio/dio.dart';

import 'cola_de_salida.dart';
import 'errores_http.dart';
import 'respaldo_de_consultas.dart';

/// El cliente HTTP de la app. Traduce todo lo que devuelve el backend a un
/// [ErrorDeApi] (HU-112) y, cuando no hay red y la operacion es encolable, la
/// manda a la [ColaDeSalida] en vez de fallar (criterio 5).
///
/// El refresco del token ante un 401 lo hace el `InterceptorDeRefresco` de
/// HU-109, que se instala en el mismo `Dio`. Si tras refrescar el 401 persiste,
/// aqui se traduce a [NoAutenticado].
class ClienteHttp {
  ClienteHttp(this._dio, {ColaDeSalida? cola, RespaldoDeConsultas? respaldo})
      : _cola = cola,
        _respaldo = respaldo;

  final Dio _dio;
  final ColaDeSalida? _cola;
  final RespaldoDeConsultas? _respaldo;

  /// Un GET. Con [conRespaldo], la respuesta se guarda en el [RespaldoDeConsultas] y, si después no
  /// hay red, se devuelve la última guardada en vez de fallar (HU-182: abrir la orden en la bodega
  /// sin señal). Si nunca se consultó con señal, falla con [ErrorDeRed] como siempre.
  Future<T> get<T>(String ruta, {Map<String, dynamic>? query, bool conRespaldo = false}) async {
    final respaldo = conRespaldo ? _respaldo : null;
    if (respaldo == null) return await _enviar<T>('GET', ruta, query: query) as T;
    final clave = query == null || query.isEmpty ? ruta : Uri(path: ruta, queryParameters: query.map(
        (k, v) => MapEntry(k, v.toString()))).toString();
    try {
      final respuesta = await _enviar<T>('GET', ruta, query: query) as T;
      await respaldo.guardar(clave, respuesta);
      return respuesta;
    } on ErrorDeRed {
      final guardada = await respaldo.leer(clave);
      if (guardada == null) rethrow;
      return _comoTipo<T>(guardada);
    }
  }

  /// Lo que vuelve del JSON guardado son `List<dynamic>` y `Map<String, dynamic>`, como lo que
  /// devuelve Dio; aquí solo se ajusta el tipo pedido.
  static T _comoTipo<T>(Object guardada) {
    if (guardada is T) return guardada as T;
    if (guardada is Map) return guardada.cast<String, dynamic>() as T;
    return guardada as T;
  }

  /// Un POST. Si [encolable] y no hay red, la operacion se encola y el resultado
  /// es un [Encolado]; usa el tipo `dynamic` en ese caso.
  Future<dynamic> post<T>(
    String ruta, {
    Object? datos,
    Map<String, dynamic>? query,
    bool encolable = false,
    String? claveDeCola,
  }) =>
      _enviar<T>('POST', ruta, datos: datos, query: query, encolable: encolable, claveDeCola: claveDeCola);

  Future<dynamic> put<T>(String ruta,
          {Object? datos, Map<String, dynamic>? query, bool encolable = false, String? claveDeCola}) =>
      _enviar<T>('PUT', ruta, datos: datos, query: query, encolable: encolable, claveDeCola: claveDeCola);

  Future<dynamic> delete<T>(String ruta,
          {Object? datos, bool encolable = false}) =>
      _enviar<T>('DELETE', ruta, datos: datos, encolable: encolable);

  /// Un pedido cuya respuesta es un archivo (ej. un .xlsx): devuelve sus bytes crudos.
  Future<List<int>> bytes(String metodo, String ruta, {Object? datos}) async {
    try {
      final respuesta = await _dio.request<List<int>>(
        ruta,
        data: datos,
        options: Options(method: metodo, responseType: ResponseType.bytes),
      );
      return respuesta.data ?? const [];
    } on DioException catch (fallo) {
      throw mapearError(fallo);
    }
  }

  Future<dynamic> _enviar<T>(
    String metodo,
    String ruta, {
    Object? datos,
    Map<String, dynamic>? query,
    bool encolable = false,
    String? claveDeCola,
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
          clave: claveDeCola,
        ));
        return const Encolado();
      }
      throw error;
    }
  }
}
