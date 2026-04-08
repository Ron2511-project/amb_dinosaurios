package com.froneus.dinosaur.domain.port.in;

/**
 * Puerto de entrada — caso de uso del scheduler.
 */
public interface UpdateDinosaurStatusUseCase {

    record StatusUpdateResult(int endangeredCount, int extinctCount) {}

    StatusUpdateResult execute();
}
