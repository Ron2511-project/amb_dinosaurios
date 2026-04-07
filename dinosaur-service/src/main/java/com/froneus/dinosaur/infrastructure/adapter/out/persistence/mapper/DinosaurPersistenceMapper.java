package com.froneus.dinosaur.infrastructure.adapter.out.persistence.mapper;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurReadEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper entre entidades JPA y entidades de dominio.
 *
 * Status mapping:
 *   Dominio ALIVE       → BD "ALIVE"
 *   Dominio ENDANGERED  → BD "INACTIVE"
 *   Dominio EXTINCT     → BD "EXTINCT"
 */
@Component
public class DinosaurPersistenceMapper {

    // ── Write model (dinosaurs_write) ─────────────────────────────────────────

    public Dinosaur toDomain(DinosaurEntity e) {
        return Dinosaur.reconstitute(
                e.getId(),
                e.getName(),
                e.getSpecies(),
                toLocal(e.getDiscoveryDate()),
                toLocal(e.getExtinctionDate()),
                toDomainStatus(e.getStatus())
        );
    }

    // ── Read model (dinosaurs_read) ───────────────────────────────────────────

    public DinosaurReadModel toReadModel(DinosaurReadEntity e) {
        return new DinosaurReadModel.Builder()
                .id(e.getId())
                .name(e.getName())
                .species(e.getSpecies())
                .status(toDomainStatus(e.getStatus()))
                .isExtinct(Boolean.TRUE.equals(e.getIsExtinct()))
                .dinosaurSummary(e.getDinosaurSummary())
                .createdAt(toLocal(e.getCreatedAt()))
                .build();
    }

    // ── Conversiones públicas usadas por el Adapter ───────────────────────────

    public OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    public String toDbStatusString(DinosaurStatus status) {
        if (status == null) return "ALIVE";
        return switch (status) {
            case ALIVE      -> "ALIVE";
            case ENDANGERED -> "INACTIVE";
            case EXTINCT    -> "EXTINCT";
        };
    }

    // ── Conversiones privadas ─────────────────────────────────────────────────

    private LocalDateTime toLocal(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }

    private DinosaurStatus toDomainStatus(String dbStatus) {
        if (dbStatus == null) return DinosaurStatus.ALIVE;
        return switch (dbStatus) {
            case "INACTIVE" -> DinosaurStatus.ENDANGERED;
            case "EXTINCT"  -> DinosaurStatus.EXTINCT;
            default         -> DinosaurStatus.ALIVE;
        };
    }
}
