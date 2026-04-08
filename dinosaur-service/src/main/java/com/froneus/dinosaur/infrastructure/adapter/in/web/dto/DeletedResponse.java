package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

/**
 * Response del DELETE exitoso.
 * { "id": 1, "message": "Dinosaur with id 1 has been successfully deleted" }
 */
public record DeletedResponse(Long id, String message) {}
