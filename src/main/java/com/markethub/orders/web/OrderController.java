package com.markethub.orders.web;

import com.markethub.orders.application.OrderService;
import com.markethub.orders.application.OrderView;
import com.markethub.orders.domain.model.CancelRequest;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.model.TransitionRequest;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import com.markethub.orders.web.dto.OrderHistoryResponse;
import com.markethub.orders.web.dto.OrderResponse;
import com.markethub.orders.web.dto.PlaceOrderRequest;
import com.markethub.orders.web.mapper.ApiMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ApiMapper mapper;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> place(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        OrderView view = orderService.placeOrder(mapper.toCommand(request, idempotencyKey));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + view.orderNumber()))
            .body(mapper.toResponse(view));
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderResponse get(@PathVariable String orderNumber) {
        return mapper.toResponse(orderService.getOrder(orderNumber));
    }

    @GetMapping("/customers/{id}/orders")
    public OrderHistoryResponse history(@PathVariable Long id,
                                        @RequestParam(required = false) OrderStatus status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Page<OrderSummary> result = orderService.history(id, status, page, size);
        return new OrderHistoryResponse(result.getContent().stream().map(mapper::toResponse).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderResponse cancel(@PathVariable String orderNumber, @Valid @RequestBody CancelRequest request) {
        return mapper.toResponse(orderService.cancel(orderNumber, request.reason()));
    }

    @PostMapping("/orders/{orderNumber}/transitions")
    public OrderResponse transition(@PathVariable String orderNumber,
                                    @Valid @RequestBody TransitionRequest request) {
        return mapper.toResponse(orderService.transition(orderNumber, request.action()));
    }
}
