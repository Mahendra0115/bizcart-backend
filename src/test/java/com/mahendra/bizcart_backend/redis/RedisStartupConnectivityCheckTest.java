package com.mahendra.bizcart_backend.redis;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;

class RedisStartupConnectivityCheckTest {

	@Test
	void isDisabledByDefaultInLocalProfile() {
		try (AnnotationConfigApplicationContext context = contextWithStartupCheck(null)) {
			assertThat(context.getBeansOfType(RedisStartupConnectivityCheck.class)).isEmpty();
		}
	}

	@Test
	void isCreatedOnlyWhenExplicitlyEnabledInLocalProfile() {
		try (AnnotationConfigApplicationContext context = contextWithStartupCheck("true")) {
			assertThat(context.getBeansOfType(RedisStartupConnectivityCheck.class)).hasSize(1);
		}
	}

	@Test
	void writesExactTemporaryKeyAndReadsItBack() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		ApplicationArguments arguments = mock(ApplicationArguments.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(RedisStartupConnectivityCheck.TEST_KEY))
			.thenReturn(RedisStartupConnectivityCheck.TEST_VALUE);

		new RedisStartupConnectivityCheck(redisTemplate).run(arguments);

		InOrder calls = inOrder(valueOperations);
		calls.verify(valueOperations).set(RedisStartupConnectivityCheck.TEST_KEY,
				RedisStartupConnectivityCheck.TEST_VALUE, RedisStartupConnectivityCheck.TEST_TTL);
		calls.verify(valueOperations).get(RedisStartupConnectivityCheck.TEST_KEY);
	}

	private AnnotationConfigApplicationContext contextWithStartupCheck(String enabled) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().setActiveProfiles("local");
		if (enabled != null) {
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
					Map.of("bizcart.redis.startup-check.enabled", enabled)));
		}
		context.registerBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
		context.register(RedisStartupConnectivityCheck.class);
		context.refresh();
		return context;
	}
}
