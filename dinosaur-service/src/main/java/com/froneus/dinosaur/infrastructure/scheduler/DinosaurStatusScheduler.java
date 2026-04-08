package com.froneus.dinosaur.infrastructure.scheduler;

import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase;
import com.froneus.dinosaur.domain.port.in.UpdateDinosaurStatusUseCase.StatusUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de actualización de estados de dinosaurios.
 *
 * Arquitectura hexagonal:
 *   El scheduler es un ADAPTADOR DE ENTRADA — igual que el REST controller
 *   pero disparado por tiempo en vez de HTTP.
 *   Toda la lógica vive en UpdateDinosaurStatusUseCase (dominio/aplicación).
 *
 * Reglas del challenge (punto II):
 *   - 24hs antes del extinctionDate: ALIVE → ENDANGERED
 *   - Al llegar extinctionDate:      ANY   → EXTINCT
 *
 * Configuración:
 *   Producción: cron cada 10 minutos → "0 0/10 * * * *"
 *   Pruebas:    fixedDelay cada 60s  → cambiar la anotación @Scheduled
 *
 * Para ver el scheduler en acción:
 *   docker compose logs -f dinosaur-service | grep -E "Scheduler|ENDANGERED|EXTINCT"
 *
 * Para forzar ejecución inmediata sin esperar el cron,
 * crear un dinosaurio con extinctionDate en el pasado o en las próximas 24hs.
 */
@Component
public class DinosaurStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(DinosaurStatusScheduler.class);

    private final UpdateDinosaurStatusUseCase updateStatusUseCase;

    public DinosaurStatusScheduler(UpdateDinosaurStatusUseCase updateStatusUseCase) {
        this.updateStatusUseCase = updateStatusUseCase;
    }

    /**
     * Producción: cada 10 minutos exactos.
     * Para pruebas rápidas cambiar a: @Scheduled(fixedDelay = 60000)
     */
    @Scheduled(cron = "0 0/10 * * * *")
    public void updateDinosaurStatuses() {
        log.info("╔══════════════════════════════════════════════");
        log.info("║ SCHEDULER STARTED — checking dinosaur statuses");
        log.info("╠══════════════════════════════════════════════");
        try {
            StatusUpdateResult result = updateStatusUseCase.execute();

            if (result.extinctCount() > 0 || result.endangeredCount() > 0) {
                log.info("║ Changes detected:");
                log.info("║   → EXTINCT:    {}", result.extinctCount());
                log.info("║   → ENDANGERED: {}", result.endangeredCount());
            } else {
                log.info("║ No status changes required");
            }
            log.info("╚══════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("║ Scheduler FAILED: {}", e.getMessage(), e);
            log.info("╚══════════════════════════════════════════════");
        }
    }
}
