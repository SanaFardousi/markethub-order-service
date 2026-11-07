package com.markethub.orders.web.dto;

import com.github.javaparser.quality.NotNull;
import com.markethub.orders.domain.model.ProductStatus;
import com.markethub.orders.domain.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
    @NotNull Long customerId,
    String couponCode,
    @NotNull PaymentMethod paymentMethod,
    @Valid CardRequest card,
    @NotNull @Valid AddressRequest shippingAddress,
    @NotEmpty List<@Valid LineRequest> lines) {

    public record CardRequest(@NotBlank String number, @NotBlank String expiry) {
        @Override public String toString() { return "CardRequest[****]"; }   // never print a card number
    }
    public record AddressRequest(@NotBlank @Size(min = 2, max = 2) String country,
                                 @NotBlank String city, @NotBlank String line1) {}
}

