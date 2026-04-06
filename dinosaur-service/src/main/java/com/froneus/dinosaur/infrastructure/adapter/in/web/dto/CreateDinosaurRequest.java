package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateDinosaurRequest(

        @NotBlank(message = "El nombre es requerido")
        String name,

        @NotBlank(message = "La especie es requerida")
        String species,

        @NotNull(message = "La fecha de descubrimiento es requerida")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime discoveryDate,

        @NotNull(message = "La fecha de extinción es requerida")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime extinctionDate
) {}
