package com.mahendra.bizcart_backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.common.exception.ApiErrorResponse;
import com.mahendra.bizcart_backend.common.exception.ApiErrorResponseFactory;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	@Test
	void propagatesCallerCorrelationIdToHeaderMdcAndErrorBody() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.addHeader(CorrelationIdFilter.HEADER, "request-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<ApiErrorResponse> error = new AtomicReference<>();
		FilterChain chain = (req, res) -> error.set(ApiErrorResponseFactory.of(HttpStatus.UNAUTHORIZED,
				"AUTH_INVALID_CREDENTIALS", "Invalid email or password", request.getRequestURI()));

		new CorrelationIdFilter().doFilter(request, response, chain);

		assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("request-123");
		assertThat(error.get().correlationId()).isEqualTo("request-123");
		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new CorrelationIdFilter().doFilter(request, response, (req, res) ->
				assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNotBlank());

		assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
	}
}
