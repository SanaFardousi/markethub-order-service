package com.markethub.orders.domain.model;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(@NotBlank String reason) {}
