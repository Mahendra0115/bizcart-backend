package com.mahendra.bizcart_backend.authentication.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

	@Test
	void untrustedRemoteCannotSpoofForwardedFor() {
		ClientIpResolver resolver = resolver("10.0.0.0/8");
		MockHttpServletRequest request = request("198.51.100.20", "203.0.113.99");

		assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
	}

	@Test
	void trustedProxyReturnsFirstUntrustedAddressFromRight() {
		ClientIpResolver resolver = resolver("10.0.0.0/8,192.168.0.0/16");
		MockHttpServletRequest request = request("10.0.0.5", "203.0.113.10, 192.168.1.8");

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
	}

	@Test
	void trustedProxyWithoutForwardedHeaderUsesRemoteAddress() {
		ClientIpResolver resolver = resolver("127.0.0.1");
		MockHttpServletRequest request = request("127.0.0.1", null);

		assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
	}

	private ClientIpResolver resolver(String trustedProxies) {
		AuthenticationProperties properties = new AuthenticationProperties();
		properties.getProxy().setTrustedProxies(trustedProxies);
		return new ClientIpResolver(properties);
	}

	private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddress);
		if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
		return request;
	}
}
