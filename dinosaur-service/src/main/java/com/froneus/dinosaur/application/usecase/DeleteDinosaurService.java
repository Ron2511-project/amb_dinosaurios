package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurEvent.EventType;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.DeleteDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

public class DeleteDinosaurService implements DeleteDinosaurUseCase {

    private final DinosaurRepository      repository;
    private final DinosaurEventOutboxPort outbox;

    public DeleteDinosaurService(DinosaurRepository repository,
                                 DinosaurEventOutboxPort outbox) {
        this.repository = repository;
        this.outbox     = outbox;
    }

    @Override
    public void execute(Long id) {
        repository.softDelete(id);

        // Emitir evento de eliminación
        outbox.store(DinosaurEvent.of(id, DinosaurStatus.EXTINCT, EventType.DELETED));
    }
}
