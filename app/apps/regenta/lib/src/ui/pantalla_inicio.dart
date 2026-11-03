import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';
import '../navegacion/catalogo.dart';

/// Pantalla de Inicio basada en la propuesta Aurora.
///
/// Refleja fielmente el diseño del panel de control de Regenta:
/// métricas en tiempo real, gráfico de ventas de los últimos 7 días,
/// tabla de últimas ventas y panel lateral de alertas de inventario.
class PantallaInicio extends ConsumerStatefulWidget {
  const PantallaInicio({super.key});

  @override
  ConsumerState<PantallaInicio> createState() => _PantallaInicioState();
}

class _PantallaInicioState extends ConsumerState<PantallaInicio> {
  String _rutaActiva = '/';

  @override
  Widget build(BuildContext context) {
    final perfil = ref.watch(perfilComoNotifierProvider);
    final cola = ref.watch(resumenDeColaProvider).valueOrNull;

    final claims = perfil.claims;
    final entradas = menuDe(claims);
    final destinos = entradas.where((e) => e.ruta != GuardiaDeRutas.inicio).toList();
    final bool sinModulos = claims != null && destinos.isEmpty;

    final nombreNegocio = (claims?.nombreNegocio != null && claims!.nombreNegocio!.isNotEmpty)
        ? claims.nombreNegocio!
        : 'Ferretería El Tornillo';

    const nombreUsuario = 'Donaldo G.';

    final plan = (claims != null && claims.plan.isNotEmpty)
        ? 'Plan ${claims.plan}'
        : 'Plan Profesional';

    return LayoutBuilder(
      builder: (context, constraints) {
        final esEscritorio = constraints.maxWidth >= 1024;

        if (esEscritorio) {
          return _EscritorioLayout(
            nombreNegocio: nombreNegocio,
            nombreUsuario: nombreUsuario,
            plan: plan,
            rutaActiva: _rutaActiva,
            entradas: entradas,
            sinModulos: sinModulos,
            onNavegar: (ruta) {
              setState(() => _rutaActiva = ruta);
              if (ruta != '/') context.go(ruta);
            },
            onCerrarSesion: () => ref.read(motorDeSesionProvider).cerrar(),
            resumenCola: cola,
          );
        } else {
          return _MovilLayout(
            nombreNegocio: nombreNegocio,
            nombreUsuario: nombreUsuario,
            plan: plan,
            rutaActiva: _rutaActiva,
            entradas: entradas,
            sinModulos: sinModulos,
            onNavegar: (ruta) {
              setState(() => _rutaActiva = ruta);
              if (ruta != '/') context.go(ruta);
            },
            onCerrarSesion: () => ref.read(motorDeSesionProvider).cerrar(),
            resumenCola: cola,
          );
        }
      },
    );
  }
}

// =========================================================================
// LAYOUT DE ESCRITORIO
// =========================================================================

class _EscritorioLayout extends StatelessWidget {
  const _EscritorioLayout({
    required this.nombreNegocio,
    required this.nombreUsuario,
    required this.plan,
    required this.rutaActiva,
    required this.entradas,
    required this.sinModulos,
    required this.onNavegar,
    required this.onCerrarSesion,
    this.resumenCola,
  });

  final String nombreNegocio;
  final String nombreUsuario;
  final String plan;
  final String rutaActiva;
  final List<EntradaDeNavegacion> entradas;
  final bool sinModulos;
  final ValueChanged<String> onNavegar;
  final VoidCallback onCerrarSesion;
  final ResumenDeCola? resumenCola;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF3F2FB),
      body: Stack(
        children: [
          // Resplandor Aurora de fondo
          Positioned(
            top: -100,
            right: -100,
            child: Container(
              width: 500,
              height: 500,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFF4FD8CF).withValues(alpha: 0.18),
                    const Color(0xFF5B3FD9).withValues(alpha: 0.05),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: -80,
            right: 200,
            child: Container(
              width: 450,
              height: 450,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFFFF7A93).withValues(alpha: 0.15),
                    const Color(0xFF7B5CFF).withValues(alpha: 0.05),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),

          // Contenedor principal con barra lateral y panel
          Row(
            children: [
              // Barra Lateral
              SizedBox(
                width: 240,
                child: _BarraLateral(
                  nombreNegocio: nombreNegocio,
                  nombreUsuario: nombreUsuario,
                  plan: plan,
                  rutaActiva: rutaActiva,
                  entradas: entradas,
                  onNavegar: onNavegar,
                  onCerrarSesion: onCerrarSesion,
                ),
              ),

              // Área de Contenido Principal
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.only(left: 24, right: 32, top: 24, bottom: 32),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Encabezado superior con búsqueda, sede y nueva venta
                      _EncabezadoSuperior(
                        onNuevaVenta: () => onNavegar('/ventas'),
                      ),
                      const SizedBox(height: 24),

                      if (sinModulos) ...[
                        _AvisoSinModulos(),
                        const SizedBox(height: 16),
                      ],

                      if (resumenCola != null && (resumenCola!.hayConflictos || resumenCola!.pendientes > 0)) ...[
                        _AvisoCola(resumen: resumenCola!),
                        const SizedBox(height: 16),
                      ],

                      // Fila de 4 tarjetas KPI
                      const _FilaKpis(),
                      const SizedBox(height: 24),

                      // Rejilla de Contenido: Gráfico + Tabla (70%) | Alertas (30%)
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            flex: 7,
                            child: Column(
                              children: [
                                const _TarjetaGraficoVentas(),
                                const SizedBox(height: 24),
                                _TarjetaUltimasVentas(onVerTodas: () => onNavegar('/ventas')),
                              ],
                            ),
                          ),
                          const SizedBox(width: 24),
                          Expanded(
                            flex: 3,
                            child: _TarjetaAlertas(
                              onGenerarOrden: () => onNavegar('/inventario'),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// =========================================================================
// LAYOUT MÓVIL
// =========================================================================

class _MovilLayout extends StatelessWidget {
  const _MovilLayout({
    required this.nombreNegocio,
    required this.nombreUsuario,
    required this.plan,
    required this.rutaActiva,
    required this.entradas,
    required this.sinModulos,
    required this.onNavegar,
    required this.onCerrarSesion,
    this.resumenCola,
  });

  final String nombreNegocio;
  final String nombreUsuario;
  final String plan;
  final String rutaActiva;
  final List<EntradaDeNavegacion> entradas;
  final bool sinModulos;
  final ValueChanged<String> onNavegar;
  final VoidCallback onCerrarSesion;
  final ResumenDeCola? resumenCola;

  @override
  Widget build(BuildContext context) {
    final modulosActivos = entradas.where((e) => e.ruta != GuardiaDeRutas.inicio).toList();

    return Scaffold(
      backgroundColor: const Color(0xFFF3F2FB),
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: Row(
          children: [
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: const Color(0xFF5B3FD9),
                borderRadius: BorderRadius.circular(8),
              ),
              alignment: Alignment.center,
              child: const Text('R',
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
            ),
            const SizedBox(width: 8),
            Text('Regenta',
                style: GoogleFonts.plusJakartaSans(
                    fontWeight: FontWeight.bold, fontSize: 18, color: const Color(0xFF1B1838))),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_shopping_cart, color: Color(0xFF5B3FD9)),
            tooltip: 'Nueva venta',
            onPressed: () => onNavegar('/ventas'),
          ),
          IconButton(
            icon: const Icon(Icons.logout, size: 20, color: Color(0xFF55527A)),
            tooltip: 'Cerrar sesión',
            onPressed: onCerrarSesion,
          ),
        ],
      ),
      drawer: Drawer(
        child: _BarraLateral(
          nombreNegocio: nombreNegocio,
          nombreUsuario: nombreUsuario,
          plan: plan,
          rutaActiva: rutaActiva,
          entradas: entradas,
          onNavegar: (ruta) {
            Navigator.of(context).pop();
            onNavegar(ruta);
          },
          onCerrarSesion: onCerrarSesion,
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Título
            Text('Inicio',
                style: GoogleFonts.plusJakartaSans(
                    fontSize: 24, fontWeight: FontWeight.bold, color: const Color(0xFF1B1838))),
            Text('Lunes 2 de noviembre · Sede Centro',
                style: GoogleFonts.manrope(fontSize: 13, color: const Color(0xFF55527A))),
            const SizedBox(height: 16),

            if (sinModulos) ...[
              _AvisoSinModulos(),
              const SizedBox(height: 16),
            ] else if (modulosActivos.isNotEmpty) ...[
              _AccesosRapidosModulos(
                entradas: modulosActivos,
                onNavegar: onNavegar,
              ),
              const SizedBox(height: 16),
            ],

            if (resumenCola != null && (resumenCola!.hayConflictos || resumenCola!.pendientes > 0)) ...[
              _AvisoCola(resumen: resumenCola!),
              const SizedBox(height: 16),
            ],

            // KPIs en móvil
            const _FilaKpis(esMovil: true),
            const SizedBox(height: 20),

            // Gráfico
            const _TarjetaGraficoVentas(),
            const SizedBox(height: 20),

            // Alertas
            _TarjetaAlertas(onGenerarOrden: () => onNavegar('/inventario')),
            const SizedBox(height: 20),

            // Últimas ventas
            _TarjetaUltimasVentas(onVerTodas: () => onNavegar('/ventas')),
          ],
        ),
      ),
    );
  }
}

// =========================================================================
// ACCESOS RÁPIDOS EN MÓVIL
// =========================================================================

class _AccesosRapidosModulos extends StatelessWidget {
  const _AccesosRapidosModulos({
    required this.entradas,
    required this.onNavegar,
  });

  final List<EntradaDeNavegacion> entradas;
  final ValueChanged<String> onNavegar;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: [
        for (final e in entradas)
          Material(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
            elevation: 0,
            child: InkWell(
              onTap: () => onNavegar(e.ruta),
              borderRadius: BorderRadius.circular(14),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: const Color(0xFFE3E0F5)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(iconoDeRuta[e.ruta] ?? Icons.apps, size: 18, color: const Color(0xFF5B3FD9)),
                    const SizedBox(width: 8),
                    Text(
                      e.titulo,
                      style: GoogleFonts.manrope(
                        fontSize: 13.5,
                        fontWeight: FontWeight.bold,
                        color: const Color(0xFF1B1838),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

// =========================================================================
// BARRA LATERAL (SIDEBAR)
// =========================================================================

class _BarraLateral extends StatelessWidget {
  const _BarraLateral({
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
    final modulosActivos = entradas.where((e) => e.ruta != GuardiaDeRutas.inicio).toList();

    return Container(
      margin: const EdgeInsets.only(left: 16, top: 16, bottom: 16),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
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
                  color: const Color(0xFF5B3FD9),
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
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: const Color(0xFF1B1838),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Selector de Negocio
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: const Color(0xFFF9F8FD),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: const Color(0xFF5B3FD9),
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
                        style: GoogleFonts.plusJakartaSans(
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          color: const Color(0xFF1B1838),
                        ),
                      ),
                      Text(
                        plan,
                        style: GoogleFonts.manrope(
                          fontSize: 11,
                          color: const Color(0xFF55527A),
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
                _ItemMenu(
                  icono: Icons.home_rounded,
                  titulo: 'Inicio',
                  activo: rutaActiva == '/',
                  onTap: () => onNavegar('/'),
                ),
                if (entradas.isEmpty) ...[
                  // Muestra de diseño completa de la propuesta Aurora
                  _ItemMenu(
                    icono: Icons.shopping_cart_outlined,
                    titulo: 'Ventas',
                    activo: rutaActiva == '/ventas',
                    onTap: () => onNavegar('/ventas'),
                  ),
                  _ItemMenu(
                    icono: Icons.point_of_sale_outlined,
                    titulo: 'Caja',
                    activo: rutaActiva == '/caja',
                    onTap: () => onNavegar('/ventas'),
                  ),
                  _ItemMenu(
                    icono: Icons.inventory_2_outlined,
                    titulo: 'Inventario',
                    activo: rutaActiva == '/inventario',
                    badge: '7',
                    onTap: () => onNavegar('/inventario'),
                  ),
                  _ItemMenu(
                    icono: Icons.local_shipping_outlined,
                    titulo: 'Compras',
                    activo: rutaActiva == '/compras',
                    onTap: () => onNavegar('/inventario'),
                  ),
                  _ItemMenu(
                    icono: Icons.people_outline_rounded,
                    titulo: 'Clientes',
                    activo: rutaActiva == '/clientes',
                    onTap: () => onNavegar('/clientes'),
                  ),
                  _ItemMenu(
                    icono: Icons.receipt_long_outlined,
                    titulo: 'Facturación',
                    activo: rutaActiva == '/facturas',
                    onTap: () => onNavegar('/facturas'),
                  ),
                  _ItemMenu(
                    icono: Icons.bar_chart_rounded,
                    titulo: 'Reportes',
                    activo: rutaActiva == '/reportes',
                    onTap: () => onNavegar('/reportes'),
                  ),
                  _ItemMenu(
                    icono: Icons.wb_sunny_outlined,
                    titulo: 'Configuración',
                    activo: rutaActiva == '/configuracion',
                    onTap: () {},
                  ),
                ] else ...[
                  // Menú conectado según los permisos y módulos activos del negocio
                  for (final entrada in modulosActivos)
                    _ItemMenu(
                      icono: iconoDeRuta[entrada.ruta] ?? Icons.apps,
                      titulo: entrada.titulo,
                      activo: rutaActiva == entrada.ruta,
                      badge: entrada.modulo == 'INVENTARIO' ? '7' : null,
                      onTap: () => onNavegar(entrada.ruta),
                    ),
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
                      color: Color(0xFFFFA000),
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
                          style: GoogleFonts.plusJakartaSans(
                            fontWeight: FontWeight.bold,
                            fontSize: 13.5,
                            color: const Color(0xFF1B1838),
                          ),
                        ),
                        Text(
                          'Administrador',
                          style: GoogleFonts.manrope(
                            fontSize: 11.5,
                            color: const Color(0xFF55527A),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(Icons.logout_rounded, size: 18, color: Color(0xFFA2A0B6)),
                ],
              ),
            ),
          ),
        ],
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
        color: activo ? const Color(0xFF5B3FD9) : Colors.transparent,
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
                  color: activo ? Colors.white : const Color(0xFF55527A),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    titulo,
                    style: GoogleFonts.manrope(
                      fontSize: 14,
                      fontWeight: activo ? FontWeight.bold : FontWeight.w500,
                      color: activo ? Colors.white : const Color(0xFF1B1838),
                    ),
                  ),
                ),
                if (badge != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF7E1E4),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      badge!,
                      style: const TextStyle(
                        color: Color(0xFFC42840),
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

// =========================================================================
// ENCABEZADO SUPERIOR (HEADER)
// =========================================================================

class _EncabezadoSuperior extends StatelessWidget {
  const _EncabezadoSuperior({required this.onNuevaVenta});

  final VoidCallback onNuevaVenta;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // Título y fecha
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Inicio',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: const Color(0xFF1B1838),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              'Lunes 2 de noviembre · Sede Centro',
              style: GoogleFonts.manrope(
                fontSize: 13,
                color: const Color(0xFF55527A),
              ),
            ),
          ],
        ),
        const Spacer(),

        // Barra de Búsqueda
        Container(
          width: 270,
          height: 42,
          padding: const EdgeInsets.symmetric(horizontal: 14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: const Color(0xFFE3E0F5)),
          ),
          child: Row(
            children: [
              const Icon(Icons.search, size: 18, color: Color(0xFFA2A0B6)),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Buscar productos, clientes, fa...',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.manrope(
                    fontSize: 12.5,
                    color: const Color(0xFFA2A0B6),
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F2FB),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  'Ctrl K',
                  style: GoogleFonts.manrope(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF55527A),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),

        // Selector de Sede
        Container(
          height: 42,
          padding: const EdgeInsets.symmetric(horizontal: 14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: const Color(0xFFE3E0F5)),
          ),
          child: Row(
            children: [
              const Icon(Icons.location_on_outlined, size: 16, color: Color(0xFF55527A)),
              const SizedBox(width: 6),
              Text(
                'Sede Centro',
                style: GoogleFonts.manrope(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: const Color(0xFF1B1838),
                ),
              ),
              const SizedBox(width: 4),
              const Icon(Icons.keyboard_arrow_down_rounded, size: 18, color: Color(0xFF55527A)),
            ],
          ),
        ),
        const SizedBox(width: 12),

        // Botón de Notificaciones
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: Colors.white,
            shape: BoxShape.circle,
            border: Border.all(color: const Color(0xFFE3E0F5)),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              const Icon(Icons.notifications_none_rounded, size: 20, color: Color(0xFF55527A)),
              Positioned(
                top: 10,
                right: 11,
                child: Container(
                  width: 7,
                  height: 7,
                  decoration: const BoxDecoration(
                    color: Color(0xFFC42840),
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),

        // Botón "+ Nueva venta"
        FilledButton.icon(
          onPressed: onNuevaVenta,
          icon: const Icon(Icons.add, size: 18),
          label: Text(
            'Nueva venta',
            style: GoogleFonts.plusJakartaSans(
              fontSize: 13.5,
              fontWeight: FontWeight.bold,
            ),
          ),
          style: FilledButton.styleFrom(
            backgroundColor: const Color(0xFF5B3FD9),
            foregroundColor: Colors.white,
            shape: const StadiumBorder(),
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          ),
        ),
      ],
    );
  }
}

// =========================================================================
// FILA DE KPIS
// =========================================================================

class _FilaKpis extends StatelessWidget {
  const _FilaKpis({this.esMovil = false});

  final bool esMovil;

  @override
  Widget build(BuildContext context) {
    final tarjetas = [
      // 1. Ventas hoy (Tarjeta destacada púrpura)
      Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(24),
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF5B3FD9), Color(0xFF7B5CFF)],
          ),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5B3FD9).withValues(alpha: 0.25),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Ventas hoy',
                  style: GoogleFonts.manrope(
                    color: Colors.white.withValues(alpha: 0.9),
                    fontSize: 13.5,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.trending_up, color: Colors.white, size: 18),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              r'$ 4.182.900',
              style: GoogleFonts.manrope(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text(
                    '↑ 12%',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  '23 ventas · vs. ayer',
                  style: GoogleFonts.manrope(
                    color: Colors.white.withValues(alpha: 0.85),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),

      // 2. Margen bruto
      _KpiBlanca(
        titulo: 'Margen bruto',
        valor: '38,4 %',
        subtitulo: r'$ 1.606.234 de ganancia',
        icono: Icons.bar_chart_rounded,
        colorIcono: const Color(0xFF0B7F7A),
        fondoIcono: const Color(0xFFDDEDEC),
      ),

      // 3. Ticket promedio
      _KpiBlanca(
        titulo: 'Ticket promedio',
        valor: r'$ 181.865',
        subtitulo: 'sobre ventas confirmadas',
        icono: Icons.shopping_cart_outlined,
        colorIcono: const Color(0xFF9A5800),
        fondoIcono: const Color(0xFFF1E8DB),
      ),

      // 4. Caja abierta
      _KpiBlanca(
        titulo: 'Caja abierta',
        valor: r'$ 612.400',
        subtitulo: 'Caja 1 · desde 7:12 a. m.',
        icono: Icons.point_of_sale_outlined,
        colorIcono: const Color(0xFFC42840),
        fondoIcono: const Color(0xFFF7E1E4),
      ),
    ];

    if (esMovil) {
      return Column(
        children: [
          for (final t in tarjetas)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: SizedBox(width: double.infinity, child: t),
            ),
        ],
      );
    }

    return Row(
      children: [
        for (int i = 0; i < tarjetas.length; i++) ...[
          if (i > 0) const SizedBox(width: 18),
          Expanded(child: tarjetas[i]),
        ],
      ],
    );
  }
}

class _KpiBlanca extends StatelessWidget {
  const _KpiBlanca({
    required this.titulo,
    required this.valor,
    required this.subtitulo,
    required this.icono,
    required this.colorIcono,
    required this.fondoIcono,
  });

  final String titulo;
  final String valor;
  final String subtitulo;
  final IconData icono;
  final Color colorIcono;
  final Color fondoIcono;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE3E0F5)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                titulo,
                style: GoogleFonts.manrope(
                  color: const Color(0xFF55527A),
                  fontSize: 13.5,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: fondoIcono,
                  shape: BoxShape.circle,
                ),
                child: Icon(icono, color: colorIcono, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            valor,
            style: GoogleFonts.manrope(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: const Color(0xFF1B1838),
              fontFeatures: const [FontFeature.tabularFigures()],
            ),
          ),
          const SizedBox(height: 12),
          Text(
            subtitulo,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.manrope(
              color: const Color(0xFF55527A),
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }
}

// =========================================================================
// TARJETA DE GRÁFICO DE VENTAS (7 DÍAS)
// =========================================================================

class _TarjetaGraficoVentas extends StatelessWidget {
  const _TarjetaGraficoVentas();

  @override
  Widget build(BuildContext context) {
    final datos = [
      (dia: 'mar', valor: 3.1, esHoy: false),
      (dia: 'mié', valor: 2.2, esHoy: false),
      (dia: 'jue', valor: 3.5, esHoy: false),
      (dia: 'vie', valor: 4.0, esHoy: false),
      (dia: 'sáb', valor: 4.9, esHoy: false),
      (dia: 'dom', valor: 1.9, esHoy: false),
      (dia: 'hoy', valor: 4.2, esHoy: true),
    ];

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE3E0F5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  'Ventas de los últimos 7 días',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF1B1838),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                'millones de pesos',
                style: GoogleFonts.manrope(
                  fontSize: 12.5,
                  color: const Color(0xFFA2A0B6),
                ),
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Área del gráfico de barras
          SizedBox(
            height: 180,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                // Eje Y con etiquetas
                Column(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('4 M', style: GoogleFonts.manrope(fontSize: 11, color: const Color(0xFFA2A0B6))),
                    Text('2 M', style: GoogleFonts.manrope(fontSize: 11, color: const Color(0xFFA2A0B6))),
                    Text('0', style: GoogleFonts.manrope(fontSize: 11, color: const Color(0xFFA2A0B6))),
                  ],
                ),
                const SizedBox(width: 14),

                // Barras
                Expanded(
                  child: Stack(
                    children: [
                      // Líneas guía horizontales
                      Column(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: const [
                          Divider(color: Color(0xFFE4E3EC), thickness: 1, height: 1),
                          Divider(color: Color(0xFFE4E3EC), thickness: 1, height: 1),
                          Divider(color: Color(0xFFE4E3EC), thickness: 1, height: 1),
                        ],
                      ),

                      // Barras alineadas
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          for (final d in datos)
                            _BarraItem(
                              dia: d.dia,
                              valor: d.valor,
                              esHoy: d.esHoy,
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BarraItem extends StatelessWidget {
  const _BarraItem({
    required this.dia,
    required this.valor,
    required this.esHoy,
  });

  final String dia;
  final double valor;
  final bool esHoy;

  @override
  Widget build(BuildContext context) {
    // Escala máxima = 5.5 M
    const maxVal = 5.5;
    final alturaBarra = (valor / maxVal) * 135;

    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Text(
          valor.toString().replaceAll('.', ','),
          style: GoogleFonts.manrope(
            fontSize: 11,
            fontWeight: esHoy ? FontWeight.bold : FontWeight.w500,
            color: esHoy ? const Color(0xFF5B3FD9) : const Color(0xFF55527A),
          ),
        ),
        const SizedBox(height: 6),
        Container(
          width: 30,
          height: alturaBarra,
          decoration: BoxDecoration(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
            gradient: esHoy
                ? const LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [Color(0xFF5B3FD9), Color(0xFFCC3359)],
                  )
                : const LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [Color(0xFF4FD8CF), Color(0xFFA897FF)],
                  ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          dia,
          style: GoogleFonts.manrope(
            fontSize: 12,
            fontWeight: esHoy ? FontWeight.bold : FontWeight.w500,
            color: esHoy ? const Color(0xFF1B1838) : const Color(0xFF55527A),
          ),
        ),
      ],
    );
  }
}

// =========================================================================
// TARJETA DE ÚLTIMAS VENTAS
// =========================================================================

class _TarjetaUltimasVentas extends StatelessWidget {
  const _TarjetaUltimasVentas({required this.onVerTodas});

  final VoidCallback onVerTodas;

  @override
  Widget build(BuildContext context) {
    final ventas = [
      (doc: 'FV-1047', cliente: 'Materiales Cruz S.A.S.', hora: '10:42', medio: 'Tarjeta', total: r'$ 1.284.500', estado: EstadoDocumento.pagado),
      (doc: 'FV-1046', cliente: 'Consumidor final', hora: '10:31', medio: 'Efectivo', total: r'$ 86.900', estado: EstadoDocumento.pagado),
      (doc: 'FV-1045', cliente: 'Jorge Rendón', hora: '10:05', medio: 'Crédito 30 d', total: r'$ 412.000', estado: EstadoDocumento.pendiente),
      (doc: 'FV-1044', cliente: 'Constructora Andes', hora: '09:48', medio: 'Transferencia', total: r'$ 245.300', estado: EstadoDocumento.anulado),
    ];

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE3E0F5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  'Últimas ventas',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF1B1838),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              InkWell(
                onTap: onVerTodas,
                child: Text(
                  'Ver todas',
                  style: GoogleFonts.manrope(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF5B3FD9),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),

          // Tabla con ancho acotado para desplazamiento horizontal
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: SizedBox(
              width: 580,
              child: Column(
                children: [
                  // Cabecera
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Row(
                      children: [
                        _columnaCabecera('Documento', flex: 2),
                        _columnaCabecera('Cliente', flex: 4),
                        _columnaCabecera('Hora', flex: 2),
                        _columnaCabecera('Medio', flex: 3),
                        _columnaCabecera('Total', flex: 3, derecha: true),
                        _columnaCabecera('Estado', flex: 3, derecha: true),
                      ],
                    ),
                  ),
                  const Divider(color: Color(0xFFE3E0F5), height: 1),

                  // Filas
                  for (final v in ventas) ...[
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: Row(
                        children: [
                          Expanded(
                            flex: 2,
                            child: Text(
                              v.doc,
                              style: GoogleFonts.manrope(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: const Color(0xFF1B1838),
                              ),
                            ),
                          ),
                          Expanded(
                            flex: 4,
                            child: Text(
                              v.cliente,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: GoogleFonts.manrope(
                                fontSize: 13,
                                color: const Color(0xFF1B1838),
                              ),
                            ),
                          ),
                          Expanded(
                            flex: 2,
                            child: Text(
                              v.hora,
                              style: GoogleFonts.manrope(
                                fontSize: 13,
                                color: const Color(0xFF55527A),
                              ),
                            ),
                          ),
                          Expanded(
                            flex: 3,
                            child: Text(
                              v.medio,
                              style: GoogleFonts.manrope(
                                fontSize: 13,
                                color: const Color(0xFF55527A),
                              ),
                            ),
                          ),
                          Expanded(
                            flex: 3,
                            child: Text(
                              v.total,
                              textAlign: TextAlign.right,
                              style: GoogleFonts.manrope(
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                                color: const Color(0xFF1B1838),
                                decoration: v.estado == EstadoDocumento.anulado
                                    ? TextDecoration.lineThrough
                                    : null,
                                fontFeatures: const [FontFeature.tabularFigures()],
                              ),
                            ),
                          ),
                          Expanded(
                            flex: 3,
                            child: Align(
                              alignment: Alignment.centerRight,
                              child: _ChipEstado(estado: v.estado),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Divider(color: Color(0xFFE4E3EC), height: 1),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _columnaCabecera(String texto, {required int flex, bool derecha = false}) {
    return Expanded(
      flex: flex,
      child: Text(
        texto,
        textAlign: derecha ? TextAlign.right : TextAlign.left,
        style: GoogleFonts.manrope(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: const Color(0xFFA2A0B6),
        ),
      ),
    );
  }
}

class _ChipEstado extends StatelessWidget {
  const _ChipEstado({required this.estado});

  final EstadoDocumento estado;

  @override
  Widget build(BuildContext context) {
    Color fondo;
    Color texto;
    String etiqueta;

    switch (estado) {
      case EstadoDocumento.pagado:
        fondo = const Color(0xFFDEECE6);
        texto = const Color(0xFF12784A);
        etiqueta = '• Pagado';
        break;
      case EstadoDocumento.pendiente:
        fondo = const Color(0xFFF1E8DB);
        texto = const Color(0xFF9A5800);
        etiqueta = '• Pendiente';
        break;
      case EstadoDocumento.anulado:
        fondo = const Color(0xFFEEEEF2);
        texto = const Color(0xFF55527A);
        etiqueta = '- Anulado';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        etiqueta,
        style: GoogleFonts.manrope(
          fontSize: 11.5,
          fontWeight: FontWeight.bold,
          color: texto,
        ),
      ),
    );
  }
}

// =========================================================================
// TARJETA DE ALERTAS
// =========================================================================

class _TarjetaAlertas extends StatelessWidget {
  const _TarjetaAlertas({required this.onGenerarOrden});

  final VoidCallback onGenerarOrden;

  @override
  Widget build(BuildContext context) {
    final alertas = [
      (
        titulo: 'Broca SDS 12 mm agotada',
        subtitulo: '0 und · mínimo 10 · Bodega Centro',
        tiempo: 'hace 12 min',
        esCritica: true,
      ),
      (
        titulo: 'Breaker 1x20 A bajo mínimo',
        subtitulo: '9 und · mínimo 12',
        tiempo: 'hace 1 h',
        esCritica: false,
      ),
      (
        titulo: 'Pintura vinilo blanco 1 gl',
        subtitulo: '12 und · mínimo 15',
        tiempo: 'hace 2 h',
        esCritica: false,
      ),
    ];

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE3E0F5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Alertas',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: const Color(0xFF1B1838),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFF7E1E4),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Text(
                  '• 4 nuevas',
                  style: TextStyle(
                    color: Color(0xFFC42840),
                    fontWeight: FontWeight.bold,
                    fontSize: 11.5,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Lista de Alertas
          for (final a in alertas)
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: a.esCritica ? const Color(0xFFF7E1E4) : const Color(0xFFF1E8DB),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    alignment: Alignment.center,
                    child: Icon(
                      Icons.warning_amber_rounded,
                      size: 20,
                      color: a.esCritica ? const Color(0xFFC42840) : const Color(0xFF9A5800),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                a.titulo,
                                style: GoogleFonts.manrope(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                  color: const Color(0xFF1B1838),
                                ),
                              ),
                            ),
                            Text(
                              a.tiempo,
                              style: GoogleFonts.manrope(
                                fontSize: 10.5,
                                color: const Color(0xFFA2A0B6),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 3),
                        Text(
                          a.subtitulo,
                          style: GoogleFonts.manrope(
                            fontSize: 11.5,
                            color: const Color(0xFF55527A),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 12),

          // Botón inferior: Generar orden de compra
          SizedBox(
            width: double.infinity,
            height: 46,
            child: FilledButton.icon(
              onPressed: onGenerarOrden,
              icon: const Icon(Icons.local_shipping_outlined, size: 18),
              label: Text(
                'Generar orden de compra',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                ),
              ),
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFE8E4FA),
                foregroundColor: const Color(0xFF5B3FD9),
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// =========================================================================
// AVISOS
// =========================================================================

class _AvisoSinModulos extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF1E8DB),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF9A5800).withValues(alpha: 0.3)),
      ),
      child: Text(
        'Tu plan todavía no tiene módulos activos. Pídele a un administrador que los habilite.',
        style: GoogleFonts.manrope(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: const Color(0xFF9A5800),
        ),
      ),
    );
  }
}

class _AvisoCola extends StatelessWidget {
  const _AvisoCola({required this.resumen});

  final ResumenDeCola resumen;

  @override
  Widget build(BuildContext context) {
    final alerta = resumen.hayConflictos;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: alerta ? const Color(0xFFF7E1E4) : const Color(0xFFF1E8DB),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            alerta ? Icons.error_outline : Icons.cloud_upload_outlined,
            size: 18,
            color: alerta ? const Color(0xFFC42840) : const Color(0xFF9A5800),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              alerta
                  ? '${resumen.conflictos} operación(es) chocaron al subir y hay que revisarlas.'
                  : '${resumen.pendientes} operación(es) esperan conexión para sincronizar.',
              style: GoogleFonts.manrope(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: const Color(0xFF1B1838),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
