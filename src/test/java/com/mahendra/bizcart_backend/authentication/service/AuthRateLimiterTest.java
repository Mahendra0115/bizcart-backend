package com.mahendra.bizcart_backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AuthRateLimiterTest {

	private static final Instant NOW = Instant.parse("2026-07-18T12:00:00Z");

	@Test
	void cleanupRemovesExpiredBucketsFromMap() {
		AuthenticationProperties properties = properties(10);
		AuthRateLimiter limiter = new AuthRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), properties);

		limiter.check("login", "first@example.com", properties.getRateLimit().getLogin());
		limiter.check("login", "second@example.com", properties.getRateLimit().getLogin());
		assertThat(limiter.bucketCount()).isEqualTo(2);

		limiter.cleanupExpiredBuckets(NOW.plusSeconds(
				properties.getRateLimit().getLogin().getWindowSeconds() + 1));

		assertThat(limiter.bucketCount()).isZero();
	}

	@Test
	void rejectsNewUniqueKeysWhenBucketCapacityIsExhausted() {
		AuthenticationProperties properties = properties(2);
		AuthRateLimiter limiter = new AuthRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), properties);

		limiter.check("login", "first@example.com", properties.getRateLimit().getLogin());
		limiter.check("login", "second@example.com", properties.getRateLimit().getLogin());

		assertThatThrownBy(() -> limiter.check("login", "attacker@example.com",
				properties.getRateLimit().getLogin()))
			.isInstanceOfSatisfying(ResponseStatusException.class,
					ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
		assertThat(limiter.bucketCount()).isEqualTo(2);
	}

	@Test
	void expiredBucketsFreeCapacityForNewKeys() {
		AuthenticationProperties properties = properties(1);
		AuthRateLimiter limiter = new AuthRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), properties);
		limiter.check("login", "expired@example.com", properties.getRateLimit().getLogin());
		limiter.cleanupExpiredBuckets(NOW.plusSeconds(
				properties.getRateLimit().getLogin().getWindowSeconds() + 1));

		limiter.check("login", "new@example.com", properties.getRateLimit().getLogin());

		assertThat(limiter.bucketCount()).isEqualTo(1);
	}

	private AuthenticationProperties properties(int maxBuckets) {
		AuthenticationProperties properties = new AuthenticationProperties();
		properties.getRateLimit().setMaxBuckets(maxBuckets);
		return properties;
	}
}
