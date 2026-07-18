package com.mahendra.bizcart_backend.authentication.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.authentication.entity.VerificationToken;
import com.mahendra.bizcart_backend.authentication.entity.VerificationType;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlAuthenticationRepositoryIntegrationTest {
	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
		.withDatabaseName("bizcart").withUsername("bizcart").withPassword("bizcart");

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
	}

	@Autowired UserRepository users;
	@Autowired VerificationTokenRepository verificationTokens;

	@Test
	void persistsOnlyVerificationTokenHashAndLocksByHash() {
		User user = new User();
		user.setFirstName("MySQL"); user.setLastName("Test"); user.setUsername("mysql-test");
		user.setEmail("mysql@example.com"); user.setPassword("$2a$12$hash");
		user.setUserType(UserType.CUSTOMER); user.setStatus(AccountStatus.PENDING);
		user = users.saveAndFlush(user);
		VerificationToken token = new VerificationToken();
		token.setUser(user); token.setTokenHash("sha256-hash-only");
		token.setVerificationType(VerificationType.EMAIL_VERIFICATION);
		token.setExpiresAt(LocalDateTime.now().plusHours(24));
		verificationTokens.saveAndFlush(token);
		assertThat(verificationTokens.findByTokenHash("sha256-hash-only")).isPresent();
	}
}
