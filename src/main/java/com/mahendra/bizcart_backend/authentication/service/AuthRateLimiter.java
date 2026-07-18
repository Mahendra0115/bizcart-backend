package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthRateLimiter {
	private final Clock clock;
	private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

	public AuthRateLimiter(Clock clock) { this.clock = clock; }

	public void check(String operation, String key, AuthenticationProperties.Limit limit) {
		String bucketKey = operation + ":" + (key == null ? "unknown" : key);
		Instant now = clock.instant();
		Deque<Instant> bucket = attempts.computeIfAbsent(bucketKey, ignored -> new ArrayDeque<>());
		synchronized (bucket) {
			Instant cutoff = now.minusSeconds(limit.getWindowSeconds());
			while (!bucket.isEmpty() && !bucket.peekFirst().isAfter(cutoff)) bucket.removeFirst();
			if (bucket.size() >= limit.getMaxRequests()) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
			}
			bucket.addLast(now);
		}
	}
}
