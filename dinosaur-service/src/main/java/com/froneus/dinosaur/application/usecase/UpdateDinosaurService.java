package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

public class UpdateDinosaurService implements UpdateDinosaurUseCase {

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public UpdateDinosaurService(DinosaurRepository repository,
                                 DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
    }

    @Override
    public Dinosaur execute(UpdateDinosaurCommand command) {
        Dinosaur existing = repository.findById(command.id()).orElseThrow();

        Dinosaur updated = Dinosaur.reconstitute(
                existing.getId(),
                command.name()           != null ? command.name()           : existing.getName(),
                command.species()        != null ? command.species()        : existing.getSpecies(),
                command.discoveryDate()  != null ? command.discoveryDate()  : existing.getDiscoveryDate(),
                command.extinctionDate() != null ? command.extinctionDate() : existing.getExtinctionDate(),
                command.status()         != null ? command.status()         : existing.getStatus()
        );

        repository.update(updated);

        // Emitir evento solo si el status cambió
        if (command.status() != null && command.status() != existing.getStatus()) {
            outbox.store(DinosaurEvent.of(updated.getId(), updated.getStatus(),
                    EventType.STATUS_CHANGED));
        }

        return updated;
    }
}
