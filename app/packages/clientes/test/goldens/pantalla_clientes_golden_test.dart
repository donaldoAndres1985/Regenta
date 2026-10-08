import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_clientes/regenta_clientes.dart';
import 'package:regenta_core/regenta_core.dart';

import 'golden.dart';

/// HU-128. El listado de clientes contra su imagen de referencia, en las dos
/// composiciones de `design/pantallas/Clientes*.html`. Datos fijos: un golden
/// que depende de la red falla por la red.

const _clientes = [
  ClienteEnLista(
      id: '1', nombre: 'Materiales Cruz S.A.S.', tipoDocumento: 'NIT',
      numeroDocumento: '900412883', saldo: 4250000, cupo: 10000000, segmento: 'MAYORISTA'),
  ClienteEnLista(
      id: '2', nombre: 'Jorge Rendón Ospina', tipoDocumento: 'CC', numeroDocumento: '71884203',
      saldo: 0, cupo: 0),
  ClienteEnLista(
      id: '3', nombre: 'Ferremax del Norte', tipoDocumento: 'NIT', numeroDocumento: '901556019',
      saldo: 1820000, cupo: 2000000, segmento: 'MAYORISTA', vencido: true),
  ClienteEnLista(
      id: '4', nombre: 'Luz Marina Peña', tipoDocumento: 'CC', numeroDocumento: '52114908',
      saldo: 0, cupo: 500000, segmento: 'VIP'),
];

class _ClientesFijos implements RepositorioDeClientes {
  @override
  Future<List<ClienteEnLista>> enCache() async => _clientes;

  @override
  Future<List<ClienteEnLista>> enRed() async => _clientes;

  @override
  Future<void> guardarEnCache(List<ClienteEnLista> clientes) async {}
}

Future<void> _montar(WidgetTester tester, Size size) async {
  await tamano(tester, size);
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeClientesProvider.overrideWithValue(_ClientesFijos())],
    child: MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: regentaTheme(PatronOperativo.ventaDirecta),
      home: const PantallaClientes(),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testGolden('Listado de clientes en móvil (390×844) coincide con su referencia', (tester) async {
    await _montar(tester, movil);
    await expectLater(
        find.byType(PantallaClientes), matchesGoldenFile('referencias/clientes_movil.png'));
  });

  testGolden('Listado de clientes en escritorio (1440×900) coincide con su referencia',
      (tester) async {
    await _montar(tester, escritorio);
    await expectLater(
        find.byType(PantallaClientes), matchesGoldenFile('referencias/clientes_web.png'));
  });
}
