package com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository;

import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DinosaurJpaRepository extends JpaRepository<DinosaurEntity, Long> {

    @Query("SELECT COUNT(d) > 0 FROM DinosaurEntity d WHERE d.name = :name AND d.deletedAt IS NULL")
    boolean existsByNameAndNotDeleted(@Param("name") String name);

    @Query("SELECT COUNT(d) > 0 FROM DinosaurEntity d WHERE d.name = :name AND d.id <> :id AND d.deletedAt IS NULL")
    boolean existsByNameAndNotId(@Param("name") String name, @Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO dinosaurs_write
                (name, species, discovery_date, extinction_date, status, created_at, updated_at)
            VALUES (:name, :species, :discoveryDate, :extinctionDate,
                    CAST(:status AS dinosaur_status), NOW(), NOW())
            """, nativeQuery = true)
    void insert(@Param("name") String name, @Param("species") String species,
                @Param("discoveryDate") OffsetDateTime discoveryDate,
                @Param("extinctionDate") OffsetDateTime extinctionDate,
                @Param("status") String status);

    DinosaurEntity findTopByNameOrderByIdDesc(String name);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE dinosaurs_write
            SET name=:name, species=:species, discovery_date=:discoveryDate,
                extinction_date=:extinctionDate,
                status=CAST(:status AS dinosaur_status), updated_at=NOW()
            WHERE id=:id AND deleted_at IS NULL
            """, nativeQuery = true)
    void update(@Param("id") Long id, @Param("name") String name,
                @Param("species") String species,
                @Param("discoveryDate") OffsetDateTime discoveryDate,
                @Param("extinctionDate") OffsetDateTime extinctionDate,
                @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE dinosaurs_write SET deleted_at=NOW(), updated_at=NOW() WHERE id=:id AND deleted_at IS NULL",
           nativeQuery = true)
    void softDelete(@Param("id") Long id);

    @Query("SELECT d FROM DinosaurEntity d WHERE d.id=:id AND d.deletedAt IS NULL")
    Optional<DinosaurEntity> findActiveById(@Param("id") Long id);

    // ── Scheduler queries ─────────────────────────────────────────────────────

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE dinosaurs_write
            SET status=CAST('INACTIVE' AS dinosaur_status), updated_at=NOW()
            WHERE status=CAST('ALIVE' AS dinosaur_status)
              AND deleted_at IS NULL
              AND extinction_date <= NOW() + INTERVAL '24 hours'
              AND extinction_date  > NOW()
            RETURNING id, name, species, discovery_date, extinction_date, status
            """, nativeQuery = true)
    List<DinosaurEntity> updateAliveToEndangeredAndReturn();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE dinosaurs_write
            SET status=CAST('EXTINCT' AS dinosaur_status), updated_at=NOW()
            WHERE status != CAST('EXTINCT' AS dinosaur_status)
              AND deleted_at IS NULL
              AND extinction_date <= NOW()
            RETURNING id, name, species, discovery_date, extinction_date, status
            """, nativeQuery = true)
    List<DinosaurEntity> updateToExtinctAndReturn();
}
