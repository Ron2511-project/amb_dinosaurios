package com.froneus.dinosaur.infrastructure.adapter.in.web.mapper;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.model.DinosaurReadModel;
import com.froneus.dinosaur.domain.model.PagedResult;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase.CreateDinosaurCommand;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurUseCase.UpdateDinosaurCommand;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.*;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DinosaurWebMapper {

    default CreateDinosaurCommand toCommand(CreateDinosaurRequest request) {
        return new CreateDinosaurCommand(
                request.name(),
                request.species(),
                request.discoveryDate(),
                request.extinctionDate()
        );
    }

    default UpdateDinosaurCommand toCommand(Long id, UpdateDinosaurRequest request) {
        return new UpdateDinosaurCommand(
                id,
                request.name(),
                request.species(),
                request.discoveryDate(),
                request.extinctionDate(),
                request.status()
        );
    }

    default DinosaurResponse toResponse(Dinosaur dinosaur) {
        return new DinosaurResponse(
                dinosaur.getId(),
                dinosaur.getName(),
                dinosaur.getSpecies(),
                dinosaur.getDiscoveryDate(),
                dinosaur.getExtinctionDate(),
                dinosaur.getStatus()
        );
    }

    default DinosaurReadResponse toReadResponse(DinosaurReadModel model) {
        return new DinosaurReadResponse(
                model.getId(),
                model.getName(),
                model.getSpecies(),
                model.getStatus(),
                model.isExtinct(),
                model.getDinosaurSummary(),
                model.getCreatedAt()
        );
    }

    default PagedResponse<DinosaurReadResponse> toPagedResponse(PagedResult<DinosaurReadModel> result) {
        List<DinosaurReadResponse> data = result.data().stream()
                .map(this::toReadResponse)
                .collect(Collectors.toList());
        return new PagedResponse<>(data,
                new PagedResponse.Meta(result.count(), result.page(), result.pageSize()));
    }
}
