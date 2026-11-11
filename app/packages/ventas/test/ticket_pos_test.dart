import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _FakeRepoParaTicket implements RepositorioDeVentas {
  final List<VentaEnProceso> ventas = [];

  @override
  Future<List<VentaEnProceso>> buscarVentas({String? q}) async => ventas;

  @override
  Future<VentaEnProceso> verVenta(String ventaId) async {
    return ventas.firstWhere((v) => v.id == ventaId);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  test('GeneradorTicketPos crea un documento PDF binario válido', () async {
    final pdfBytes = await GeneradorTicketPos.crearPdf(
      nombreNegocio: 'Ferretería Central',
      numeroVenta: '00123',
      fechaHora: '2026-11-10 14:30:00',
      nombreCliente: 'Juan Valdez',
      identificacionCliente: '10203040',
      lineas: const [
        LineaTicket(
          nombre: 'Tubo PVC 1/2 pulgada',
          cantidad: 3,
          precioUnitario: 12000,
          total: 36000,
        ),
      ],
      total: 36000,
      pagos: const [
        PagoTicket(metodo: 'EFECTIVO', monto: 50000),
      ],
      cambio: 14000,
      nota: 'Entregar en portería',
    );

    expect(pdfBytes, isNotEmpty);
    // Un archivo PDF inicia con la cabecera %PDF-
    final cabecera = utf8.decode(pdfBytes.sublist(0, 5));
    expect(cabecera, '%PDF-');
  });

  testWidgets('PantallaBuscarVenta tiene acceso para reimprimir ticket', (tester) async {
    final repo = _FakeRepoParaTicket();
    repo.ventas.add(
      const VentaEnProceso(
        id: 'v-200',
        numero: 'FV-200',
        estado: 'CONFIRMADA',
        total: 36000,
        lineas: [
          LineaDeVentaResumen(
            linea: 1,
            productoId: 'p-1',
            sku: 'SKU-1',
            nombre: 'Tubo PVC',
            cantidad: 3,
            precioUnitario: 12000,
            total: 36000,
          ),
        ],
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeVentasProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          home: PantallaBuscarVenta(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // El botón de ticket debe existir para la venta confirmada
    expect(find.byKey(const Key('boton-ticket-venta-v-200')), findsOneWidget);
  });
}
