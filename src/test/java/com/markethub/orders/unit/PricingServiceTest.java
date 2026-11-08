package com.markethub.orders.unit;

import com.markethub.orders.application.PricedBasket;
import com.markethub.orders.application.PricingService;
import com.markethub.orders.application.RequestedLine;
import com.markethub.orders.domain.exception.ConflictException;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.model.Customer;
import com.markethub.orders.domain.model.CustomerTier;
import com.markethub.orders.domain.model.Product;
import com.markethub.orders.domain.model.ProductStatus;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.pricing.Basket;
import com.markethub.orders.domain.pricing.CustomerFact;
import com.markethub.orders.domain.pricing.PricingResult;
import com.markethub.orders.infrastructure.drools.DroolsPricingEngine;
import com.markethub.orders.infrastructure.persistence.CustomerRepository;
import com.markethub.orders.infrastructure.persistence.OrderRepository;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static com.markethub.orders.OrderFixtures.NOW;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    private static final PricingResult RESULT = new PricingResult(new BigDecimal("25.000"),
        new BigDecimal("0.000"), new BigDecimal("0.000"), new BigDecimal("25.000"), List.of(), false);

    @Mock CustomerRepository customers;
    @Mock ProductRepository products;
    @Mock OrderRepository orders;
    @Mock DroolsPricingEngine engine;
    @InjectMocks PricingService service;

    private final Customer gold = new Customer("gold@example.com", CustomerTier.GOLD, "KW");

    private static Product product(String sku, String name, String price, ProductStatus status) {
        return new Product(sku, name, "ar-name", "MAISON", "fragrance", new BigDecimal(price), status, NOW);
    }

    @Test
    void priceBasket_shouldUseDatabasePricesAndCustomerHistory_whenInputIsValid() {
        when(customers.findById(1L)).thenReturn(Optional.of(gold));
        when(products.findBySkuIn(anyCollection()))
            .thenReturn(List.of(product("MH-OUD-50", "Royal Oud 50ml", "12.500", ProductStatus.ACTIVE)));
        when(orders.countByCustomerId(1L)).thenReturn(3L);
        when(engine.price(any(Basket.class), any(CustomerFact.class))).thenReturn(RESULT);

        PricedBasket priced = service.priceBasket(1L, "WELCOME10", PaymentMethod.CARD,
            List.of(new RequestedLine("MH-OUD-50", 2)));

        ArgumentCaptor<Basket> basket = ArgumentCaptor.forClass(Basket.class);
        ArgumentCaptor<CustomerFact> fact = ArgumentCaptor.forClass(CustomerFact.class);
        verify(engine).price(basket.capture(), fact.capture());
        assertAll(
            () -> assertThat(basket.getValue().getSubtotal()).isEqualByComparingTo("25.000"),
            () -> assertThat(basket.getValue().getCouponCode()).isEqualTo("WELCOME10"),
            () -> assertThat(fact.getValue().getTier()).isEqualTo(CustomerTier.GOLD),
            () -> assertThat(fact.getValue().getEarlierOrders()).isEqualTo(3),
            () -> assertThat(priced.lines()).extracting(PricedBasket.PricedLine::name)
                .containsExactly("Royal Oud 50ml"));
    }

    @Test
    void price_shouldReturnTheEnginesResult_whenInputIsValid() {
        when(customers.findById(1L)).thenReturn(Optional.of(gold));
        when(products.findBySkuIn(anyCollection()))
            .thenReturn(List.of(product("MH-OUD-50", "Royal Oud 50ml", "12.500", ProductStatus.ACTIVE)));
        when(orders.countByCustomerId(1L)).thenReturn(0L);
        when(engine.price(any(Basket.class), any(CustomerFact.class))).thenReturn(RESULT);

        PricingResult result = service.price(1L, null, PaymentMethod.CARD, List.of(new RequestedLine("MH-OUD-50", 2)));

        assertThat(result).isSameAs(RESULT);
    }

    @Test
    void priceBasket_shouldThrowNotFound_whenCustomerDoesNotExist() {
        when(customers.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.priceBasket(99L, null, PaymentMethod.CARD,
            List.of(new RequestedLine("MH-OUD-50", 1))))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(products, engine);
    }

    @Test
    void priceBasket_shouldThrowConflict_whenSkuIsUnknown() {
        when(customers.findById(1L)).thenReturn(Optional.of(gold));
        when(products.findBySkuIn(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.priceBasket(1L, null, PaymentMethod.CARD,
            List.of(new RequestedLine("NOPE", 1))))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getCode()).isEqualTo("PRODUCT_NOT_AVAILABLE"));
        verifyNoInteractions(engine);
    }

    @Test
    void priceBasket_shouldThrowConflict_whenSkuIsInactive() {
        when(customers.findById(1L)).thenReturn(Optional.of(gold));
        when(products.findBySkuIn(anyCollection()))
            .thenReturn(List.of(product("MH-OLD-1", "Old", "5.000", ProductStatus.INACTIVE)));

        assertThatThrownBy(() -> service.priceBasket(1L, null, PaymentMethod.CARD,
            List.of(new RequestedLine("MH-OLD-1", 1))))
            .isInstanceOfSatisfying(ConflictException.class,
                e -> assertThat(e.getCode()).isEqualTo("PRODUCT_NOT_AVAILABLE"));
    }
}
