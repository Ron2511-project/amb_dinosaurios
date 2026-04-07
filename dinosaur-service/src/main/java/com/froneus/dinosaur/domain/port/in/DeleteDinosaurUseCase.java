package com.froneus.dinosaur.domain.port.in;

/**
 * Puerto de entrada — soft delete de dinosaurio.
 * Actualiza deleted_at en dinosaurs_write.
 */
public interface DeleteDinosaurUseCase {
    void execute(Long id);
}
