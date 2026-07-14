# ============================================================
#  GestProy — Dockerfile (build multi-etapa)
#
#  Etapa 1 (build): compila el JAR con Maven sobre JDK 21.
#  Etapa 2 (runtime): solo el JRE + el JAR, sin Maven ni código
#  fuente, para una imagen final pequeña.
#
#  No necesita db.properties: en el contenedor la configuración
#  (URL/usuario/contraseña de la BD y el secreto JWT) llega por
#  variables de entorno, que Spring Boot mapea automáticamente
#  (SPRING_DATASOURCE_URL -> spring.datasource.url, etc.).
# ============================================================

# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom para cachear la descarga de dependencias:
# mientras el pom no cambie, esta capa se reutiliza entre builds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Luego el código fuente y el empaquetado (sin tests: los tests de
# este proyecto viven en SQL, ver db/tests, no en JUnit).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Etapa 2: imagen de ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios (no correr la app como root).
RUN useradd --system --create-home --shell /usr/sbin/nologin gestproy
USER gestproy

# El fat-jar de Spring Boot (gestproy-1.0.0.jar); el comodín evita
# tener que actualizar la versión aquí si cambia en el pom.
COPY --from=build /build/target/gestproy-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
