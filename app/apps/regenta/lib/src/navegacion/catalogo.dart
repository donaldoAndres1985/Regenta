import 'dart:async';
import 'package:flutter/material.dart';
import 'package:regenta_clientes/regenta_clientes.dart';
import 'package:regenta_comandas/regenta_comandas.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';
import 'package:regenta_inventario/regenta_inventario.dart';
import 'package:regenta_mesas/regenta_mesas.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// Qué módulos hay y dónde vive cada uno (HU-119).
///
/// Es el único sitio del monorepo donde se nombran todos: un paquete de módulo
/// no sabe de los demás, pero la carcasa tiene que saber de todos para armar la
/// navegación.
///
/// Cada ruta declara **el módulo que exige**, y de ahí salen dos cosas a la vez:
/// lo que se ve en el menú y lo que la guardia deja pasar. Si fueran dos listas
/// distintas, tarde o temprano dirían cosas distintas.

/// El permiso mínimo para que un módulo aparezca en el menú.
///
/// No basta con que el negocio tenga el módulo en su plan: un cajero no tiene
/// por qué ver *Inventario* aunque la ferretería lo haya pagado.
const Map<String, String> permisoParaVer = {
  'VENTAS': 'VENTAS_VENTA_VER',
  'INVENTARIO': 'INVENTARIO_PRODUCTO_VER',
  'CLIENTES': 'CLIENTES_CLIENTE_VER',
  'MESAS': 'MESAS_MESA_VER',
  'COMANDAS': 'COMANDAS_COMANDA_VER',
  'FACTURACION': 'FACTURACION_FACTURA_VER',
  'CAJA': 'CAJA_TURNO_VER',
  'COMPRAS': 'COMPRAS_COMPRA_VER',
  'REPORTES': 'REPORTES_REPORTE_VER',
  'CONFIGURACION': 'CONFIGURACION_NEGOCIO_VER',
};

/// Las pantallas enrutables, con el módulo que cada una exige.
///
/// `crearEnrutador` genera además `/<ruta>/:id` para cada una, que es por donde
/// entran las que necesitan un identificador.
final List<RutaProtegida> rutasDeLaApp = [
  RutaProtegida(
    ruta: '/ventas',
    modulo: 'VENTAS',
    builder: (context, parametros) => const PantallaPos(),
  ),
  RutaProtegida(
    ruta: '/inventario',
    modulo: 'INVENTARIO',
    builder: (context, parametros) =>
        PantallaInventario(onProductoSeleccionado: (productoId, factor) {}),
  ),
  RutaProtegida(
    ruta: '/clientes',
    modulo: 'CLIENTES',
    builder: (context, parametros) => const PantallaClientes(),
  ),
  RutaProtegida(
    ruta: '/mesas',
    modulo: 'MESAS',
    builder: (context, parametros) => const PantallaMesas(),
  ),
  RutaProtegida(
    ruta: '/comandas',
    modulo: 'COMANDAS',
    builder: (context, parametros) {
      final id = parametros['id'];
      if (id == null) return const PantallaKds();
      return PantallaComanda(comandaId: id);
    },
  ),
  RutaProtegida(
    ruta: '/cocina',
    modulo: 'COMANDAS',
    builder: (context, parametros) => const PantallaKds(),
  ),
  RutaProtegida(
    ruta: '/facturas',
    modulo: 'FACTURACION',
    builder: (context, parametros) {
      final id = parametros['id'];
      if (id == null) {
        return PantallaListaFacturas(
          onFacturaSeleccionada: (facturaId) {
            unawaited(Navigator.of(context).pushNamed('/facturas/$facturaId'));
          },
        );
      }
      return PantallaFactura(facturaId: id);
    },
  ),
  // Módulos que ya tienen backend pero todavía no su pantalla en la app: la
  // entrada existe para que el menú sea el del diseño, y la guardia los trata
  // igual que a los demás.
  RutaProtegida(
    ruta: '/caja',
    modulo: 'CAJA',
    builder: (context, parametros) => const PantallaCaja(),
  ),
  RutaProtegida(
    ruta: '/compras',
    modulo: 'COMPRAS',
    builder: (context, parametros) =>
        const _SinPantallaTodavia(modulo: 'Compras'),
  ),
  RutaProtegida(
    ruta: '/reportes',
    modulo: 'REPORTES',
    builder: (context, parametros) =>
        const _SinPantallaTodavia(modulo: 'Reportes'),
  ),
  RutaProtegida(
    ruta: '/configuracion',
    modulo: 'CONFIGURACION',
    builder: (context, parametros) =>
        const _SinPantallaTodavia(modulo: 'Configuración'),
  ),
];

/// El menú completo, antes de filtrar por lo que tiene el negocio.
/// En el orden en que se pintan, grupo por grupo.
const List<EntradaDeNavegacion> menuCompleto = [
  EntradaDeNavegacion(ruta: GuardiaDeRutas.inicio, titulo: 'Inicio'),
  EntradaDeNavegacion(ruta: '/ventas', titulo: 'Ventas', modulo: 'VENTAS'),
  EntradaDeNavegacion(ruta: '/caja', titulo: 'Caja', modulo: 'CAJA'),
  EntradaDeNavegacion(ruta: '/mesas', titulo: 'Mesas', modulo: 'MESAS'),
  EntradaDeNavegacion(ruta: '/cocina', titulo: 'Cocina', modulo: 'COMANDAS'),
  EntradaDeNavegacion(
    ruta: '/inventario',
    titulo: 'Inventario',
    modulo: 'INVENTARIO',
  ),
  EntradaDeNavegacion(ruta: '/compras', titulo: 'Compras', modulo: 'COMPRAS'),
  EntradaDeNavegacion(
    ruta: '/clientes',
    titulo: 'Clientes',
    modulo: 'CLIENTES',
  ),
  EntradaDeNavegacion(
    ruta: '/facturas',
    titulo: 'Facturación',
    modulo: 'FACTURACION',
  ),
  EntradaDeNavegacion(
    ruta: '/reportes',
    titulo: 'Reportes',
    modulo: 'REPORTES',
  ),
  EntradaDeNavegacion(
    ruta: '/configuracion',
    titulo: 'Configuración',
    modulo: 'CONFIGURACION',
  ),
];

/// Los grupos del menú lateral.
enum GrupoDeMenu {
  operacion('Operación'),
  gestion('Gestión'),
  analisis('Análisis');

  const GrupoDeMenu(this.titulo);
  final String titulo;
}

/// A qué grupo va cada entrada del menú. Es presentación, como los iconos.
const Map<String, GrupoDeMenu> grupoDeRuta = {
  GuardiaDeRutas.inicio: GrupoDeMenu.operacion,
  '/ventas': GrupoDeMenu.operacion,
  '/caja': GrupoDeMenu.operacion,
  '/mesas': GrupoDeMenu.operacion,
  '/cocina': GrupoDeMenu.operacion,
  '/inventario': GrupoDeMenu.gestion,
  '/compras': GrupoDeMenu.gestion,
  '/clientes': GrupoDeMenu.gestion,
  '/facturas': GrupoDeMenu.gestion,
  '/reportes': GrupoDeMenu.analisis,
  '/configuracion': GrupoDeMenu.analisis,
};

/// El menú de este negocio y de esta persona (criterio 3).
///
/// Dos filtros encadenados, y los dos hacen falta: el módulo dice qué compró el
/// negocio, el permiso dice qué le toca a quien entró.
List<EntradaDeNavegacion> menuDe(ClaimsDeSesion? claims) {
  if (claims == null) return const [];
  final activos = ModulosActivos.deClaims(claims);
  final permisos = claims.permisos.toSet();
  return activos.visiblesDe(menuCompleto).where((entrada) {
    final modulo = entrada.modulo;
    if (modulo == null) return true;
    final exigido = permisoParaVer[modulo.toUpperCase()];
    return exigido == null || permisos.contains(exigido);
  }).toList();
}

/// Iconos del menú, por ruta. Separado del catálogo porque es presentación.
const Map<String, IconData> iconoDeRuta = {
  GuardiaDeRutas.inicio: Icons.home_outlined,
  '/ventas': Icons.shopping_cart_outlined,
  '/caja': Icons.payments_outlined,
  '/compras': Icons.local_shipping_outlined,
  '/reportes': Icons.bar_chart_rounded,
  '/configuracion': Icons.settings_outlined,
  '/inventario': Icons.inventory_2_outlined,
  '/clientes': Icons.people_outline,
  '/mesas': Icons.table_restaurant_outlined,
  '/cocina': Icons.soup_kitchen_outlined,
  '/facturas': Icons.receipt_long_outlined,
};

/// Cuando se entra a una pantalla de detalle sin decir a cuál.
class _SinDocumento extends StatelessWidget {
  const _SinDocumento({required this.que});

  final String que;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(
          'Sin $que',
          style: RegentaType.seccion.copyWith(
            fontSize: 16,
            color: RegentaColors.ink,
          ),
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'Elige una $que de la lista para verla.',
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(
              fontSize: 14,
              color: RegentaColors.ink2,
            ),
          ),
        ),
      ),
    );
  }
}

/// Un módulo del menú cuya pantalla todavía no existe en la app.
class _SinPantallaTodavia extends StatelessWidget {
  const _SinPantallaTodavia({required this.modulo});

  final String modulo;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(
          modulo,
          style: RegentaType.seccion.copyWith(
            fontSize: 16,
            color: RegentaColors.ink,
          ),
        ),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            'La pantalla de $modulo todavía no está disponible en la app.',
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(
              fontSize: 14,
              color: RegentaColors.ink2,
            ),
          ),
        ),
      ),
    );
  }
}
