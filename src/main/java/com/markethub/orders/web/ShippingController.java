package com.markethub.orders.web;

import com.markethub.orders.application.ShippingService;
import com.markethub.orders.web.dto.ShippingOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/options")
    public List<ShippingOptionResponse> options(@RequestParam String country) {
        return shippingService.options(country).stream().map(ShippingOptionResponse::from).toList();
    }
}
