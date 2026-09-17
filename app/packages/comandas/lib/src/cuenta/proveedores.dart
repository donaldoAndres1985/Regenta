import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../comanda/proveedores.dart';
import '../datos/repositorio_de_cuentas.dart';
import 'controlador_de_division.dart';
import 'estado_de_division.dart';

/// La app lo sobrescribe con un [RepositorioDeCuentas] real; los tests, con un doble.
final repositorioDeCuentasProvider = Provider<RepositorioDeCuentas>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeCuentasProvider al montar el ProviderScope.',
  );
});

/// La división de una comanda por id.
final controladorDeDivisionProvider = StateNotifierProvider.family<ControladorDeDivision,
    EstadoDeDivision, String>(
  (ref, comandaId) => ControladorDeDivision(
    ref.watch(repositorioDeComandasProvider),
    ref.watch(repositorioDeCuentasProvider),
    comandaId,
  ),
);
