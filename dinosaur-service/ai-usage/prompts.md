# Prompts utilizados — dinosaur-service

## 1. Diseño inicial de arquitectura hexagonal
Prompt: "Actúa como un desarrollador senior especializado en Java 17 y Spring Boot 3.x.
Implementa un endpoint POST /v1/dinosaur con arquitectura hexagonal (Ports & Adapters)
con puertos de entrada/salida, adaptadores REST, PostgreSQL y Redis para idempotencia."

## 2. Adaptación al schema CQRS existente
Prompt: "El proyecto ya tiene un schema PostgreSQL con arquitectura CQRS:
tablas dinosaurs_write y dinosaurs_read sincronizadas por trigger.
ID es BIGSERIAL (Long), ENUM dinosaur_status tiene ALIVE/EXTINCT/INACTIVE.
Adapta el microservicio Java para conectarse a esa BD sin modificar el schema."

## 3. Mapeo de estados dominio ↔ BD
Prompt: "El dominio Java usa ENDANGERED pero la BD usa INACTIVE.
Implementa el mapeo en la capa de persistencia sin exponer esta diferencia al dominio."

## 4. Docker multi-stage para Apple Silicon (arm64)
Prompt: "El Dockerfile con eclipse-temurin:17-jdk-alpine falla en Mac M1/M2/M3.
Reemplaza por eclipse-temurin:17-jdk-jammy que soporta arm64."

## 5. Red Docker compartida entre proyectos hermanos
Prompt: "dinosaur-db y dinosaur-service son proyectos hermanos bajo AMB_DINOSAURIOS/.
El microservicio debe unirse a la red froneus_network del proyecto de BD
usando networks.external: true en su docker-compose.yml."
