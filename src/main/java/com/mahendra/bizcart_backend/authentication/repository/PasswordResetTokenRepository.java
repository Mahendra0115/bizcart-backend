package com.mahendra.bizcart_backend.authentication.repository;

import com.mahendra.bizcart_backend.authentication.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	String PARAM_TOKEN_HASH = "tokenHash";
	String PARAM_USER_ID = "userId";
	String PARAM_INVALIDATED_AT = "invalidatedAt";
	String PARAM_NOW = "now";
	String PARAM_CUTOFF = "cutoff";

	String FIND_BY_TOKEN_HASH = """
			select prt
			from PasswordResetToken prt
			where prt.tokenHash = :tokenHash
			""";

	String FIND_VALID_UNUSED_BY_TOKEN_HASH = """
			select prt
			from PasswordResetToken prt
			where prt.tokenHash = :tokenHash
			  and prt.usedAt is null
			  and prt.invalidatedAt is null
			  and prt.expiresAt > :now
			""";

	String INVALIDATE_ACTIVE_TOKENS_BY_USER_ID = """
			update PasswordResetToken prt
			set prt.invalidatedAt = :invalidatedAt
			where prt.user.id = :userId
			  and prt.usedAt is null
			  and prt.invalidatedAt is null
			  and prt.expiresAt > :now
			""";

	String DELETE_EXPIRED_OR_INACTIVE_TOKENS = """
			delete from PasswordResetToken prt
			where prt.expiresAt < :cutoff
			   or prt.usedAt < :cutoff
			   or prt.invalidatedAt < :cutoff
			""";

	@Query(FIND_BY_TOKEN_HASH)
	Optional<PasswordResetToken> findByTokenHash(@Param(PARAM_TOKEN_HASH) String tokenHash);

	@Query(FIND_VALID_UNUSED_BY_TOKEN_HASH)
	Optional<PasswordResetToken> findValidUnusedByTokenHash(@Param(PARAM_TOKEN_HASH) String tokenHash,
			@Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(INVALIDATE_ACTIVE_TOKENS_BY_USER_ID)
	int invalidateActiveTokensByUserId(@Param(PARAM_USER_ID) Long userId,
			@Param(PARAM_INVALIDATED_AT) LocalDateTime invalidatedAt, @Param(PARAM_NOW) LocalDateTime now);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(DELETE_EXPIRED_OR_INACTIVE_TOKENS)
	int deleteExpiredOrInactiveTokens(@Param(PARAM_CUTOFF) LocalDateTime cutoff);
}
