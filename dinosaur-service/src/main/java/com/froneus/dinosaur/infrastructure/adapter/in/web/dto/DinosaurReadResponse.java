package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.froneus.dinosaur.domain.model.DinosaurStatus;

import java.time.LocalDateTime;

/**
 * Response para GET — incluye campos del Read Model:
 * isExtinct, dinosaurSummary, createdAt.
 */
public record DinosaurReadResponse(
        Long id,
        String name,
        String species,
        DinosaurStatus status,
        boolean isExtinct,
        String dinosaurSummary,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
