package com.mahendra.bizcart_backend.authentication.config;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("security-test")
@RestController
@RequestMapping("/api/v1")
class SecurityTestController {

	@PostMapping("/auth/login")
	ResponseEntity<Void> login() {
		return ResponseEntity.ok().build();
	}

	@PostMapping("/auth/refresh-token")
	ResponseEntity<Void> refreshToken() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/auth/me")
	ResponseEntity<Void> currentUser() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/auth/csrf")
	ResponseEntity<Void> csrfToken() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/admin/dashboard")
	ResponseEntity<Void> adminDashboard() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/customer/orders")
	@PreAuthorize("hasAuthority('ORDER_READ')")
	ResponseEntity<Void> customerOrders() {
		return ResponseEntity.ok().build();
	}
}
