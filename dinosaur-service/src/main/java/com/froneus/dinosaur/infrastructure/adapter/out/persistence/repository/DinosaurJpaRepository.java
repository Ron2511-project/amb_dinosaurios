package com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository;

import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

public interface DinosaurJpaRepository extends JpaRepository<DinosaurEntity, Long> {

    @Query("SELECT COUNT(d) > 0 FROM DinosaurEntity d WHERE d.name = :name AND d.deletedAt IS NULL")
    boolean existsByNameAndNotDeleted(@Param("name") String name);

    /**
     * INSERT nativo con CAST explícito al ENUM PostgreSQL.
     * @Modifying solo admite void o int — el id lo buscamos después con findTopByNameOrderByIdDesc.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO dinosaurs_write
                (name, species, discovery_date, extinction_date, status, created_at, updated_at)
            VALUES
                (:name, :species, :discoveryDate, :extinctionDate,
                 CAST(:status AS dinosaur_status),
                 NOW(), NOW())
            """, nativeQuery = true)
    void insert(
            @Param("name")           String name,
            @Param("species")        String species,
            @Param("discoveryDate")  OffsetDateTime discoveryDate,
            @Param("extinctionDate") OffsetDateTime extinctionDate,
            @Param("status")         String status
    );

    /**
     * Recupera el último registro insertado por nombre.
     * Se usa inmediatamente después de insert() dentro de la misma transacción.
     */
    DinosaurEntity findTopByNameOrderByIdDesc(String name);
}
