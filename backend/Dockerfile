# Un solo Dockerfile para los 16 modulos. El modulo se elige con --build-arg.
#   docker build --build-arg MODULO=servicio-ventas .
FROM eclipse-temurin:17-jdk AS build
ARG MODULO
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY gateway/pom.xml gateway/pom.xml
COPY estructura/pom.xml estructura/pom.xml
COPY servicio-usuarios/pom.xml servicio-usuarios/pom.xml
COPY servicio-clientes/pom.xml servicio-clientes/pom.xml
COPY servicio-inventario/pom.xml servicio-inventario/pom.xml
COPY servicio-ventas/pom.xml servicio-ventas/pom.xml
COPY servicio-compras/pom.xml servicio-compras/pom.xml
COPY servicio-recursos/pom.xml servicio-recursos/pom.xml
COPY servicio-reservas/pom.xml servicio-reservas/pom.xml
COPY servicio-menu/pom.xml servicio-menu/pom.xml
COPY servicio-mesas/pom.xml servicio-mesas/pom.xml
COPY servicio-comandas/pom.xml servicio-comandas/pom.xml
COPY servicio-facturacion/pom.xml servicio-facturacion/pom.xml
COPY servicio-caja/pom.xml servicio-caja/pom.xml
COPY servicio-alertas/pom.xml servicio-alertas/pom.xml
COPY servicio-reportes/pom.xml servicio-reportes/pom.xml
COPY servicio-auditoria/pom.xml servicio-auditoria/pom.xml
# Capa de dependencias: se rehace solo cuando cambia un pom.
RUN ./mvnw -B -q -pl ${MODULO} -am dependency:go-offline
COPY . .
RUN ./mvnw -B -q -pl ${MODULO} -am -DskipTests package

FROM eclipse-temurin:17-jre AS run
ARG MODULO
WORKDIR /app
COPY --from=build /src/${MODULO}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
