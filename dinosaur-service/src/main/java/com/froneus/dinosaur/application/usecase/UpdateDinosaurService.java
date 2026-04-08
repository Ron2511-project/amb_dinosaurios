package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caso de uso de actualización.
 *
 * Solo emite evento al Outbox cuando el STATUS cambia.
 * Si solo cambian nombre, especie o fechas → no se notifica.
 * Esto sigue la regla del challenge punto III.
 */
public class UpdateDinosaurService implements UpdateDinosaurUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateDinosaurService.class);

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public UpdateDinosaurService(DinosaurRepository repository,
                                 DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
    }

    @Override
    public Dinosaur execute(UpdateDinosaurCommand command) {
        Dinosaur existing = repository.findById(command.id())
                .orElseThrow(() -> new com.froneus.dinosaur.domain.exception.DinosaurNotFoundException("Dinosaur not found"));

        Dinosaur updated = Dinosaur.reconstitute(
                existing.getId(),
                command.name()           != null ? command.name()           : existing.getName(),
                command.species()        != null ? command.species()        : existing.getSpecies(),
                command.discoveryDate()  != null ? command.discoveryDate()  : existing.getDiscoveryDate(),
                command.extinctionDate() != null ? command.extinctionDate() : existing.getExtinctionDate(),
                command.status()         != null ? command.status()         : existing.getStatus()
        );

        repository.update(updated);

        // Solo notificar si el status cambió (regla del challenge punto III)
        if (command.status() != null && command.status() != existing.getStatus()) {
            outbox.store(DinosaurEvent.of(updated.getId(), updated.getStatus(),
                    EventType.STATUS_CHANGED));
            log.info("Status changed — id={} {} → {} — event stored",
                    updated.getId(), existing.getStatus(), updated.getStatus());
        } else {
            log.info("PUT id={} — no status change, no event emitted", updated.getId());
        }

        return updated;
    }
}
