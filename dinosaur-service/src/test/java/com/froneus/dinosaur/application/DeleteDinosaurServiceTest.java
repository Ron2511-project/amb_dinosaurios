package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.DeleteDinosaurService;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteDinosaurServiceTest {

    private DinosaurRepository      repository;
    private DinosaurEventOutboxPort outbox;
    private DeleteDinosaurService   service;

    private static final LocalDateTime DISCOVERY  = LocalDateTime.of(1902, 1, 1, 0, 0);
    private static final LocalDateTime EXTINCTION = LocalDateTime.of(2023, 12, 31, 23, 59);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        outbox     = Mockito.mock(DinosaurEventOutboxPort.class);
        service    = new DeleteDinosaurService(repository, outbox);
    }

    @Test
    void execute_shouldSoftDeleteAndStoreEvent() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                        DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE)));

        service.execute(1L);

        verify(repository).softDelete(1L);
        verify(outbox).store(any());
    }

    @Test
    void execute_shouldStoreDeletedEventType() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                        DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE)));

        service.execute(1L);

        ArgumentCaptor<DinosaurEvent> captor = ArgumentCaptor.forClass(DinosaurEvent.class);
        verify(outbox).store(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(DinosaurEvent.EventType.DELETED);
        assertThat(captor.getValue().dinosaurId()).isEqualTo(1L);
    }

    @Test
    void execute_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(DinosaurNotFoundException.class)
                .hasMessage("Dinosaur not found");

        verify(repository, never()).softDelete(any());
        verify(outbox, never()).store(any());
    }

    @Test
    void execute_shouldWorkWithEndangeredDinosaur() {
        when(repository.findById(2L)).thenReturn(Optional.of(
                Dinosaur.reconstitute(2L, "Raptor", "Dromaeosauridae",
                        DISCOVERY, EXTINCTION, DinosaurStatus.ENDANGERED)));

        service.execute(2L);

        verify(repository).softDelete(2L);
        verify(outbox).store(any());
    }
}
