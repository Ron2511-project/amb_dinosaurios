package com.froneus.dinosaur.infrastructure.adapter.out.persistence;

import com.froneus.dinosaur.domain.exception.ServiceUnavailableException;
import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurReadEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.mapper.DinosaurPersistenceMapper;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurJpaRepository;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurReadJpaRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class DinosaurPostgresAdapter implements DinosaurRepository {

    private static final Logger log     = LoggerFactory.getLogger(DinosaurPostgresAdapter.class);
    private static final String CB_NAME = "dinosaurService";

    private final DinosaurJpaRepository     writeRepo;
    private final DinosaurReadJpaRepository readRepo;
    private final DinosaurPersistenceMapper mapper;
    private final CircuitBreaker            cb;

    public DinosaurPostgresAdapter(DinosaurJpaRepository writeRepo,
                                   DinosaurReadJpaRepository readRepo,
                                   DinosaurPersistenceMapper mapper,
                                   CircuitBreakerRegistry circuitBreakerRegistry) {
        this.writeRepo = writeRepo;
        this.readRepo  = readRepo;
        this.mapper    = mapper;
        this.cb        = circuitBreakerRegistry.circuitBreaker(CB_NAME);
    }

    private <T> T execute(Supplier<T> supplier) {
        return CircuitBreaker.decorateSupplier(cb, supplier).get();
    }

    private void executeVoid(Runnable runnable) {
        CircuitBreaker.decorateRunnable(cb, runnable).run();
    }

    @Override
    public boolean existsByName(String name) {
        try {
            return execute(() -> writeRepo.existsByNameAndNotDeleted(name));
        } catch (Exception e) {
            log.error("CB — existsByName failed: {}", e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    public boolean existsByNameAndNotId(String name, Long excludeId) {
        try {
            return execute(() -> writeRepo.existsByNameAndNotId(name, excludeId));
        } catch (Exception e) {
            log.error("CB — existsByNameAndNotId failed: {}", e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    @Transactional
    public Dinosaur save(Dinosaur d) {
        try {
            return execute(() -> {
                writeRepo.insert(d.getName(), d.getSpecies(),
                        mapper.toOffset(d.getDiscoveryDate()),
                        mapper.toOffset(d.getExtinctionDate()),
                        mapper.toDbStatusString(d.getStatus()));
                return mapper.toDomain(writeRepo.findTopByNameOrderByIdDesc(d.getName()));
            });
        } catch (Exception e) {
            log.error("CB — save failed: {}", e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    public Optional<Dinosaur> findById(Long id) {
        try {
            return execute(() -> writeRepo.findActiveById(id).map(mapper::toDomain));
        } catch (Exception e) {
            log.error("CB — findById id={} failed: {}", id, e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void update(Dinosaur d) {
        try {
            executeVoid(() -> writeRepo.update(d.getId(), d.getName(), d.getSpecies(),
                    mapper.toOffset(d.getDiscoveryDate()),
                    mapper.toOffset(d.getExtinctionDate()),
                    mapper.toDbStatusString(d.getStatus())));
        } catch (Exception e) {
            log.error("CB — update id={} failed: {}", d.getId(), e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        try {
            executeVoid(() -> writeRepo.softDelete(id));
        } catch (Exception e) {
            log.error("CB — softDelete id={} failed: {}", id, e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    @Transactional
    public List<Dinosaur> updateAliveToEndangeredAndReturn() {
        try {
            return execute(() -> writeRepo.updateAliveToEndangeredAndReturn()
                    .stream().map(mapper::toDomain).toList());
        } catch (Exception e) {
            log.error("CB — updateAliveToEndangered failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional
    public List<Dinosaur> updateToExtinctAndReturn() {
        try {
            return execute(() -> writeRepo.updateToExtinctAndReturn()
                    .stream().map(mapper::toDomain).toList());
        } catch (Exception e) {
            log.error("CB — updateToExtinct failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<DinosaurReadModel> findReadById(Long id) {
        try {
            return execute(() -> readRepo.findActiveById(id).map(mapper::toReadModel));
        } catch (Exception e) {
            log.error("CB — findReadById id={} failed: {}", id, e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }

    @Override
    public PagedResult<DinosaurReadModel> findAllActive(int page, int pageSize) {
        try {
            return execute(() -> {
                PageRequest pageable = PageRequest.of(page - 1, pageSize);
                Page<DinosaurReadEntity> result = readRepo.findAllActive(pageable);
                List<DinosaurReadModel> data = result.getContent().stream()
                        .map(mapper::toReadModel).toList();
                return new PagedResult<>(data, result.getTotalElements(), page, pageSize);
            });
        } catch (Exception e) {
            log.error("CB — findAllActive failed: {}", e.getMessage());
            throw new ServiceUnavailableException("Service temporarily unavailable. Please try again later.");
        }
    }
}
