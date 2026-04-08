package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Caso de uso del scheduler.
 *
 * Orden importa:
 *   1. EXTINCT  primero — los que ya vencieron
 *   2. ENDANGERED luego — los próximos a vencer
 *
 * Por cada dinosaurio afectado → guarda evento en el Outbox (Redis).
 * El OutboxPublisher lo lee y publica en RabbitMQ de forma asíncrona.
 */
public class UpdateDinosaurStatusService implements UpdateDinosaurStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateDinosaurStatusService.class);

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public UpdateDinosaurStatusService(DinosaurRepository repository,
                                       DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
    }

    @Override
    public StatusUpdateResult execute() {
        // 1. EXTINCT — extinctionDate ya pasó
        List<Dinosaur> extinct = repository.updateToExtinctAndReturn();
        extinct.forEach(d -> outbox.store(
                DinosaurEvent.of(d.getId(), d.getStatus(), EventType.SCHEDULER_UPDATE)));

        // 2. ENDANGERED — ≤ 24hs para extinctionDate
        List<Dinosaur> endangered = repository.updateAliveToEndangeredAndReturn();
        endangered.forEach(d -> outbox.store(
                DinosaurEvent.of(d.getId(), d.getStatus(), EventType.SCHEDULER_UPDATE)));

        if (!extinct.isEmpty() || !endangered.isEmpty()) {
            log.info("Scheduler — EXTINCT: {}, ENDANGERED: {}",
                    extinct.size(), endangered.size());
        }

        return new StatusUpdateResult(endangered.size(), extinct.size());
    }
}
