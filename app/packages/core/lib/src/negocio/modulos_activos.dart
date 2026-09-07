import 'package:meta/meta.dart';

import 'claims_de_sesion.dart';

/// Una entrada del menu de navegacion. Si [modulo] es null, es de Core y se ve
/// siempre; si no, solo se ve cuando ese modulo esta activo.
@immutable
class EntradaDeNavegacion {
  const EntradaDeNavegacion({
    required this.ruta,
    required this.titulo,
    this.modulo,
  });

  final String ruta;
  final String titulo;
  final String? modulo;
}

/// Los modulos que el negocio tiene encendidos, con su plan y su patron.
/// Esconder una opcion es cortesia: el backend revalida igual.
@immutable
class ModulosActivos {
  ModulosActivos({
    required this.plan,
    required this.patron,
    required List<String> codigos,
  }) : _codigos = {for (final c in codigos) c.toUpperCase()};

  factory ModulosActivos.deClaims(ClaimsDeSesion claims) => ModulosActivos(
        plan: claims.plan,
        patron: claims.patron,
        codigos: claims.modulos,
      );

  final String plan;
  final String patron;
  final Set<String> _codigos;

  /// Vacio: no hay sesion todavia.
  const ModulosActivos.ninguno()
      : plan = '',
        patron = '',
        _codigos = const {};

  List<String> get todos => _codigos.toList()..sort();

  bool tiene(String codigo) => _codigos.contains(codigo.toUpperCase());

  /// El catalogo filtrado a lo que este negocio puede ver (criterio 2).
  List<EntradaDeNavegacion> visiblesDe(Iterable<EntradaDeNavegacion> catalogo) =>
      catalogo.where((e) => e.modulo == null || tiene(e.modulo!)).toList();
}
