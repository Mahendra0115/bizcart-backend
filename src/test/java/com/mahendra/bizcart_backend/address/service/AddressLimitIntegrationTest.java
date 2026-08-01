package com.mahendra.bizcart_backend.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressResponseDto;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import com.mahendra.bizcart_backend.address.repository.AddressRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "bizcart.address.max-active-addresses=2")
class AddressLimitIntegrationTest {

	@Autowired
	private AddressService addressService;

	@Autowired
	private AddressRepository addressRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		addressRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createRejectsBeyondConfiguredActiveLimitAndSoftDeleteReleasesCapacity() {
		User user = userRepository.save(user("limit"));
		AddressResponseDto first = addressService.create(user.getId(), request("First"));
		addressService.create(user.getId(), request("Second"));

		assertThatThrownBy(() -> addressService.create(user.getId(), request("Rejected")))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getReason()).isEqualTo(AppConstants.Address.LIMIT_EXCEEDED);
			});

		addressService.delete(user.getId(), first.id());
		addressService.create(user.getId(), request("Replacement"));

		assertThat(addressRepository.countByUserIdAndDeletedFalse(user.getId())).isEqualTo(2);
	}

	@Test
	void concurrentCreatesCannotExceedConfiguredActiveLimit() throws Exception {
		User user = userRepository.save(user("concurrent-limit"));
		addressService.create(user.getId(), request("Existing"));
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Boolean> first = executor.submit(() -> createAfterStart(start, user.getId(), "Candidate One"));
			Future<Boolean> second = executor.submit(() -> createAfterStart(start, user.getId(), "Candidate Two"));
			start.countDown();

			long successCount = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
				.stream()
				.filter(Boolean::booleanValue)
				.count();

			assertThat(successCount).isEqualTo(1);
			assertThat(addressRepository.countByUserIdAndDeletedFalse(user.getId())).isEqualTo(2);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private boolean createAfterStart(CountDownLatch start, Long userId, String fullName) {
		try {
			start.await();
			addressService.create(userId, request(fullName));
			return true;
		}
		catch (ResponseStatusException exception) {
			assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(exception.getReason()).isEqualTo(AppConstants.Address.LIMIT_EXCEEDED);
			return false;
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Concurrent address creation was interrupted", exception);
		}
	}

	private CreateAddressRequestDto request(String fullName) {
		return new CreateAddressRequestDto(fullName, "+919876543210", "B-101 Sector 62", null,
				null, "Noida", "Uttar Pradesh", "201309", "India", AddressType.HOME, false);
	}

	private User user(String suffix) {
		User user = new User();
		user.setFirstName("Address");
		user.setLastName("Limit");
		user.setUsername("address." + suffix);
		user.setEmail("address." + suffix + "@example.com");
		user.setPassword("encoded-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}
}
