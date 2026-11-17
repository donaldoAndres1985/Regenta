import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/inventario_general.dart';
import 'formato.dart';
import 'pantalla_ficha_de_producto.dart';

/// HU-153 · inventario general: cada producto con su existencia, costo, precio y valor, con
/// filtros (tarifa por revisar, existencia negativa, inactivos, texto) y los totales de todo lo
/// filtrado. Tocar una fila abre su ficha (HU-150).
class PantallaInventarioGeneral extends ConsumerStatefulWidget {
  const PantallaInventarioGeneral({super.key});

  @override
  ConsumerState<PantallaInventarioGeneral> createState() => _PantallaInventarioGeneralState();
}

class _PantallaInventarioGeneralState extends ConsumerState<PantallaInventarioGeneral> {
  final _buscador = TextEditingController();
  ConsultaDeInventario _consulta = const ConsultaDeInventario();
  ResultadoDeInventario? _resultado;
  String? _error;
  bool _cargando = false;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  @override
  void dispose() {
    _buscador.dispose();
    super.dispose();
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final r = await ref.read(repositorioDeInventarioProvider).inventarioGeneral(_consulta);
      if (mounted) setState(() => _resultado = r);
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  void _cambiar(ConsultaDeInventario nueva) {
    _consulta = nueva.copiar(pagina: nueva.pagina);
    _cargar();
  }

  Future<void> _abrir(FilaDeInventario f) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => PantallaFichaDeProducto(productoId: f.id),
    ));
    if (mounted) _cargar();
  }

  @override
  Widget build(BuildContext context) {
    final r = _resultado;
    final paginas = r == null ? 1 : ((r.totalFilas + _consulta.tamano - 1) ~/ _consulta.tamano);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Inventario general')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              key: const Key('inventario-general-buscar'),
              controller: _buscador,
              textInputAction: TextInputAction.done,
              style: const TextStyle(color: RegentaColors.inkFuerte),
              decoration: const InputDecoration(
                prefixIcon: Icon(Icons.search),
                hintText: 'Buscar por nombre, SKU o código (varias palabras)',
                border: OutlineInputBorder(),
              ),
              onSubmitted: (t) => _cambiar(_consulta.copiar(q: t, pagina: 0)),
            ),
            const SizedBox(height: 8),
            Wrap(spacing: 8, runSpacing: 8, children: [
              FilterChip(
                key: const Key('filtro-por-revisar'),
                label: const Text('Tarifa por revisar'),
                selected: _consulta.tarifaPorRevisar,
                onSelected: (v) => _cambiar(_consulta.copiar(tarifaPorRevisar: v, pagina: 0)),
              ),
              FilterChip(
                key: const Key('filtro-negativa'),
                label: const Text('Existencia negativa'),
                selected: _consulta.existenciaNegativa,
                onSelected: (v) => _cambiar(_consulta.copiar(existenciaNegativa: v, pagina: 0)),
              ),
              FilterChip(
                key: const Key('filtro-inactivos'),
                label: const Text('Inactivos'),
                selected: _consulta.estado == 'INACTIVOS',
                onSelected: (v) =>
                    _cambiar(_consulta.copiar(estado: v ? 'INACTIVOS' : 'ACTIVOS', pagina: 0)),
              ),
            ]),
            const SizedBox(height: 12),
            if (r != null) _Totales(resultado: r),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
              ),
            const SizedBox(height: 8),
            Expanded(
              child: _cargando && r == null
                  ? const Center(child: CircularProgressIndicator())
                  : r == null || r.filas.isEmpty
                      ? const Center(child: Text('No hay productos con estos filtros.'))
                      : ListView.separated(
                          itemCount: r.filas.length,
                          separatorBuilder: (_, _) => const Divider(height: 1),
                          itemBuilder: (_, i) => _Fila(fila: r.filas[i], onTap: () => _abrir(r.filas[i])),
                        ),
            ),
            if (paginas > 1)
              Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left),
                  onPressed: _consulta.pagina == 0
                      ? null
                      : () => _cambiar(_consulta.copiar(pagina: _consulta.pagina - 1)),
                ),
                Text('Página ${_consulta.pagina + 1} de $paginas'),
                IconButton(
                  icon: const Icon(Icons.chevron_right),
                  onPressed: _consulta.pagina + 1 >= paginas
                      ? null
                      : () => _cambiar(_consulta.copiar(pagina: _consulta.pagina + 1)),
                ),
              ]),
          ],
        ),
      ),
    );
  }
}

class _Totales extends StatelessWidget {
  const _Totales({required this.resultado});

  final ResultadoDeInventario resultado;

  @override
  Widget build(BuildContext context) {
    Widget dato(String etiqueta, String valor) => Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(etiqueta, style: RegentaType.etiqueta.copyWith(color: RegentaColors.ink2)),
            Text(valor, style: RegentaType.dinero.copyWith(fontSize: 18, color: RegentaColors.inkFuerte)),
          ]),
        );
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Row(children: [
        dato('Productos', '${resultado.totalFilas}'),
        dato('Valor a costo', formatearPesos(resultado.valorCosto)),
        dato('Valor a precio de venta', formatearPesos(resultado.valorPrecio)),
      ]),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila({required this.fila, required this.onTap});

  final FilaDeInventario fila;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final negativa = fila.existencia < 0;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
        child: Row(children: [
          SizedBox(
            width: 120,
            child: Text(fila.sku, style: RegentaType.codigo.copyWith(color: RegentaColors.ink2)),
          ),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(fila.nombre,
                  style: RegentaType.cuerpo.copyWith(
                      fontWeight: FontWeight.w600, color: RegentaColors.inkFuerte)),
              Wrap(spacing: 6, children: [
                Text(fila.categoria,
                    style: RegentaType.etiqueta.copyWith(color: RegentaColors.ink2)),
                if (fila.tarifaPorRevisar)
                  Text('Tarifa por revisar',
                      style: RegentaType.etiqueta.copyWith(color: RegentaColors.warn)),
                if (!fila.activo)
                  Text('Inactivo', style: RegentaType.etiqueta.copyWith(color: RegentaColors.crit)),
              ]),
            ]),
          ),
          SizedBox(
            width: 90,
            child: Text(formatearCantidad(fila.existencia),
                textAlign: TextAlign.right,
                style: RegentaType.dinero.copyWith(
                    color: negativa ? RegentaColors.crit : RegentaColors.inkFuerte)),
          ),
          SizedBox(
            width: 130,
            child: Text(formatearPesos(fila.valorCosto),
                textAlign: TextAlign.right,
                style: RegentaType.dinero.copyWith(color: RegentaColors.inkFuerte)),
          ),
          SizedBox(
            width: 130,
            child: Text(formatearPesos(fila.valorPrecio),
                textAlign: TextAlign.right,
                style: RegentaType.dinero.copyWith(color: RegentaColors.inkFuerte)),
          ),
        ]),
      ),
    );
  }
}
