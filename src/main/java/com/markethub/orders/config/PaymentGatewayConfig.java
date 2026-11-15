package com.markethub.orders.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.markethub.orders.application.PaymentGateway;
import com.markethub.orders.application.RetryingPaymentGateway;
import com.markethub.orders.infrastructure.provider.fakepay.FakePayGateway;

@Configuration
public class PaymentGatewayConfig {

    @Bean
    @Primary
    PaymentGateway paymentGateway(FakePayGateway vendorGateway,
                                  @Value("${markethub.payment.retry.max-attempts:3}") int maxAttempts,
                                  @Value("${markethub.payment.retry.backoff:100ms}") Duration backoff) {
        return new RetryingPaymentGateway(vendorGateway, maxAttempts, backoff);
    }
}
