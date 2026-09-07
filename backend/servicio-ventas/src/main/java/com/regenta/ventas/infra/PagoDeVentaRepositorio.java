package com.regenta.ventas.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.ventas.domain.MetodoDePago;
import com.regenta.ventas.domain.PagoDeVenta;

public interface PagoDeVentaRepositorio extends JpaRepository<PagoDeVenta, UUID> {

    List<PagoDeVenta> findByVentaIdOrderByRecibidoEn(UUID ventaId);

    @Query("select coalesce(sum(p.monto), 0) from PagoDeVenta p where p.ventaId = :ventaId")
    BigDecimal sumaAplicadaDe(@Param("ventaId") UUID ventaId);

    @Query("select distinct p.metodo from PagoDeVenta p where p.ventaId = :ventaId")
    List<MetodoDePago> metodosDe(@Param("ventaId") UUID ventaId);
}
