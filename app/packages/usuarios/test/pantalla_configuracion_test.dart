import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
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
