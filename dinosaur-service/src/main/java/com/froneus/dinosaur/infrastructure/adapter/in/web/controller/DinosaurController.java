package com.froneus.dinosaur.infrastructure.adapter.in.web.controller;

import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.CreateDinosaurRequest;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.DinosaurResponse;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.ErrorResponse;
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
 * Adaptador REST para POST /v1/dinosaur.
 *
 * Separación de responsabilidades:
 *   create()          → valida request, maneja idempotencia, delega a createWithCircuitBreaker()
 *   createWithCB()    → ÚNICO método protegido por CB (solo llama a infraestructura)
 *   createFallback()  → responde 503 cuando el CB está abierto
 *
 * Las excepciones de dominio (400) se lanzan ANTES de entrar al CB,
 * por lo que NUNCA cuentan como fallo del circuito.
 */
@RestController
@RequestMapping("/v1/dinosaur")
public class DinosaurController {

    private static final Logger log     = LoggerFactory.getLogger(DinosaurController.class);
    private static final String CB_NAME = "dinosaurService";

    private final CreateDinosaurUseCase   createDinosaurUseCase;
    private final DinosaurIdempotencyPort idempotencyPort;
    private final DinosaurRepository      dinosaurRepository;
    private final DinosaurWebMapper       mapper;

    public DinosaurController(CreateDinosaurUseCase createDinosaurUseCase,
                              DinosaurIdempotencyPort idempotencyPort,
                              DinosaurRepository dinosaurRepository,
                              DinosaurWebMapper mapper) {
        this.createDinosaurUseCase = createDinosaurUseCase;
        this.idempotencyPort       = idempotencyPort;
        this.dinosaurRepository    = dinosaurRepository;
        this.mapper                = mapper;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateDinosaurRequest request) {

        // ── 1. Idempotencia — consulta Redis ANTES de cualquier validación ────
        if (isValidKey(idempotencyKey)) {
            Optional<Long> cached = idempotencyPort.getDinosaurId(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Idempotency hit for key={}", idempotencyKey);
                return dinosaurRepository.findById(cached.get())
                        .map(d -> ResponseEntity.status(HttpStatus.CREATED)
                                .body((Object) mapper.toResponse(d)))
                        .orElse(ResponseEntity.status(HttpStatus.CREATED).build());
            }
        }

        // ── 2. Validaciones de dominio ANTES del Circuit Breaker ──────────────
        //    Estas excepciones (400) nunca tocan el CB
        validateBusinessRules(request);

        // ── 3. Llamada a infraestructura protegida por Circuit Breaker ─────────
        return createWithCircuitBreaker(idempotencyKey, request);
    }

    /**
     * Método protegido por Circuit Breaker.
     * Solo llega aquí si las validaciones de dominio pasaron.
     * Únicamente falla por problemas de infraestructura (BD, Redis).
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "createFallback")
    public ResponseEntity<?> createWithCircuitBreaker(String idempotencyKey,
                                                      CreateDinosaurRequest request) {
        Dinosaur created = createDinosaurUseCase.execute(mapper.toCommand(request));
        log.info("Dinosaur created id={} name={}", created.getId(), created.getName());

        if (isValidKey(idempotencyKey)) {
            idempotencyPort.store(idempotencyKey, created.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    /**
     * Fallback — solo se ejecuta cuando el CB está ABIERTO por fallos de infra.
     * Nunca se ejecuta por errores de validación de negocio.
     */
    public ResponseEntity<?> createFallback(String idempotencyKey,
                                            CreateDinosaurRequest request,
                                            Throwable cause) {
        log.error("Circuit Breaker OPEN — fallback activated. Cause: {}", cause.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503,
                        "Servicio temporalmente no disponible. Intente nuevamente."));
    }

    /**
     * Validaciones de dominio ejecutadas FUERA del Circuit Breaker.
     * Un 400 de negocio no debe contar como fallo de infraestructura.
     */
    private void validateBusinessRules(CreateDinosaurRequest request) {
        // Validación de fechas (regla de dominio)
        if (request.discoveryDate() != null && request.extinctionDate() != null
                && !request.discoveryDate().isBefore(request.extinctionDate())) {
            throw new InvalidDinosaurDateException(
                    "La fecha de descubrimiento debe ser menor a la fecha de extinción");
        }

        // Validación de nombre único (consulta ligera a BD — si BD está caída,
        // esto también fallará y el CB lo contará, lo cual es correcto)
        if (dinosaurRepository.existsByName(request.name())) {
            throw new DuplicateDinosaurNameException("El nombre del dinosaurio ya existe");
        }
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank();
    }
}
