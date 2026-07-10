package com.mahendra.bizcart_backend.authentication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = extractBearerToken(request);
		if (token != null && jwtTokenProvider.validateToken(token)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					jwtTokenProvider.extractEmail(token), null, authorities(token));
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
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

	private List<SimpleGrantedAuthority> authorities(String token) {
		return jwtTokenProvider.extractRoles(token).stream()
			.map(CustomUserDetailsService::toRoleAuthority)
			.map(SimpleGrantedAuthority::new)
			.toList();
	}
}
