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

    /**
     * HU-162 · abre {@code cantidad} del origen en su destino (I17): sale {@code cantidad} del
     * origen y entra {@code cantidad × factor} al destino, en la misma bodega y transacción. I10:
     * no se abre más de lo que hay. I18: el costo que entra es el del origen dividido por el
     * factor, promediado con el que ya tenía el destino.
     */
    @Transactional
    @RequierePermiso("INVENTARIO_DETALLAR")
    public DetalladoHecho detallar(UUID origenId, UUID bodegaId, BigDecimal cantidad) {
        Producto origen = delNegocio(origenId);
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("Escribe cuántos vas a abrir.");
        }
        EquivalenciaDeDetallado eq = equivalencias.findByOrigenId(origenId).stream().findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException("Este producto no tiene equivalencia de detallado."));
        Producto destino = delNegocio(eq.getDestinoId());
        var exOrigen = existencias.findByProductoIdAndBodegaId(origenId, bodegaId);
        BigDecimal hayOrigen = exOrigen.map(e -> e.getCantidad()).orElse(BigDecimal.ZERO);
        if (cantidad.compareTo(hayOrigen) > 0) {
            throw new ReglaDeNegocioException("Solo hay " + texto(hayOrigen) + " de " + origen.getNombre()
                    + ". No puedes abrir " + texto(cantidad) + ".");
        }
        BigDecimal costoOrigen = exOrigen.map(e -> e.getCostoPromedio()).filter(c -> c.signum() > 0)
                .orElse(origen.getCostoUltimo());
        var exDestinoAntes = existencias.findByProductoIdAndBodegaId(destino.getId(), bodegaId);
        BigDecimal hayDestino = exDestinoAntes.map(e -> e.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal costoDestino = exDestinoAntes.map(e -> e.getCostoPromedio()).filter(c -> c.signum() > 0)
                .orElse(destino.getCostoUltimo());

        BigDecimal entra = cantidad.multiply(eq.getFactor());
        BigDecimal costoEntrante = costoOrigen.divide(eq.getFactor(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal base = hayDestino.max(BigDecimal.ZERO);
        BigDecimal nuevoCosto = base.multiply(costoDestino).add(entra.multiply(costoEntrante))
                .divide(base.add(entra), 4, java.math.RoundingMode.HALF_UP);

        UUID detalladoId = UUID.randomUUID();
        String motivo = "Detallado: " + texto(cantidad) + " " + origen.getNombre() + " → " + texto(entra)
                + " " + destino.getNombre();
        libro.registrar(new SolicitudDeMovimiento(origenId, bodegaId,
                com.regenta.inventario.domain.TipoMovimiento.SALIDA_DETALLE, cantidad,
                com.regenta.inventario.domain.OrigenMovimiento.DETALLE, detalladoId, motivo,
                "detallado:" + detalladoId + ":salida", null, null, null));
        libro.registrar(new SolicitudDeMovimiento(destino.getId(), bodegaId,
                com.regenta.inventario.domain.TipoMovimiento.ENTRADA_DETALLE, entra,
                com.regenta.inventario.domain.OrigenMovimiento.DETALLE, detalladoId, motivo,
                "detallado:" + detalladoId + ":entrada", null, null, null));
        existencias.findByProductoIdAndBodegaId(destino.getId(), bodegaId).ifPresent(e -> {
            e.fijarCostoPromedio(nuevoCosto);
            existencias.save(e);
        });
        return new DetalladoHecho(detalladoId, origenId, origen.getNombre(), cantidad, destino.getId(),
                destino.getNombre(), entra, nuevoCosto);
    }

    /**
     * HU-163 · si falta {@code destinoId}, qué se puede abrir para conseguirlo: la primera
     * equivalencia hacia él cuyo origen tenga existencia en la bodega. Vacío si no hay.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_DETALLAR")
    public java.util.Optional<OfertaDeDetallado> ofertaPara(UUID destinoId, UUID bodegaId) {
        Producto destino = delNegocio(destinoId);
        for (EquivalenciaDeDetallado eq : equivalencias.findByDestinoId(destinoId)) {
            BigDecimal hay = existencias.findByProductoIdAndBodegaId(eq.getOrigenId(), bodegaId)
                    .map(e -> e.getCantidad()).orElse(BigDecimal.ZERO);
            if (hay.compareTo(BigDecimal.ONE) >= 0) {
                Producto origen = delNegocio(eq.getOrigenId());
                return java.util.Optional.of(new OfertaDeDetallado(origen.getId(), origen.getNombre(),
                        destino.getId(), destino.getNombre(), eq.getFactor(), hay));
            }
        }
        return java.util.Optional.empty();
    }

    private static String texto(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
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
