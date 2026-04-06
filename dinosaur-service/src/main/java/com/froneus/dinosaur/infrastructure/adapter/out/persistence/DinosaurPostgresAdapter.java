package com.froneus.dinosaur.infrastructure.adapter.out.persistence;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.mapper.DinosaurPersistenceMapper;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador de salida: escribe en dinosaurs_write con CAST explícito
 * al ENUM PostgreSQL dinosaur_status via native query.
 *
 * Patrón insert + findTop en la misma transacción para recuperar el id generado.
 */
@Component
public class DinosaurPostgresAdapter implements DinosaurRepository {

    private final DinosaurJpaRepository     jpaRepository;
    private final DinosaurPersistenceMapper mapper;

    public DinosaurPostgresAdapter(DinosaurJpaRepository jpaRepository,
                                   DinosaurPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper        = mapper;
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByNameAndNotDeleted(name);
    }

    @Override
    @Transactional
    public Dinosaur save(Dinosaur dinosaur) {
        // 1. INSERT con CAST(:status AS dinosaur_status)
        jpaRepository.insert(
                dinosaur.getName(),
                dinosaur.getSpecies(),
                mapper.toOffset(dinosaur.getDiscoveryDate()),
                mapper.toOffset(dinosaur.getExtinctionDate()),
                mapper.toDbStatusString(dinosaur.getStatus())
        );

        // 2. Recupera el registro recién insertado para obtener el id BIGSERIAL
        DinosaurEntity saved = jpaRepository.findTopByNameOrderByIdDesc(dinosaur.getName());

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Dinosaur> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
