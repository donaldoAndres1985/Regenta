import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';
import '../navegacion/catalogo.dart';
import 'barra_lateral.dart';
import 'carcasa_con_menu.dart';

/// Pantalla de Inicio basada en la propuesta Pastel.
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
    final destinos = entradas
        .where((e) => e.ruta != GuardiaDeRutas.inicio)
        .toList();
    final bool sinModulos = claims != null && destinos.isEmpty;

    final nombreNegocio =
        (claims?.nombreNegocio != null && claims!.nombreNegocio!.isNotEmpty)
        ? claims.nombreNegocio!
        : 'Ferretería El Tornillo';

    const nombreUsuario = 'Donaldo G.';

    final plan = (claims != null && claims.plan.isNotEmpty)
        ? 'Plan ${claims.plan}'
        : 'Plan Profesional';

    return LayoutBuilder(
      builder: (context, constraints) {
        // En escritorio el menú lo pone la carcasa; aquí solo va el contenido.
        final esEscritorio = MenuFijo.presente(context);

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
      backgroundColor: RegentaColors.paper,
      body: Stack(
        children: [
          Row(
            children: [
              // Área de Contenido Principal
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.only(
                    left: 24,
                    right: 32,
                    top: 24,
                    bottom: 32,
                  ),
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

                      if (resumenCola != null &&
                          (resumenCola!.hayConflictos ||
                              resumenCola!.pendientes > 0)) ...[
                        _AvisoCola(resumen: resumenCola!),
                        const SizedBox(height: 16),
                      ],

                      // Fila de 4 tarjetas KPI
                      const _FilaKpis(),
                      const SizedBox(height: 24),

                      // Rejilla de Contenido: Gráfico + Tabla (70%) | Alertas (30%)
                      // La columna izquierda fija el alto; Alertas se estira
                      // hasta su fondo para que la tarjeta no quede cortada.
                      Stack(
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                flex: 7,
                                child: Column(
                                  children: [
                                    const _TarjetaGraficoVentas(),
                                    const SizedBox(height: 24),
                                    _TarjetaUltimasVentas(
                                      onVerTodas: () => onNavegar('/ventas'),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 24),
                              const Spacer(flex: 3),
                            ],
                          ),
                          Positioned.fill(
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                const Spacer(flex: 7),
                                const SizedBox(width: 24),
                                Expanded(
                                  flex: 3,
                                  child: _TarjetaAlertas(
                                    onGenerarOrden: () =>
                                        onNavegar('/inventario'),
                                  ),
                                ),
                              ],
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
    final modulosActivos = entradas
        .where((e) => e.ruta != GuardiaDeRutas.inicio)
        .toList();

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: Row(
          children: [
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: RegentaColors.accent,
                borderRadius: BorderRadius.circular(8),
              ),
              alignment: Alignment.center,
              child: const Text(
                'R',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Text(
              'Regenta',
              style: PastelTheme.titulo(
                fontWeight: FontWeight.bold,
                fontSize: 18,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.add_shopping_cart,
              color: RegentaColors.accent,
            ),
            tooltip: 'Nueva venta',
            onPressed: () => onNavegar('/ventas'),
          ),
          IconButton(
            icon: const Icon(Icons.logout, size: 20, color: RegentaColors.ink2),
            tooltip: 'Cerrar sesión',
            onPressed: onCerrarSesion,
          ),
        ],
      ),
      drawer: Drawer(
        child: BarraLateral(
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
            Text(
              'Inicio',
              style: PastelTheme.titulo(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: RegentaColors.ink,
              ),
            ),
            Text(
              'Lunes 2 de noviembre · Sede Centro',
              style: PastelTheme.cuerpo(
                fontSize: 13,
                color: RegentaColors.ink2,
              ),
            ),
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

            if (resumenCola != null &&
                (resumenCola!.hayConflictos ||
                    resumenCola!.pendientes > 0)) ...[
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
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: RegentaColors.line),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      iconoDeRuta[e.ruta] ?? Icons.apps,
                      size: 18,
                      color: RegentaColors.accent,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      e.titulo,
                      style: PastelTheme.cuerpo(
                        fontSize: 13.5,
                        fontWeight: FontWeight.bold,
                        color: RegentaColors.ink,
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
              style: PastelTheme.titulo(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: RegentaColors.ink,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              'Lunes 2 de noviembre · Sede Centro',
              style: PastelTheme.cuerpo(
                fontSize: 13,
                color: RegentaColors.ink2,
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
            border: Border.all(color: RegentaColors.line),
          ),
          child: Row(
            children: [
              const Icon(Icons.search, size: 18, color: RegentaColors.faint),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Buscar productos, clientes, fa...',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: PastelTheme.cuerpo(
                    fontSize: 12.5,
                    color: RegentaColors.faint,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: RegentaColors.paper,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  'Ctrl K',
                  style: PastelTheme.cuerpo(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.ink2,
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
            border: Border.all(color: RegentaColors.line),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.location_on_outlined,
                size: 16,
                color: RegentaColors.ink2,
              ),
              const SizedBox(width: 6),
              Text(
                'Sede Centro',
                style: PastelTheme.cuerpo(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.ink,
                ),
              ),
              const SizedBox(width: 4),
              const Icon(
                Icons.keyboard_arrow_down_rounded,
                size: 18,
                color: RegentaColors.ink2,
              ),
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
            border: Border.all(color: RegentaColors.line),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              const Icon(
                Icons.notifications_none_rounded,
                size: 20,
                color: RegentaColors.ink2,
              ),
              Positioned(
                top: 10,
                right: 11,
                child: Container(
                  width: 7,
                  height: 7,
                  decoration: const BoxDecoration(
                    color: RegentaColors.crit,
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
            style: PastelTheme.titulo(
              fontSize: 13.5,
              fontWeight: FontWeight.bold,
            ),
          ),
          style: FilledButton.styleFrom(
            backgroundColor: RegentaColors.accent,
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
      // 1. Ventas hoy
      _KpiBlanca(
        titulo: 'Ventas hoy',
        valor: r'$ 4.182.900',
        subtitulo: '23 ventas · vs. ayer',
        variacion: '12 %',
        icono: Icons.trending_up,
        colorIcono: RegentaColors.accent,
        fondo: RegentaColors.moduloVentas,
      ),

      // 2. Margen bruto
      _KpiBlanca(
        titulo: 'Margen bruto',
        valor: '38,4 %',
        subtitulo: r'$ 1.606.234 de ganancia',
        icono: Icons.bar_chart_rounded,
        colorIcono: RegentaColors.ok,
        fondo: RegentaColors.moduloInventario,
      ),

      // 3. Ticket promedio
      _KpiBlanca(
        titulo: 'Ticket promedio',
        valor: r'$ 181.865',
        subtitulo: 'sobre ventas confirmadas',
        icono: Icons.shopping_cart_outlined,
        colorIcono: RegentaColors.comanda,
        fondo: RegentaColors.moduloClientes,
      ),

      // 4. Caja abierta
      _KpiBlanca(
        titulo: 'Caja abierta',
        valor: r'$ 612.400',
        subtitulo: 'Caja 1 · desde 7:12 a. m.',
        icono: Icons.point_of_sale_outlined,
        colorIcono: RegentaColors.accent,
        fondo: RegentaColors.moduloCaja,
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
    required this.fondo,
    this.variacion,
  });

  final String titulo;
  final String valor;
  final String subtitulo;
  final IconData icono;
  final Color colorIcono;
  final Color fondo;
  final String? variacion;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(20),
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
                style: PastelTheme.cuerpo(
                  color: RegentaColors.ink2,
                  fontSize: 13.5,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: RegentaColors.surface,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icono, color: colorIcono, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            valor,
            style: PastelTheme.cuerpo(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: RegentaColors.ink,
              fontFeatures: const [FontFeature.tabularFigures()],
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              if (variacion != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 7,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: RegentaColors.okSoft,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.arrow_upward,
                        size: 11,
                        color: RegentaColors.ok,
                      ),
                      const SizedBox(width: 2),
                      Text(
                        variacion!,
                        style: PastelTheme.cuerpo(
                          color: RegentaColors.ok,
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
              ],
              Flexible(
                child: Text(
                  subtitulo,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: PastelTheme.cuerpo(
                    color: RegentaColors.ink2,
                    fontSize: 12,
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
        border: Border.all(color: RegentaColors.line),
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
                  style: PastelTheme.titulo(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.ink,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                'millones de pesos',
                style: PastelTheme.cuerpo(
                  fontSize: 12.5,
                  color: RegentaColors.faint,
                ),
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Línea con área sombreada: la tendencia de la semana de un vistazo.
          SizedBox(
            height: 190,
            width: double.infinity,
            child: CustomPaint(
              key: const ValueKey('grafico-ventas-linea'),
              painter: _PintorLineaDeVentas(datos),
            ),
          ),
        ],
      ),
    );
  }
}

/// Ventas por día como línea: eje en millones, guías en 0, 2 M y 4 M, el valor
/// sobre cada punto y hoy destacado.
class _PintorLineaDeVentas extends CustomPainter {
  _PintorLineaDeVentas(this.datos);

  final List<({String dia, double valor, bool esHoy})> datos;

  static const _maximo = 5.5;
  static const _ejeY = 34.0;
  static const _arriba = 22.0;
  static const _abajo = 28.0;

  TextPainter _texto(String t, TextStyle estilo) => TextPainter(
    text: TextSpan(text: t, style: estilo),
    textDirection: TextDirection.ltr,
  )..layout();

  @override
  void paint(Canvas canvas, Size size) {
    final alto = size.height - _arriba - _abajo;
    final ancho = size.width - _ejeY;
    double y(double v) => _arriba + alto * (1 - v / _maximo);

    // Guías y etiquetas del eje.
    final guia = Paint()
      ..color = RegentaColors.line2
      ..strokeWidth = 1;
    final estiloEje = PastelTheme.cuerpo(
      fontSize: 11,
      color: RegentaColors.faint,
    );
    for (final (v, etiqueta) in [(0.0, '0'), (2.0, '2 M'), (4.0, '4 M')]) {
      canvas.drawLine(Offset(_ejeY, y(v)), Offset(size.width, y(v)), guia);
      final t = _texto(etiqueta, estiloEje);
      t.paint(canvas, Offset(0, y(v) - t.height / 2));
    }

    // Un punto por día, centrado en su franja.
    final paso = ancho / datos.length;
    final puntos = [
      for (var k = 0; k < datos.length; k++)
        Offset(_ejeY + paso * (k + 0.5), y(datos[k].valor)),
    ];

    // Área bajo la línea.
    final area = Path()..moveTo(puntos.first.dx, y(0));
    for (final p in puntos) {
      area.lineTo(p.dx, p.dy);
    }
    area
      ..lineTo(puntos.last.dx, y(0))
      ..close();
    canvas.drawPath(
      area,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            RegentaColors.accent.withValues(alpha: 0.28),
            RegentaColors.accent.withValues(alpha: 0.02),
          ],
        ).createShader(Rect.fromLTRB(0, _arriba, size.width, y(0))),
    );

    // La línea.
    final linea = Path()..moveTo(puntos.first.dx, puntos.first.dy);
    for (final p in puntos.skip(1)) {
      linea.lineTo(p.dx, p.dy);
    }
    canvas.drawPath(
      linea,
      Paint()
        ..color = RegentaColors.accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.5
        ..strokeJoin = StrokeJoin.round,
    );

    // Puntos, valores y días.
    for (var k = 0; k < datos.length; k++) {
      final d = datos[k];
      final p = puntos[k];
      if (d.esHoy) {
        canvas.drawCircle(p, 6.5, Paint()..color = RegentaColors.surface);
        canvas.drawCircle(p, 5, Paint()..color = RegentaColors.ok);
      } else {
        canvas.drawCircle(p, 4.5, Paint()..color = RegentaColors.surface);
        canvas.drawCircle(
          p,
          4.5,
          Paint()
            ..color = RegentaColors.accent
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2,
        );
      }

      final valor = _texto(
        d.valor.toStringAsFixed(1).replaceAll('.', ','),
        PastelTheme.cuerpo(
          fontSize: 12,
          fontWeight: d.esHoy ? FontWeight.w800 : FontWeight.w600,
          color: d.esHoy ? RegentaColors.ink : RegentaColors.ink2,
          fontFeatures: const [FontFeature.tabularFigures()],
        ),
      );
      valor.paint(
        canvas,
        Offset(p.dx - valor.width / 2, p.dy - valor.height - 7),
      );

      final dia = _texto(
        d.dia,
        PastelTheme.cuerpo(
          fontSize: 12,
          fontWeight: d.esHoy ? FontWeight.w800 : FontWeight.w500,
          color: d.esHoy ? RegentaColors.ink : RegentaColors.ink2,
        ),
      );
      dia.paint(canvas, Offset(p.dx - dia.width / 2, y(0) + 8));
    }
  }

  @override
  bool shouldRepaint(_PintorLineaDeVentas old) => old.datos != datos;
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
      (
        doc: 'FV-1047',
        cliente: 'Materiales Cruz S.A.S.',
        hora: '10:42',
        medio: 'Tarjeta',
        total: r'$ 1.284.500',
        estado: EstadoDocumento.pagado,
      ),
      (
        doc: 'FV-1046',
        cliente: 'Consumidor final',
        hora: '10:31',
        medio: 'Efectivo',
        total: r'$ 86.900',
        estado: EstadoDocumento.pagado,
      ),
      (
        doc: 'FV-1045',
        cliente: 'Jorge Rendón',
        hora: '10:05',
        medio: 'Crédito 30 d',
        total: r'$ 412.000',
        estado: EstadoDocumento.pendiente,
      ),
      (
        doc: 'FV-1044',
        cliente: 'Constructora Andes',
        hora: '09:48',
        medio: 'Transferencia',
        total: r'$ 245.300',
        estado: EstadoDocumento.anulado,
      ),
    ];

    return Container(
      key: const ValueKey('tarjeta-ultimas-ventas'),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: RegentaColors.line),
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
                  style: PastelTheme.titulo(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.ink,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              InkWell(
                onTap: onVerTodas,
                child: Text(
                  'Ver todas',
                  style: PastelTheme.cuerpo(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.accent,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),

          // La tabla ocupa todo el ancho de la tarjeta; solo si no caben las
          // columnas (580 px) se desplaza en horizontal.
          LayoutBuilder(
            builder: (context, restricciones) => SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: SizedBox(
                key: const ValueKey('tabla-ultimas-ventas'),
                width: restricciones.maxWidth < 580
                    ? 580
                    : restricciones.maxWidth,
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
                    const Divider(color: RegentaColors.line, height: 1),

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
                                style: PastelTheme.cuerpo(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: RegentaColors.ink,
                                ),
                              ),
                            ),
                            Expanded(
                              flex: 4,
                              child: Text(
                                v.cliente,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: PastelTheme.cuerpo(
                                  fontSize: 13,
                                  color: RegentaColors.ink,
                                ),
                              ),
                            ),
                            Expanded(
                              flex: 2,
                              child: Text(
                                v.hora,
                                style: PastelTheme.cuerpo(
                                  fontSize: 13,
                                  color: RegentaColors.ink2,
                                ),
                              ),
                            ),
                            Expanded(
                              flex: 3,
                              child: Text(
                                v.medio,
                                style: PastelTheme.cuerpo(
                                  fontSize: 13,
                                  color: RegentaColors.ink2,
                                ),
                              ),
                            ),
                            Expanded(
                              flex: 3,
                              child: Text(
                                v.total,
                                textAlign: TextAlign.right,
                                style: PastelTheme.cuerpo(
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                  color: RegentaColors.ink,
                                  decoration:
                                      v.estado == EstadoDocumento.anulado
                                      ? TextDecoration.lineThrough
                                      : null,
                                  fontFeatures: const [
                                    FontFeature.tabularFigures(),
                                  ],
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
                      const Divider(color: RegentaColors.line2, height: 1),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _columnaCabecera(
    String texto, {
    required int flex,
    bool derecha = false,
  }) {
    return Expanded(
      flex: flex,
      child: Text(
        texto,
        textAlign: derecha ? TextAlign.right : TextAlign.left,
        style: PastelTheme.cuerpo(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: RegentaColors.faint,
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
        fondo = RegentaColors.okSoft;
        texto = RegentaColors.ok;
        etiqueta = '• Pagado';
        break;
      case EstadoDocumento.pendiente:
        fondo = RegentaColors.warnSoft;
        texto = RegentaColors.warn;
        etiqueta = '• Pendiente';
        break;
      case EstadoDocumento.anulado:
        fondo = RegentaColors.anuladoSoft;
        texto = RegentaColors.ink2;
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
        style: PastelTheme.cuerpo(
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
      key: const ValueKey('tarjeta-alertas'),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Alertas',
                style: PastelTheme.titulo(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: RegentaColors.ink,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: RegentaColors.critSoft,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Text(
                  '• 4 nuevas',
                  style: TextStyle(
                    color: RegentaColors.crit,
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
                      color: a.esCritica
                          ? RegentaColors.critSoft
                          : RegentaColors.warnSoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    alignment: Alignment.center,
                    child: Icon(
                      Icons.warning_amber_rounded,
                      size: 20,
                      color: a.esCritica
                          ? RegentaColors.crit
                          : RegentaColors.warn,
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
                                style: PastelTheme.cuerpo(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                  color: RegentaColors.ink,
                                ),
                              ),
                            ),
                            Text(
                              a.tiempo,
                              style: PastelTheme.cuerpo(
                                fontSize: 10.5,
                                color: RegentaColors.faint,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 3),
                        Text(
                          a.subtitulo,
                          style: PastelTheme.cuerpo(
                            fontSize: 11.5,
                            color: RegentaColors.ink2,
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
                style: PastelTheme.titulo(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                ),
              ),
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accentSoft,
                foregroundColor: RegentaColors.accent,
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
        color: RegentaColors.warnSoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: RegentaColors.warn.withValues(alpha: 0.3)),
      ),
      child: Text(
        'Tu plan todavía no tiene módulos activos. Pídele a un administrador que los habilite.',
        style: PastelTheme.cuerpo(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: RegentaColors.warn,
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
        color: alerta ? RegentaColors.critSoft : RegentaColors.warnSoft,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            alerta ? Icons.error_outline : Icons.cloud_upload_outlined,
            size: 18,
            color: alerta ? RegentaColors.crit : RegentaColors.warn,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              alerta
                  ? '${resumen.conflictos} operación(es) chocaron al subir y hay que revisarlas.'
                  : '${resumen.pendientes} operación(es) esperan conexión para sincronizar.',
              style: PastelTheme.cuerpo(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: RegentaColors.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
