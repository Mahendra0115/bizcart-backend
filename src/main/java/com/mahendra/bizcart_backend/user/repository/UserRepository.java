package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	String PARAM_EMAIL = "email";

	String FIND_BY_NORMALIZED_EMAIL = """
			select u
			from User u
			where u.email = :email
			""";

	@Query(FIND_BY_NORMALIZED_EMAIL)
	Optional<User> findByNormalizedEmail(@Param(PARAM_EMAIL) String email);

	boolean existsByEmail(String email);
}
