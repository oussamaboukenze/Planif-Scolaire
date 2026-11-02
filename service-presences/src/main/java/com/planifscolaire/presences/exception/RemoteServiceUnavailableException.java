package com.planifscolaire.presences.exception;

import org.springframework.http.HttpStatus;

public class RemoteServiceUnavailableException extends ApiException {
    public RemoteServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}