package com.planifscolaire.presences.exception;

import org.springframework.http.HttpStatus;

public class ConflitMetierException extends ApiException {
    public ConflitMetierException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
