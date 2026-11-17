import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/fraccion_de_venta.dart';
import '../pos/linea_de_carrito.dart';
import '../pos/proveedores.dart';
import 'formato.dart';

/// HU-158 C1/C2 · muestra las fracciones reales del producto de [linea] (de su tabla o con
/// precio propio) y, al elegir una, convierte la línea a esa fracción. Sin fracciones, avisa
/// «Este producto no se vende fraccionado.».
Future<void> mostrarFraccionesDe(
  BuildContext context,
  LineaDeCarrito linea,
  ProviderContainer contenedor,
) {
  final carga = contenedor.read(consultaDeFraccionesProvider).de(linea.producto.id);
  return showModalBottomSheet<void>(
    context: context,
    backgroundColor: RegentaColors.posModalFondo,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (ctx) => SafeArea(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Fracciones de ${linea.producto.nombre}',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: Colors.white),
            ),
            const SizedBox(height: 14),
            FutureBuilder<List<FraccionDeVenta>>(
              future: carga,
              builder: (ctx, snap) {
                if (snap.connectionState != ConnectionState.done) {
                  return const Center(child: CircularProgressIndicator());
                }
                final fracciones = snap.data ?? const <FraccionDeVenta>[];
                if (snap.hasError || fracciones.isEmpty) {
                  return const Text(
                    'Este producto no se vende fraccionado.',
                    style: TextStyle(fontSize: 14, color: RegentaColors.posModalTextoSecundario),
                  );
                }
                return Wrap(spacing: 10, runSpacing: 10, children: [
                  for (final f in fracciones)
                    ActionChip(
                      key: Key('fraccion-${f.medida}'),
                      backgroundColor: linea.fraccion?.medida == f.medida
                          ? RegentaColors.posAcentoFuerte
                          : RegentaColors.posChipFondo,
                      label: Text('${f.medida} · ${formatearPesos(f.precio)}',
                          style: const TextStyle(color: RegentaColors.posModalTitulo)),
                      onPressed: () {
                        contenedor.read(controladorDelPosProvider.notifier).venderFraccion(linea, f);
                        Navigator.of(ctx).pop();
                      },
                    ),
                ]);
              },
            ),
          ],
        ),
      ),
    ),
  );
}
