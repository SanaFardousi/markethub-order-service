package com.markethub.orders.web.dto;

import java.time.Instant;

public record OrderSummaryResponse(String orderNumber, String status, String total, Instant createdAt) {}
