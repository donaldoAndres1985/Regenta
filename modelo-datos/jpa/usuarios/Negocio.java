package com.regenta.usuarios.dominio;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Raíz del tenant. Esta es la fila que se crea al vender Regenta a un
 * negocio nuevo: NO se despliega infraestructura, no se toca Docker ni
 * Railway. Una fila aquí + un usuario admin y el cliente ya opera.
 *
 * Vive SOLO en servicio-usuarios. Los demás servicios no consultan esta
 * tabla en cada request: reciben negocio_id, plan y patrón como claims del
 * JWT, y mantienen una réplica local alimentada por eventos.
 */
@Entity
@Table(name = "negocios", schema = "core_identidad")
public class Negocio {

    @Id private UUID id;

    @Column(name = "nombre_comercial", nullable = false, length = 150) private String nombreComercial;
    @Column(name = "razon_social", length = 200) private String razonSocial;
    @Column(name = "numero_documento", nullable = false, length = 30) private String numeroDocumento;

    /**
     * En la práctica INMUTABLE: cambiar de patrón después de operar exige
     * migrar datos entre modelos distintos (productos → recursos → ítems de
     * menú). El servicio lo bloquea si el negocio ya tiene transacciones.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "patron_operativo", nullable = false, length = 30, updatable = false)
    private PatronOperativo patronOperativo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNegocio estado = EstadoNegocio.TRIAL;

    @Column(name = "zona_horaria", nullable = false, length = 50) private String zonaHoraria = "America/Bogota";
    @Column(name = "moneda", nullable = false, length = 3) private String moneda = "COP";
    @Column(name = "pais", nullable = false, length = 2) private String pais = "CO";
    @Column(name = "creado_en", nullable = false, updatable = false) private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Version @Column(name = "version", nullable = false) private Long version;

    public enum PatronOperativo {
        VENTA_DIRECTA, RESERVA, COMANDA;

        /** Ferretería, papelería, droguería, veterinaria… todas caen aquí.
         *  Un tipo de negocio nuevo NO agrega un valor a este enum. */
        public String moduloCatalogo() {
            return switch (this) { case VENTA_DIRECTA -> "INVENTARIO";
                                   case RESERVA -> "RECURSOS";
                                   case COMANDA -> "MENU"; };
        }
        public String moduloTransaccion() {
            return switch (this) { case VENTA_DIRECTA -> "VENTAS";
                                   case RESERVA -> "RESERVAS";
                                   case COMANDA -> "COMANDAS"; };
        }
        public String eventoCierre() {
            return switch (this) { case VENTA_DIRECTA -> "venta_completada";
                                   case RESERVA -> "estancia_finalizada";
                                   case COMANDA -> "pedido_completado"; };
        }
    }

    public enum EstadoNegocio { TRIAL, ACTIVO, SUSPENDIDO, CANCELADO }
}
