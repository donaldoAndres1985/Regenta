import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/producto_encontrado.dart';
import 'formato.dart';

/// Una fila de la lista de inventario. Medidas y colores literales de
/// `design/pantallas/InventarioMovil.html`.
class FilaDeProducto extends StatelessWidget {
  const FilaDeProducto({super.key, required this.producto, this.onTap});

  final ProductoEncontrado producto;
  final VoidCallback? onTap;

  /// El color de la cantidad según el nivel de stock (mockup: verde / ámbar / rojo).
  static Color colorDeStock(NivelStock nivel) => switch (nivel) {
        NivelStock.normal => RegentaColors.ok,
        NivelStock.bajo => RegentaColors.warn,
        NivelStock.cero => RegentaColors.crit,
      };

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: RegentaColors.sunken,
                borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
              ),
              child: const Icon(Icons.inventory_2_outlined,
                  size: 20, color: RegentaColors.faint),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    producto.nombre,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item.copyWith(color: RegentaColors.ink),
                  ),
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      Text(producto.sku,
                          style: RegentaType.codigo
                              .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                      const SizedBox(width: 8),
                      Flexible(
                        child: Text(
                          producto.categoriaNombre,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: RegentaType.codigo
                              .copyWith(fontSize: 10.5, color: RegentaColors.faint),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  formatearPesos(producto.precioVenta),
                  style: RegentaType.codigo.copyWith(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  formatearCantidad(producto.stockTotal),
                  style: RegentaType.codigo.copyWith(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: colorDeStock(producto.nivelStock),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
