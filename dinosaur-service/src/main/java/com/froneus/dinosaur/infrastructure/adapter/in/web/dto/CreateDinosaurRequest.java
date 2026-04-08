package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateDinosaurRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Species is required")
        String species,

        @NotNull(message = "Discovery date is required")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime discoveryDate,

        @NotNull(message = "Extinction date is required")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime extinctionDate
) {}
