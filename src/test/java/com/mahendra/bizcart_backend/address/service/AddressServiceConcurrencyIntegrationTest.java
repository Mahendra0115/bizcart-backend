package com.mahendra.bizcart_backend.address.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.address.entity.Address;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import com.mahendra.bizcart_backend.address.repository.AddressRepository;
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

@SpringBootTest
class AddressServiceConcurrencyIntegrationTest {

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
	void concurrentSetDefaultRequestsLeaveExactlyOneActiveDefaultAddress() throws Exception {
		User user = userRepository.save(user());
		Address currentDefault = address(user, "Current Default", true);
		Address firstCandidate = address(user, "First Candidate", false);
		Address secondCandidate = address(user, "Second Candidate", false);
		addressRepository.saveAllAndFlush(List.of(currentDefault, firstCandidate, secondCandidate));

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<?> first = executor.submit(() -> setDefaultAfterStart(start, user.getId(), firstCandidate.getId()));
			Future<?> second = executor.submit(() -> setDefaultAfterStart(start, user.getId(), secondCandidate.getId()));
			start.countDown();

			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);

			List<Address> activeAddresses =
					addressRepository.findAllByUserIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(user.getId());
			List<Address> defaultAddresses = activeAddresses.stream()
				.filter(Address::isDefaultAddress)
				.toList();

			assertThat(defaultAddresses).hasSize(1);
			assertThat(defaultAddresses.getFirst().getId())
				.isIn(firstCandidate.getId(), secondCandidate.getId());
		}
		finally {
			executor.shutdownNow();
		}
	}

	private void setDefaultAfterStart(CountDownLatch start, Long userId, Long addressId) {
		try {
			start.await();
			addressService.setDefault(userId, addressId);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Concurrent address update was interrupted", ex);
		}
	}

	private User user() {
		User user = new User();
		user.setFirstName("Address");
		user.setLastName("Concurrency");
		user.setUsername("address.concurrency");
		user.setEmail("address.concurrency@example.com");
		user.setPassword("encoded-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}

	private Address address(User user, String fullName, boolean defaultAddress) {
		Address address = new Address();
		address.setUser(user);
		address.setFullName(fullName);
		address.setPhone("+919876543210");
		address.setAddressLine1("B-101 Sector 62");
		address.setCity("Noida");
		address.setState("Uttar Pradesh");
		address.setPostalCode("201309");
		address.setCountry("India");
		address.setAddressType(AddressType.HOME);
		address.setDefaultAddress(defaultAddress);
		address.setDeleted(false);
		return address;
	}
}
