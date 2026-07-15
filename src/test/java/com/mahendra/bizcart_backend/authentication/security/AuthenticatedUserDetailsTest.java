package com.mahendra.bizcart_backend.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class AuthenticatedUserDetailsTest {

	@Test
	void activeUserIsEnabledAndExposesSafeAccountProperties() {
		User user = user(AccountStatus.ACTIVE);

		AuthenticatedUserDetails details = new AuthenticatedUserDetails(user,
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

		assertThat(details.getId()).isEqualTo(7L);
		assertThat(details.getUsername()).isEqualTo("active@example.com");
		assertThat(details.getPassword()).isEqualTo("$2a$12$encodedPasswordPlaceholder");
		assertThat(details.isEnabled()).isTrue();
		assertThat(details.isAccountNonLocked()).isTrue();
		assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
	}

	@Test
	void blockedUserIsLockedAndInactiveUserIsDisabled() {
		AuthenticatedUserDetails blocked = new AuthenticatedUserDetails(user(AccountStatus.BLOCKED), List.of());
		AuthenticatedUserDetails inactive = new AuthenticatedUserDetails(user(AccountStatus.INACTIVE), List.of());

		assertThat(blocked.isAccountNonLocked()).isFalse();
		assertThat(inactive.isEnabled()).isFalse();
	}

	@Test
	void unapprovedSellerIsNotSellerApproved() {
		User seller = user(AccountStatus.ACTIVE);
		seller.setUserType(UserType.SELLER);
		seller.setAdminApproved(false);

		AuthenticatedUserDetails details = new AuthenticatedUserDetails(seller, List.of());

		assertThat(details.isSellerApproved()).isFalse();
	}

	private User user(AccountStatus status) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 7L);
		user.setFirstName("Active");
		user.setLastName("User");
		user.setEmail("active@example.com");
		user.setPassword("$2a$12$encodedPasswordPlaceholder");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(status);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}
}
