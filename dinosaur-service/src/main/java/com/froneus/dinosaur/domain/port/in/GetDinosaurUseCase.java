package com.froneus.dinosaur.domain.port.in;

import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;

/**
 * Puerto de entrada — consultas de dinosaurios (Query Side CQRS).
 * Lee desde dinosaurs_read.
 */
public interface GetDinosaurUseCase {
    DinosaurReadModel        getById(Long id);
    PagedResult<DinosaurReadModel> getAll(int page, int pageSize);
}
