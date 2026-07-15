package com.mahendra.bizcart_backend.authentication.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LoginRequestDtoValidationTest {

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
	void acceptsPasswordWithoutRegistrationLengthOrComplexityPolicy() {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("customer@example.com");
		request.setPassword("short");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void rejectsBlankPassword() {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("customer@example.com");
		request.setPassword(" ");

		assertThat(validator.validate(request)).extracting(violation -> violation.getPropertyPath().toString())
			.contains("password");
	}
}
