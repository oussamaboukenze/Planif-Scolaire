package com.planifscolaire.servicereferentiel.exception;

import org.springframework.http.HttpStatus;

public class RessourceIntrouvableException extends ApiException {
    public RessourceIntrouvableException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
