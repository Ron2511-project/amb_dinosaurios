package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.GetDinosaurService;
import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetDinosaurServiceTest {

    private DinosaurRepository repository;
    private GetDinosaurService  service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        service    = new GetDinosaurService(repository);
    }

    @Test
    void getById_shouldReturnReadModel() {
        DinosaurReadModel model = new DinosaurReadModel.Builder()
                .id(1L).name("T-Rex").species("Theropod")
                .status(DinosaurStatus.ALIVE).isExtinct(false)
                .dinosaurSummary("T-Rex - Theropod").build();

        when(repository.findReadById(1L)).thenReturn(Optional.of(model));

        DinosaurReadModel result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("T-Rex");
        assertThat(result.isExtinct()).isFalse();
        verify(repository).findReadById(1L);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(repository.findReadById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(DinosaurNotFoundException.class)
                .hasMessage("Dinosaur not found");
    }

    @Test
    void getAll_shouldReturnPagedResult() {
        DinosaurReadModel model = new DinosaurReadModel.Builder()
                .id(1L).name("T-Rex").species("Theropod")
                .status(DinosaurStatus.ALIVE).isExtinct(false).build();

        PagedResult<DinosaurReadModel> paged = new PagedResult<>(List.of(model), 1L, 1, 10);
        when(repository.findAllActive(1, 10)).thenReturn(paged);

        PagedResult<DinosaurReadModel> result = service.getAll(1, 10);

        assertThat(result.data()).hasSize(1);
        assertThat(result.count()).isEqualTo(1L);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
        verify(repository).findAllActive(1, 10);
    }

    @Test
    void getAll_shouldReturnEmptyWhenNoDinosaurs() {
        PagedResult<DinosaurReadModel> empty = new PagedResult<>(List.of(), 0L, 1, 10);
        when(repository.findAllActive(1, 10)).thenReturn(empty);

        PagedResult<DinosaurReadModel> result = service.getAll(1, 10);

        assertThat(result.data()).isEmpty();
        assertThat(result.count()).isZero();
    }
}
