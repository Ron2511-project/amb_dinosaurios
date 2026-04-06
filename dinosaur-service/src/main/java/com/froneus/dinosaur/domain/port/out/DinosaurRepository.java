package com.froneus.dinosaur.domain.port.out;

import com.froneus.dinosaur.domain.model.Dinosaur;

import java.util.Optional;

/**
 * Puerto de salida para persistencia — escribe en dinosaurs_write (Command Side CQRS).
 * El trigger sync_dinosaurs_read propaga automáticamente a dinosaurs_read.
 */
public interface DinosaurRepository {
    boolean existsByName(String name);
    Dinosaur save(Dinosaur dinosaur);
    Optional<Dinosaur> findById(Long id);
}
