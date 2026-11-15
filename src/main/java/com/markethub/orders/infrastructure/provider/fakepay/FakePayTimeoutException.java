package com.markethub.orders.infrastructure.provider.fakepay;

public class FakePayTimeoutException extends RuntimeException {

    public FakePayTimeoutException(String message) {
        super(message);
    }
}
