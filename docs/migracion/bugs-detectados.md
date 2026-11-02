# Bugs Preexistentes Detectados

Durante la Fase 0 (Inventario y Análisis) y las pruebas iniciales de regresión sobre la arquitectura de microservicios previa a la migración, se identificaron los siguientes comportamientos y defectos preexistentes en el código original. De acuerdo con las reglas de la migración ("NO los corrijas durante la migración; anótalos en este archivo"), se documentan a continuación sin modificar la lógica original:

---

## 1. Desfase de Zona Horaria UTC en `ReporteDeCaja.sesiones` vs `ReporteDeCajaTest`

- **Ubicación:** `com.regenta.caja.aplicacion.ReporteDeCaja.java` (líneas 57-60) y `com.regenta.caja.aplicacion.ReporteDeCajaTest.java` (líneas 100-106, 118-120, 132-134).
- **Descripción:**
  En `ReporteDeCaja.sesiones`:
  ```java
  OffsetDateTime inicio = desde.atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
  OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
  ```
  `desde.atStartOfDay()` genera un `LocalDateTime` sin zona horaria basado en la hora local de la máquina. Al convertirlo con `.toInstant(ZoneOffset.UTC)`, se asume que ese inicio de día está en UTC. Cuando las pruebas se ejecutan en zonas horarias con desfase negativo respecto a UTC (como UTC-5 / Colombia) en horario nocturno (ej. después de las 19:00 COT), la fecha actual en la máquina es el día $D$, pero en la base de datos PostgreSQL `now()` ya es el día $D+1$ en UTC.
  Como resultado, las sesiones de caja creadas con `now()` quedan con timestamp UTC del día $D+1$, cayendo fuera del rango filtrado `[inicio, fin)`, provocando que `reportes.sesiones(LocalDate.now(), ...)` devuelva una lista vacía en vez de la sesión recién creada.
- **Impacto:** Falla en 3 tests de `ReporteDeCajaTest` (`descuadradaSeMarca`, `filtroPorEstado`, `listadoPorRango`) si se ejecutan entre las 19:00 y las 23:59 en husos UTC-5.
- **Recomendación post-migración:** Usar la zona horaria del negocio o `ZoneId.systemDefault()` para anclar `atStartOfDay(ZoneId)` y luego convertir a UTC de manera coherente con `now()`.

---

## 2. Ruta `/api/cotizaciones` no expuesta en el Spring Cloud Gateway

- **Ubicación:** `gateway/src/main/resources/application.yml` (predicados de rutas del Gateway) y `com.regenta.ventas.api.CotizacionesControlador.java` (`@RequestMapping("/api/cotizaciones")`).
- **Descripción:**
  El microservicio `servicio-ventas` expone el controlador de cotizaciones mapeado en `/api/cotizaciones`:
  - `POST /api/cotizaciones`
  - `GET /api/cotizaciones/{cotizacionId}`
  - `POST /api/cotizaciones/{cotizacionId}/conversion`
  Sin embargo, el Spring Cloud Gateway solo enrutaba a `servicio-ventas` el prefijo:
  ```yaml
  - id: servicio-ventas
    uri: ${SERVICIO_VENTAS_URI:http://localhost:8084}
    predicates:
      - Path=/api/ventas/**
  ```
  El Gateway no tenía ninguna regla de enrutamiento para `/api/cotizaciones/**`. Por ende, cualquier petición externa a través del Gateway hacia `/api/cotizaciones` resultaba en 404 Not Found.
- **Impacto:** En microservicios, el frontend no podía invocar cotizaciones directamente a través del Gateway a menos que se agregara la ruta explícita. En el monolito modular, al eliminarse el Gateway y tener una única aplicación web, `/api/cotizaciones` pasa a estar disponible automáticamente en la raíz HTTP de la aplicación sin alterar su contrato.

---

## 3. Ruta `/api/cocina` no expuesta en el Spring Cloud Gateway

- **Ubicación:** `gateway/src/main/resources/application.yml` (predicados de rutas del Gateway) y `com.regenta.comandas.api.CocinaControlador.java` (`@RequestMapping("/api/cocina")`).
- **Descripción:**
  El microservicio `servicio-comandas` expone el controlador KDS de cocina en `/api/cocina`:
  - `GET /api/cocina/estaciones/{estacionId}/tickets`
  - `POST /api/cocina/tickets/{ticketId}/avance`
  Sin embargo, el Spring Cloud Gateway solo enrutaba a `servicio-comandas` el prefijo `/api/comandas/**`.
- **Impacto:** En microservicios, cualquier petición al Gateway en `/api/cocina` retornaba 404. En el monolito modular, `CocinaControlador` se mapea dualmente a `@RequestMapping({"/api/cocina", "/api/comandas/cocina"})` para asegurar compatibilidad total tanto si el cliente llama directo a `/api/cocina` como si lo hace a `/api/comandas/cocina`.
