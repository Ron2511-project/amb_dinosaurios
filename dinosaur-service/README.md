# dinosaur-service

Microservicio Spring Boot para gestión de dinosaurios.
Arquitectura hexagonal (Ports & Adapters) con PostgreSQL CQRS y Redis para idempotencia.

> **Para levantar el servicio**, usar el `docker-compose.yml` de la raíz del proyecto:
> ```bash
> cd ..   # raíz AMB_DINOSAURIOS
> docker compose up --build
> ```

---

## 🗂️ Estructura de paquetes

```
com.froneus.dinosaur/
│
├── domain/                          ← Núcleo — sin dependencias de framework
│   ├── model/
│   │   ├── Dinosaur.java            → Entidad de dominio inmutable
│   │   └── DinosaurStatus.java      → ALIVE | ENDANGERED | EXTINCT
│   ├── port/
│   │   ├── in/
│   │   │   └── CreateDinosaurUseCase.java   → Puerto de entrada
│   │   └── out/
│   │       ├── DinosaurRepository.java      → Puerto de salida (Postgres)
│   │       └── DinosaurIdempotencyPort.java → Puerto de salida (Redis)
│   └── exception/
│       ├── InvalidDinosaurDateException.java
│       └── DuplicateDinosaurNameException.java
│
├── application/                     ← Orquestación de casos de uso
│   └── usecase/
│       └── CreateDinosaurService.java
│
└── infrastructure/                  ← Adaptadores (detalles técnicos)
    ├── adapter/
    │   ├── in/web/
    │   │   ├── controller/DinosaurController.java
    │   │   ├── dto/CreateDinosaurRequest.java
    │   │   ├── dto/DinosaurResponse.java
    │   │   └── mapper/DinosaurWebMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── DinosaurPostgresAdapter.java
    │       │   ├── entity/DinosaurEntity.java
    │       │   ├── mapper/DinosaurPersistenceMapper.java
    │       │   └── repository/DinosaurJpaRepository.java
    │       └── redis/
    │           └── DinosaurRedisAdapter.java
    ├── config/
    │   └── BeanConfig.java          → Wiring de puertos e implementaciones
    └── exception/
        └── GlobalExceptionHandler.java
```

---

## 🗺️ Mapeo de estados

| Java (dominio) | PostgreSQL ENUM |
|----------------|-----------------|
| `ALIVE`        | `ALIVE`         |
| `ENDANGERED`   | `INACTIVE`      |
| `EXTINCT`      | `EXTINCT`       |

---

## 🧪 Tests

```bash
# Unitarios (sin Docker)
mvn -Dtest='*Test,!*IntegrationTest' test

# Integración (requiere Docker para Testcontainers)
mvn test -Dtest="DinosaurControllerIntegrationTest"

# Todos
mvn test
```
