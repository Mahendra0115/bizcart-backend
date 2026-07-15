package com.mahendra.bizcart_backend.authentication.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.user.enums.UserType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestDtoValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void acceptsCustomerPublicRegistration() {
		RegisterRequestDto request = validRequest(UserType.CUSTOMER);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void acceptsSellerPublicRegistration() {
		RegisterRequestDto request = validRequest(UserType.SELLER);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void rejectsAdminPublicRegistration() {
		RegisterRequestDto request = validRequest(UserType.ADMIN);

		assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
			.contains("allowedPublicRegistrationUserType");
	}

	private RegisterRequestDto validRequest(UserType userType) {
		RegisterRequestDto request = new RegisterRequestDto();
		request.setFirstName("Test");
		request.setLastName("User");
		request.setEmail("test@example.com");
		request.setPhone("+919999999999");
		request.setPassword("Password@123");
		request.setConfirmPassword("Password@123");
		request.setUserType(userType);
		return request;
	}
}
