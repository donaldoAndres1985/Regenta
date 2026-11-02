# Fase 2: Mapeo de Rutas (Gateway → Monolito Modular)

Este documento registra la equivalencia exacta entre las rutas públicas que expone el Gateway en la arquitectura de microservicios y las rutas servidas directamente por el monolito modular Spring Modulith.

---

## 1. Principio de Preservación de Contrato

El frontend Flutter (`regenta-app`) se comunica a través de HTTP/REST con el backend sin conocer la topología interna.
- En la arquitectura anterior, el API Gateway recibía `/api/<servicio>/**` y lo enrutaba al microservicio correspondiente en su puerto local (8081–8095) sin alterar el path (`StripPrefix = 0`).
- En el monolito modular, los controladores REST conservan exactamente los mismos prefijos de ruta `@RequestMapping`, por lo que **la URL pública observada por el frontend es 100% idéntica**.

---

## 2. Mapeo Módulo por Módulo

| Módulo | Ruta en el Gateway | Servicio Origen | Ruta en el Monolito Modular | Controlador |
|---|---|---|---|---|
| **usuarios** | `/api/usuarios/auth/**` | `servicio-usuarios:8081` | `/api/usuarios/auth/**` | `AutenticacionControlador` |
| **usuarios** | `/api/usuarios/usuarios/**` | `servicio-usuarios:8081` | `/api/usuarios/usuarios/**` | `UsuariosControlador` |
| **usuarios** | `/api/usuarios/roles/**` | `servicio-usuarios:8081` | `/api/usuarios/roles/**` | `RolesControlador` |
| **usuarios** | `/api/usuarios/permisos` | `servicio-usuarios:8081` | `/api/usuarios/permisos` | `RolesControlador` |
| **usuarios** | `/api/usuarios/sucursales/**`| `servicio-usuarios:8081`| `/api/usuarios/sucursales/**` | `SucursalesControlador` |
| **usuarios** | `/api/usuarios/negocios/**` | `servicio-usuarios:8081` | `/api/usuarios/negocios/**` | `NegociosControlador` |
| **usuarios** | `/api/usuarios/modulos/**` | `servicio-usuarios:8081` | `/api/usuarios/modulos/**` | `ModulosControlador` |
| **usuarios** | `/api/usuarios/mi-negocio` | `servicio-usuarios:8081` | `/api/usuarios/mi-negocio` | `ModulosControlador` |
| **usuarios** | `/api/usuarios/configuracion/**`| `servicio-usuarios:8081`| `/api/usuarios/configuracion/**`| `ConfiguracionControlador` |
| **clientes** | `/api/clientes/**` | `servicio-clientes:8082` | `/api/clientes/**` | `ClientesControlador` |
| **clientes** | `/api/clientes/{id}/cartera` | `servicio-clientes:8082` | `/api/clientes/{id}/cartera` | `CarteraControlador` |
| **clientes** | `/api/clientes/{id}/metricas`| `servicio-clientes:8082` | `/api/clientes/{id}/metricas`| `MetricasControlador` |
| **clientes** | `/api/clientes/{id}/interacciones/**`| `servicio-clientes:8082`| `/api/clientes/{id}/interacciones/**`| `InteraccionesControlador` |
| **clientes** | `/api/clientes/{id}/cuentas/**`| `servicio-clientes:8082`| `/api/clientes/{id}/cuentas/**`| `CuentasControlador` |
| **inventario**| `/api/inventario/productos/**`| `servicio-inventario:8083`| `/api/inventario/productos/**`| `ProductosControlador` |
| **inventario**| `/api/inventario/categorias/**`| `servicio-inventario:8083`| `/api/inventario/categorias/**`| `CategoriasControlador` |
| **inventario**| `/api/inventario/lotes/**` | `servicio-inventario:8083`| `/api/inventario/lotes/**` | `LotesControlador` |
| **inventario**| `/api/inventario/bodegas/**` | `servicio-inventario:8083`| `/api/inventario/bodegas/**` | `BodegasControlador` |
| **inventario**| `/api/inventario/movimientos/**`| `servicio-inventario:8083`| `/api/inventario/movimientos/**`| `MovimientosControlador` |
| **inventario**| `/api/inventario/listas-precios/**`| `servicio-inventario:8083`| `/api/inventario/listas-precios/**`| `ListasDePreciosControlador` |
| **inventario**| `/api/inventario/reservas-stock/**`| `servicio-inventario:8083`| `/api/inventario/reservas-stock/**`| `ReservasStockControlador` |
| **inventario**| `/api/inventario/traslados/**`| `servicio-inventario:8083`| `/api/inventario/traslados/**`| `TrasladosControlador` |
| **inventario**| `/api/inventario/ajustes/**` | `servicio-inventario:8083`| `/api/inventario/ajustes/**` | `AjustesControlador` |
| **ventas** | `/api/ventas/**` | `servicio-ventas:8084` | `/api/ventas/**` | `VentasControlador` |
| **ventas** | `/api/ventas/{id}/pagos` | `servicio-ventas:8084` | `/api/ventas/{id}/pagos` | `PagosControlador` |
| **ventas** | `/api/ventas/{id}/cierre` | `servicio-ventas:8084` | `/api/ventas/{id}/cierre` | `PagosControlador` |
| **ventas** | `/api/ventas/{id}/devoluciones/**`| `servicio-ventas:8084`| `/api/ventas/{id}/devoluciones/**`| `DevolucionesControlador` |
| **ventas** | `/api/ventas/offline/**` | `servicio-ventas:8084` | `/api/ventas/offline/**` | `VentasOfflineControlador` |
| **ventas** | `/api/ventas/reglas-de-cobro`| `servicio-ventas:8084` | `/api/ventas/reglas-de-cobro`| `ReglasDeCobroControlador` |
| **ventas** | `/api/cotizaciones/**` | `servicio-ventas:8084` | `/api/cotizaciones/**` | `CotizacionesControlador` |
| **compras** | `/api/compras/proveedores/**`| `servicio-compras:8085`| `/api/compras/proveedores/**`| `ProveedoresControlador` |
| **compras** | `/api/compras/ordenes/**` | `servicio-compras:8085`| `/api/compras/ordenes/**` | `OrdenesDeCompraControlador` |
| **compras** | `/api/compras/recepciones/**`| `servicio-compras:8085`| `/api/compras/recepciones/**`| `RecepcionesControlador` |
| **compras** | `/api/compras/cuentas-por-pagar/**`| `servicio-compras:8085`| `/api/compras/cuentas-por-pagar/**`| `CuentasPorPagarControlador` |
| **compras** | `/api/compras/sugerencias/**`| `servicio-compras:8085`| `/api/compras/sugerencias/**`| `SugerenciasDeCompraControlador` |
| **recursos** | `/api/recursos/**` | `servicio-recursos:8086` | `/api/recursos/**` | `RecursosControlador` |
| **recursos** | `/api/recursos/tipos/**` | `servicio-recursos:8086` | `/api/recursos/tipos/**` | `TiposDeRecursoControlador` |
| **recursos** | `/api/recursos/tarifas/**` | `servicio-recursos:8086` | `/api/recursos/tarifas/**` | `TarifasControlador` |
| **recursos** | `/api/recursos/bloqueos/**`| `servicio-recursos:8086` | `/api/recursos/bloqueos/**`| `BloqueosControlador` |
| **recursos** | `/api/recursos/politicas-cancelacion/**`| `servicio-recursos:8086`| `/api/recursos/politicas-cancelacion/**`| `PoliticasDeCancelacionControlador` |
| **recursos** | `/api/recursos/servicios-adicionales/**`| `servicio-recursos:8086`| `/api/recursos/servicios-adicionales/**`| `ServiciosAdicionalesControlador` |
| **reservas** | `/api/reservas/**` | `servicio-reservas:8087` | `/api/reservas/**` | `ReservasControlador` |
| **reservas** | `/api/reservas/disponibilidad/**`| `servicio-reservas:8087`| `/api/reservas/disponibilidad/**`| `DisponibilidadControlador` |
| **reservas** | `/api/reservas/estancias/**`| `servicio-reservas:8087`| `/api/reservas/estancias/**`| `EstanciasControlador` |
| **menu** | `/api/menu/cartas/**` | `servicio-menu:8088` | `/api/menu/cartas/**` | `CartasControlador` |
| **menu** | `/api/menu/items/**` | `servicio-menu:8088` | `/api/menu/items/**` | `ItemsDeMenuControlador` |
| **menu** | `/api/menu/modificadores/**`| `servicio-menu:8088`| `/api/menu/modificadores/**`| `ModificadoresControlador` |
| **menu** | `/api/menu/recetas/**` | `servicio-menu:8088` | `/api/menu/recetas/**` | `RecetasControlador` |
| **menu** | `/api/menu/estaciones/**` | `servicio-menu:8088` | `/api/menu/estaciones/**` | `EstacionesDeCocinaControlador` |
| **menu** | `/api/menu/disponibilidad/**`| `servicio-menu:8088`| `/api/menu/disponibilidad/**`| `DisponibilidadControlador` |
| **mesas** | `/api/mesas/**` | `servicio-mesas:8089` | `/api/mesas/**` | `MesasControlador` |
| **mesas** | `/api/mesas/zonas/**` | `servicio-mesas:8089` | `/api/mesas/zonas/**` | `ZonasControlador` |
| **mesas** | `/api/mesas/sesiones/**` | `servicio-mesas:8089` | `/api/mesas/sesiones/**` | `SesionesDeMesaControlador` |
| **comandas** | `/api/comandas/**` | `servicio-comandas:8090` | `/api/comandas/**` | `ComandasControlador` |
| **comandas** | `/api/comandas/cocina/**`| `servicio-comandas:8090` | `/api/comandas/cocina/**`| `CocinaControlador` |
| **comandas** | `/api/comandas/cuentas/**`| `servicio-comandas:8090` | `/api/comandas/cuentas/**`| `CuentasControlador` |
| **facturacion**| `/api/facturacion/facturas/**`| `servicio-facturacion:8091`| `/api/facturacion/facturas/**`| `FacturasControlador` |
| **facturacion**| `/api/facturacion/resoluciones/**`| `servicio-facturacion:8091`| `/api/facturacion/resoluciones/**`| `ResolucionesControlador` |
| **facturacion**| `/api/facturacion/notas-credito/**`| `servicio-facturacion:8091`| `/api/facturacion/notas-credito/**`| `NotasCreditoControlador` |
| **facturacion**| `/api/facturacion/contingencias/**`| `servicio-facturacion:8091`| `/api/facturacion/contingencias/**`| `ContingenciasControlador` |
| **facturacion**| `/api/facturacion/transmisiones/**`| `servicio-facturacion:8091`| `/api/facturacion/transmisiones/**`| `TransmisionesControlador` |
| **facturacion**| `/api/facturacion/certificados/**`| `servicio-facturacion:8091`| `/api/facturacion/certificados/**`| `CertificadosControlador` |
| **caja** | `/api/caja/cajas/**` | `servicio-caja:8092` | `/api/caja/cajas/**` | `CajasControlador` |
| **caja** | `/api/caja/sesiones/**` | `servicio-caja:8092` | `/api/caja/sesiones/**` | `SesionesDeCajaControlador` |
| **caja** | `/api/caja/movimientos/**`| `servicio-caja:8092` | `/api/caja/movimientos/**`| `MovimientosDeCajaControlador` |
| **caja** | `/api/caja/reportes/**` | `servicio-caja:8092` | `/api/caja/reportes/**` | `ReportesDeCajaControlador` |
| **alertas** | `/api/alertas/mias/**` | `servicio-alertas:8093` | `/api/alertas/mias/**` | `NotificacionesControlador` |
| **alertas** | `/api/alertas/reglas/**` | `servicio-alertas:8093` | `/api/alertas/reglas/**` | `ReglasDeAlertaControlador` |
| **alertas** | `/api/alertas/dispositivos/**`| `servicio-alertas:8093`| `/api/alertas/dispositivos/**`| `NotificacionesControlador` |
| **alertas** | `/api/alertas/preferencias/**`| `servicio-alertas:8093`| `/api/alertas/preferencias/**`| `NotificacionesControlador` |
| **alertas** | `/api/alertas/vigilancia/**`| `servicio-alertas:8093`| `/api/alertas/vigilancia/**`| `VigilanciaControlador` |
| **reportes** | `/api/reportes/ventas/**`| `servicio-reportes:8094` | `/api/reportes/ventas/**`| `ReportesDeVentasControlador` |
| **reportes** | `/api/reportes/panel/**` | `servicio-reportes:8094` | `/api/reportes/panel/**` | `PanelControlador` |
| **reportes** | `/api/reportes/metricas/**`| `servicio-reportes:8094`| `/api/reportes/metricas/**`| `MetricasPorPatronControlador` |
| **reportes** | `/api/reportes/exportaciones/**`| `servicio-reportes:8094`| `/api/reportes/exportaciones/**`| `ExportacionesControlador` |
| **auditoria** | `/api/auditoria/eventos/**`| `servicio-auditoria:8095`| `/api/auditoria/eventos/**`| `AuditoriaControlador` |
| **auditoria** | `/api/auditoria/sincronizacion/**`| `servicio-auditoria:8095`| `/api/auditoria/sincronizacion/**`| `SincronizacionControlador` |
| **auditoria** | `/api/auditoria/conflictos/**`| `servicio-auditoria:8095`| `/api/auditoria/conflictos/**`| `ConflictosControlador` |
