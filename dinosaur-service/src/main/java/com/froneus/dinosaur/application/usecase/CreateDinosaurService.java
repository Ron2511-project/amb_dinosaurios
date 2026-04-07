package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

public class CreateDinosaurService implements CreateDinosaurUseCase {

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public CreateDinosaurService(DinosaurRepository repository,
                                 DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
    }

    @Override
    public Dinosaur execute(CreateDinosaurCommand command) {
        if (repository.existsByName(command.name()))
            throw new DuplicateDinosaurNameException("Dinosaur name already exists");

        Dinosaur dinosaur = Dinosaur.create(
                command.name(), command.species(),
                command.discoveryDate(), command.extinctionDate());

        Dinosaur saved = repository.save(dinosaur);

        // Emitir evento al Outbox
        outbox.store(DinosaurEvent.of(saved.getId(), saved.getStatus(), EventType.CREATED));

        return saved;
    }
}
