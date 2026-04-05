# 🦕 Challenge Froneus — Infraestructura de Base de Datos

Stack de base de datos para el microservicio de gestión de dinosaurios.
Incluye **PostgreSQL 16** con arquitectura **CQRS** y **Redis 7** para idempotencia.

---

## 📋 Requisitos previos

| Herramienta | Versión mínima | Verificar con |
|-------------|---------------|---------------|
| Docker      | 24+           | `docker --version` |
| Docker Compose | 2.20+      | `docker compose version` |

> **Windows:** usar Docker Desktop. **Mac:** Docker Desktop o OrbStack. **Linux:** Docker Engine + plugin Compose.

---

## 🚀 Levantar el entorno (3 pasos)

### 1. Clonar / descomprimir el proyecto

```bash
unzip dinosaur-db.zip
cd dinosaur-db
```

### 2. (Opcional) Revisar variables de entorno

El archivo `.env` ya tiene valores por defecto listos para usar.
Si necesitás cambiar puertos o credenciales, editalo antes de continuar:

```
POSTGRES_USER=froneus
POSTGRES_PASSWORD=froneus_pass
POSTGRES_DB=froneus_db
POSTGRES_PORT=5432
REDIS_PORT=6379
```

### 3. Levantar los contenedores

```bash
docker compose up -d
```

Docker va a:
1. Descargar las imágenes `postgres:16-alpine` y `redis:7-alpine`
2. Inicializar la base de datos ejecutando los scripts SQL en orden
3. Configurar Redis con persistencia AOF

**Verificar que todo esté saludable:**

```bash
docker compose ps
```

Deberías ver `healthy` en ambos servicios:

```
NAME                STATUS
froneus_postgres    running (healthy)
froneus_redis       running (healthy)
```

---

## 🗂️ Estructura del proyecto

```
dinosaur-db/
├── docker-compose.yml          # Orquestación de servicios
├── .env                        # Variables de entorno (credenciales)
├── sql/
│   ├── 01_postgres_setup.sql   # Extensión pgcrypto + ENUM dinosaur_status
│   ├── 02_write_model.sql      # Tabla dinosaurs_write + índices (Command Side)
│   ├── 03_read_model.sql       # Tabla dinosaurs_read  + índices (Query Side)
│   └── 04_sync_and_triggers.sql# Triggers CQRS + auditoría updated_at
└── redis/
    └── redis.conf              # Configuración Redis productiva (AOF + LRU)
```

---

## 🔌 Cadenas de conexión

### PostgreSQL

```
Host:     localhost
Puerto:   5432
Base:     froneus_db
Usuario:  froneus
Password: froneus_pass

JDBC URL: jdbc:postgresql://localhost:5432/froneus_db
```

### Redis

```
Host:   localhost
Puerto: 6379
```

### Spring Boot (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/froneus_db
    username: froneus
    password: froneus_pass
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
```

> Si la app Spring Boot también corre en Docker, reemplazar `localhost` por el nombre del servicio: `froneus_postgres` y `froneus_redis`.

---

## 🛠️ Comandos útiles

```bash
# Ver logs en tiempo real
docker compose logs -f

# Ver logs solo de PostgreSQL
docker compose logs -f postgres

# Conectarse a PostgreSQL desde terminal
docker exec -it froneus_postgres psql -U froneus -d froneus_db

# Conectarse a Redis desde terminal
docker exec -it froneus_redis redis-cli

# Detener los contenedores (conserva los datos)
docker compose down

# ⚠️  Detener Y BORRAR todos los datos (reset completo)
docker compose down -v
```

---

## 🖥️ PgAdmin 4 (interfaz web — opcional)

### 1. Levantar PgAdmin

```bash
docker compose --profile tools up -d
```

### 2. Abrir en el navegador

```
http://localhost:5050
```

### 3. Login

| Campo    | Valor               |
|----------|---------------------|
| Email    | `admin@froneus.com` |
| Password | `admin`             |

### 4. Registrar el servidor PostgreSQL

Click derecho en **Servers → Register → Server** y completar:

**Tab General:**

| Campo | Valor                          |
|-------|--------------------------------|
| Name  | `Froneus` (o cualquier nombre) |

**Tab Connection:**

| Campo    | Valor              |
|----------|--------------------|
| Host     | `froneus_postgres` |
| Port     | `5432`             |
| Database | `froneus_db`       |
| Username | `froneus`          |
| Password | `froneus_pass`     |

> ⚠️ El host es `froneus_postgres` (nombre del contenedor Docker), **no** `localhost`.
> PgAdmin corre dentro de Docker y se comunica por la red interna del compose.

### 5. Ver las tablas

```
Servers → Froneus → Databases → froneus_db → Schemas → public → Tables
```

Vas a encontrar: `dinosaurs_write` y `dinosaurs_read`

---

## 🔴 Redis Insight (cliente externo)

Si usás **Redis Insight** para explorar las idempotency keys:

**Connection URL:**
```
redis://localhost:6379
```

O por campos separados:

| Campo    | Valor       |
|----------|-------------|
| Host     | `localhost` |
| Port     | `6379`      |
| Password | *(vacío)*   |

---

## 🏗️ Arquitectura CQRS implementada

```
POST/PUT/DELETE  →  dinosaurs_write  (Command Side)
                         │
                    TRIGGER (AFTER INSERT/UPDATE)
                         │
                         ▼
GET              ←  dinosaurs_read   (Query Side)
                    [campos derivados pre-calculados]

POST /dinosaur   →  Redis idempotency:{uuid}  TTL 24hs
```

### Scripts SQL y su responsabilidad

| Script | Responsabilidad |
|--------|----------------|
| `01` | Habilita `pgcrypto` para UUIDs. Define el ENUM `dinosaur_status` |
| `02` | Tabla `dinosaurs_write`: fuente de verdad. Soft delete. Unique parcial en `name` |
| `03` | Tabla `dinosaurs_read`: proyección desnormalizada con `is_extinct` y `dinosaur_summary` |
| `04` | Trigger `sync_dinosaurs_read` (CQRS sync) + trigger `update_updated_at_column` (auditoría) |

---

## ❓ Solución de problemas

**Los scripts SQL no se ejecutaron:**
Los scripts de `docker-entrypoint-initdb.d/` solo corren cuando el volumen está vacío (primera vez). Si el contenedor ya existía, hacer reset:
```bash
docker compose down -v
docker compose up -d
```

**Puerto 5432 ocupado:**
Cambiar en `.env`: `POSTGRES_PORT=5433` y reiniciar.

**Puerto 6379 ocupado:**
Cambiar en `.env`: `REDIS_PORT=6380` y reiniciar.

**Docker no conecta (Mac):**
Abrir Docker Desktop, esperar a que el ícono de la ballena quede estático y volver a correr `docker compose up -d`.

---

## ▶️ Ejecutar scripts SQL por separado

Útil cuando los scripts no corrieron automáticamente al iniciar Docker
(por ejemplo, si el volumen ya existía de una corrida anterior).

### Ejecutar un script específico

```bash
# Reemplazar NOMBRE_SCRIPT por el archivo que querés ejecutar
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/NOMBRE_SCRIPT.sql
```

### Ejecutar cada script individualmente

```bash
# 01 — Extensiones + ENUM
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/01_postgres_setup.sql

# 02 — Tabla dinosaurs_write + índices
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/02_write_model.sql

# 03 — Tabla dinosaurs_read + índices
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/03_read_model.sql

# 04 — Triggers CQRS + auditoría
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/04_sync_and_triggers.sql

# 05 — Seed: 20 dinosaurios de ejemplo
docker exec -i froneus_postgres psql -U froneus -d froneus_db < sql/05_seed_data.sql
```

### Verificar que los datos se insertaron

```bash
docker exec -it froneus_postgres psql -U froneus -d froneus_db -c "SELECT name, status FROM dinosaurs_write;"
```

> ⚠️ Corré los comandos siempre desde la carpeta `dinosaur-db/` para que la ruta `sql/` resuelva correctamente.
> Si estás en otra carpeta, reemplazá `sql/` por la ruta absoluta, por ejemplo:
> `/Users/ron/Documents/java/dinosaur-project/dinosaur-db/sql/05_seed_data.sql`
