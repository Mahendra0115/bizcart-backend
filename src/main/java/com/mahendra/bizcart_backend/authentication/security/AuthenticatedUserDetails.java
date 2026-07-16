package com.mahendra.bizcart_backend.authentication.security;

import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUserDetails implements UserDetails {

	private final Long id;
	private final String email;
	private final String password;
	private final UserType userType;
	private final AccountStatus status;
	private final boolean emailVerified;
	private final boolean adminApproved;
	private final long tokenVersion;
	private final List<GrantedAuthority> authorities;

	public AuthenticatedUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.userType = user.getUserType();
		this.status = user.getStatus();
		this.emailVerified = user.isEmailVerified();
		this.adminApproved = user.isAdminApproved();
		this.tokenVersion = user.getTokenVersion();
		this.authorities = List.copyOf(authorities);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public boolean isSellerApproved() {
		return userType != UserType.SELLER || adminApproved;
	}

	public long getTokenVersion() {
		return tokenVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != AccountStatus.BLOCKED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return status == AccountStatus.ACTIVE;
	}
}
