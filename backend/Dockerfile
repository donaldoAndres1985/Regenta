# Multi-stage Dockerfile para el Monolito Modular Regenta (desde backend/)
# =======================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /src

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY regenta-app-server/pom.xml regenta-app-server/pom.xml

RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw && ./mvnw dependency:go-offline -pl regenta-app-server -am -B

COPY regenta-app-server regenta-app-server

RUN ./mvnw -pl regenta-app-server -am clean package -DskipTests -B

# =======================================================
# Runtime Stage
# =======================================================
FROM eclipse-temurin:17-jre-alpine AS runner

RUN addgroup -S regenta && adduser -S regenta -G regenta

WORKDIR /app

COPY --from=builder /src/regenta-app-server/target/regenta-app-server.jar /app/app.jar

RUN chown -R regenta:regenta /app

USER regenta

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
