package com.markethub.orders.web;

import com.markethub.orders.application.ShippingService;
import com.markethub.orders.web.dto.ShippingOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Shipping", description = "Delivery options from three carriers, fetched in parallel")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected error (a bug)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @Operation(summary = "Get delivery options",
        description = "Asks three carriers at the same time with an 800 ms limit each, cheapest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Options from the carriers that answered in time"),
        @ApiResponse(responseCode = "503", description = "No carrier answered in time",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/options")
    public List<ShippingOptionResponse> options(@RequestParam String country) {
        return shippingService.options(country).stream().map(ShippingOptionResponse::from).toList();
    }
}
