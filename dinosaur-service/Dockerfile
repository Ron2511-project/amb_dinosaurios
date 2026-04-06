# ── Stage 1: Build ───────────────────────────────────────────────────────────
# eclipse-temurin:17-jdk-jammy soporta amd64 y arm64 (Apple Silicon M1/M2/M3)
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Instala Maven (imagen jammy = Ubuntu 22.04, usa apt)
RUN apt-get update -q && apt-get install -y -q maven && rm -rf /var/lib/apt/lists/*

# Descarga dependencias primero — capa cacheada si pom.xml no cambia
COPY pom.xml .
RUN mvn -q dependency:go-offline -B

# Copia el código fuente y compila
COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# eclipse-temurin:17-jre-jammy también soporta arm64
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Usuario no-root por seguridad (sintaxis Debian/Ubuntu)
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/target/dinosaur-service-*.jar app.jar

USER appuser

EXPOSE 8080

# Opciones JVM optimizadas para contenedores
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
