package com.saltmarsh.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
