# 🦕 AMB_DINOSAURIOS — Challenge Froneus Backend Java Developer

Proyecto completo de gestión de dinosaurios.
**Java 17 · Spring Boot 3.2 · Arquitectura Hexagonal · PostgreSQL CQRS · Redis · Circuit Breaker · Docker**

---

## 📁 Estructura del proyecto

```
AMB_DINOSAURIOS/
│
├── docker-compose.yml          ← ⭐ UN solo comando levanta todo el stack
├── .env                        ← Variables de entorno (credenciales)
├── .gitignore
├── LICENSE
├── README.md                   ← Este archivo
│
├── dinosaur-db/                ← Infraestructura de base de datos
│   ├── sql/
│   │   ├── 01_postgres_setup.sql    → ENUM dinosaur_status + pgcrypto
│   │   ├── 02_write_model.sql       → Tabla dinosaurs_write (Command Side)
│   │   ├── 03_read_model.sql        → Tabla dinosaurs_read  (Query Side)
│   │   ├── 04_sync_and_triggers.sql → Trigger CQRS + auditoría
│   │   └── 05_seed_data.sql         → 20 dinosaurios de ejemplo
│   ├── redis/
│   │   └── redis.conf               → Config Redis (AOF + LRU)
│   ├── docs/
│   │   └── redis_idempotency_reference.md
│   └── ai-usage/
│       ├── prompts.md
│       └── ai-explanation.md
│
└── dinosaur-service/           ← Microservicio Java Spring Boot
    ├── src/
    │   ├── main/
    │   │   ├── java/com/froneus/dinosaur/
    │   │   │   ├── domain/          → Entidades, puertos, excepciones
    │   │   │   ├── application/     → Casos de uso
    │   │   │   └── infrastructure/  → Adaptadores REST, JPA, Redis
    │   │   └── resources/
    │   │       └── application.yml
    │   └── test/
    │       ├── java/                → Tests unitarios e integración
    │       └── resources/
    │           └── db/test-init.sql → Schema para Testcontainers
    ├── ai-usage/
    ├── Dockerfile
    └── pom.xml
```

---

## 🚀 Levantar el proyecto

### Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| Docker      | 24+           |
| Docker Compose | 2.20+      |

### Un solo comando

```bash
docker compose up --build
```

Docker va a:
1. Inicializar PostgreSQL con el schema CQRS completo (scripts 01→04)
2. Levantar Redis con persistencia AOF
3. Compilar y arrancar el microservicio Java
4. Esperar a que Postgres y Redis estén `healthy` antes de arrancar la app

**Verificar que todo esté corriendo:**

```bash
docker compose ps
```

```
NAME                STATUS          PORTS
froneus_postgres    running (healthy)   0.0.0.0:5432->5432/tcp
froneus_redis       running (healthy)   0.0.0.0:6379->6379/tcp
dinosaur_service    running (healthy)   0.0.0.0:8080->8080/tcp
```

---

## 🔌 API REST

**Base URL:** `http://localhost:8080`

### POST /v1/dinosaur

Crea un nuevo dinosaurio.

**Headers:**

| Header | Valor | Requerido |
|--------|-------|-----------|
| `Content-Type` | `application/json` | ✅ |
| `Accept` | `application/json` | ✅ |
| `Idempotency-Key` | `idem-{uuid}` | ❌ opcional |

**Request body:**

```json
{
  "name":           "Tyrannosaurus Rex",
  "species":        "Theropod",
  "discoveryDate":  "1902-01-01T23:59:59",
  "extinctionDate": "2023-12-31T23:59:59"
}
```

**Response 201 Created:**

```json
{
  "id":             1,
  "name":           "Tyrannosaurus Rex",
  "species":        "Theropod",
  "discoveryDate":  "1902-01-01T23:59:59",
  "extinctionDate": "2023-12-31T23:59:59",
  "status":         "ALIVE"
}
```

**Errores:**

| Caso | Status | Mensaje |
|------|--------|---------|
| Nombre duplicado | 400 | `El nombre del dinosaurio ya existe` |
| Fechas inválidas | 400 | `La fecha de descubrimiento debe ser menor a la fecha de extinción` |
| JSON mal formado | 400 | `El cuerpo de la petición es inválido` |
| Error interno | 500 | `Error interno al crear el dinosaurio` |

**Ejemplo con curl:**

```bash
curl -X POST http://localhost:8080/v1/dinosaur \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-abc123" \
  -d '{
    "name": "Tyrannosaurus Rex",
    "species": "Theropod",
    "discoveryDate": "1902-01-01T23:59:59",
    "extinctionDate": "2023-12-31T23:59:59"
  }'
```

---

## 🏗️ Arquitectura

### Hexagonal (Ports & Adapters)

```
HTTP Request
     │
     ▼
[DinosaurController]            ← Adaptador de ENTRADA (REST)
     │
     ├─ Busca en Redis por Idempotency-Key
     │       ├── Existe → replay 201 (sin tocar Postgres)
     │       └── No existe → continúa
     │
     ▼
[CreateDinosaurUseCase]         ← Puerto de ENTRADA (interfaz)
     │
     ▼
[CreateDinosaurService]         ← Caso de USO (aplicación)
     │  Valida nombre único
     │  Crea entidad Dinosaur (valida fechas)
     │
     ▼
[DinosaurPostgresAdapter]       ← Adaptador de SALIDA (JPA)
     │  INSERT en dinosaurs_write
     │
     ▼  (trigger PostgreSQL automático)
[dinosaurs_read]                ← Read Model CQRS sincronizado
     │
     ▼
[DinosaurRedisAdapter]          ← Adaptador de SALIDA (Redis)
     │  Guarda idempotencyKey → dinosaurId (TTL 24h)
```

### CQRS en PostgreSQL

```
POST /v1/dinosaur → dinosaurs_write  (Command Side — fuente de verdad)
                          │
                    TRIGGER automático
                          │
                          ▼
GET  /v1/dinosaur ← dinosaurs_read   (Query Side — lectura optimizada)
```

---

## 🗺️ Mapeo de estados

| Java (dominio) | PostgreSQL ENUM `dinosaur_status` |
|----------------|-----------------------------------|
| `ALIVE`        | `ALIVE`                           |
| `ENDANGERED`   | `INACTIVE`                        |
| `EXTINCT`      | `EXTINCT`                         |

---

## 🔴 Idempotencia Redis

```
Header:    Idempotency-Key: idem-{uuid}
Key Redis: idempotency:idem-{uuid}
Valor:     dinosaurId (Long)
TTL:       24 horas
```

Si el cliente reenvía la misma `Idempotency-Key`, el servidor devuelve la respuesta original sin crear un duplicado en Postgres.

---

## 🖥️ pgAdmin 4 (opcional)

```bash
docker compose --profile tools up
```

Abrir `http://localhost:5050`

| Campo    | Valor              |
|----------|--------------------|
| Email    | `admin@froneus.com`|
| Password | `admin`            |

Registrar servidor → Connection:

| Campo    | Valor              |
|----------|--------------------|
| Host     | `postgres`         |
| Port     | `5432`             |
| Database | `froneus_db`       |
| Username | `froneus`          |
| Password | `froneus_pass`     |

> ⚠️ El host es `postgres` (nombre del servicio Docker), **no** `localhost`.

---

## 🛠️ Comandos útiles

```bash
# Ver logs en tiempo real
docker compose logs -f

# Ver solo logs del microservicio
docker compose logs -f dinosaur-service

# Consultar dinosaurios creados
docker exec -it froneus_postgres psql -U froneus -d froneus_db \
  -c "SELECT id, name, status FROM dinosaurs_write ORDER BY id DESC LIMIT 10;"

# Ver read model (CQRS)
docker exec -it froneus_postgres psql -U froneus -d froneus_db \
  -c "SELECT id, name, status, is_extinct, dinosaur_summary FROM dinosaurs_read;"

# Ver idempotency keys en Redis
docker exec -it froneus_redis redis-cli SCAN 0 MATCH "idempotency:*" COUNT 100

# Health check
curl http://localhost:8080/actuator/health

# Detener (conserva datos)
docker compose down

# ⚠️ Reset completo (elimina todos los datos)
docker compose down -v
```

---

## 🧪 Tests

```bash
cd dinosaur-service

# Tests unitarios (sin Docker)
mvn test -Dtest="DinosaurTest,CreateDinosaurServiceTest"

# Tests de integración (requiere Docker para Testcontainers)
mvn test -Dtest="DinosaurControllerIntegrationTest"

# Todos los tests
mvn test
```
