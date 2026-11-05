package com.markethub.orders.application;

public record CardDetails(String number, String expiry) {
    public String last4() { return number.substring(Math.max(0, number.length() - 4)); }
    @Override public String toString() { return "CardDetails[****" + last4() + ", " + expiry + "]"; }
}

