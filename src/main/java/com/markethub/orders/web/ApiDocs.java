package com.markethub.orders.web;

final class ApiDocs {

    static final String ERROR = """
            {"code": "ILLEGAL_TRANSITION", "message": "Cannot SHIP an order in status ON_HOLD", \
            "traceId": "a1b2c3d4", "timestamp": "2026-11-02T10:16:02Z", \
            "fieldErrors": [{"field": "lines[0].quantity", "message": "must be greater than 0"}]}""";

    static final String PLACE_ORDER_REQUEST = """
            {"customerId": 1, "couponCode": "WELCOME10", "paymentMethod": "CARD", \
            "card": {"number": "4111111111111111", "expiry": "12/28"}, \
            "shippingAddress": {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"}, \
            "lines": [{"sku": "MH-OUD-50", "quantity": 2}, {"sku": "MH-ROSE-30", "quantity": 1}]}""";

    static final String PLACE_ORDER_RESPONSE = """
            {"orderNumber": "MH-2026-000123", "status": "CONFIRMED", "currency": "KWD", "subtotal": "33.000", \
            "discount": "3.300", "shipping": "0.000", "total": "29.700", \
            "promotions": [{"id": "R2", "description": "WELCOME10 coupon", "amount": "3.300"}], \
            "payment": {"method": "CARD", "status": "AUTHORISED", "cardLast4": "1111"}}""";

    private ApiDocs() {}
}
