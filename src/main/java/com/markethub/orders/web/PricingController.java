package com.markethub.orders.web;

import com.markethub.orders.application.PricingService;
import com.markethub.orders.application.RequestedLine;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.web.dto.PricingRequest;
import com.markethub.orders.web.dto.PricingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/preview")
    public PricingResponse preview(@RequestBody PricingRequest request) {
        List<RequestedLine> lines = request.lines().stream()
            .map(l -> new RequestedLine(l.sku(), l.quantity()))
            .toList();
        PricingResult result = pricingService.price(
            request.customerId(), request.couponCode(), request.paymentMethod(), lines);
        return PricingResponse.from(result);
    }
}
