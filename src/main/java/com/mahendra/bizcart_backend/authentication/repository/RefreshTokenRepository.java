package com.mahendra.bizcart_backend.authentication.repository;

import com.mahendra.bizcart_backend.authentication.entity.RefreshToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	String PARAM_TOKEN_HASH = "tokenHash";
	String PARAM_TOKEN_ID = "tokenId";
	String PARAM_TOKEN_FAMILY_ID = "tokenFamilyId";
	String PARAM_USER_ID = "userId";
	String PARAM_REVOKED_AT = "revokedAt";
	String PARAM_REVOCATION_REASON = "revocationReason";
	String PARAM_NOW = "now";
	String PARAM_CUTOFF = "cutoff";

	String FIND_BY_TOKEN_HASH = """
			select rt
			from RefreshToken rt
			where rt.tokenHash = :tokenHash
			""";

	String FIND_ACTIVE_BY_USER_ID = """
			select rt
			from RefreshToken rt
			where rt.user.id = :userId
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String FIND_ACTIVE_BY_TOKEN_FAMILY_ID = """
			select rt
			from RefreshToken rt
			where rt.tokenFamilyId = :tokenFamilyId
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String REVOKE_ACTIVE_TOKEN_ATOMICALLY = """
			update RefreshToken rt
			set rt.revokedAt = :revokedAt,
			    rt.revocationReason = :revocationReason
			where rt.id = :tokenId
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String REVOKE_ACTIVE_TOKEN_BY_HASH = """
			update RefreshToken rt
			set rt.revokedAt = :revokedAt,
			    rt.revocationReason = :revocationReason
			where rt.tokenHash = :tokenHash
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String REVOKE_ACTIVE_TOKENS_BY_USER_ID = """
			update RefreshToken rt
			set rt.revokedAt = :revokedAt,
			    rt.revocationReason = :revocationReason
			where rt.user.id = :userId
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String REVOKE_ACTIVE_TOKENS_BY_FAMILY_ID = """
			update RefreshToken rt
			set rt.revokedAt = :revokedAt,
			    rt.revocationReason = :revocationReason
			where rt.tokenFamilyId = :tokenFamilyId
			  and rt.revokedAt is null
			  and rt.expiresAt > :now
			""";

	String DELETE_EXPIRED_TOKENS = """
			delete from RefreshToken rt
			where rt.expiresAt < :cutoff
			""";

	@Query(FIND_BY_TOKEN_HASH)
	Optional<RefreshToken> findByTokenHash(@Param(PARAM_TOKEN_HASH) String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(FIND_BY_TOKEN_HASH)
	Optional<RefreshToken> findByTokenHashForUpdate(@Param(PARAM_TOKEN_HASH) String tokenHash);

	@Query(FIND_ACTIVE_BY_USER_ID)
	List<RefreshToken> findActiveByUserId(@Param(PARAM_USER_ID) Long userId, @Param(PARAM_NOW) LocalDateTime now);

	@Query(FIND_ACTIVE_BY_TOKEN_FAMILY_ID)
	List<RefreshToken> findActiveByTokenFamilyId(@Param(PARAM_TOKEN_FAMILY_ID) String tokenFamilyId,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(REVOKE_ACTIVE_TOKEN_ATOMICALLY)
	int revokeActiveTokenAtomically(@Param(PARAM_TOKEN_ID) Long tokenId,
			@Param(PARAM_REVOKED_AT) LocalDateTime revokedAt,
			@Param(PARAM_REVOCATION_REASON) RefreshTokenRevocationReason revocationReason,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(REVOKE_ACTIVE_TOKEN_BY_HASH)
	int revokeActiveTokenByHash(@Param(PARAM_TOKEN_HASH) String tokenHash,
			@Param(PARAM_REVOKED_AT) LocalDateTime revokedAt,
			@Param(PARAM_REVOCATION_REASON) RefreshTokenRevocationReason revocationReason,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(REVOKE_ACTIVE_TOKENS_BY_USER_ID)
	int revokeActiveTokensByUserId(@Param(PARAM_USER_ID) Long userId,
			@Param(PARAM_REVOKED_AT) LocalDateTime revokedAt,
			@Param(PARAM_REVOCATION_REASON) RefreshTokenRevocationReason revocationReason,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(REVOKE_ACTIVE_TOKENS_BY_FAMILY_ID)
	int revokeActiveTokensByFamilyId(@Param(PARAM_TOKEN_FAMILY_ID) String tokenFamilyId,
			@Param(PARAM_REVOKED_AT) LocalDateTime revokedAt,
			@Param(PARAM_REVOCATION_REASON) RefreshTokenRevocationReason revocationReason,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(DELETE_EXPIRED_TOKENS)
	int deleteExpiredTokens(@Param(PARAM_CUTOFF) LocalDateTime cutoff);
}
