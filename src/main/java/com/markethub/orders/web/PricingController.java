package com.markethub.orders.web;

import com.markethub.orders.application.PricingService;
import com.markethub.orders.application.RequestedLine;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.web.dto.PricingRequest;
import com.markethub.orders.web.dto.PricingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pricing", description = "Price a basket with the rules without saving anything")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected error (a bug)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @Operation(summary = "Preview a basket price",
        description = "Applies the basket tier, welcome coupon, shipping and fraud-hold rules. Nothing is saved.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The priced basket"),
        @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Unknown or inactive SKU",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })

    @PostMapping("/preview")
    public PricingResponse preview(@Valid @RequestBody PricingRequest request) {
        List<RequestedLine> lines = request.lines().stream()
            .map(l -> new RequestedLine(l.sku(), l.quantity()))
            .toList();
        PricingResult result = pricingService.price(
            request.customerId(), request.couponCode(), request.paymentMethod(), lines);
        return PricingResponse.from(result);
    }
}
