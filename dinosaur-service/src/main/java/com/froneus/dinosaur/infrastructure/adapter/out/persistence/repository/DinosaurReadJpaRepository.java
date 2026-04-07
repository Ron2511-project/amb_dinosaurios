package com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository;

import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurReadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio JPA para dinosaurs_read (Query Side CQRS).
 * Solo lectura — excluye registros con deleted_at IS NOT NULL.
 */
public interface DinosaurReadJpaRepository extends JpaRepository<DinosaurReadEntity, Long> {

    @Query("SELECT d FROM DinosaurReadEntity d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<DinosaurReadEntity> findActiveById(@Param("id") Long id);

    @Query("SELECT d FROM DinosaurReadEntity d WHERE d.deletedAt IS NULL")
    Page<DinosaurReadEntity> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(d) FROM DinosaurReadEntity d WHERE d.deletedAt IS NULL")
    long countActive();
}
