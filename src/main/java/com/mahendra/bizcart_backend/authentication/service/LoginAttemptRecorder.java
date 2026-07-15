package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.entity.LoginAttempt;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptRecorder {

	private final LoginAttemptRepository loginAttemptRepository;

	public LoginAttemptRecorder(LoginAttemptRepository loginAttemptRepository) {
		this.loginAttemptRepository = loginAttemptRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(LoginAttempt loginAttempt) {
		loginAttemptRepository.save(loginAttempt);
	}
}
