/// HU-107, criterio 4. Un enlace profundo de una notificacion (FCM, o el tap en
/// una alerta local) llega como un mapa de datos. Aqui solo se arma la ruta; la
/// guardia decide despues si hay sesion y permiso para entrar.
String? resolverEnlaceProfundo(Map<String, dynamic> payload) {
  final rutaCruda = payload['ruta'];
  if (rutaCruda is! String || rutaCruda.trim().isEmpty) return null;

  var ruta = rutaCruda.trim();
  if (!ruta.startsWith('/')) ruta = '/$ruta';

  final id = payload['id'];
  if (id != null) {
    final sufijo = id.toString();
    if (sufijo.isNotEmpty && !ruta.endsWith('/$sufijo')) {
      ruta = '$ruta/$sufijo';
    }
  }
  return ruta;
}
