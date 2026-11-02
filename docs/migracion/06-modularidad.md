# Verificación de Modularidad y Documentación (Fase 6)

## 1. Verificación de Reglas Modulares con Spring Modulith

Spring Modulith impone reglas estrictas de encapsulamiento mediante ArchUnit:
1. **Límites de Módulo:**
   - Un módulo no puede importar clases de paquetes internos de otro módulo (por ejemplo `infrastructure`, `domain`, `application`, `web`).
   - Solo puede invocar contratos declarados explícitamente en el paquete `api` de otro módulo, marcado con `@NamedInterface("api")` en su `package-info.java`.
2. **Grafo Acíclico de Dependencias (DAG):**
   - El grafo de dependencias entre los 15 módulos del monolito no contiene ningún ciclo directo ni indirecto.
   - El acoplamiento bidireccional histórico entre `recursos` y `reservas` se desacopló mediante Inversión de Dependencias (DIP): `recursos` declara la SPI `ConsultaDeReservasDeRecurso` en su API pública, y `reservas` la implementa mediante un adaptador en su infraestructura.
3. **Módulo Compartido (`shared`):**
   - El paquete `com.regenta.shared` está marcado con `@ApplicationModule(type = Type.OPEN)` en `package-info.java`, permitiendo que todos los módulos de dominio consuman sus utilidades transversales (seguridad, multi-tenancy, excepciones estándar, infraestructura de eventos).

---

## 2. Test Automatizado de Arquitectura

El test `ModularityTests.java` en `regenta-app-server` valida de manera continua:
```java
@Test
@DisplayName("Verifica la integridad modular de Regenta con Spring Modulith")
void modularidadValida() {
    ApplicationModules modulos = ApplicationModules.of(RegentaApplication.class);
    modulos.verify();
}
```
Cualquier importación ilegal entre módulos o introducción de un ciclo causa el fallo inmediato del pipeline de compilación.

---

## 3. Artefactos de Documentación Generados

A través de `Documenter(modulos)`, se generan automáticamente en `docs/modulith/`:
- `all-docs.adoc`: Documento maestro en formato AsciiDoc con todos los módulos, dependencias y eventos publicados/escuchados.
- `components.puml`: Diagrama de componentes general en PlantUML / C4 model.
- `module-<modulo>.adoc` y `module-<modulo>.puml`: Fichas técnicas individuales por módulo con sus contratos públicos, listeners y modelo de dominio.
