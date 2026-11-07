package com.markethub.orders.application;

import com.markethub.orders.domain.model.Order;
import com.markethub.orders.domain.model.OrderLine;
import com.markethub.orders.domain.model.Product;
import com.markethub.orders.domain.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ViewMapper {
    OrderView toView(Order order);
    OrderView.LineView toView(OrderLine line);

    @Mapping(target = "method", expression = "java(payment.method())")
    @Mapping(target = "cardLast4", expression = "java(payment.cardLast4())")
    OrderView.PaymentView toView(Payment payment);

    @Mapping(target = "price", source = "priceAmount")
    ProductView toView(Product product);
}
