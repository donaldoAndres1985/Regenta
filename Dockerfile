# Multi-stage Dockerfile para el Monolito Modular Regenta
# =======================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copiar dependencias de Maven wrapper y POMs para cacheo de capas
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/pom.xml backend/pom.xml
COPY backend/regenta-app-server/pom.xml backend/regenta-app-server/pom.xml

# Descargar dependencias
WORKDIR /workspace/backend
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -pl regenta-app-server -am -B

# Copiar el código fuente completo del monolito
COPY backend/regenta-app-server /workspace/backend/regenta-app-server

# Empaquetar el JAR ejecutable omitiendo tests (los tests corren en CI)
RUN ./mvnw clean package -pl regenta-app-server -DskipTests -B

# =======================================================
# Runtime Stage (Imagen final ligera y segura)
# =======================================================
FROM eclipse-temurin:17-jre-alpine AS runner

# Crear usuario y grupo sin privilegios para ejecución segura
RUN addgroup -S regenta && adduser -S regenta -G regenta

WORKDIR /app

# Copiar el JAR compilado desde la etapa de compilación
COPY --from=builder /workspace/backend/regenta-app-server/target/regenta-app-server-*.jar /app/app.jar

# Permisos
RUN chown -R regenta:regenta /app

USER regenta

EXPOSE 8080

# Opciones de JVM optimizadas para contenedores (Railway)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
