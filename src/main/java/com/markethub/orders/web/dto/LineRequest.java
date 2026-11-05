package com.markethub.orders.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LineRequest(@NotBlank String sku, @Positive int quantity) {}
