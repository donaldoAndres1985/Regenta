package com.regenta.comun.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

/**
 * Lo que hace efectiva la RLS. Aqui se comprueba la mecanica; que la politica
 * de PostgreSQL corte de verdad se prueba contra una base real en el servicio.
 */
class AislamientoPorNegocioTest {

    private static final UUID NEGOCIO = UUID.fromString("9f1c6b0e-3a2d-4c58-9d21-5b7a0e4f1c33");

    private final EntityManagerFactory fabrica = mock(EntityManagerFactory.class);
    private final EntityManager gestor = mock(EntityManager.class);
    private final Query consulta = mock(Query.class);
    private final ProceedingJoinPoint punto = mock(ProceedingJoinPoint.class);
    private final AislamientoPorNegocio aspecto = new AislamientoPorNegocio(fabrica, 10);

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
        if (TransactionSynchronizationManager.hasResource(fabrica)) {
            TransactionSynchronizationManager.unbindResource(fabrica);
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("Dentro de una transaccion, fija el negocio con set_config local")
    void fijaElNegocioEnLaTransaccion() throws Throwable {
        when(gestor.createNativeQuery(anyString())).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(punto.proceed()).thenReturn("hecho");
        enTransaccion();
        ContextoDeNegocio.establecer(datos());

        Object resultado = aspecto.fijarElNegocio(punto);

        assertThat(resultado).isEqualTo("hecho");
        verify(gestor).createNativeQuery(AislamientoPorNegocio.FIJAR);
        verify(consulta).setParameter("negocio", NEGOCIO.toString());
        verify(consulta).getSingleResult();
        assertThat(AislamientoPorNegocio.FIJAR)
                .as("el tercer argumento en true es lo que lo hace LOCAL a la transaccion")
                .contains("set_config")
                .contains("true");
    }

    @Test
    @DisplayName("Sin transaccion no toca la base: un SET fuera de transaccion se pierde")
    void sinTransaccionNoHaceNada() throws Throwable {
        when(punto.proceed()).thenReturn("hecho");
        ContextoDeNegocio.establecer(datos());

        aspecto.fijarElNegocio(punto);

        verify(gestor, never()).createNativeQuery(anyString());
    }

    @Test
    @DisplayName("Sin negocio en contexto no fija nada y deja que la RLS corte")
    void sinNegocioNoFijaNada() throws Throwable {
        when(punto.proceed()).thenReturn("hecho");
        enTransaccion();

        aspecto.fijarElNegocio(punto);

        verify(gestor, never()).createNativeQuery(anyString());
    }

    private void enTransaccion() {
        TransactionSynchronizationManager.bindResource(fabrica, new EntityManagerHolder(gestor));
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private static DatosDelNegocio datos() {
        return new DatosDelNegocio(NEGOCIO, UUID.randomUUID(), "BASICO", "VENTA_DIRECTA",
                Set.of("ADMINISTRADOR"), Set.of("VENTAS"), Set.of());
    }
}
