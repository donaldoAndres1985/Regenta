import 'package:regenta_core/regenta_core.dart';

/// Una sesion de prueba, con valores por defecto sensatos.
Sesion sesionDe({
  String tokenDeAcceso = 'acceso-1',
  String tokenDeRefresco = 'refresco-1',
  DateTime? expiraEn,
  String negocioId = '11111111-1111-1111-1111-111111111111',
  String usuarioId = '22222222-2222-2222-2222-222222222222',
  String plan = 'PROFESIONAL',
  String patron = 'VENTA_DIRECTA',
  List<String> roles = const ['ADMINISTRADOR'],
  List<String> modulos = const ['VENTAS', 'INVENTARIO'],
}) {
  return Sesion(
    tokenDeAcceso: tokenDeAcceso,
    tokenDeRefresco: tokenDeRefresco,
    expiraEn: (expiraEn ?? DateTime.now().add(const Duration(minutes: 15))).toUtc(),
    negocioId: negocioId,
    usuarioId: usuarioId,
    plan: plan,
    patron: patron,
    roles: roles,
    modulos: modulos,
  );
}

/// Almacen en memoria: el mismo contrato, sin plataforma.
class AlmacenEnMemoria implements AlmacenDeSesion {
  Sesion? _sesion;

  @override
  Future<void> guardar(Sesion sesion) async => _sesion = sesion;

  @override
  Future<Sesion?> leer() async => _sesion;

  @override
  Future<void> borrar() async => _sesion = null;
}

/// Cliente de auth de mentira: se le dice que responder.
class ClienteAuthFalso implements ClienteAuth {
  ClienteAuthFalso({this.alRefrescar, this.alEntrar});

  /// Lo que devuelve (o lanza) `refrescar`. Si es null, lanza RefrescoRechazado.
  Sesion Function(String tokenDeRefresco)? alRefrescar;
  Sesion Function(Credenciales credenciales)? alEntrar;

  int refrescos = 0;
  int entradas = 0;
  final List<String> refrescosCon = [];

  @override
  Future<Sesion> entrar(Credenciales credenciales) async {
    entradas++;
    final f = alEntrar;
    if (f == null) throw const CredencialesInvalidas();
    return f(credenciales);
  }

  @override
  Future<Sesion> refrescar({
    required String tokenDeRefresco,
    String? dispositivoId,
    String? plataforma,
  }) async {
    refrescos++;
    refrescosCon.add(tokenDeRefresco);
    final f = alRefrescar;
    if (f == null) throw const RefrescoRechazado();
    return f(tokenDeRefresco);
  }
}
