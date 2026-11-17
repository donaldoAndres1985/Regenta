package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.domain.EquivalenciaDeDetallado;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.EquivalenciaDeDetalladoRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-161 · equivalencias de detallado (I16). */
class EquivalenciasDeDetalladoTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final EquivalenciaDeDetalladoRepositorio equivalencias = mock(EquivalenciaDeDetalladoRepositorio.class);
    private final List<EquivalenciaDeDetallado> guardadas = new ArrayList<>();
    private final GestionDeDetallado gestion = new GestionDeDetallado(productos, equivalencias, null, null);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID galones = UUID.randomUUID();
    private final List<Producto> catalogo = new ArrayList<>();

    private Producto producto(String nombre, UUID unidad) {
        Producto p = Producto.nuevo(negocioId, nombre, nombre, UUID.randomUUID(), unidad, TarifaIva.IVA_19,
                UUID.randomUUID());
        catalogo.add(p);
        when(productos.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_DETALLAR"), Set.of()));
        when(productos.findByNegocioIdOrderByNombreAsc(negocioId)).thenAnswer(i -> List.copyOf(catalogo));
        when(equivalencias.save(any())).thenAnswer(i -> {
            guardadas.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(equivalencias.existsByOrigenIdAndDestinoId(any(), any())).thenAnswer(i -> guardadas.stream()
                .anyMatch(e -> e.getOrigenId().equals(i.getArgument(0)) && e.getDestinoId().equals(i.getArgument(1))));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-161 C1 · Cuñete y galón en galones: se guarda «1 cuñete = 5 galones»")
    void seGuarda() {
        Producto cunete = producto("Pintulux blanco cuñete", galones);
        Producto galon = producto("Pintulux blanco galón", galones);

        EquivalenciaDelNegocio e = gestion.crearEquivalencia(cunete.getId(), galon.getId(), new BigDecimal("5"));

        assertThat(e.factor()).isEqualByComparingTo("5");
        assertThat(e.origen()).isEqualTo("Pintulux blanco cuñete");
        assertThat(e.destino()).isEqualTo("Pintulux blanco galón");
    }

    @Test
    @DisplayName("HU-161 C2 · Distinta unidad, el mismo producto o un factor ≤ 0 se rechazan con el motivo")
    void reglas() {
        Producto cunete = producto("Pintulux blanco cuñete", galones);
        Producto kilo = producto("Masilla kilo", UUID.randomUUID());
        Producto galon = producto("Pintulux blanco galón", galones);

        assertThatThrownBy(() -> gestion.crearEquivalencia(cunete.getId(), kilo.getId(), BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("misma unidad");
        assertThatThrownBy(() -> gestion.crearEquivalencia(cunete.getId(), cunete.getId(), BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("distinto");
        assertThatThrownBy(() -> gestion.crearEquivalencia(cunete.getId(), galon.getId(), BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("mayor que 0");
    }

    @Test
    @DisplayName("HU-161 C3 · Replicar a los cuñetes une cada uno con el galón de su color; los que no tienen par se listan")
    void replicar() {
        Producto cBlanco = producto("Pintulux blanco cuñete", galones);
        Producto cRojo = producto("Pintulux rojo cuñete", galones);
        Producto cVerde = producto("Pintulux verde cuñete", galones);
        Producto gBlanco = producto("Pintulux blanco galón", galones);
        Producto gRojo = producto("PINTULUX ROJO GALON", galones);

        ResultadoDeReplica r = gestion.replicar(new SolicitudDeReplica(
                List.of(cBlanco.getId(), cRojo.getId(), cVerde.getId()), "cuñete", "galón", new BigDecimal("5")));

        assertThat(r.creadas()).isEqualTo(2);
        assertThat(r.sinPar()).containsExactly("Pintulux verde cuñete");
        assertThat(guardadas).extracting(EquivalenciaDeDetallado::getDestinoId)
                .containsExactlyInAnyOrder(gBlanco.getId(), gRojo.getId());
    }

    @Test
    @DisplayName("HU-161 C4 · Un par que ya tiene equivalencia no se repite")
    void parRepetido() {
        Producto cunete = producto("Pintulux blanco cuñete", galones);
        Producto galon = producto("Pintulux blanco galón", galones);
        gestion.crearEquivalencia(cunete.getId(), galon.getId(), new BigDecimal("5"));

        assertThatThrownBy(() -> gestion.crearEquivalencia(cunete.getId(), galon.getId(), new BigDecimal("4")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }
}
