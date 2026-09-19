package com.regenta.mesas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.mesas.BaseDeMesas;

/**
 * El catálogo de mesas tiene que salir al bus (HU-134). {@code hechos_comanda.mesa_codigo}
 * existe desde V1 y siempre está en NULL porque nada lo publica: la rotación cuenta bien
 * porque agrupa por {@code mesa_id}, pero un listado por mesa mostraría UUIDs.
 *
 * <p>Es catálogo, no transacción: se publica la mesa completa en cada cambio, no un delta,
 * igual que {@code recurso_creado}/{@code recurso_actualizado}/{@code recurso_eliminado}
 * de HU-099.
 */
class EventosDelCatalogoDeMesasTest extends BaseDeMesas {

    private static final Set<String> ADMIN = Set.of("MESAS_MESA_VER", "MESAS_MESA_CREAR",
            "MESAS_MESA_EDITAR");

    @Autowired
    private GestionDeMesas mesas;
    @Autowired
    private GestionDeZonas zonas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID zona(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN,
                () -> zonas.crear(new SolicitudDeZona(nombre, null, null))).id();
    }

    private MesaDelNegocio crear(UUID negocio, UUID zonaId, String codigo) {
        return enContexto(negocio, admin, ADMIN, () -> mesas.crear(new SolicitudDeMesa(zonaId, codigo,
                null, 4, "CUADRADA", 0, 0, 80, 80)));
    }

    private List<String> payloads(UUID mesaId, String tipoEvento) {
        return consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + mesaId
                + "' AND tipo_evento = '" + tipoEvento + "' ORDER BY id");
    }

    @Test
    @DisplayName("Crear una mesa publica mesa_creada con su código, su zona y su capacidad")
    void crearPublicaLaMesaCompleta() {
        UUID zonaId = zona(negocioA, "Terraza");

        MesaDelNegocio mesa = crear(negocioA, zonaId, "T1");

        List<String> eventos = payloads(mesa.id(), "mesa_creada");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"negocio_id\": \"" + negocioA + "\"")
                .contains("\"mesa_id\": \"" + mesa.id() + "\"")
                .contains("\"zona_id\": \"" + zonaId + "\"")
                .contains("\"zona_nombre\": \"Terraza\"")
                .contains("\"codigo\": \"T1\"")
                .contains("\"capacidad\": 4")
                .contains("\"activa\": true");
    }

    @Test
    @DisplayName("Una mesa sin zona publica mesa_creada con la zona en null")
    void crearSinZonaPublicaZonaNula() {
        MesaDelNegocio mesa = crear(negocioA, null, "BARRA");

        List<String> eventos = payloads(mesa.id(), "mesa_creada");
        assertThat(eventos.get(0)).contains("\"zona_nombre\": null");
    }

    @Test
    @DisplayName("Editar una mesa publica mesa_actualizada con lo que quedó")
    void actualizarPublicaLoQueQuedo() {
        UUID zonaId = zona(negocioA, "Salón");
        MesaDelNegocio mesa = crear(negocioA, zonaId, "S1");

        enContexto(negocioA, admin, ADMIN, () -> mesas.actualizar(mesa.id(),
                new SolicitudDeMesa(zonaId, "S1", "Mesa del rincón", 6, "REDONDA", 0, 0, 80, 80)));

        List<String> eventos = payloads(mesa.id(), "mesa_actualizada");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"nombre\": \"Mesa del rincón\"")
                .contains("\"capacidad\": 6");
    }

    @Test
    @DisplayName("Eliminar una mesa publica mesa_eliminada: deja de contar en el salón")
    void eliminarPublicaQueYaNoCuenta() {
        MesaDelNegocio mesa = crear(negocioA, null, "M1");

        enContexto(negocioA, admin, ADMIN, () -> mesas.eliminar(mesa.id()));

        List<String> eventos = payloads(mesa.id(), "mesa_eliminada");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"mesa_id\": \"" + mesa.id() + "\"")
                .contains("\"activa\": false");
    }

    @Test
    @DisplayName("Una mesa ocupada no se elimina, y entonces tampoco publica mesa_eliminada")
    void loQueNoSeEliminaNoSePublica() {
        MesaDelNegocio mesa = crear(negocioA, null, "OCUPADA1");
        ejecutarComoElServicio(negocioA, "INSERT INTO sesiones_mesa "
                + "(id, negocio_id, mesa_principal_id, estado) VALUES ('" + UUID.randomUUID()
                + "', '" + negocioA + "', '" + mesa.id() + "', 'ABIERTA')");

        try {
            enContexto(negocioA, admin, ADMIN, () -> mesas.eliminar(mesa.id()));
        } catch (RuntimeException esperada) {
            // El criterio 4 de HU-081 ya prueba el rechazo; aquí importa el bus.
        }

        assertThat(payloads(mesa.id(), "mesa_eliminada")).isEmpty();
    }

    @Test
    @DisplayName("Cada evento se registra a nombre de su negocio, no del otro")
    void cadaEventoEsDeSuNegocio() {
        MesaDelNegocio deA = crear(negocioA, null, "201");
        MesaDelNegocio deB = crear(negocioB, null, "201");

        assertThat(consultar("SELECT count(*) FROM outbox_eventos WHERE negocio_id = '" + negocioA
                + "' AND agregado_id = '" + deB.id() + "'")).containsExactly("0");
        assertThat(payloads(deA.id(), "mesa_creada").get(0))
                .contains("\"negocio_id\": \"" + negocioA + "\"");
        assertThat(payloads(deB.id(), "mesa_creada").get(0))
                .contains("\"negocio_id\": \"" + negocioB + "\"");
    }
}
