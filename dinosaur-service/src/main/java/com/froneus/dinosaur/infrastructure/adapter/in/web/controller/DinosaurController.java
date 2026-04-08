package com.froneus.dinosaur.infrastructure.adapter.in.web.controller;

import com.froneus.dinosaur.domain.exception.DinosaurExtinctException;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.in.DeleteDinosaurUseCase;
import com.froneus.dinosaur.domain.port.in.GetDinosaurUseCase;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.*;
import com.froneus.dinosaur.infrastructure.adapter.in.web.mapper.DinosaurWebMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller REST — todos los endpoints de dinosaurios.
 *
 * Circuit Breaker removido del controller para evitar problemas
 * de proxy de Spring AOP con llamadas internas al mismo bean.
 * Los use cases manejan la resiliencia a nivel de dominio.
 *
 * El flujo de eventos es:
 *   Controller → UseCase → outbox.store() → Redis
 *   OutboxPublisher (cada 5s) → RabbitMQ
 *   DinosaurEventConsumer ← RabbitMQ
 */
@RestController
@RequestMapping("/v1/dinosaur")
public class DinosaurController {

    private static final Logger log = LoggerFactory.getLogger(DinosaurController.class);

    private final CreateDinosaurUseCase   createUseCase;
    private final GetDinosaurUseCase      getUseCase;
    private final UpdateDinosaurUseCase   updateUseCase;
    private final DeleteDinosaurUseCase   deleteUseCase;
    private final DinosaurIdempotencyPort idempotencyPort;
    private final DinosaurRepository      repository;
    private final DinosaurWebMapper       mapper;

    public DinosaurController(CreateDinosaurUseCase createUseCase,
                              GetDinosaurUseCase getUseCase,
                              UpdateDinosaurUseCase updateUseCase,
                              DeleteDinosaurUseCase deleteUseCase,
                              DinosaurIdempotencyPort idempotencyPort,
                              DinosaurRepository repository,
                              DinosaurWebMapper mapper) {
        this.createUseCase   = createUseCase;
        this.getUseCase      = getUseCase;
        this.updateUseCase   = updateUseCase;
        this.deleteUseCase   = deleteUseCase;
        this.idempotencyPort = idempotencyPort;
        this.repository      = repository;
        this.mapper          = mapper;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /v1/dinosaur
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateDinosaurRequest request) {

        // Idempotencia — replay si la key ya existe
        if (isValidKey(idempotencyKey)) {
            Optional<Long> cached = idempotencyPort.getDinosaurId(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Idempotency replay — key={}", idempotencyKey);
                return repository.findById(cached.get())
                        .map(d -> ResponseEntity.status(HttpStatus.CREATED)
                                .body((Object) mapper.toResponse(d)))
                        .orElse(ResponseEntity.status(HttpStatus.CREATED).build());
            }
        }

        // Validaciones de negocio
        if (request.discoveryDate() != null && request.extinctionDate() != null
                && !request.discoveryDate().isBefore(request.extinctionDate()))
            throw new InvalidDinosaurDateException(
                    "Discovery date must be earlier than extinction date");

        if (repository.existsByName(request.name()))
            throw new DuplicateDinosaurNameException("Dinosaur name already exists");

        // Crear → use case emite evento al outbox
        Dinosaur created = createUseCase.execute(mapper.toCommand(request));
        log.info("POST — dinosaur created id={} name={}", created.getId(), created.getName());

        // Guardar idempotency key
        if (isValidKey(idempotencyKey)) {
            idempotencyPort.store(idempotencyKey, created.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /v1/dinosaur?page=1&pageSize=10
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        PagedResult<DinosaurReadModel> result = getUseCase.getAll(page, pageSize);
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /v1/dinosaur/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        DinosaurReadModel model = getUseCase.getById(id);
        return ResponseEntity.ok(mapper.toReadResponse(model));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PUT /v1/dinosaur/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody UpdateDinosaurRequest request) {
        // Validaciones de negocio
        Dinosaur existing = repository.findById(id)
                .orElseThrow(() -> new DinosaurNotFoundException("Dinosaur not found"));

        if (existing.getStatus() == DinosaurStatus.EXTINCT)
            throw new DinosaurExtinctException("Cannot modify an EXTINCT dinosaur");

        if (request.name() != null && !request.name().equals(existing.getName())
                && repository.existsByNameAndNotId(request.name(), id))
            throw new DuplicateDinosaurNameException("Dinosaur name already exists");

        if (request.discoveryDate() != null && request.extinctionDate() != null
                && !request.discoveryDate().isBefore(request.extinctionDate()))
            throw new InvalidDinosaurDateException(
                    "Discovery date must be earlier than extinction date");

        // Actualizar → use case emite evento si cambia el status
        Dinosaur updated = updateUseCase.execute(mapper.toCommand(id, request));
        log.info("PUT — dinosaur updated id={}", id);

        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE /v1/dinosaur/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repository.findById(id)
                .orElseThrow(() -> new DinosaurNotFoundException("Dinosaur not found"));

        // Eliminar → use case emite evento DELETED
        deleteUseCase.execute(id);
        log.info("DELETE — dinosaur soft deleted id={}", id);

        return ResponseEntity.ok(
                new DeletedResponse(id, "Dinosaur with id " + id + " has been successfully deleted"));
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank();
    }
}
