package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

/**
 * Implementación del caso de uso de creación.
 * Escribe en dinosaurs_write; el trigger CQRS propaga a dinosaurs_read.
 * No lleva @Service — se instancia como bean puro en BeanConfig.
 */
public class CreateDinosaurService implements CreateDinosaurUseCase {

    private final DinosaurRepository repository;

    public CreateDinosaurService(DinosaurRepository repository) {
        this.repository = repository;
    }

    @Override
    public Dinosaur execute(CreateDinosaurCommand command) {
        // Regla de negocio: nombre único (índice parcial uq_dw_name_active)
        if (repository.existsByName(command.name())) {
            throw new DuplicateDinosaurNameException("El nombre del dinosaurio ya existe");
        }

        // Validación de fechas ocurre dentro de Dinosaur.create()
        // ID es null — lo genera PostgreSQL con BIGSERIAL
        Dinosaur dinosaur = Dinosaur.create(
                command.name(),
                command.species(),
                command.discoveryDate(),
                command.extinctionDate()
        );

        return repository.save(dinosaur);
    }
}
