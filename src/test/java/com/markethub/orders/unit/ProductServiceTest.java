package com.markethub.orders.unit;

import com.markethub.orders.application.ProductCommand;
import com.markethub.orders.application.ProductService;
import com.markethub.orders.application.ProductView;
import com.markethub.orders.application.ViewMapperImpl;
import com.markethub.orders.domain.model.Product;
import com.markethub.orders.domain.model.ProductStatus;
import com.markethub.orders.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.markethub.orders.OrderFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;

    private final Clock clock = Clock.fixed(NOW.plusSeconds(3600), ZoneOffset.UTC);
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, new ViewMapperImpl(), clock);
        lenient().when(products.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void upsert_shouldCreateProduct_whenSkuIsNew() {
        when(products.findBySku("MH-NEW-1")).thenReturn(Optional.empty());

        ProductView view = service.upsert(new ProductCommand("MH-NEW-1", "New", "جديد", "MAISON", "fragrance",
            new BigDecimal("9.9"), ProductStatus.ACTIVE));

        assertAll(
            () -> assertThat(view.sku()).isEqualTo("MH-NEW-1"),
            () -> assertThat(view.price()).isEqualByComparingTo("9.900"),
            () -> assertThat(view.currency()).isEqualTo("KWD"));
    }

    @Test
    void upsert_shouldUpdateExistingProductAndStampTheClock_whenSkuExists() {
        Product existing = new Product("MH-OUD-50", "Old name", "قديم", "MAISON", "fragrance",
            new BigDecimal("12.500"), ProductStatus.ACTIVE, NOW);
        when(products.findBySku("MH-OUD-50")).thenReturn(Optional.of(existing));

        service.upsert(new ProductCommand("MH-OUD-50", "Royal Oud 50ml", "عود ملكي", "MAISON", "fragrance",
            new BigDecimal("9.900"), ProductStatus.INACTIVE));

        assertAll(
            () -> assertThat(existing.getNameEn()).isEqualTo("Royal Oud 50ml"),
            () -> assertThat(existing.getPriceAmount()).isEqualByComparingTo("9.900"),
            () -> assertThat(existing.getStatus()).isEqualTo(ProductStatus.INACTIVE),
            () -> assertThat(existing.getUpdatedAt()).isEqualTo(NOW.plusSeconds(3600)));
    }
}
