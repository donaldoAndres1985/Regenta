import 'dart:convert';

import 'package:meta/meta.dart';

/// Lo que la app lee del propio JWT, sin una llamada extra (HU-020, criterio 1).
///
/// No valida la firma: de eso se encarga el gateway. Aqui solo se decodifica el
/// payload para saber que puede ver quien entro.
@immutable
class ClaimsDeSesion {
  const ClaimsDeSesion({
    required this.negocioId,
    required this.usuarioId,
    required this.plan,
    required this.patron,
    required this.modulos,
    required this.roles,
    required this.permisos,
  });

  final String negocioId;
  final String usuarioId;
  final String plan;
  final String patron;
  final List<String> modulos;
  final List<String> roles;
  final List<String> permisos;

  factory ClaimsDeSesion.deJwt(String jwt) {
    final partes = jwt.split('.');
    if (partes.length != 3) {
      throw const FormatException('Un JWT tiene tres partes separadas por punto');
    }
    final payload = jsonDecode(utf8.decode(base64Url.decode(base64Url.normalize(partes[1]))))
        as Map<String, dynamic>;

    List<String> lista(String clave) =>
        (payload[clave] as List<dynamic>? ?? const []).cast<String>();

    return ClaimsDeSesion(
      negocioId: (payload['negocio_id'] ?? '') as String,
      usuarioId: (payload['sub'] ?? '') as String,
      plan: (payload['plan'] ?? '') as String,
      patron: (payload['patron'] ?? '') as String,
      modulos: lista('modulos'),
      roles: lista('roles'),
      permisos: lista('permisos'),
    );
  }
}
