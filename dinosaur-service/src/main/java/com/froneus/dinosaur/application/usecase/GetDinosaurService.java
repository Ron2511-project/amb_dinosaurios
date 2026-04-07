package com.froneus.dinosaur.application.usecase;

import com.froneus.dinosaur.domain.exception.DinosaurNotFoundException;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.in.GetDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;

/**
 * Caso de uso de consulta — lee desde dinosaurs_read (Query Side CQRS).
 * Solo devuelve registros activos (deleted_at IS NULL).
 */
public class GetDinosaurService implements GetDinosaurUseCase {

    private final DinosaurRepository repository;

    public GetDinosaurService(DinosaurRepository repository) {
        this.repository = repository;
    }

    @Override
    public DinosaurReadModel getById(Long id) {
        return repository.findReadById(id)
                .orElseThrow(() -> new DinosaurNotFoundException("Dinosaur not found"));
    }

    @Override
    public PagedResult<DinosaurReadModel> getAll(int page, int pageSize) {
        return repository.findAllActive(page, pageSize);
    }
}
