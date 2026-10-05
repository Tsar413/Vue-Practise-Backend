package com.study.vuePractiseBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class BusinessExceptions {

    private BusinessExceptions() {
    }

    public static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, message);
    }

    public static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, message);
    }

    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, message);
    }
}