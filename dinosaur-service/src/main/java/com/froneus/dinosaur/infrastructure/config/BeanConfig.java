package com.froneus.dinosaur.infrastructure.config;

import com.froneus.dinosaur.application.usecase.*;
import com.froneus.dinosaur.domain.port.in.*;
import com.froneus.dinosaur.domain.port.out.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public CreateDinosaurUseCase createDinosaurUseCase(DinosaurRepository repo,
                                                        DinosaurEventOutboxPort outbox) {
        return new CreateDinosaurService(repo, outbox);
    }

    @Bean
    public GetDinosaurUseCase getDinosaurUseCase(DinosaurRepository repo) {
        return new GetDinosaurService(repo);
    }

    @Bean
    public UpdateDinosaurUseCase updateDinosaurUseCase(DinosaurRepository repo,
                                                        DinosaurEventOutboxPort outbox) {
        return new UpdateDinosaurService(repo, outbox);
    }

    @Bean
    public DeleteDinosaurUseCase deleteDinosaurUseCase(DinosaurRepository repo,
                                                        DinosaurEventOutboxPort outbox) {
        return new DeleteDinosaurService(repo, outbox);
    }

    @Bean
    public UpdateDinosaurStatusUseCase updateDinosaurStatusUseCase(DinosaurRepository repo,
                                                                    DinosaurEventOutboxPort outbox) {
        return new UpdateDinosaurStatusService(repo, outbox);
    }
}
