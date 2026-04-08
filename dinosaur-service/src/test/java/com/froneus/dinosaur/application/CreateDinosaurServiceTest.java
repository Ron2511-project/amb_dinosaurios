package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.CreateDinosaurService;
import com.froneus.dinosaur.domain.exception.DuplicateDinosaurNameException;
import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase.CreateDinosaurCommand;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateDinosaurServiceTest {

    private DinosaurRepository      repository;
    private DinosaurEventOutboxPort outbox;
    private CreateDinosaurService   service;

    private static final LocalDateTime DISCOVERY  = LocalDateTime.of(1902, 1, 1, 0, 0);
    private static final LocalDateTime EXTINCTION = LocalDateTime.of(2023, 12, 31, 23, 59);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        outbox     = Mockito.mock(DinosaurEventOutboxPort.class);
        service    = new CreateDinosaurService(repository, outbox);
    }

    @Test
    void execute_shouldSaveAndReturnDinosaur() {
        var cmd = new CreateDinosaurCommand("T-Rex", "Theropod", DISCOVERY, EXTINCTION);
        when(repository.existsByName("T-Rex")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            Dinosaur d = inv.getArgument(0);
            return Dinosaur.reconstitute(1L, d.getName(), d.getSpecies(),
                    d.getDiscoveryDate(), d.getExtinctionDate(), d.getStatus());
        });

        Dinosaur result = service.execute(cmd);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("T-Rex");
        assertThat(result.getStatus()).isEqualTo(DinosaurStatus.ALIVE);
        verify(repository).save(any());
        verify(outbox).store(any()); // event stored in outbox
    }

    @Test
    void execute_shouldThrowWhenNameIsDuplicated() {
        var cmd = new CreateDinosaurCommand("T-Rex", "Theropod", DISCOVERY, EXTINCTION);
        when(repository.existsByName("T-Rex")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(DuplicateDinosaurNameException.class)
                .hasMessage("Dinosaur name already exists");

        verify(repository, never()).save(any());
        verify(outbox, never()).store(any());
    }

    @Test
    void execute_shouldThrowWhenDiscoveryDateIsAfterExtinctionDate() {
        var cmd = new CreateDinosaurCommand("T-Rex", "Theropod", EXTINCTION, DISCOVERY);
        when(repository.existsByName(any())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(InvalidDinosaurDateException.class);

        verify(repository, never()).save(any());
        verify(outbox, never()).store(any());
    }
}
