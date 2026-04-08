package com.froneus.dinosaur.domain.exception;

public class InvalidDinosaurDateException extends RuntimeException {
    public InvalidDinosaurDateException(String message) {
        super(message);
    }
}
