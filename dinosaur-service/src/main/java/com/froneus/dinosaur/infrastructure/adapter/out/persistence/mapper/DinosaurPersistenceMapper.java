package com.froneus.dinosaur.infrastructure.adapter.out.persistence.mapper;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper entre entidad JPA y entidad de dominio.
 *
 * Expone toOffset() y toDbStatusString() como métodos públicos
 * para que DinosaurPostgresAdapter los use en la native query.
 *
 * Mapeo de status:
 *   Dominio ALIVE       → BD "ALIVE"
 *   Dominio ENDANGERED  → BD "INACTIVE"
 *   Dominio EXTINCT     → BD "EXTINCT"
 */
@Component
public class DinosaurPersistenceMapper {

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

    // ── Métodos públicos usados por DinosaurPostgresAdapter ───────────────────

    public OffsetDateTime toOffset(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    public String toDbStatusString(DinosaurStatus status) {
        return switch (status) {
            case ALIVE      -> "ALIVE";
            case ENDANGERED -> "INACTIVE";
            case EXTINCT    -> "EXTINCT";
        };
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

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
