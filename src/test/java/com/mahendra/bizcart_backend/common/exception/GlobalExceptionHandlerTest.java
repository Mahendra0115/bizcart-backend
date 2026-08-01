package com.mahendra.bizcart_backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

	@Test
	void mapsMissingBlockReasonToDedicatedUserErrorCode() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/users/admin/users/2/status");

		ResponseEntity<ApiErrorResponse> response = new GlobalExceptionHandler().handleResponseStatusException(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, AppConstants.User.BLOCK_REASON_REQUIRED), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo(AppConstants.User.BLOCK_REASON_REQUIRED_CODE);
		assertThat(response.getBody().message()).isEqualTo(AppConstants.User.BLOCK_REASON_REQUIRED);
	}
}
