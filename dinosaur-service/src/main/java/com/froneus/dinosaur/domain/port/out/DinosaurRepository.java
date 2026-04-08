package com.froneus.dinosaur.domain.port.out;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de dinosaurios.
 * Write → dinosaurs_write | Read → dinosaurs_read
 */
public interface DinosaurRepository {

    // ── Command Side ──────────────────────────────────────────────────────────
    boolean            existsByName(String name);
    boolean            existsByNameAndNotId(String name, Long excludeId);
    Dinosaur           save(Dinosaur dinosaur);
    Optional<Dinosaur> findById(Long id);
    void               update(Dinosaur dinosaur);
    void               softDelete(Long id);

    // ── Scheduler ─────────────────────────────────────────────────────────────
    /** ALIVE → INACTIVE. Retorna los dinosaurios afectados para emitir eventos. */
    List<Dinosaur> updateAliveToEndangeredAndReturn();

    /** ANY → EXTINCT. Retorna los dinosaurios afectados para emitir eventos. */
    List<Dinosaur> updateToExtinctAndReturn();

    // ── Query Side ────────────────────────────────────────────────────────────
    Optional<DinosaurReadModel>    findReadById(Long id);
    PagedResult<DinosaurReadModel> findAllActive(int page, int pageSize);
}
