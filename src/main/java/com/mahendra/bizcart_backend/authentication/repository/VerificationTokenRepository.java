package com.mahendra.bizcart_backend.authentication.repository;

import com.mahendra.bizcart_backend.authentication.entity.VerificationToken;
import com.mahendra.bizcart_backend.authentication.entity.VerificationType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

	String PARAM_TOKEN_HASH = "tokenHash";
	String PARAM_USER_ID = "userId";
	String PARAM_VERIFICATION_TYPE = "verificationType";
	String PARAM_INVALIDATED_AT = "invalidatedAt";
	String PARAM_NOW = "now";
	String PARAM_CUTOFF = "cutoff";

	String FIND_BY_TOKEN_HASH = """
			select vt
			from VerificationToken vt
			where vt.tokenHash = :tokenHash
			""";

	String FIND_BY_TOKEN_HASH_AND_TYPE = """
			select vt
			from VerificationToken vt
			where vt.tokenHash = :tokenHash
			  and vt.verificationType = :verificationType
			""";

	String FIND_VALID_UNVERIFIED_BY_TOKEN_HASH = """
			select vt
			from VerificationToken vt
			where vt.tokenHash = :tokenHash
			  and vt.verifiedAt is null
			  and vt.invalidatedAt is null
			  and vt.expiresAt > :now
			""";

	String INVALIDATE_ACTIVE_TOKENS_BY_USER_ID_AND_TYPE = """
			update VerificationToken vt
			set vt.invalidatedAt = :invalidatedAt
			where vt.user.id = :userId
			  and vt.verificationType = :verificationType
			  and vt.verifiedAt is null
			  and vt.invalidatedAt is null
			  and vt.expiresAt > :now
			""";

	String DELETE_EXPIRED_TOKENS = """
			delete from VerificationToken vt
			where vt.expiresAt < :cutoff
			   or vt.verifiedAt < :cutoff
			   or vt.invalidatedAt < :cutoff
			""";

	@Query(FIND_BY_TOKEN_HASH)
	Optional<VerificationToken> findByTokenHash(@Param(PARAM_TOKEN_HASH) String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(FIND_BY_TOKEN_HASH_AND_TYPE)
	Optional<VerificationToken> findByTokenHashAndVerificationTypeForUpdate(
			@Param(PARAM_TOKEN_HASH) String tokenHash,
			@Param(PARAM_VERIFICATION_TYPE) VerificationType verificationType);

	@Query(FIND_VALID_UNVERIFIED_BY_TOKEN_HASH)
	Optional<VerificationToken> findValidUnverifiedByTokenHash(@Param(PARAM_TOKEN_HASH) String tokenHash,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(INVALIDATE_ACTIVE_TOKENS_BY_USER_ID_AND_TYPE)
	int invalidateActiveTokensByUserIdAndType(@Param(PARAM_USER_ID) Long userId,
			@Param(PARAM_VERIFICATION_TYPE) VerificationType verificationType,
			@Param(PARAM_INVALIDATED_AT) LocalDateTime invalidatedAt, @Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(DELETE_EXPIRED_TOKENS)
	int deleteExpiredTokens(@Param(PARAM_CUTOFF) LocalDateTime cutoff);
}
