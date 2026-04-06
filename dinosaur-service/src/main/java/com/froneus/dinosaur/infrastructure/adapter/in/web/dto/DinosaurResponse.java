package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.froneus.dinosaur.domain.model.DinosaurStatus;

import java.time.LocalDateTime;

/**
 * Response del endpoint POST /v1/dinosaur.
 * El id es Long (BIGSERIAL de PostgreSQL).
 */
public record DinosaurResponse(
        Long id,
        String name,
        String species,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime discoveryDate,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime extinctionDate,

        DinosaurStatus status
) {}
