package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

/**
 * Response de error uniforme para toda la API.
 * Mismo formato en todos los casos: 400, 500, 503.
 *
 * {
 *   "status": 503,
 *   "message": "Servicio temporalmente no disponible. Intente nuevamente."
 * }
 */
public record ErrorResponse(int status, String message) {}
