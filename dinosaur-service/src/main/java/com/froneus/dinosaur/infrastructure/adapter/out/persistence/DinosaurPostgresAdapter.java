package com.froneus.dinosaur.infrastructure.adapter.out.persistence;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurReadEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.mapper.DinosaurPersistenceMapper;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurJpaRepository;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurReadJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class DinosaurPostgresAdapter implements DinosaurRepository {

    private final DinosaurJpaRepository     writeRepo;
    private final DinosaurReadJpaRepository readRepo;
    private final DinosaurPersistenceMapper mapper;

    public DinosaurPostgresAdapter(DinosaurJpaRepository writeRepo,
                                   DinosaurReadJpaRepository readRepo,
                                   DinosaurPersistenceMapper mapper) {
        this.writeRepo = writeRepo;
        this.readRepo  = readRepo;
        this.mapper    = mapper;
    }

    @Override public boolean existsByName(String name) {
        return writeRepo.existsByNameAndNotDeleted(name);
    }

    @Override public boolean existsByNameAndNotId(String name, Long excludeId) {
        return writeRepo.existsByNameAndNotId(name, excludeId);
    }

    @Override
    @Transactional
    public Dinosaur save(Dinosaur d) {
        writeRepo.insert(d.getName(), d.getSpecies(),
                mapper.toOffset(d.getDiscoveryDate()),
                mapper.toOffset(d.getExtinctionDate()),
                mapper.toDbStatusString(d.getStatus()));
        return mapper.toDomain(writeRepo.findTopByNameOrderByIdDesc(d.getName()));
    }

    @Override public Optional<Dinosaur> findById(Long id) {
        return writeRepo.findActiveById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void update(Dinosaur d) {
        writeRepo.update(d.getId(), d.getName(), d.getSpecies(),
                mapper.toOffset(d.getDiscoveryDate()),
                mapper.toOffset(d.getExtinctionDate()),
                mapper.toDbStatusString(d.getStatus()));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        writeRepo.softDelete(id);
    }

    // ── Scheduler ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<Dinosaur> updateAliveToEndangeredAndReturn() {
        return writeRepo.updateAliveToEndangeredAndReturn()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<Dinosaur> updateToExtinctAndReturn() {
        return writeRepo.updateToExtinctAndReturn()
                .stream().map(mapper::toDomain).toList();
    }

    // ── Query Side ────────────────────────────────────────────────────────────

    @Override public Optional<DinosaurReadModel> findReadById(Long id) {
        return readRepo.findActiveById(id).map(mapper::toReadModel);
    }

    @Override
    public PagedResult<DinosaurReadModel> findAllActive(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        Page<DinosaurReadEntity> result = readRepo.findAllActive(pageable);
        List<DinosaurReadModel> data = result.getContent().stream()
                .map(mapper::toReadModel).toList();
        return new PagedResult<>(data, result.getTotalElements(), page, pageSize);
    }
}
