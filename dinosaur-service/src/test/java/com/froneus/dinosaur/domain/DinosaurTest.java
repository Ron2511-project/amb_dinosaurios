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
    void create_shouldSetStatusAlive() {
        Dinosaur dino = Dinosaur.create("T-Rex", "Theropod", DISCOVERY, EXTINCTION);
        assertThat(dino.getId()).isNull();
        assertThat(dino.getName()).isEqualTo("T-Rex");
        assertThat(dino.getSpecies()).isEqualTo("Theropod");
        assertThat(dino.getStatus()).isEqualTo(DinosaurStatus.ALIVE);
        assertThat(dino.getDiscoveryDate()).isEqualTo(DISCOVERY);
        assertThat(dino.getExtinctionDate()).isEqualTo(EXTINCTION);
    }

    @Test
    void create_shouldThrowWhenDiscoveryDateEqualsExtinctionDate() {
        var same = LocalDateTime.of(2020, 1, 1, 0, 0);
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", same, same))
                .isInstanceOf(InvalidDinosaurDateException.class)
                .hasMessageContaining("Discovery date must be earlier than extinction date");
    }

    @Test
    void create_shouldThrowWhenDiscoveryDateAfterExtinctionDate() {
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", EXTINCTION, DISCOVERY))
                .isInstanceOf(InvalidDinosaurDateException.class)
                .hasMessageContaining("Discovery date must be earlier than extinction date");
    }

    @Test
    void create_shouldThrowWhenDiscoveryDateIsNull() {
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", null, EXTINCTION))
                .isInstanceOf(InvalidDinosaurDateException.class)
                .hasMessageContaining("Discovery and extinction dates are required");
    }

    @Test
    void create_shouldThrowWhenExtinctionDateIsNull() {
        assertThatThrownBy(() -> Dinosaur.create("Raptor", "Theropod", DISCOVERY, null))
                .isInstanceOf(InvalidDinosaurDateException.class)
                .hasMessageContaining("Discovery and extinction dates are required");
    }

    @Test
    void reconstitute_shouldPreserveAllFields() {
        Dinosaur dino = Dinosaur.reconstitute(42L, "Stego", "Stegosauria",
                DISCOVERY, EXTINCTION, DinosaurStatus.ENDANGERED);
        assertThat(dino.getId()).isEqualTo(42L);
        assertThat(dino.getName()).isEqualTo("Stego");
        assertThat(dino.getSpecies()).isEqualTo("Stegosauria");
        assertThat(dino.getStatus()).isEqualTo(DinosaurStatus.ENDANGERED);
        assertThat(dino.getDiscoveryDate()).isEqualTo(DISCOVERY);
        assertThat(dino.getExtinctionDate()).isEqualTo(EXTINCTION);
    }

    @Test
    void reconstitute_shouldSupportExtinctStatus() {
        Dinosaur dino = Dinosaur.reconstitute(1L, "T-Rex", "Theropod",
                DISCOVERY, EXTINCTION, DinosaurStatus.EXTINCT);
        assertThat(dino.getStatus()).isEqualTo(DinosaurStatus.EXTINCT);
    }
}
