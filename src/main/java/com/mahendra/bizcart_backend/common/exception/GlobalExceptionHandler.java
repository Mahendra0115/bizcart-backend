package com.mahendra.bizcart_backend.common.exception;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();
		return error(HttpStatus.BAD_REQUEST, AppConstants.Auth.AUTH_VALIDATION_FAILED,
				AppConstants.Auth.VALIDATION_FAILED, request.getRequestURI(), fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = ex.getConstraintViolations()
			.stream()
			.map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
			.sorted(Comparator.comparing(FieldErrorResponse::field))
			.toList();
		return error(HttpStatus.BAD_REQUEST, AppConstants.Auth.AUTH_VALIDATION_FAILED,
				AppConstants.Auth.VALIDATION_FAILED, request.getRequestURI(), fieldErrors);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
		return error(status, authCode(status, message), message, request.getRequestURI(), List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String detail = mostSpecificMessage(ex);
		if (containsAny(detail, AppConstants.Indexes.UX_USERS_EMAIL, "users.email", "email")) {
			return error(HttpStatus.CONFLICT, AppConstants.Auth.AUTH_EMAIL_ALREADY_EXISTS,
					AppConstants.Auth.DUPLICATE_EMAIL, request.getRequestURI(), List.of());
		}
		if (containsAny(detail, AppConstants.Indexes.UX_USERS_USERNAME, "users.username", "username")) {
			return error(HttpStatus.CONFLICT, AppConstants.Auth.AUTH_USERNAME_ALREADY_EXISTS,
					AppConstants.Auth.DUPLICATE_USERNAME, request.getRequestURI(), List.of());
		}
		return error(HttpStatus.CONFLICT, AppConstants.Auth.AUTH_CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(),
				request.getRequestURI(), List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, AppConstants.Auth.AUTH_ACCESS_DENIED, "Access is denied",
				request.getRequestURI(), List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, AppConstants.Auth.AUTH_INTERNAL_ERROR,
				AppConstants.Auth.INTERNAL_ERROR, request.getRequestURI(), List.of());
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, String path,
			List<FieldErrorResponse> fieldErrors) {
		return ResponseEntity.status(status).body(ApiErrorResponseFactory.of(status, code, message, path, fieldErrors));
	}

	private String authCode(HttpStatus status, String message) {
		if (status == HttpStatus.UNAUTHORIZED && AppConstants.Auth.INVALID_CREDENTIALS.equals(message)) {
			return AppConstants.Auth.AUTH_INVALID_CREDENTIALS;
		}
		if (status == HttpStatus.UNAUTHORIZED && AppConstants.Auth.REFRESH_TOKEN_EXPIRED.equals(message)) {
			return AppConstants.Auth.AUTH_REFRESH_TOKEN_EXPIRED;
		}
		if (status == HttpStatus.UNAUTHORIZED && AppConstants.Auth.REFRESH_TOKEN_REVOKED.equals(message)) {
			return AppConstants.Auth.AUTH_REFRESH_TOKEN_REVOKED;
		}
		if (status == HttpStatus.UNAUTHORIZED) {
			return AppConstants.Auth.AUTH_INVALID_TOKEN;
		}
		if (status == HttpStatus.FORBIDDEN && AppConstants.Auth.ACCOUNT_NOT_ACTIVE.equals(message)) {
			return AppConstants.Auth.AUTH_ACCOUNT_INACTIVE;
		}
		if (status == HttpStatus.FORBIDDEN && AppConstants.Auth.EMAIL_NOT_VERIFIED.equals(message)) {
			return AppConstants.Auth.AUTH_EMAIL_NOT_VERIFIED;
		}
		if (status == HttpStatus.FORBIDDEN && AppConstants.Auth.SELLER_NOT_APPROVED.equals(message)) {
			return AppConstants.Auth.AUTH_SELLER_NOT_APPROVED;
		}
		if (status == HttpStatus.CONFLICT && AppConstants.Auth.DUPLICATE_EMAIL.equals(message)) {
			return AppConstants.Auth.AUTH_EMAIL_ALREADY_EXISTS;
		}
		if (status == HttpStatus.CONFLICT && AppConstants.Auth.DUPLICATE_USERNAME.equals(message)) {
			return AppConstants.Auth.AUTH_USERNAME_ALREADY_EXISTS;
		}
		return "AUTH_" + status.name();
	}

	private String mostSpecificMessage(DataIntegrityViolationException ex) {
		Throwable mostSpecificCause = ex.getMostSpecificCause();
		String message = mostSpecificCause == null ? ex.getMessage() : mostSpecificCause.getMessage();
		return message == null ? "" : message.toLowerCase(Locale.ROOT);
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}
}
