tienes un roll de DBA y necesito que generes scripts SQL para PostgreSQL siguiendo buenas prácticas de backend senior y arquitectura CQRS.

REQUISITOS GENERALES:
- Usar PostgreSQL
- Scripts listos para ejecutar
- Incluir orden correcto de ejecución
- Pensado para un sistema productivo

---

1. EXTENSIONES
- Habilitar extensión para UUID (pgcrypto)

---

2. ENUM
Crear un ENUM llamado dinosaur_status con los siguientes valores:
- DISCOVERED
- ALIVE
- EXTINCT
- INACTIVE
- ARCHIVED

---

3. IDEMPOTENCIA
Crear una tabla idempotency_keys con:
- key (TEXT, primary key)
- response_body (JSONB)
- status_code (INT)
- created_at (TIMESTAMPTZ, default NOW)

---

4. WRITE MODEL (dinosaurs_write)
Debe incluir:
- id UUID PRIMARY KEY DEFAULT gen_random_uuid()
- name TEXT NOT NULL
- species TEXT NOT NULL
- discovery_date TIMESTAMPTZ NOT NULL
- extinction_date TIMESTAMPTZ
- status dinosaur_status NOT NULL
- created_at TIMESTAMPTZ DEFAULT NOW()
- updated_at TIMESTAMPTZ DEFAULT NOW()
- deleted_at TIMESTAMPTZ (para soft delete)

---

5. ÍNDICES WRITE
Crear índices para:
- búsqueda por name
- filtro por status
- orden/paginación por created_at DESC
- registros activos (deleted_at IS NULL)

IMPORTANTE:
- Crear un índice único parcial para evitar duplicados de nombre:
  UNIQUE(name) WHERE deleted_at IS NULL

---

6. READ MODEL (dinosaurs_read)
Debe incluir:
- id UUID PRIMARY KEY
- name TEXT
- species TEXT
- status TEXT
- is_extinct BOOLEAN (campo derivado)
- dinosaur_summary TEXT (name + ' - ' + species)
- created_at TIMESTAMPTZ
- deleted_at TIMESTAMPTZ

---

7. ÍNDICES READ
Optimizar para:
- filtros por status
- filtros por is_extinct
- paginación por created_at DESC
- búsqueda por name
- registros activos (deleted_at IS NULL)

---

8. SINCRONIZACIÓN (CQRS)
Crear una función llamada sync_dinosaurs_read que:
- copie datos desde dinosaurs_write a dinosaurs_read
- calcule:
  - is_extinct (status = 'EXTINCT')
  - dinosaur_summary (concatenación)
- propague deleted_at
- use ON CONFLICT (id) DO UPDATE

---

9. TRIGGER DE SYNC
Crear un trigger:
- AFTER INSERT OR UPDATE ON dinosaurs_write
- ejecute sync_dinosaurs_read

---

10. AUDITORÍA
Crear función update_updated_at_column que:
- actualice updated_at automáticamente

Crear trigger:
- BEFORE UPDATE ON dinosaurs_write

---

11. CONSIDERACIONES IMPORTANTES
- Usar soft delete (deleted_at), no eliminar registros físicamente
- Garantizar unicidad solo en registros activos
- Mantener separación CQRS (write vs read)
- Optimizar para consultas GET con paginación

---

OUTPUT ESPERADO:
- Scripts SQL completos
- Ordenados correctamente
- Listos para ejecutar sin modificaciones