package com.markethub.orders.domain.exception;

public class NoCarrierAvailableException extends RuntimeException {
    public NoCarrierAvailableException() { super("No carrier answered in time"); }
}
