# Guía de Despliegue en Railway (Fase 7)

## 1. Arquitectura de Despliegue

La migración transforma una topología costosa de 19 componentes distribuidos en una solución unificada y eficiente:
- **Antes:** 15 microservicios Spring Boot + 1 Spring Cloud Gateway + 1 Broker RabbitMQ + PostgreSQL con conexiones fragmentadas. Cada JVM requería entre 300MB y 512MB de RAM base, sumando >6GB de memoria fija en ejecución constante.
- **Ahora:** 1 solo servicio en Railway ejecutando una única JVM optimizada en Alpine Linux, comunicándose con un único clúster PostgreSQL gestionado.

---

## 2. Dockerfile Multi-Stage Optimizado

El contenedor utiliza una imagen base `eclipse-temurin:17-jre-alpine` con:
- Separación de capas para acelerar builds en Railway (cacheo de dependencias Maven).
- Usuario no privilegiado `regenta` para máxima seguridad.
- Parámetros JVM diseñados específicamente para entornos cloud con límites de memoria:
  ```bash
  -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom
  ```
- Soporte dinámico para la variable `$PORT` inyectada por Railway (`${PORT:8080}`).

---

## 3. Manifiesto de Railway (`railway.json`)

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 5,
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 120
  }
}
```

---

## 4. Variables de Entorno en Railway

| Variable | Requerida | Valor Ejemplo / Descripción |
|---|---|---|
| `DATABASE_URL` | Sí | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | Sí | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | Sí | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRETO` | Sí | `clave-secreta-produccion-de-al-menos-32-caracteres!` |
| `JWT_EMISOR` | No | `regenta` (por defecto) |
| `PORT` | Automática | Inyectada por Railway (ej. 8080) |
| `DB_POOL_MAX` | No | `15` (por defecto) |

---

## 5. Análisis de Costos y Ahorro Estimado

| Concepto | Arquitectura Microservicios | Monolito Modular | Ahorro |
|---|---|---|---|
| Instancias JVM activas | 16 (15 servicios + 1 Gateway) | 1 (Monolito) | -93.7% |
| Broker de Mensajería | 1 RabbitMQ gestionado ($15-$25/mes) | 0 (Eventos en proceso con Spring Modulith) | -100% |
| Consumo de Memoria RAM | ~6 GB - 8 GB | ~512 MB - 1 GB | -87.5% |
| Costo Estimado en Railway | **$95 - $150 USD / mes** | **$7 - $12 USD / mes** | **> 90% de ahorro** |
