package com.froneus.dinosaur.domain.port.in;

import com.froneus.dinosaur.domain.model.Dinosaur;

import java.time.LocalDateTime;

/**
 * Puerto de entrada — caso de uso de creación de dinosaurio.
 */
public interface CreateDinosaurUseCase {

    record CreateDinosaurCommand(
            String name,
            String species,
            LocalDateTime discoveryDate,
            LocalDateTime extinctionDate
    ) {}

    Dinosaur execute(CreateDinosaurCommand command);
}
