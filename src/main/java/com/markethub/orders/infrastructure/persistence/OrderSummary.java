package com.markethub.orders.infrastructure.persistence;

import com.markethub.orders.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(String orderNumber, OrderStatus status, BigDecimal total, Instant createdAt) {}

