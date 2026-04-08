package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.UpdateDinosaurService;
import com.froneus.dinosaur.domain.exception.DinosaurExtinctException;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase.UpdateDinosaurCommand;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateDinosaurServiceTest {

    private DinosaurRepository      repository;
    private DinosaurEventOutboxPort outbox;
    private UpdateDinosaurService   service;

    private static final LocalDateTime DISCOVERY  = LocalDateTime.of(1902, 1, 1, 0, 0);
    private static final LocalDateTime EXTINCTION = LocalDateTime.of(2023, 12, 31, 23, 59);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        outbox     = Mockito.mock(DinosaurEventOutboxPort.class);
        service    = new UpdateDinosaurService(repository, outbox);
    }

    @Test
    void execute_shouldUpdateSuccessfully() {
        Dinosaur existing = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        var cmd = new UpdateDinosaurCommand(1L, "T-Rex Updated", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);

        Dinosaur result = service.execute(cmd);

        assertThat(result.getName()).isEqualTo("T-Rex Updated");
        verify(repository).update(any());
        // Status did not change → no event
        verify(outbox, never()).store(any());
    }

    @Test
    void execute_shouldStoreEventWhenStatusChanges() {
        Dinosaur existing = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        var cmd = new UpdateDinosaurCommand(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ENDANGERED);

        service.execute(cmd);

        verify(outbox).store(any()); // status changed → event emitted
    }

    @Test
    void execute_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var cmd = new UpdateDinosaurCommand(99L, "X", "Y",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(DinosaurNotFoundException.class);

        verify(outbox, never()).store(any());
    }
}
