package com.mahendra.bizcart_backend.address.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.address.service.AddressService;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AddressService addressService;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private CustomUserDetailsService customUserDetailsService;

	@BeforeEach
	void authenticate() {
		AuthenticatedUserDetails principal = principal();
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createRejectsRestrictedUserIdField() throws Exception {
		assertCreateRejectsExtraField("\"userId\": 999");
	}

	@Test
	void updateRejectsRestrictedDeletedField() throws Exception {
		assertUpdateRejectsExtraField("\"deleted\": false");
	}

	@Test
	void updateRejectsDefaultAddressField() throws Exception {
		assertUpdateRejectsExtraField("\"defaultAddress\": true");
	}

	@Test
	void createRejectsUnknownTypoField() throws Exception {
		assertCreateRejectsExtraField("\"adressLine1\": \"typo\"");
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC", "1", "000000", "000000000000" })
	void createRejectsInvalidIndianPostalCode(String postalCode) throws Exception {
		mockMvc.perform(post(AppConstants.Address.API_BASE)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson(postalCode, "", true)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("postalCode"))
			.andExpect(jsonPath("$.fieldErrors[0].message")
				.value("Postal code must be a valid 6-digit Indian PIN code"));

		verifyNoInteractions(addressService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "ABC", "1", "000000", "000000000000" })
	void updateRejectsInvalidIndianPostalCode(String postalCode) throws Exception {
		mockMvc.perform(put(AppConstants.Address.API_BASE + "/1")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson(postalCode, "", false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("postalCode"))
			.andExpect(jsonPath("$.fieldErrors[0].message")
				.value("Postal code must be a valid 6-digit Indian PIN code"));

		verifyNoInteractions(addressService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "1234567890", "+14155552671", "98765", "+9198765432100" })
	void createRejectsInvalidIndianMobileNumber(String phone) throws Exception {
		mockMvc.perform(post(AppConstants.Address.API_BASE)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson("201309", "", true).replace("+919876543210", phone)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
			.andExpect(jsonPath("$.fieldErrors[0].message")
				.value("Phone number must be a valid Indian mobile number"));

		verifyNoInteractions(addressService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "1234567890", "+14155552671", "98765", "+9198765432100" })
	void updateRejectsInvalidIndianMobileNumber(String phone) throws Exception {
		mockMvc.perform(put(AppConstants.Address.API_BASE + "/1")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson("201309", "", false).replace("+919876543210", phone)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
			.andExpect(jsonPath("$.fieldErrors[0].message")
				.value("Phone number must be a valid Indian mobile number"));

		verifyNoInteractions(addressService);
	}

	private void assertCreateRejectsExtraField(String extraField) throws Exception {
		mockMvc.perform(post(AppConstants.Address.API_BASE)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson("201309", ",\n  " + extraField, true)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.VALIDATION_FAILED));

		verifyNoInteractions(addressService);
	}

	private void assertUpdateRejectsExtraField(String extraField) throws Exception {
		mockMvc.perform(put(AppConstants.Address.API_BASE + "/1")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(addressJson("201309", ",\n  " + extraField, false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.VALIDATION_FAILED));

		verifyNoInteractions(addressService);
	}

	private String addressJson(String postalCode, String extraJson, boolean includeDefaultAddress) {
		String defaultAddressJson = includeDefaultAddress ? ",\n  \"defaultAddress\": false" : "";
		return """
				{
				  "fullName": "Test Customer",
				  "phone": "+919876543210",
				  "addressLine1": "B-101 Sector 62",
				  "city": "Noida",
				  "state": "Uttar Pradesh",
				  "postalCode": "%s",
				  "country": "India",
				  "addressType": "HOME"%s%s
				}
				""".formatted(postalCode, defaultAddressJson, extraJson);
	}

	private AuthenticatedUserDetails principal() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 1L);
		user.setEmail("user@example.com");
		user.setPassword("encoded");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		return new AuthenticatedUserDetails(user, java.util.List.of());
	}
}
