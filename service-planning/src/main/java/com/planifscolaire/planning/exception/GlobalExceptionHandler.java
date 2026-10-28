package com.planifscolaire.planning.exception;

import feign.FeignException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> domain(ApiException e) {
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
        return error(
                HttpStatus.BAD_REQUEST,
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .collect(java.util.stream.Collectors.joining("; ")));
    }

    @ExceptionHandler(FeignException.class)
    ResponseEntity<ProblemDetail> remote(FeignException e) {
        if (e.status() == 404) {
            return error(
                    HttpStatus.NOT_FOUND,
                    "Ressource externe introuvable");
        }

        if (e.status() == -1) {
            return error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Échec de communication avec un service");
        }

        if (e.status() == 503) {
            return error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service ou instance indisponible");
        }

        return error(
                HttpStatus.BAD_GATEWAY,
                "Erreur HTTP du service distant : " + e.status());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> refus(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException e) {
        return error(
                HttpStatus.CONFLICT, "Cette opération crée un doublon ou viole une contrainte");
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
