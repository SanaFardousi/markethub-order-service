package com.markethub.orders.application;

public interface Carrier {
    String name();
    ShippingOption quote(String country);     // slow, and may fail: it's a call to an outside company
}
