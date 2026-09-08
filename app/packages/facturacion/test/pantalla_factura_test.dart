import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';

const _cufe =
    '8f3c1a90d4b27e5661af0c3d92be7148aa5d0e2f6c81b4739e0a2d5c7f18b3946e2c0d7a15f8b3c94a1b2c3d4';

class _RepoFake implements RepositorioDeFacturas {
  _RepoFake(this._factura);
  final FacturaVista _factura;
  final List<String> enviosRealizados = [];

  @override
  Future<FacturaVista> ver(String facturaId) async => _factura;

  @override
  Future<String> enviarPorCorreo(String facturaId, {String? correo}) async {
    enviosRealizados.add(correo ?? 'snapshot');
    return correo ?? 'cliente@correo.co';
  }
}

FacturaVista _factura({
  String estado = 'ACEPTADA',
  String? codigoRechazo,
  String? mensajeRechazo,
}) =>
    FacturaVista(
      id: 'f1',
      numeroCompleto: 'FE1047',
      tipoDocumento: 'FACTURA_VENTA',
      estado: switch (estado) {
        'RECHAZADA' => EstadoFacturaVista.rechazada,
        'CONTINGENCIA' => EstadoFacturaVista.contingencia,
        _ => EstadoFacturaVista.aceptada,
      },
      fechaEmision: '2026-09-02T14:02:00Z',
      emisor: const {'razon_social': 'Mi Negocio SAS'},
      cliente: const {
        'razon_social': 'Materiales Cruz S.A.S.',
        'tipo_documento': 'NIT',
        'numero_documento': '900412883',
      },
      lineas: const [
        LineaVista(
            descripcion: 'Cemento gris 50 kg',
            codigo: 'CEM-050',
            cantidad: 40,
            precioUnitario: 32000,
            total: 1280000),
      ],
      impuestos: const [ImpuestoVista(nombre: 'IVA', porcentaje: 19, valor: 316920)],
      subtotal: 1720000,
      descuentoTotal: 52000,
      baseGravable: 1668000,
      impuestosTotal: 316920,
      total: 1984920,
      cufe: _cufe,
      codigoRechazo: codigoRechazo,
      mensajeRechazo: mensajeRechazo,
      trazabilidad: const [
        PasoDeTrazabilidad(evento: 'ENVIO', mensaje: 'Documento validado', ocurridoEn: '14:03'),
      ],
    );

Future<void> _montar(WidgetTester tester, _RepoFake repo,
    {Size size = const Size(400, 1800)}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeFacturasProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaFactura(facturaId: 'f1')),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterio 1: se ven emisor, adquiriente, líneas, impuestos, CUFE y trazabilidad',
      (tester) async {
    await _montar(tester, _RepoFake(_factura()));

    expect(find.text('Mi Negocio SAS'), findsOneWidget);
    expect(find.text('Materiales Cruz S.A.S.'), findsOneWidget);
    expect(find.text('NIT 900412883'), findsOneWidget);
    expect(find.text('Cemento gris 50 kg'), findsOneWidget);
    expect(find.text('IVA 19%'), findsOneWidget);
    expect(find.text('Trazabilidad'), findsOneWidget);
    expect(find.textContaining('Documento validado'), findsOneWidget);
  });

  testWidgets('Criterio 3: el CUFE se ve completo y se puede copiar', (tester) async {
    final copiado = <String>[];
    tester.binding.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, (call) async {
      if (call.method == 'Clipboard.setData') {
        copiado.add((call.arguments as Map)['text'] as String);
      }
      return null;
    });
    addTearDown(() => tester.binding.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, null));

    await _montar(tester, _RepoFake(_factura()));

    expect(find.text(_cufe), findsOneWidget); // completo, sin recortar
    await tester.tap(find.bySemanticsLabel('Copiar CUFE'));
    await tester.pump();
    expect(copiado, [_cufe]);
  });

  testWidgets('Criterio 4: una factura rechazada muestra el motivo sin buscarlo', (tester) async {
    await _montar(tester,
        _RepoFake(_factura(estado: 'RECHAZADA', codigoRechazo: 'FAD09', mensajeRechazo: 'RUT no válido')));

    expect(find.textContaining('RECHAZADA POR LA DIAN'), findsOneWidget);
    expect(find.textContaining('FAD09'), findsOneWidget);
    expect(find.text('RUT no válido'), findsOneWidget);
  });

  testWidgets('Criterio 2: enviar una factura aceptada llama al backend', (tester) async {
    final repo = _RepoFake(_factura());
    await _montar(tester, repo);

    await tester.tap(find.widgetWithText(FilledButton, 'Enviar'));
    await tester.pumpAndSettle();

    expect(repo.enviosRealizados, hasLength(1));
    expect(find.textContaining('Enviada a'), findsOneWidget); // snackbar
  });

  testWidgets('En una rechazada el botón Enviar está deshabilitado', (tester) async {
    await _montar(tester, _RepoFake(_factura(estado: 'RECHAZADA', codigoRechazo: 'X')));

    final boton = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Enviar'));
    expect(boton.onPressed, isNull);
  });

  testWidgets('El mismo widget se adapta a móvil y a escritorio', (tester) async {
    await _montar(tester, _RepoFake(_factura()));
    expect(find.byKey(const Key('factura-movil')), findsOneWidget);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('factura-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('factura-movil')), findsNothing);
  });

  testWidgets('En móvil los botones de acción miden al menos 44 px', (tester) async {
    await _montar(tester, _RepoFake(_factura()));

    expect(tester.getSize(find.widgetWithText(FilledButton, 'Enviar')).height,
        greaterThanOrEqualTo(44));
  });
}
