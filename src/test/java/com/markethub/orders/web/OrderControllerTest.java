package com.markethub.orders.web;

import com.markethub.orders.application.OrderService;
import com.markethub.orders.application.OrderView;
import com.markethub.orders.application.PlaceOrderCommand;
import com.markethub.orders.domain.exception.*;
import com.markethub.orders.domain.model.OrderAction;
import com.markethub.orders.domain.model.OrderStatus;
import com.markethub.orders.domain.payment.PaymentMethod;
import com.markethub.orders.domain.payment.PaymentStatus;
import com.markethub.orders.domain.pricing.AppliedPromotion;
import com.markethub.orders.infrastructure.persistence.OrderSummary;
import com.markethub.orders.web.mapper.ApiMapperImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static com.markethub.orders.OrderFixtures.NOW;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(OrderController.class)
@Import({ApiMapperImpl.class, FixedClockConfig.class, ApiExceptionHandler.class})
class OrderControllerTest {

    private static final String KEY = "3f6c0e2a-8d4b-4c1e-9a77-1b2c3d4e5f60";
    private static final String ADDRESS = """
            {"country": "KW", "city": "Kuwait City", "line1": "Block 3, Street 12"}""";
    private static final String LINES = """
            [{"sku": "MH-OUD-50", "quantity": 2}, {"sku": "MH-ROSE-30", "quantity": 1}]""";

    private static final OrderView CONFIRMED = new OrderView("MH-2026-000123", OrderStatus.CONFIRMED,
        new BigDecimal("33.000"), new BigDecimal("3.300"), new BigDecimal("0.000"), new BigDecimal("29.700"),
        List.of(new AppliedPromotion("R2", "WELCOME10 coupon", new BigDecimal("3.300"))),
        List.of(new OrderView.LineView("MH-OUD-50", "Royal Oud 50ml", 2, new BigDecimal("12.500"))),
        new OrderView.PaymentView(PaymentMethod.CARD, PaymentStatus.AUTHORISED, "1111"));

    @Autowired
    MockMvc mvc;
    @MockitoBean
    OrderService orderService;

    private static String body(String customerId, String method, String address, String lines) {
        return """
                {"customerId": %s, "paymentMethod": %s, "card": {"number": "4111111111111111", "expiry": "12/28"},
                 "shippingAddress": %s, "lines": %s}
                """.formatted(customerId, method, address, lines);
    }

    private ResultActions place(String json) throws Exception {
        return mvc.perform(post("/api/v1/orders").header("Idempotency-Key", KEY)
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ---------- success ----------

    @Test
    void placeOrder_shouldReturn201WithLocation_whenRequestIsValid() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(CONFIRMED);

        place(body("1", "\"CARD\"", ADDRESS, LINES))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/orders/MH-2026-000123"))
            .andExpect(jsonPath("$.orderNumber").value("MH-2026-000123"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.currency").value("KWD"))
            .andExpect(jsonPath("$.total").value("29.700"))
            .andExpect(jsonPath("$.promotions[0].id").value("R2"))
            .andExpect(jsonPath("$.payment.cardLast4").value("1111"));
    }

    @Test
    void placeOrder_shouldPassHeaderAndBodyToTheService_whenRequestIsValid() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(CONFIRMED);

        place(body("1", "\"CARD\"", ADDRESS, LINES)).andExpect(status().isCreated());

        ArgumentCaptor<PlaceOrderCommand> captor = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(orderService).placeOrder(captor.capture());
        PlaceOrderCommand command = captor.getValue();
        assertAll(
            () -> assertThat(command.idempotencyKey()).isEqualTo(KEY),
            () -> assertThat(command.customerId()).isEqualTo(1L),
            () -> assertThat(command.paymentMethod()).isEqualTo(PaymentMethod.CARD),
            () -> assertThat(command.lines()).hasSize(2),
            () -> assertThat(command.shippingAddress().city()).isEqualTo("Kuwait City"));
    }

    // ---------- validation: 400 with field errors ----------

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
            Arguments.of("quantity zero", body("1", "\"CARD\"", ADDRESS, LINES.replace("\"quantity\": 2", "\"quantity\": 0")), "lines[0].quantity"),
            Arguments.of("blank sku", body("1", "\"CARD\"", ADDRESS, LINES.replace("MH-OUD-50", "")), "lines[0].sku"),
            Arguments.of("no lines", body("1", "\"CARD\"", ADDRESS, "[]"), "lines"),
            Arguments.of("no customer", body("null", "\"CARD\"", ADDRESS, LINES), "customerId"),
            Arguments.of("no payment method", body("1", "null", ADDRESS, LINES), "paymentMethod"),
            Arguments.of("no address", body("1", "\"CARD\"", "null", LINES), "shippingAddress"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void placeOrder_shouldReturn400WithFieldErrors_whenBodyIsInvalid(String description, String json, String field)
        throws Exception {
        place(json)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andDo(print())
            .andExpect(jsonPath("$.fieldErrors[?(@.field == '" + field + "')]").exists());
        verifyNoInteractions(orderService);
    }

    // ---------- malformed requests: 400 MALFORMED_REQUEST ----------

    @Test
    void placeOrder_shouldReturn400_whenIdempotencyKeyHeaderIsMissing() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .content(body("1", "\"CARD\"", ADDRESS, LINES)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void placeOrder_shouldReturn400_whenJsonIsMalformed() throws Exception {
        place("{ this is not json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void placeOrder_shouldReturn400_whenPaymentMethodIsUnknown() throws Exception {
        place(body("1", "\"BITCOIN\"", ADDRESS, LINES))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // ---------- every exception becomes the documented status and code ----------

    static Stream<Arguments> failures() {
        return Stream.of(
            Arguments.of(new NotFoundException("missing"), 404, "NOT_FOUND"),
            Arguments.of(new IllegalTransitionException(OrderStatus.ON_HOLD, "SHIP"), 409, "ILLEGAL_TRANSITION"),
            Arguments.of(new ConflictException("PRODUCT_NOT_AVAILABLE", "x"), 409, "PRODUCT_NOT_AVAILABLE"),
            Arguments.of(new ConflictException("IDEMPOTENCY_KEY_REUSED", "x"), 409, "IDEMPOTENCY_KEY_REUSED"),
            Arguments.of(new OptimisticLockingFailureException("stale"), 409, "CONCURRENT_MODIFICATION"),
            Arguments.of(new PaymentDeclinedException("declined"), 422, "PAYMENT_DECLINED"),
            Arguments.of(new BadRequestException("bad"), 400, "VALIDATION_FAILED"),
            Arguments.of(new IllegalStateException("boom"), 500, "INTERNAL_ERROR"),
            Arguments.of(new NoCarrierAvailableException(), 503, "NO_CARRIER_AVAILABLE"));

    }

    @ParameterizedTest(name = "{0} gives {1} {2}")
    @MethodSource("failures")
    void getOrder_shouldMapEveryExceptionToItsStatusAndCode(RuntimeException failure, int status, String code)
        throws Exception {
        when(orderService.getOrder("MH-1")).thenThrow(failure);

        mvc.perform(get("/api/v1/orders/MH-1"))
            .andExpect(status().is(status))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").value("2026-11-01T10:00:00Z"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void getOrder_shouldNotLeakInternalDetails_whenAnUnexpectedErrorHappens() throws Exception {
        when(orderService.getOrder("MH-1")).thenThrow(new IllegalStateException("secret SQL: select * from orders"));

        String response = mvc.perform(get("/api/v1/orders/MH-1"))
            .andExpect(status().isInternalServerError())
            .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("secret").doesNotContain("select");
    }

    @Test
    void unknownUrl_shouldReturn404InTheStandardErrorShape() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    // ---------- the other endpoints ----------

    @Test
    void cancel_shouldReturn400_whenReasonIsBlank() throws Exception {
        mvc.perform(post("/api/v1/orders/MH-1/cancel").contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void transition_shouldReturn400_whenActionIsUnknown() throws Exception {
        mvc.perform(post("/api/v1/orders/MH-1/transitions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"EXPLODE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void transition_shouldReturn200_whenActionIsValid() throws Exception {
        when(orderService.transition("MH-1", OrderAction.SHIP)).thenReturn(CONFIRMED);

        mvc.perform(post("/api/v1/orders/MH-1/transitions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\": \"SHIP\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void history_shouldReturnPagedEnvelope_whenCustomerExists() throws Exception {
        Page<OrderSummary> page = new PageImpl<>(
            List.of(new OrderSummary("MH-1", OrderStatus.CONFIRMED, new BigDecimal("29.700"), NOW)),
            PageRequest.of(0, 20), 1);
        when(orderService.history(1L, null, 0, 20)).thenReturn(page);

        mvc.perform(get("/api/v1/customers/1/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].orderNumber").value("MH-1"))
            .andExpect(jsonPath("$.content[0].total").value("29.700"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void history_shouldReturn400_whenStatusFilterIsNotAStatus() throws Exception {
        mvc.perform(get("/api/v1/customers/1/orders?status=NOPE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
