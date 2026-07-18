package com.mahendra.bizcart_backend.redis;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "bizcart.redis.startup-check", name = "enabled", havingValue = "true")
public class RedisStartupConnectivityCheck implements ApplicationRunner {

	static final String TEST_KEY = "bizcart:redis:test";
	static final String TEST_VALUE = "Redis connected successfully";
	static final Duration TEST_TTL = Duration.ofMinutes(5);

	private static final Logger log = LoggerFactory.getLogger(RedisStartupConnectivityCheck.class);

	private final StringRedisTemplate redisTemplate;

	public RedisStartupConnectivityCheck(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		redisTemplate.opsForValue().set(TEST_KEY, TEST_VALUE, TEST_TTL);
		String storedValue = redisTemplate.opsForValue().get(TEST_KEY);
		log.info("Redis startup connectivity check: key={}, value={}", TEST_KEY, storedValue);
	}
}
