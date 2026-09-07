import 'package:meta/meta.dart';

/// Lo que la app necesita saber de quien esta dentro: los dos tokens, cuando
/// caduca el de acceso y los claims que deciden que puede ver (negocio, plan,
/// patron, roles, modulos).
///
/// `expiraEn` siempre en UTC.
@immutable
class Sesion {
  const Sesion({
    required this.tokenDeAcceso,
    required this.tokenDeRefresco,
    required this.expiraEn,
    required this.negocioId,
    required this.usuarioId,
    required this.plan,
    required this.patron,
    required this.roles,
    required this.modulos,
  });

  final String tokenDeAcceso;
  final String tokenDeRefresco;
  final DateTime expiraEn;
  final String negocioId;
  final String usuarioId;
  final String plan;
  final String patron;
  final List<String> roles;
  final List<String> modulos;

  /// El token de acceso todavia sirve en [ahora] con [margen] de sobra. Con el
  /// margen por defecto (cero) es literal; el motor pasa un margen para
  /// refrescar antes de que se quede corto.
  bool accesoVigente(DateTime ahora, {Duration margen = Duration.zero}) {
    return expiraEn.isAfter(ahora.toUtc().add(margen));
  }

  Sesion copyWith({
    String? tokenDeAcceso,
    String? tokenDeRefresco,
    DateTime? expiraEn,
  }) {
    return Sesion(
      tokenDeAcceso: tokenDeAcceso ?? this.tokenDeAcceso,
      tokenDeRefresco: tokenDeRefresco ?? this.tokenDeRefresco,
      expiraEn: expiraEn ?? this.expiraEn,
      negocioId: negocioId,
      usuarioId: usuarioId,
      plan: plan,
      patron: patron,
      roles: roles,
      modulos: modulos,
    );
  }

  Map<String, dynamic> toJson() => {
        'tokenDeAcceso': tokenDeAcceso,
        'tokenDeRefresco': tokenDeRefresco,
        'expiraEn': expiraEn.toUtc().toIso8601String(),
        'negocioId': negocioId,
        'usuarioId': usuarioId,
        'plan': plan,
        'patron': patron,
        'roles': roles,
        'modulos': modulos,
      };

  factory Sesion.fromJson(Map<String, dynamic> json) => Sesion(
        tokenDeAcceso: json['tokenDeAcceso'] as String,
        tokenDeRefresco: json['tokenDeRefresco'] as String,
        expiraEn: DateTime.parse(json['expiraEn'] as String).toUtc(),
        negocioId: json['negocioId'] as String,
        usuarioId: json['usuarioId'] as String,
        plan: json['plan'] as String,
        patron: json['patron'] as String,
        roles: (json['roles'] as List<dynamic>).cast<String>(),
        modulos: (json['modulos'] as List<dynamic>).cast<String>(),
      );
}
