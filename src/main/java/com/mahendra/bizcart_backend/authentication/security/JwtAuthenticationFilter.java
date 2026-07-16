package com.mahendra.bizcart_backend.authentication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomUserDetailsService customUserDetailsService;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.customUserDetailsService = customUserDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = extractBearerToken(request);
		if (token != null && jwtTokenProvider.validateToken(token)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticateRequest(request, token);
		}
		filterChain.doFilter(request, response);
	}

	private String extractBearerToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	private void authenticateRequest(HttpServletRequest request, String token) {
		try {
			AuthenticatedUserDetails userDetails = loadCurrentUser(token);
			if (!isCurrentTokenValidForUser(token, userDetails)) {
				return;
			}
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
					userDetails.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (UsernameNotFoundException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
		}
	}

	private AuthenticatedUserDetails loadCurrentUser(String token) {
		UserDetails userDetails = customUserDetailsService.loadUserByUsername(jwtTokenProvider.extractEmail(token));
		if (!(userDetails instanceof AuthenticatedUserDetails authenticatedUserDetails)) {
			throw new IllegalArgumentException("Unsupported authenticated principal");
		}
		return authenticatedUserDetails;
	}

	private boolean isCurrentTokenValidForUser(String token, AuthenticatedUserDetails userDetails) {
		return userDetails.isEnabled()
				&& userDetails.isAccountNonLocked()
				&& userDetails.isEmailVerified()
				&& userDetails.isSellerApproved()
				&& userDetails.getId().equals(jwtTokenProvider.extractUserId(token))
				&& userDetails.getTokenVersion() == jwtTokenProvider.extractTokenVersion(token);
	}
}
