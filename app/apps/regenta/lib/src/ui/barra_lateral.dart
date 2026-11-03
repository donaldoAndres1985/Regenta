import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../navegacion/catalogo.dart';

/// Ancho del menú lateral en escritorio.
const double anchoBarraLateral = 272;

/// Desde este ancho de ventana el menú lateral queda fijo a la izquierda en
/// todas las pantallas; por debajo, Inicio lo ofrece en un cajón.
const double anchoConMenuFijo = 1024;

/// Menú principal de escritorio: columna blanca pegada a la izquierda, de alto
/// completo, con los módulos agrupados en Operación, Gestión y Análisis. La
/// monta la carcasa ([CarcasaConMenu]) para que se vea en todas las pantallas.
class BarraLateral extends StatelessWidget {
  const BarraLateral({
    super.key,
    required this.nombreNegocio,
    required this.nombreUsuario,
    required this.plan,
    required this.rutaActiva,
    required this.onNavegar,
    required this.onCerrarSesion,
    this.entradas = const [],
  });

  final String nombreNegocio;
  final String nombreUsuario;
  final String plan;
  final String rutaActiva;
  final ValueChanged<String> onNavegar;
  final VoidCallback onCerrarSesion;
  final List<EntradaDeNavegacion> entradas;

  String _iniciales(String texto) {
    final partes = texto.trim().split(RegExp(r'\s+'));
    if (partes.isEmpty || partes.first.isEmpty) return 'R';
    if (partes.length == 1) return partes.first.substring(0, 1).toUpperCase();
    return (partes.first[0] + partes[1][0]).toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    // Inicio se ve siempre; lo demás, lo que el negocio y la persona tienen.
    final visibles = entradas.isEmpty
        ? const [
            EntradaDeNavegacion(ruta: GuardiaDeRutas.inicio, titulo: 'Inicio'),
          ]
        : entradas;

    return Container(
      width: anchoBarraLateral,
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 16),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(right: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Logo Regenta
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: RegentaColors.accent,
                  borderRadius: BorderRadius.circular(10),
                ),
                alignment: Alignment.center,
                child: const Text(
                  'R',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 20,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Text(
                'Regenta',
                style: PastelTheme.titulo(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: RegentaColors.ink,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Selector de Negocio
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: RegentaColors.paper,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: RegentaColors.accent,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    _iniciales(nombreNegocio),
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        nombreNegocio,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: PastelTheme.titulo(
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          color: RegentaColors.ink,
                        ),
                      ),
                      Text(
                        plan,
                        style: PastelTheme.cuerpo(
                          fontSize: 11,
                          color: RegentaColors.ink2,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Lista de Menú
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                for (final grupo in GrupoDeMenu.values)
                  if (visibles.any((e) => grupoDeRuta[e.ruta] == grupo)) ...[
                    _TituloDeGrupo(grupo.titulo),
                    for (final entrada in visibles.where(
                      (e) => grupoDeRuta[e.ruta] == grupo,
                    ))
                      _ItemMenu(
                        icono: iconoDeRuta[entrada.ruta] ?? Icons.apps,
                        titulo: entrada.titulo,
                        activo: rutaActiva == entrada.ruta,
                        badge: entrada.modulo == 'INVENTARIO' ? '7' : null,
                        onTap: () => onNavegar(entrada.ruta),
                      ),
                    const SizedBox(height: 10),
                  ],
              ],
            ),
          ),

          // Perfil de Usuario en el pie
          InkWell(
            onTap: onCerrarSesion,
            borderRadius: BorderRadius.circular(14),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: const BoxDecoration(
                      color: RegentaColors.warn,
                      shape: BoxShape.circle,
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      _iniciales(nombreUsuario),
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          nombreUsuario,
                          style: PastelTheme.titulo(
                            fontWeight: FontWeight.bold,
                            fontSize: 13.5,
                            color: RegentaColors.ink,
                          ),
                        ),
                        Text(
                          'Administrador',
                          style: PastelTheme.cuerpo(
                            fontSize: 11.5,
                            color: RegentaColors.ink2,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.logout_rounded,
                    size: 18,
                    color: RegentaColors.faint,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TituloDeGrupo extends StatelessWidget {
  const _TituloDeGrupo(this.titulo);

  final String titulo;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
      child: Text(
        titulo.toUpperCase(),
        style: PastelTheme.cuerpo(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.8,
          color: RegentaColors.ink2,
        ),
      ),
    );
  }
}

class _ItemMenu extends StatelessWidget {
  const _ItemMenu({
    required this.icono,
    required this.titulo,
    required this.activo,
    required this.onTap,
    this.badge,
  });

  final IconData icono;
  final String titulo;
  final bool activo;
  final VoidCallback onTap;
  final String? badge;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Material(
        color: activo ? RegentaColors.accentSoft : Colors.transparent,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: Container(
            height: 44,
            padding: const EdgeInsets.symmetric(horizontal: 14),
            child: Row(
              children: [
                Icon(
                  icono,
                  size: 20,
                  color: activo ? RegentaColors.ink : RegentaColors.ink2,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    titulo,
                    style: PastelTheme.cuerpo(
                      fontSize: 14,
                      fontWeight: activo ? FontWeight.bold : FontWeight.w500,
                      color: RegentaColors.ink,
                    ),
                  ),
                ),
                if (badge != null)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: RegentaColors.critSoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      badge!,
                      style: const TextStyle(
                        color: RegentaColors.crit,
                        fontWeight: FontWeight.bold,
                        fontSize: 11,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
