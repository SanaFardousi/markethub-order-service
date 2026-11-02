package com.markethub.orders.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String country, String city, String line1) { }

