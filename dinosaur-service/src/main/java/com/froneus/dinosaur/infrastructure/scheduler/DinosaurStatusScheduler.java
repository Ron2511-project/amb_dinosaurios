package com.froneus.dinosaur.infrastructure.scheduler;

import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase.StatusUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que corre cada 10 minutos.
 *
 * Delega toda la lógica al caso de uso UpdateDinosaurStatusUseCase
 * para mantener la separación de responsabilidades de la arquitectura hexagonal:
 *   - El scheduler es un adaptador de entrada (como el REST controller)
 *   - La lógica de negocio vive en el use case y el dominio
 *
 * Cron: "0 0/10 * * * *"
 *   Segundo 0, cada 10 minutos: xx:00, xx:10, xx:20, xx:30, xx:40, xx:50
 */
@Component
public class DinosaurStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(DinosaurStatusScheduler.class);

    private final UpdateDinosaurStatusUseCase updateStatusUseCase;

    public DinosaurStatusScheduler(UpdateDinosaurStatusUseCase updateStatusUseCase) {
        this.updateStatusUseCase = updateStatusUseCase;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void updateDinosaurStatuses() {
        log.info("Scheduler started — checking dinosaur statuses");
        try {
            StatusUpdateResult result = updateStatusUseCase.execute();
            log.info("Scheduler completed — ENDANGERED: {}, EXTINCT: {}",
                    result.endangeredCount(), result.extinctCount());
        } catch (Exception e) {
            log.error("Scheduler failed: {}", e.getMessage(), e);
        }
    }
}
