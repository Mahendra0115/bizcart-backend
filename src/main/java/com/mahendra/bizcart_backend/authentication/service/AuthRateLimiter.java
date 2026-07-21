package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthRateLimiter {
	private final Clock clock;
	private final AuthenticationProperties properties;
	private final ConcurrentHashMap<String, Bucket> attempts = new ConcurrentHashMap<>();

	public AuthRateLimiter(Clock clock, AuthenticationProperties properties) {
		this.clock = clock;
		this.properties = properties;
	}

	public void check(String operation, String key, AuthenticationProperties.Limit limit) {
		String bucketKey = operation + ":" + (key == null ? "unknown" : key);
		Instant now = clock.instant();
		Bucket bucket = getOrCreateBucket(bucketKey, limit.getWindowSeconds(), now);
		synchronized (bucket) {
			Instant cutoff = now.minusSeconds(limit.getWindowSeconds());
			removeExpiredAttempts(bucket.attempts, cutoff);
			bucket.windowSeconds = limit.getWindowSeconds();
			if (bucket.attempts.size() >= limit.getMaxRequests()) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
			}
			bucket.attempts.addLast(now);
		}
	}

	private Bucket getOrCreateBucket(String bucketKey, long windowSeconds, Instant now) {
		Bucket existing = attempts.get(bucketKey);
		if (existing != null) return existing;
		synchronized (attempts) {
			existing = attempts.get(bucketKey);
			if (existing != null) return existing;
			if (attempts.size() >= properties.getRateLimit().getMaxBuckets()) {
				cleanupExpiredBuckets(now);
			}
			if (attempts.size() >= properties.getRateLimit().getMaxBuckets()) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
			}
			Bucket created = new Bucket(windowSeconds);
			attempts.put(bucketKey, created);
			return created;
		}
	}

	@Scheduled(fixedDelayString = "${bizcart.auth.rate-limit.cleanup-interval-ms:60000}")
	public void cleanupExpiredBuckets() {
		cleanupExpiredBuckets(clock.instant());
	}

	void cleanupExpiredBuckets(Instant now) {
		attempts.forEach((key, bucket) -> {
			synchronized (bucket) {
				removeExpiredAttempts(bucket.attempts, now.minusSeconds(bucket.windowSeconds));
				if (bucket.attempts.isEmpty()) attempts.remove(key, bucket);
			}
		});
	}

	int bucketCount() {
		return attempts.size();
	}

	private void removeExpiredAttempts(Deque<Instant> bucket, Instant cutoff) {
		while (!bucket.isEmpty() && !bucket.peekFirst().isAfter(cutoff)) bucket.removeFirst();
	}

	private static final class Bucket {
		private final Deque<Instant> attempts = new ArrayDeque<>();
		private long windowSeconds;
		private Bucket(long windowSeconds) {
			this.windowSeconds = windowSeconds;
		}
	}
}
