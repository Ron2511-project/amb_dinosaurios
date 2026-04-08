package com.froneus.dinosaur.application;

import com.froneus.dinosaur.application.usecase.UpdateDinosaurStatusService;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase.StatusUpdateResult;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UpdateDinosaurStatusServiceTest {

    private DinosaurRepository          repository;
    private DinosaurEventOutboxPort     outbox;
    private UpdateDinosaurStatusService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DinosaurRepository.class);
        outbox     = Mockito.mock(DinosaurEventOutboxPort.class);
        service    = new UpdateDinosaurStatusService(repository, outbox);
    }

    @Test
    void execute_shouldRunExtinctBeforeEndangered() {
        Dinosaur extinctDino = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                DinosaurStatus.EXTINCT);

        Dinosaur endangeredDino = Dinosaur.reconstitute(2L, "Raptor", "Theropod",
                LocalDateTime.of(1969, 1, 1, 0, 0),
                LocalDateTime.now().plusHours(12),
                DinosaurStatus.ENDANGERED);

        when(repository.updateToExtinctAndReturn()).thenReturn(List.of(extinctDino));
        when(repository.updateAliveToEndangeredAndReturn()).thenReturn(List.of(endangeredDino));

        StatusUpdateResult result = service.execute();

        assertThat(result.extinctCount()).isEqualTo(1);
        assertThat(result.endangeredCount()).isEqualTo(1);

        // Orden: EXTINCT primero, ENDANGERED segundo
        var order = inOrder(repository);
        order.verify(repository).updateToExtinctAndReturn();
        order.verify(repository).updateAliveToEndangeredAndReturn();

        verify(outbox, times(2)).store(any(DinosaurEvent.class));
    }

    @Test
    void execute_shouldStoreSchedulerUpdateEventType() {
        Dinosaur dino = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                DinosaurStatus.EXTINCT);

        when(repository.updateToExtinctAndReturn()).thenReturn(List.of(dino));
        when(repository.updateAliveToEndangeredAndReturn()).thenReturn(List.of());

        service.execute();

        ArgumentCaptor<DinosaurEvent> captor = ArgumentCaptor.forClass(DinosaurEvent.class);
        verify(outbox).store(captor.capture());

        DinosaurEvent event = captor.getValue();
        assertThat(event.dinosaurId()).isEqualTo(1L);
        assertThat(event.eventType()).isEqualTo(DinosaurEvent.EventType.SCHEDULER_UPDATE);
        assertThat(event.newStatus()).isEqualTo(DinosaurStatus.EXTINCT);
    }

    @Test
    void execute_shouldReturnZeroCountsWhenNoChanges() {
        when(repository.updateToExtinctAndReturn()).thenReturn(List.of());
        when(repository.updateAliveToEndangeredAndReturn()).thenReturn(List.of());

        StatusUpdateResult result = service.execute();

        assertThat(result.extinctCount()).isZero();
        assertThat(result.endangeredCount()).isZero();
        verify(outbox, never()).store(any());
    }

    @Test
    void execute_shouldStoreOneEventPerAffectedDinosaur() {
        List<Dinosaur> extinctList = List.of(
                Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                        LocalDateTime.of(1902, 1, 1, 0, 0),
                        LocalDateTime.of(2020, 1, 1, 0, 0), DinosaurStatus.EXTINCT),
                Dinosaur.reconstitute(2L, "Raptor", "Dromaeosauridae",
                        LocalDateTime.of(1969, 1, 1, 0, 0),
                        LocalDateTime.of(2019, 1, 1, 0, 0), DinosaurStatus.EXTINCT)
        );
        List<Dinosaur> endangeredList = List.of(
                Dinosaur.reconstitute(3L, "Triceratops", "Ceratopsidae",
                        LocalDateTime.of(1887, 1, 1, 0, 0),
                        LocalDateTime.now().plusHours(6), DinosaurStatus.ENDANGERED)
        );

        when(repository.updateToExtinctAndReturn()).thenReturn(extinctList);
        when(repository.updateAliveToEndangeredAndReturn()).thenReturn(endangeredList);

        StatusUpdateResult result = service.execute();

        assertThat(result.extinctCount()).isEqualTo(2);
        assertThat(result.endangeredCount()).isEqualTo(1);
        verify(outbox, times(3)).store(any(DinosaurEvent.class));
    }
}
