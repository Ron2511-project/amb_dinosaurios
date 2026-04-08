package com.froneus.dinosaur.domain.exception;

public class DuplicateDinosaurNameException extends RuntimeException {
    public DuplicateDinosaurNameException(String message) {
        super(message);
    }
}
