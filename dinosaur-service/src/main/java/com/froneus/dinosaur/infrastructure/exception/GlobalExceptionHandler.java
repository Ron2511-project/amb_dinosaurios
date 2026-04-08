package com.froneus.dinosaur.infrastructure.exception;

import com.froneus.dinosaur.domain.exception.*;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateDinosaurNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateDinosaurNameException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidDinosaurDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidDinosaurDateException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DinosaurExtinctException.class)
    public ResponseEntity<ErrorResponse> handleExtinct(DinosaurExtinctException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid");
    }

    // ── 404 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(DinosaurNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DinosaurNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 503 — Circuit Breaker ─────────────────────────────────────────────────

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit Breaker OPEN: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Service temporarily unavailable. Please try again.");
    }

    // ── 500 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
