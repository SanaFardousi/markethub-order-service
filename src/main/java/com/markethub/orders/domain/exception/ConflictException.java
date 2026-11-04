package com.markethub.orders.domain.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
