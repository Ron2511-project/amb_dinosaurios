package com.froneus.dinosaur.infrastructure.adapter.in.web.mapper;

import com.froneus.dinosaur.domain.model.Dinosaur;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase.CreateDinosaurCommand;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.CreateDinosaurRequest;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.DinosaurResponse;
import org.mapstruct.Mapper;

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
}
