package com.froneus.dinosaur.domain.port.in;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import java.time.LocalDateTime;

/**
 * Puerto de entrada — actualización de dinosaurio.
 * Escribe en dinosaurs_write; el trigger CQRS propaga a dinosaurs_read.
 */
public interface UpdateDinosaurUseCase {

    record UpdateDinosaurCommand(
            Long          id,
            String        name,
            String        species,
            LocalDateTime discoveryDate,
            LocalDateTime extinctionDate,
            DinosaurStatus status
    ) {}

    Dinosaur execute(UpdateDinosaurCommand command);
}
