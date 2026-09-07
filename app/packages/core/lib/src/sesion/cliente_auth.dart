import 'package:meta/meta.dart';

import 'sesion.dart';

/// Lo que la app manda para entrar. El negocio solo hace falta cuando el mismo
/// correo trabaja en mas de uno.
@immutable
class Credenciales {
  const Credenciales({
    required this.email,
    required this.password,
    this.negocioId,
    this.dispositivoId,
    this.plataforma,
  });

  final String email;
  final String password;
  final String? negocioId;
  final String? dispositivoId;
  final String? plataforma;

  Map<String, dynamic> toJson() => {
        'email': email,
        'password': password,
        'negocioId': ?negocioId,
        'dispositivoId': ?dispositivoId,
        'plataforma': ?plataforma,
      };
}

/// Un negocio en el que ese correo puede entrar.
@immutable
class NegocioParaElegir {
  const NegocioParaElegir({required this.negocioId, required this.nombreComercial});

  final String negocioId;
  final String nombreComercial;

  factory NegocioParaElegir.fromJson(Map<String, dynamic> json) => NegocioParaElegir(
        negocioId: json['negocioId'] as String,
        nombreComercial: json['nombreComercial'] as String,
      );
}

/// Emite y renueva la sesion contra el servicio de usuarios, a traves del
/// gateway.
abstract interface class ClienteAuth {
  Future<Sesion> entrar(Credenciales credenciales);

  Future<Sesion> refrescar({
    required String tokenDeRefresco,
    String? dispositivoId,
    String? plataforma,
  });
}

/// El correo y la contrasena no cuadran.
class CredencialesInvalidas implements Exception {
  const CredencialesInvalidas();
  @override
  String toString() => 'Credenciales invalidas';
}

/// La cuenta esta bloqueada por intentos fallidos (HTTP 423).
class CuentaBloqueada implements Exception {
  const CuentaBloqueada();
  @override
  String toString() => 'Cuenta bloqueada por intentos fallidos';
}

/// Ese correo trabaja en varios negocios: hay que reintentar el login diciendo
/// en cual.
class DebeElegirNegocio implements Exception {
  const DebeElegirNegocio(this.negocios);
  final List<NegocioParaElegir> negocios;
  @override
  String toString() => 'Debe elegir negocio (${negocios.length} opciones)';
}

/// El refresh token esta vencido, revocado o ya se uso. No se reintenta: la
/// sesion se acabo.
class RefrescoRechazado implements Exception {
  const RefrescoRechazado();
  @override
  String toString() => 'Refresco rechazado';
}
