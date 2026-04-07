package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.UpdateDinosaurService;
import com.froneus.dinosaur.domain.exception.DinosaurExtinctException;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase.UpdateDinosaurCommand;
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

    private DinosaurRepository   repository;
    private UpdateDinosaurService service;

    private static final LocalDateTime DISCOVERY  = LocalDateTime.of(1902, 1, 1, 0, 0);
    private static final LocalDateTime EXTINCTION = LocalDateTime.of(2025, 12, 31, 23, 59);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        service    = new UpdateDinosaurService(repository);
    }

    @Test
    void execute_shouldUpdateDinosaur() {
        Dinosaur existing = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndNotId("New Name", 1L)).thenReturn(false);
        doNothing().when(repository).update(any());

        var cmd = new UpdateDinosaurCommand(1L, "New Name", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);
        Dinosaur result = service.execute(cmd);

        assertThat(result.getName()).isEqualTo("New Name");
        verify(repository).update(any());
    }

    @Test
    void execute_shouldThrowWhenExtinct() {
        Dinosaur extinct = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.EXTINCT);
        when(repository.findById(1L)).thenReturn(Optional.of(extinct));

        var cmd = new UpdateDinosaurCommand(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.EXTINCT);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(DinosaurExtinctException.class)
                .hasMessage("Cannot modify an EXTINCT dinosaur");

        verify(repository, never()).update(any());
    }

    @Test
    void execute_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var cmd = new UpdateDinosaurCommand(99L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(DinosaurNotFoundException.class);
    }

    @Test
    void execute_shouldThrowWhenNameDuplicated() {
        Dinosaur existing = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndNotId("Raptor", 1L)).thenReturn(true);

        var cmd = new UpdateDinosaurCommand(1L, "Raptor", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.ALIVE);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(DuplicateDinosaurNameException.class);
    }
}
