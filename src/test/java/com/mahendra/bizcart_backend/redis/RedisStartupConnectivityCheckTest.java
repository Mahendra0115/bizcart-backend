package com.mahendra.bizcart_backend.redis;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisStartupConnectivityCheckTest {

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
}
