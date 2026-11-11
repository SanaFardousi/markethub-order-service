package com.markethub.orders.application;

public class CarrierUnavailableException extends RuntimeException {
    public CarrierUnavailableException(String message) { super(message); }
}
