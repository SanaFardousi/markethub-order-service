package com.markethub.orders.web;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markethub.orders.application.OrderService;
import com.markethub.orders.application.OrderView;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import com.markethub.orders.domain.model.CancelRequest;
import com.markethub.orders.web.dto.OrderHistoryResponse;
import com.markethub.orders.web.dto.OrderResponse;
import com.markethub.orders.web.dto.PlaceOrderRequest;
import com.markethub.orders.domain.model.TransitionRequest;
import com.markethub.orders.web.mapper.ApiMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Orders", description = "Place orders and move them through their lifecycle")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validation failed, malformed body or missing Idempotency-Key",
        content = @Content(schema = @Schema(implementation = ApiError.class),
            examples = @ExampleObject(value = ApiDocs.ERROR))),
    @ApiResponse(responseCode = "500", description = "Unexpected error (a bug)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ApiMapper mapper;

    @Operation(summary = "Place an order",
        description = "Prices the basket with the rules, takes payment and saves the order. Sending the same "
            + "Idempotency-Key with the same body returns the original order and places no second one.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = ApiDocs.PLACE_ORDER_REQUEST))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created",
            content = @Content(schema = @Schema(implementation = OrderResponse.class),
                examples = @ExampleObject(value = ApiDocs.PLACE_ORDER_RESPONSE))),
        @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Unknown or inactive SKU, or the Idempotency-Key was reused with a different body",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "Payment declined, no order was created",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> place(
        @Parameter(description = "A unique key for this order attempt. Retrying with the same key and body is safe.",
            example = "3f6c0e2a-8d4b-4c1e-9a77-1b2c3d4e5f60")
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody PlaceOrderRequest request) {
        OrderView view = orderService.placeOrder(mapper.toCommand(request, idempotencyKey));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + view.orderNumber()))
            .body(mapper.toResponse(view));
    }

    @Operation(summary = "Get an order", description = "Returns the order with its lines and payment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The order"),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/orders/{orderNumber}")
    public OrderResponse get(@PathVariable String orderNumber) {
        return mapper.toResponse(orderService.getOrder(orderNumber));
    }

    @Operation(summary = "List a customer's orders",
        description = "Paged order history, newest first, with an optional status filter.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of order summaries"),
        @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/customers/{id}/orders")
    public OrderHistoryResponse history(@PathVariable Long id,
                                        @RequestParam(required = false) OrderStatus status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Page<OrderSummary> result = orderService.history(id, status, page, size);
        return new OrderHistoryResponse(result.getContent().stream().map(mapper::toResponse).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Operation(summary = "Cancel an order", description = "Cancels the order and refunds the payment if it was paid.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The cancelled order"),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "The order can no longer be cancelled, or another request changed it first",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderResponse cancel(@PathVariable String orderNumber, @Valid @RequestBody CancelRequest request) {
        return mapper.toResponse(orderService.cancel(orderNumber, request.reason()));
    }

    @Operation(summary = "Approve, ship or deliver an order", description = "Back-office action.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The updated order"),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "The action is not allowed in the order's current status",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/orders/{orderNumber}/transitions")
    public OrderResponse transition(@PathVariable String orderNumber, @Valid @RequestBody TransitionRequest request) {
        return mapper.toResponse(orderService.transition(orderNumber, request.action()));
    }
}
