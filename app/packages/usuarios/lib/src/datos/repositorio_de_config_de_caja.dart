import 'package:regenta_core/regenta_core.dart';

/// Configuración de caja del negocio (HU-139): qué métodos de pago no se cuadran al cerrar.
class RepositorioDeConfigDeCaja {
  RepositorioDeConfigDeCaja(this._http);

  final ClienteHttp _http;

  Future<List<String>> verMetodosNoCuadrables() async {
    final res = await _http.get<Map<String, dynamic>>('/api/caja/config');
    return _metodos(res);
  }

  Future<List<String>> fijarMetodosNoCuadrables(List<String> metodos) async {
    final res = await _http.put<Map<String, dynamic>>(
      '/api/caja/config/metodos-no-cuadrables',
      datos: {'metodos': metodos},
    );
    return _metodos(res);
  }

  static List<String> _metodos(Map<String, dynamic> json) =>
      ((json['metodosNoCuadrables'] as List?) ?? const []).cast<String>();
}
