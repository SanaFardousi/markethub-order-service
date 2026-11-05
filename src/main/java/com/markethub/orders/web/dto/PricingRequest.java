package com.markethub.orders.web.dto;

import com.github.javaparser.quality.NotNull;
import com.markethub.orders.domain.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PricingRequest(@NotNull Long customerId, String couponCode, PaymentMethod paymentMethod,
                             @NotEmpty List<@Valid LineRequest> lines) {}
