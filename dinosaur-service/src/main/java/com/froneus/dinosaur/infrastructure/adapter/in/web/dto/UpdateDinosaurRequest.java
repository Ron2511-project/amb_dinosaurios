package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.froneus.dinosaur.domain.model.DinosaurStatus;

import java.time.LocalDateTime;

/**
 * Request para PUT /v1/dinosaur/{id}.
 * Todos los campos son opcionales — solo se actualizan los que vienen.
 */
public record UpdateDinosaurRequest(
        String name,
        String species,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime discoveryDate,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime extinctionDate,

        DinosaurStatus status
) {}
