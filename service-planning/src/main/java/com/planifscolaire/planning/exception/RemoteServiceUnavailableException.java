package com.planifscolaire.planning.exception;

import org.springframework.http.HttpStatus;

public class RemoteServiceUnavailableException extends ApiException {
    public RemoteServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}