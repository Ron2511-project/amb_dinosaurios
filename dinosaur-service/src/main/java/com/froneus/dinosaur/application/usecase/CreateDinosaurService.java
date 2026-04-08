package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDinosaurService implements CreateDinosaurUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateDinosaurService.class);

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public CreateDinosaurService(DinosaurRepository repository,
                                 DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
        log.info(">>> CreateDinosaurService initialized with outbox={}", outbox.getClass().getSimpleName());
    }

    @Override
    public Dinosaur execute(CreateDinosaurCommand command) {
        log.info(">>> CreateDinosaurService.execute() called for name={}", command.name());

        if (repository.existsByName(command.name()))
            throw new DuplicateDinosaurNameException("Dinosaur name already exists");

        Dinosaur dinosaur = Dinosaur.create(
                command.name(), command.species(),
                command.discoveryDate(), command.extinctionDate());

        Dinosaur saved = repository.save(dinosaur);
        log.info(">>> Dinosaur saved with id={}, now storing event in outbox", saved.getId());

        DinosaurEvent event = DinosaurEvent.of(saved.getId(), saved.getStatus(), EventType.CREATED);
        outbox.store(event);
        log.info(">>> outbox.store() called successfully for dinosaurId={}", saved.getId());

        return saved;
    }
}
