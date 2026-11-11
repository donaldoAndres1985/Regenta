import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _FakeRepoDevoluciones implements RepositorioDeVentas {
  SolicitudDeDevolucionParaEnviar? ultimaSolicitud;
  String? ultimaVentaId;

  @override
  Future<DevolucionRegistrada> registrarDevolucion(
    String ventaId,
    SolicitudDeDevolucionParaEnviar solicitud,
  ) async {
    ultimaVentaId = ventaId;
    ultimaSolicitud = solicitud;
    return DevolucionRegistrada(
      id: 'dev-99',
      numero: 'DEV-001',
      tipo: solicitud.lineas.first.cantidad == 2 ? 'TOTAL' : 'PARCIAL',
      motivo: solicitud.motivo,
      reintegraStock: solicitud.reintegraStock,
      total: 20000,
    );
  }

  @override
  Future<VentaEnProceso> verVenta(String ventaId) async {
    return const VentaEnProceso(
      id: 'v-100',
      numero: 'FV-100',
      estado: 'CONFIRMADA',
      total: 20000,
      lineas: [
        LineaDeVentaResumen(
          linea: 1,
          productoId: 'p-1',
          sku: 'SKU-1',
          nombre: 'Pintura Galon',
          cantidad: 2,
          precioUnitario: 10000,
          total: 20000,
        ),
      ],
    );
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  Widget crearApp(Widget child, _FakeRepoDevoluciones repo) {
    return ProviderScope(
      overrides: [
        repositorioDeVentasProvider.overrideWithValue(repo),
      ],
      child: MaterialApp(home: child),
    );
  }

  testWidgets('PantallaDevolucion muestra alerta DIAN si la venta fue facturada',
      (tester) async {
    final repo = _FakeRepoDevoluciones();
    const venta = VentaEnProceso(
      id: 'v-100',
      numero: 'FV-100',
      estado: 'CONFIRMADA',
      total: 20000,
      facturada: true,
      lineas: [
        LineaDeVentaResumen(
          linea: 1,
          productoId: 'p-1',
          sku: 'SKU-1',
          nombre: 'Pintura Galon',
          cantidad: 2,
          precioUnitario: 10000,
          total: 20000,
        ),
      ],
    );

    await tester.pumpWidget(crearApp(const PantallaDevolucion(venta: venta), repo));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('alerta-factura-dian')), findsOneWidget);
    expect(
      find.text(
        'Esta venta fue facturada electrónicamente; al procesar la devolución se generará la respectiva nota crédito ante la DIAN.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Registrar devolución envía la solicitud con motivo y reintegro',
      (tester) async {
    final repo = _FakeRepoDevoluciones();
    const venta = VentaEnProceso(
      id: 'v-100',
      numero: 'FV-100',
      estado: 'CONFIRMADA',
      total: 20000,
      lineas: [
        LineaDeVentaResumen(
          linea: 1,
          productoId: 'p-1',
          sku: 'SKU-1',
          nombre: 'Pintura Galon',
          cantidad: 2,
          precioUnitario: 10000,
          total: 20000,
        ),
      ],
    );

    await tester.pumpWidget(crearApp(const PantallaDevolucion(venta: venta), repo));
    await tester.pumpAndSettle();

    // Cambiar motivo a DEFECTUOSO
    await tester.tap(find.byKey(const Key('dropdown-motivo-devolucion')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Producto defectuoso').last);
    await tester.pumpAndSettle();

    // Al ser defectuoso, el switch de reintegro de stock se desmarca por defecto
    final switchWidget =
        tester.widget<SwitchListTile>(find.byKey(const Key('switch-reintegrar-stock')));
    expect(switchWidget.value, isFalse);

    // Confirmar devolución
    await tester.ensureVisible(find.byKey(const Key('boton-confirmar-devolucion')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('boton-confirmar-devolucion')));
    await tester.pumpAndSettle();

    expect(repo.ultimaVentaId, 'v-100');
    expect(repo.ultimaSolicitud?.motivo, 'DEFECTUOSO');
    expect(repo.ultimaSolicitud?.reintegraStock, isFalse);
    expect(repo.ultimaSolicitud?.lineas.first.cantidad, 2);
  });
}
