package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.exception.DinosaurExtinctException;
import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

import java.time.LocalDateTime;

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

// 1. No modificar EXTINCT
if (existing.getStatus() == DinosaurStatus.EXTINCT)
    throw new DinosaurExtinctException("Cannot modify an EXTINCT dinosaur");

// 2. Nombre único
if (command.name() != null
        && !command.name().equals(existing.getName())
        && repository.existsByNameAndNotId(command.name(), command.id()))
    throw new DuplicateDinosaurNameException("Dinosaur name already exists");

// 3. Fechas (solo si vienen en el comando)
LocalDateTime finalDiscovery  = command.discoveryDate()  != null ? command.discoveryDate()  : existing.getDiscoveryDate();
LocalDateTime finalExtinction = command.extinctionDate() != null ? command.extinctionDate() : existing.getExtinctionDate();
if (!finalDiscovery.isBefore(finalExtinction))
    throw new InvalidDinosaurDateException("Discovery date must be earlier than extinction date");


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
