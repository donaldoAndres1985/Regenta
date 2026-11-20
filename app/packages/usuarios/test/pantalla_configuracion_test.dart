import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_usuarios/regenta_usuarios.dart';

class _RepoUsuariosFake implements RepositorioDeUsuarios {
  _RepoUsuariosFake(this.configuracion);

  ConfiguracionNegocioVista configuracion;
  bool guardado = false;

  @override
  Future<ConfiguracionNegocioVista> verConfiguracion() async => configuracion;

  @override
  Future<ConfiguracionNegocioVista> actualizarConfiguracion(
    ConfiguracionNegocioVista config,
  ) async {
    configuracion = config;
    guardado = true;
    return config;
  }
}

/// La pantalla incluye la tarjeta de cierre de caja (HU-139), que lee su propio repositorio.
class _RepoCajaFake implements RepositorioDeConfigDeCaja {
  @override
  Future<List<String>> verMetodosNoCuadrables() async => const ['BONO'];

  @override
  Future<List<String>> fijarMetodosNoCuadrables(List<String> metodos) async => metodos;
}

void main() {
  testWidgets('Encabezado E1-E5 · en web, el encabezado común con ícono; en móvil, la barra', (tester) async {
    Future<void> montar(Size tamano) async {
      await tester.binding.setSurfaceSize(tamano);
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(ProviderScope(
        overrides: [repositorioDeUsuariosProvider.overrideWithValue(_RepoUsuariosFake(const ConfiguracionNegocioVista(razonSocial: 'X', nombreComercial: 'X', tipoDocumento: 'NIT', numeroDocumento: '1', direccion: '', ciudad: '', telefono: '', email: '', preciosIncluyenImpuesto: true, politicaStockNegativo: false))), repositorioDeConfigDeCajaProvider.overrideWithValue(_RepoCajaFake())],
        child: MaterialApp(home: const PantallaConfiguracion()),
      ));
      await tester.pumpAndSettle();
    }

    await montar(const Size(1440, 900));
    final encabezado = find.byType(EncabezadoDeSeccion);
    expect(encabezado, findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.byIcon(Icons.settings_outlined)), findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.text('Configuración del Negocio')), findsOneWidget);

    await montar(const Size(390, 844));
    expect(find.byType(EncabezadoDeSeccion), findsNothing);
    expect(find.byType(AppBar), findsOneWidget);
  });

  testWidgets('Carga y muestra el formulario de configuración', (tester) async {
    const configInicial = ConfiguracionNegocioVista(
      razonSocial: 'Ferretería Central SAS',
      nombreComercial: 'Ferretería Central',
      tipoDocumento: 'NIT',
      numeroDocumento: '900123456-7',
      direccion: 'Calle 10 # 20-30',
      ciudad: 'Bogotá',
      telefono: '3001234567',
      email: 'contacto@ferreteria.co',
      preciosIncluyenImpuesto: true,
      politicaStockNegativo: false,
    );

    final fakeRepo = _RepoUsuariosFake(configInicial);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeUsuariosProvider.overrideWithValue(fakeRepo),
          repositorioDeConfigDeCajaProvider.overrideWithValue(_RepoCajaFake()),
        ],
        child: const MaterialApp(
          home: PantallaConfiguracion(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Configuración del Negocio'), findsOneWidget);
    expect(find.text('Ferretería Central SAS'), findsOneWidget);
    expect(find.text('900123456-7'), findsOneWidget);
    expect(find.text('Guardar Configuración'), findsOneWidget);

    await tester.ensureVisible(find.text('Guardar Configuración'));
    await tester.tap(find.text('Guardar Configuración'));
    await tester.pumpAndSettle();

    expect(fakeRepo.guardado, isTrue);
  });
}
