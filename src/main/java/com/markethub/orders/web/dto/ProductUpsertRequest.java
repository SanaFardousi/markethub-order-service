package com.markethub.orders.web.dto;

import com.github.javaparser.quality.NotNull;
import com.markethub.orders.domain.model.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductUpsertRequest(@NotBlank String nameEn, @NotBlank String nameAr, @NotBlank String brand,
                                   @NotBlank String category, @NotNull @DecimalMin("0.000") BigDecimal price,
                                   @NotNull ProductStatus status) {}
