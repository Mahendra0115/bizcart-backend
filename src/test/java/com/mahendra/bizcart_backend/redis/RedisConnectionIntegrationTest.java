package com.mahendra.bizcart_backend.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = RedisConnectionIntegrationTest.RedisTestApplication.class,
		properties = {
				"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
						+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
		})
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "redis.integration.enabled", matches = "true",
		disabledReason = "Requires Redis on localhost:6379; enable with -Dredis.integration.enabled=true")
class RedisConnectionIntegrationTest {

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	void storesReadsAndExpiresTemporaryKey() {
		String key = "bizcart:test:connection:" + UUID.randomUUID();
		String value = "connected";
		Duration ttl = Duration.ofSeconds(30);

		redisTemplate.opsForValue().set(key, value, ttl);

		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(value);
		assertThat(redisTemplate.getExpire(key)).isPositive().isLessThanOrEqualTo(ttl.toSeconds());
		redisTemplate.delete(key);
	}

	@SpringBootApplication
	@EnableAutoConfiguration
	static class RedisTestApplication {
	}
}
