package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Criterio 3 de HU-029: un negocio siempre tiene al menos una bodega. El POS
 * necesita un id de bodega real para que la saga de confirmacion (HU-038)
 * encuentre la fila de existencia correcta -sin esto, toda venta se rechaza
 * con "no hay stock" sin importar cuanto haya.
 */
class GestionDeBodegasTest {

    private final BodegaRepositorio bodegas = mock(BodegaRepositorio.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);

    private final GestionDeBodegas gestion = new GestionDeBodegas(bodegas, existencias);

    private final UUID negocioId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_BODEGA_VER"), Set.of()));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void sinNingunaBodegaListarCreaLaPrincipalPorDefecto() {
        List<Bodega> guardadas = new ArrayList<>();
        when(bodegas.findByNegocioIdOrderByCodigo(negocioId))
                .thenAnswer(inv -> List.copyOf(guardadas));
        when(bodegas.findFirstByNegocioIdAndEsDefaultTrue(negocioId)).thenReturn(Optional.empty());
        when(bodegas.existsByNegocioId(negocioId)).thenReturn(false);
        when(bodegas.save(any())).thenAnswer(inv -> {
            Bodega b = inv.getArgument(0);
            guardadas.add(b);
            return b;
        });

        List<BodegaDelNegocio> resultado = gestion.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("BOD-PRAL");
        assertThat(resultado.get(0).esDefault()).isTrue();
        verify(bodegas).save(any());
    }

    @Test
    void conBodegasYaExistentesNoCreaNinguna() {
        Bodega existente = Bodega.nueva(negocioId, "BOD-01", "Bodega 1", null, true);
        when(bodegas.findByNegocioIdOrderByCodigo(negocioId)).thenReturn(List.of(existente));

        List<BodegaDelNegocio> resultado = gestion.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigo()).isEqualTo("BOD-01");
    }
}
