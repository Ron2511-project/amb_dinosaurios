package com.froneus.dinosaur.domain;

import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class DinosaurTest {

    private static final LocalDateTime DISCOVERY  = LocalDateTime.of(1902, 1, 1, 0, 0);
    private static final LocalDateTime EXTINCTION = LocalDateTime.of(2023, 12, 31, 23, 59);

    @Test
    void create_shouldSetStatusAliveAndNullId() {
        Dinosaur dino = Dinosaur.create("T-Rex", "Theropod", DISCOVERY, EXTINCTION);

        // ID es null — lo asigna PostgreSQL con BIGSERIAL al hacer save()
        assertThat(dino.getId()).isNull();
        assertThat(dino.getName()).isEqualTo("T-Rex");
        assertThat(dino.getStatus()).isEqualTo(DinosaurStatus.ALIVE);
    }

    @Test
    void create_shouldThrowWhenDiscoveryDateEqualsExtinctionDate() {
        var same = LocalDateTime.of(2020, 1, 1, 0, 0);
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", same, same))
                .isInstanceOf(InvalidDinosaurDateException.class)
                .hasMessageContaining("menor a la fecha de extinción");
    }

    @Test
    void create_shouldThrowWhenDiscoveryDateAfterExtinctionDate() {
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", EXTINCTION, DISCOVERY))
                .isInstanceOf(InvalidDinosaurDateException.class);
    }

    @Test
    void create_shouldThrowWhenDatesAreNull() {
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", null, EXTINCTION))
                .isInstanceOf(InvalidDinosaurDateException.class);
    }

    @Test
    void reconstitute_shouldPreserveAllFields() {
        Dinosaur dino = Dinosaur.reconstitute(
                42L, "Stego", "Stegosauria", DISCOVERY, EXTINCTION, DinosaurStatus.ENDANGERED);

        assertThat(dino.getId()).isEqualTo(42L);
        assertThat(dino.getStatus()).isEqualTo(DinosaurStatus.ENDANGERED);
    }
}
