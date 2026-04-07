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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Patrón aplicado en TODOS los endpoints:
 *   1. Validaciones de negocio FUERA del CB → lanzan 400/404, nunca 503
 *   2. Solo la llamada a infraestructura va dentro del CB
 *   3. El fallback solo se activa por fallos reales de infra (BD/Redis caídos)
 */
@RestController
@RequestMapping("/v1/dinosaur")
public class DinosaurController {

    private static final Logger log     = LoggerFactory.getLogger(DinosaurController.class);
    private static final String CB_NAME = "dinosaurService";

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

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /v1/dinosaur
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateDinosaurRequest request) {

        // Idempotencia — antes de cualquier validación
        if (isValidKey(idempotencyKey)) {
            Optional<Long> cached = idempotencyPort.getDinosaurId(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Idempotency hit key={}", idempotencyKey);
                return repository.findById(cached.get())
                        .map(d -> ResponseEntity.status(HttpStatus.CREATED)
                                .body((Object) mapper.toResponse(d)))
                        .orElse(ResponseEntity.status(HttpStatus.CREATED).build());
            }
        }

        // Validaciones de negocio FUERA del CB → 400, nunca 503
        if (request.discoveryDate() != null && request.extinctionDate() != null
                && !request.discoveryDate().isBefore(request.extinctionDate()))
            throw new InvalidDinosaurDateException(
                    "Discovery date must be earlier than extinction date");
        if (repository.existsByName(request.name()))
            throw new DuplicateDinosaurNameException("Dinosaur name already exists");

        return createInfra(idempotencyKey, request);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "createFallback")
    public ResponseEntity<?> createInfra(String idempotencyKey, CreateDinosaurRequest request) {
        Dinosaur created = createUseCase.execute(mapper.toCommand(request));
        log.info("Created id={}", created.getId());
        if (isValidKey(idempotencyKey)) idempotencyPort.store(idempotencyKey, created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    public ResponseEntity<?> createFallback(String key, CreateDinosaurRequest req, Throwable t) {
        log.error("CB OPEN on POST: {}", t.getMessage());
        return cbUnavailable();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /v1/dinosaur?page=1&pageSize=10
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return getAllInfra(page, pageSize);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getAllFallback")
    public ResponseEntity<?> getAllInfra(int page, int pageSize) {
        PagedResult<DinosaurReadModel> result = getUseCase.getAll(page, pageSize);
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }

    public ResponseEntity<?> getAllFallback(int page, int pageSize, Throwable t) {
        log.error("CB OPEN on GET all: {}", t.getMessage());
        return cbUnavailable();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /v1/dinosaur/{id}
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        // Verificar existencia FUERA del CB → 404, nunca 503
        DinosaurReadModel model = getUseCase.getById(id);   // lanza DinosaurNotFoundException
        return getByIdInfra(model);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getByIdFallback")
    public ResponseEntity<?> getByIdInfra(DinosaurReadModel model) {
        return ResponseEntity.ok(mapper.toReadResponse(model));
    }

    public ResponseEntity<?> getByIdFallback(DinosaurReadModel model, Throwable t) {
        log.error("CB OPEN on GET /{id}: {}", t.getMessage());
        return cbUnavailable();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUT /v1/dinosaur/{id}
    // ═══════════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody UpdateDinosaurRequest request) {
        // Validaciones de negocio FUERA del CB → 400/404, nunca 503
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

        return updateInfra(id, request);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "updateFallback")
    public ResponseEntity<?> updateInfra(Long id, UpdateDinosaurRequest request) {
        Dinosaur updated = updateUseCase.execute(mapper.toCommand(id, request));
        log.info("Updated id={}", id);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    public ResponseEntity<?> updateFallback(Long id, UpdateDinosaurRequest req, Throwable t) {
        log.error("CB OPEN on PUT /{id}: {}", t.getMessage());
        return cbUnavailable();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE /v1/dinosaur/{id}
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // Verificar existencia FUERA del CB → 404, nunca 503
        repository.findById(id)
                .orElseThrow(() -> new DinosaurNotFoundException("Dinosaur not found"));

        return deleteInfra(id);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "deleteFallback")
    public ResponseEntity<?> deleteInfra(Long id) {
        deleteUseCase.execute(id);
        log.info("Soft deleted id={}", id);
        return ResponseEntity.ok(
                new DeletedResponse(id, "Dinosaur with id " + id + " has been successfully deleted"));
    }

    public ResponseEntity<?> deleteFallback(Long id, Throwable t) {
        log.error("CB OPEN on DELETE /{id}: {}", t.getMessage());
        return cbUnavailable();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private ResponseEntity<?> cbUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "Service temporarily unavailable. Please try again."));
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank();
    }
}
