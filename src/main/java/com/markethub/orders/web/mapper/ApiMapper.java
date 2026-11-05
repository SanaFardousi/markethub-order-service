package com.markethub.orders.web.mapper;

import com.markethub.orders.application.OrderView;
import com.markethub.orders.application.PlaceOrderCommand;
import com.markethub.orders.application.ProductCommand;
import com.markethub.orders.application.ProductView;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import com.markethub.orders.web.dto.*;
import org.mapstruct.Mapper;
import java.math.BigDecimal;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ApiMapper {

    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    PlaceOrderCommand toCommand(PlaceOrderRequest request, String idempotencyKey);

    @Mapping(target = "sku", source = "sku")
    ProductCommand toCommand(String sku, ProductUpsertRequest request);

    @Mapping(target = "currency", constant = "KWD")
    OrderResponse toResponse(OrderView view);

    ProductResponse toResponse(ProductView view);

    OrderSummaryResponse toResponse(OrderSummary summary);

    default String money(BigDecimal value) { return value.toPlainString(); }
}
