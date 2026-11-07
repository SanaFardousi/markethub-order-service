package com.markethub.orders.infrastructure.provider.fakepay;

import java.math.BigInteger;
import java.util.Map;

public class FakePayClient {
    private static final BigInteger WALLET_LIMIT_FILS = BigInteger.valueOf(1_000_000);   // 1000.000 KWD

    /** One method for everything. Returns "00" (approved), "51" (declined) or "96" (unknown request). */
    public String execute(Map<String, String> request) {
        String type = request.get("type");
        if ("CARD_AUTH".equals(type)) {
            return request.get("pan").endsWith("0002") ? "51" : "00";
        }
        if ("WALLET_CHARGE".equals(type)) {
            return new BigInteger(request.get("amt")).compareTo(WALLET_LIMIT_FILS) > 0 ? "51" : "00";
        }
        if ("REFUND".equals(type)) {
            return "00";
        }
        return "96";
    }
}
