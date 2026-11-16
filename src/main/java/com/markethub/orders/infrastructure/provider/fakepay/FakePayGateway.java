package com.markethub.orders.infrastructure.provider.fakepay;

import com.markethub.orders.application.CardDetails;
import com.markethub.orders.application.GatewayResult;
import com.markethub.orders.application.PaymentGateway;
import com.markethub.orders.domain.exception.PaymentTimeoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class FakePayGateway implements PaymentGateway {

    private final FakePayClient client = new FakePayClient();

    @Override
    public GatewayResult authoriseCard(CardDetails card, BigDecimal amount) {
        String reference = newReference();
        String code = call(Map.of("type", "CARD_AUTH", "pan", card.number(), "exp", card.expiry(),
            "amt", toFils(amount), "cur", "414", "ref", reference));
        return new GatewayResult("00".equals(code), reference);
    }

    @Override
    public GatewayResult chargeWallet(Long customerId, BigDecimal amount) {
        String reference = newReference();
        String code = call(Map.of("type", "WALLET_CHARGE", "cust", String.valueOf(customerId),
            "amt", toFils(amount), "cur", "414", "ref", reference));
        return new GatewayResult("00".equals(code), reference);
    }

    @Override
    public void refund(String reference, BigDecimal amount) {
        String code = call(Map.of("type", "REFUND", "ref", reference, "amt", toFils(amount)));
        if (!"00".equals(code)) {
            throw new IllegalStateException("Refund failed with code " + code);
        }
    }

    private String call(Map<String, String> request) {
        try {
            return client.execute(request);
        } catch (FakePayTimeoutException e) {
            throw new PaymentTimeoutException("The payment provider did not answer in time", e);
        }
    }

    private static String toFils(BigDecimal amount) {
        return amount.movePointRight(3).toBigInteger().toString();     // 29.700 -> "29700"
    }

    private static String newReference() {
        return "FP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
