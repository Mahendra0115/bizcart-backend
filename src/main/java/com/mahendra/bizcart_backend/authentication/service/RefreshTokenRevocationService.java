package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenRevocationService {

	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshTokenRevocationService(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeActiveFamilyTokens(String tokenFamilyId, LocalDateTime revokedAt,
			RefreshTokenRevocationReason revocationReason) {
		refreshTokenRepository.revokeActiveTokensByFamilyId(tokenFamilyId, revokedAt, revocationReason, revokedAt);
	}
}
