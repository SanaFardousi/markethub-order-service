package com.markethub.orders.unit;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.markethub.orders.web.RequestIdFilter;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private final AtomicReference<String> seenInsideTheRequest = new AtomicReference<>();
    private final FilterChain chain = (request, response) -> seenInsideTheRequest.set(MDC.get(RequestIdFilter.MDC_KEY));

    private MockHttpServletResponse run(String incomingId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (incomingId != null) {
            request.addHeader(RequestIdFilter.HEADER, incomingId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void doFilter_shouldGenerateAnIdForMdcAndHeader_whenNoneIsSent() throws Exception {
        MockHttpServletResponse response = run(null);

        assertAll(
            () -> assertThat(seenInsideTheRequest.get()).hasSize(8),
            () -> assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seenInsideTheRequest.get()));
    }

    @Test
    void doFilter_shouldReuseTheIncomingId_whenItIsSafe() throws Exception {
        MockHttpServletResponse response = run("req-42.a_b");

        assertAll(
            () -> assertThat(seenInsideTheRequest.get()).isEqualTo("req-42.a_b"),
            () -> assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("req-42.a_b"));
    }

    @Test
    void doFilter_shouldReplaceTheIncomingId_whenItContainsUnsafeCharacters() throws Exception {
        run("bad id with spaces");

        assertThat(seenInsideTheRequest.get()).hasSize(8).doesNotContain(" ");
    }

    @Test
    void doFilter_shouldClearMdc_afterTheRequestEnds() throws Exception {
        run("req-1");

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
