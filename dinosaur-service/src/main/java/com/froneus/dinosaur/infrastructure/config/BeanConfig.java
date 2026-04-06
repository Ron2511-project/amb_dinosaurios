package com.froneus.dinosaur.infrastructure.config;

import com.froneus.dinosaur.application.usecase.CreateDinosaurService;
import com.froneus.dinosaur.domain.port.in.CreateDinosaurUseCase;
import com.froneus.dinosaur.domain.port.out.DinosaurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de beans de aplicación.
 * Conecta los puertos con sus implementaciones (wiring hexagonal).
 * El dominio y los casos de uso no llevan anotaciones de Spring.
 */
@Configuration
public class BeanConfig {

    @Bean
    public CreateDinosaurUseCase createDinosaurUseCase(DinosaurRepository repository) {
        return new CreateDinosaurService(repository);
    }
}
