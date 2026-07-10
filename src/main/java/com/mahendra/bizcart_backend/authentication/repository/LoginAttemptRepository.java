package com.mahendra.bizcart_backend.authentication.repository;

import com.mahendra.bizcart_backend.authentication.entity.LoginAttempt;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

	String PARAM_EMAIL = "email";
	String PARAM_IP_ADDRESS = "ipAddress";
	String PARAM_FROM = "from";
	String PARAM_TO = "to";
	String PARAM_CUTOFF = "cutoff";

	String FIND_BY_EMAIL_AND_ATTEMPTED_AT_RANGE = """
			select la
			from LoginAttempt la
			where la.email = :email
			  and la.attemptedAt between :from and :to
			order by la.attemptedAt desc
			""";

	String FIND_BY_IP_ADDRESS_AND_ATTEMPTED_AT_RANGE = """
			select la
			from LoginAttempt la
			where la.ipAddress = :ipAddress
			  and la.attemptedAt between :from and :to
			order by la.attemptedAt desc
			""";

	String DELETE_OLD_ATTEMPTS = """
			delete from LoginAttempt la
			where la.attemptedAt < :cutoff
			""";

	@Query(FIND_BY_EMAIL_AND_ATTEMPTED_AT_RANGE)
	List<LoginAttempt> findByEmailAndAttemptedAtBetween(@Param(PARAM_EMAIL) String email,
			@Param(PARAM_FROM) LocalDateTime from, @Param(PARAM_TO) LocalDateTime to);

	@Query(FIND_BY_IP_ADDRESS_AND_ATTEMPTED_AT_RANGE)
	List<LoginAttempt> findByIpAddressAndAttemptedAtBetween(@Param(PARAM_IP_ADDRESS) String ipAddress,
			@Param(PARAM_FROM) LocalDateTime from, @Param(PARAM_TO) LocalDateTime to);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(DELETE_OLD_ATTEMPTS)
	int deleteOldAttempts(@Param(PARAM_CUTOFF) LocalDateTime cutoff);
}
