package com.froneus.dinosaur.infrastructure.exception;

import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de excepciones.
 * Todos los errores devuelven el mismo formato: { "status": N, "message": "..." }
 *
 * Jerarquía de responses:
 *   400 → errores de negocio / validación
 *   500 → errores internos inesperados
 *   503 → Circuit Breaker abierto (infraestructura no disponible)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 — Errores de negocio ───────────────────────────────────────────────

    @ExceptionHandler(DuplicateDinosaurNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateDinosaurNameException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidDinosaurDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidDinosaurDateException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido");
    }

    // ── 503 — Circuit Breaker abierto ─────────────────────────────────────────

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit Breaker OPEN — request rejected: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio temporalmente no disponible. Intente nuevamente.");
    }

    // ── 500 — Error genérico ───────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al crear el dinosaurio");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message));
    }
}
