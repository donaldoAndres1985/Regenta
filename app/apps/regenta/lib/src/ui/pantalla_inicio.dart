import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';
import '../navegacion/catalogo.dart';

/// Donde se aterriza (HU-119, reglas de `design/comportamiento/Inicio.md`).
///
/// El menú sale de los módulos del token y de los permisos de quien entró
/// (R2): esta pantalla no tiene una lista de módulos escrita a mano.
class PantallaInicio extends ConsumerWidget {
  const PantallaInicio({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final perfil = ref.watch(perfilComoNotifierProvider);
    final entradas = menuDe(perfil.claims);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Inicio',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        actions: [
          Semantics(
            button: true,
            label: 'Cerrar sesión',
            child: IconButton(
              icon: const Icon(Icons.logout, size: 20, color: RegentaColors.ink2),
              onPressed: () => ref.read(motorDeSesionProvider).cerrar(),
            ),
          ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final escritorio = restricciones.maxWidth >= kBreakpointEscritorio;
          final modulos = _Modulos(entradas: entradas, enRejilla: escritorio);
          return ListView(
            key: Key(escritorio ? 'inicio-escritorio' : 'inicio-movil'),
            padding: const EdgeInsets.all(14),
            children: [
              _Encabezado(plan: perfil.claims?.plan ?? '', patron: perfil.modulos.patron),
              const SizedBox(height: 14),
              modulos,
            ],
          );
        },
      ),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.plan, required this.patron});

  final String plan;
  final String patron;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: RegentaColors.line2),
        borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Tu negocio',
            style: RegentaType.seccion.copyWith(fontSize: 15, color: RegentaColors.ink)),
        const SizedBox(height: 4),
        Text([plan, patron].where((t) => t.isNotEmpty).join(' · '),
            style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
      ]),
    );
  }
}

/// Los módulos activos, que son la navegación de verdad mientras el panel del
/// día no esté construido.
class _Modulos extends StatelessWidget {
  const _Modulos({required this.entradas, required this.enRejilla});

  final List<EntradaDeNavegacion> entradas;
  final bool enRejilla;

  @override
  Widget build(BuildContext context) {
    final destinos = entradas.where((e) => e.ruta != GuardiaDeRutas.inicio).toList();
    if (destinos.isEmpty) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 24),
        child: Text(
          'Tu plan todavía no tiene módulos activos. Pídele a un administrador '
          'que los habilite.',
          style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2),
        ),
      );
    }
    final fichas = [for (final e in destinos) _Ficha(entrada: e)];
    if (!enRejilla) {
      return Column(children: [
        for (final ficha in fichas)
          Padding(padding: const EdgeInsets.only(bottom: 10), child: ficha),
      ]);
    }
    return Wrap(spacing: 10, runSpacing: 10, children: [
      for (final ficha in fichas) SizedBox(width: 260, child: ficha),
    ]);
  }
}

class _Ficha extends StatelessWidget {
  const _Ficha({required this.entrada});

  final EntradaDeNavegacion entrada;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: entrada.titulo,
      container: true,
      excludeSemantics: true,
      child: InkWell(
        onTap: () => context.go(entrada.ruta),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        child: Container(
          constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          decoration: BoxDecoration(
            color: RegentaColors.surface,
            border: Border.all(color: RegentaColors.line2),
            borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          ),
          child: Row(children: [
            Icon(iconoDeRuta[entrada.ruta] ?? Icons.apps,
                size: 20, color: RegentaColors.accent),
            const SizedBox(width: 11),
            Expanded(
              child: Text(entrada.titulo,
                  style: RegentaType.item.copyWith(fontSize: 14, color: RegentaColors.ink)),
            ),
            const Icon(Icons.chevron_right, size: 16, color: RegentaColors.faint),
          ]),
        ),
      ),
    );
  }
}
