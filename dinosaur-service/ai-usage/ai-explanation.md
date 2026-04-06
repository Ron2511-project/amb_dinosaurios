# AI-Assisted Development — dinosaur-service

## ¿Para qué se utilizó la IA?

- Generación del boilerplate de arquitectura hexagonal (estructura de paquetes, interfaces de puertos)
- Configuración de Resilience4j Circuit Breaker con Spring Boot 3.x
- Resolución de incompatibilidades de imagen Docker en Apple Silicon (arm64)
- Adaptación del modelo de dominio al schema CQRS preexistente

## Sugerencias descartadas

- **UUID como ID:** La IA sugirió usar UUID para el id del dinosaurio.
  Se descartó porque el schema real usa BIGSERIAL (Long autoincremental).
- **Flyway para migrations:** Se descartó porque el schema ya está gestionado
  por los scripts SQL del proyecto dinosaur-db. Usar Flyway generaría conflictos.
- **ddl-auto: validate/create:** Se descartó en favor de `none` para no
  interferir con el schema CQRS existente.
- **ENUM @Enumerated en JPA:** JPA no puede mapear directamente ENUMs PostgreSQL
  personalizados. Se resolvió usando String + columnDefinition.

## Validación del código generado

- Revisión manual de cada mapper (especialmente status ENDANGERED↔INACTIVE)
- Verificación de que JPA usa `@GeneratedValue(IDENTITY)` para respetar BIGSERIAL
- Confirmación de que `ddl-auto: none` no modifica el schema de producción
- Tests unitarios ejecutados sin Spring context para validar lógica de dominio pura
