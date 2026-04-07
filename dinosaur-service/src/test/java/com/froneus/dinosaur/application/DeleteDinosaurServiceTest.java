package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.DeleteDinosaurService;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteDinosaurServiceTest {

    private DinosaurRepository   repository;
    private DeleteDinosaurService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        service    = new DeleteDinosaurService(repository);
    }

    @Test
    void execute_shouldSoftDelete() {
        Dinosaur existing = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59),
                DinosaurStatus.ALIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(repository).softDelete(1L);

        service.execute(1L);

        verify(repository).softDelete(1L);
    }

    @Test
    void execute_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(DinosaurNotFoundException.class)
                .hasMessage("Dinosaurio no encontrado");

        verify(repository, never()).softDelete(any());
    }
}
