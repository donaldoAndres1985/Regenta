package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.EquivalenciaDeDetallado;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infrastructure.EquivalenciaDeDetalladoRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-161/162 · detallar: abrir un producto (cuñete) y pasar sus unidades a otro (galones) según
 * una equivalencia guardada (I16-I18).
 */
@Service
public class GestionDeDetallado {

    private final ProductoRepositorio productos;
    private final EquivalenciaDeDetalladoRepositorio equivalencias;
    private final LibroMayorDeInventario libro;
    private final ExistenciaRepositorio existencias;

    public GestionDeDetallado(ProductoRepositorio productos, EquivalenciaDeDetalladoRepositorio equivalencias,
            LibroMayorDeInventario libro, ExistenciaRepositorio existencias) {
        this.productos = productos;
        this.equivalencias = equivalencias;
        this.libro = libro;
        this.existencias = existencias;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_DETALLAR")
    public List<EquivalenciaDelNegocio> listar() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Map<UUID, Producto> porId = productos.findByNegocioIdOrderByNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        return equivalencias.findByNegocioId(negocioId).stream().map(e -> comoDto(e,
                porId.get(e.getOrigenId()), porId.get(e.getDestinoId()))).toList();
    }

    /** HU-161 C1/C2/C4. */
    @Transactional
    @RequierePermiso("INVENTARIO_DETALLAR")
    public EquivalenciaDelNegocio crearEquivalencia(UUID origenId, UUID destinoId, BigDecimal factor) {
        Producto origen = delNegocio(origenId);
        Producto destino = delNegocio(destinoId);
        EquivalenciaDeDetallado e = EquivalenciaDeDetallado.entre(origen, destino, factor);
        if (equivalencias.existsByOrigenIdAndDestinoId(origenId, destinoId)) {
            throw new RecursoDuplicadoException("Ya hay una equivalencia entre «" + origen.getNombre()
                    + "» y «" + destino.getNombre() + "».");
        }
        return comoDto(equivalencias.save(e), origen, destino);
    }

    /** HU-161 C3 · cada origen se une a su par por nombre; los que no tienen par se listan. */
    @Transactional
    @RequierePermiso("INVENTARIO_DETALLAR")
    public ResultadoDeReplica replicar(SolicitudDeReplica solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Map<String, Producto> porNombre = productos.findByNegocioIdOrderByNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(p -> normalizar(p.getNombre()), Function.identity(), (a, b) -> a));
        String buscar = normalizar(solicitud.buscar());
        String reemplazar = normalizar(solicitud.reemplazar());
        int creadas = 0;
        List<String> sinPar = new ArrayList<>();
        for (UUID origenId : solicitud.origenIds()) {
            Producto origen = delNegocio(origenId);
            String nombre = normalizar(origen.getNombre());
            Producto destino = nombre.contains(buscar) ? porNombre.get(nombre.replace(buscar, reemplazar)) : null;
            if (destino == null || equivalencias.existsByOrigenIdAndDestinoId(origenId, destino.getId())) {
                sinPar.add(origen.getNombre());
                continue;
            }
            try {
                equivalencias.save(EquivalenciaDeDetallado.entre(origen, destino, solicitud.factor()));
                creadas++;
            } catch (ReglaDeNegocioException e) {
                sinPar.add(origen.getNombre());
            }
        }
        return new ResultadoDeReplica(creadas, sinPar);
    }

    Producto delNegocio(UUID productoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return productos.findById(productoId).filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
    }

    static EquivalenciaDelNegocio comoDto(EquivalenciaDeDetallado e, Producto origen, Producto destino) {
        return new EquivalenciaDelNegocio(e.getId(), e.getOrigenId(), origen == null ? "" : origen.getNombre(),
                e.getDestinoId(), destino == null ? "" : destino.getNombre(), e.getFactor());
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
