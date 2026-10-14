package com.planifscolaire.presences.exception;

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
        return error(
                e.status() == 404
                        ? HttpStatus.UNPROCESSABLE_ENTITY
                        : HttpStatus.SERVICE_UNAVAILABLE,
                e.status() == 404
                        ? "La ressource distante demandée n'existe pas"
                        : "Service distant indisponible, réessayez plus tard");
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
