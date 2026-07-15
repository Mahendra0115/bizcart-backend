package com.mahendra.bizcart_backend.authentication.config;

import com.mahendra.bizcart_backend.authentication.security.JwtAuthenticationFilter;
import com.mahendra.bizcart_backend.authentication.security.RestAccessDeniedHandler;
import com.mahendra.bizcart_backend.authentication.security.RestAuthenticationEntryPoint;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String API_AUTH_BASE = "/api/v1/auth";
	private static final String API_AUTH_LOGIN = API_AUTH_BASE + "/login";
	private static final String API_AUTH_REGISTER = API_AUTH_BASE + "/register";
	private static final String API_AUTH_FORGOT_PASSWORD = API_AUTH_BASE + "/forgot-password";
	private static final String API_AUTH_RESEND_VERIFICATION = API_AUTH_BASE + "/resend-verification";
	private static final String API_AUTH_REFRESH_TOKEN = API_AUTH_BASE + "/refresh-token";
	private static final String API_AUTH_LOGOUT = API_AUTH_BASE + "/logout";
	private static final String API_AUTH_LOGOUT_ALL = API_AUTH_BASE + "/logout-all";
	private static final String API_AUTH_RESET_PASSWORD = API_AUTH_BASE + "/reset-password";
	private static final String API_AUTH_VERIFY_EMAIL = API_AUTH_BASE + "/verify-email";
	private static final String API_AUTH_CHANGE_PASSWORD = API_AUTH_BASE + "/change-password";
	private static final String API_AUTH_ME = API_AUTH_BASE + "/me";
	private static final String API_AUTH_CSRF = API_AUTH_BASE + "/csrf";
	private static final String ACTUATOR_HEALTH = "/actuator/health/**";
	private static final String API_ADMIN = "/api/v1/admin/**";
	private static final String API_SELLER = "/api/v1/seller/**";
	private static final String API_CUSTOMER = "/api/v1/customer/**";
	private static final String API_ALL = "/api/**";
	private static final String ALL_PATHS = "/**";
	private static final String ROLE_ADMIN = "ADMIN";
	private static final String ROLE_SELLER = "SELLER";
	private static final String ROLE_CUSTOMER = "CUSTOMER";

	private final AuthenticationProperties authenticationProperties;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	public SecurityConfig(AuthenticationProperties authenticationProperties, JwtAuthenticationFilter jwtAuthenticationFilter,
			RestAuthenticationEntryPoint restAuthenticationEntryPoint,
			RestAccessDeniedHandler restAccessDeniedHandler) {
		this.authenticationProperties = authenticationProperties;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
		this.restAccessDeniedHandler = restAccessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setHeaderName(AppConstants.Auth.CSRF_HEADER);
		http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
				.csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
				.ignoringRequestMatchers(API_AUTH_REGISTER, API_AUTH_FORGOT_PASSWORD, API_AUTH_RESEND_VERIFICATION,
						API_AUTH_RESET_PASSWORD, API_AUTH_VERIFY_EMAIL, ACTUATOR_HEALTH))
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint)
				.accessDeniedHandler(restAccessDeniedHandler))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, ALL_PATHS).permitAll()
				.requestMatchers(ACTUATOR_HEALTH).permitAll()
				.requestMatchers(HttpMethod.GET, API_AUTH_CSRF).permitAll()
				.requestMatchers(HttpMethod.POST, API_AUTH_LOGIN, API_AUTH_REGISTER, API_AUTH_FORGOT_PASSWORD,
						API_AUTH_RESEND_VERIFICATION, API_AUTH_REFRESH_TOKEN, API_AUTH_RESET_PASSWORD,
						API_AUTH_VERIFY_EMAIL).permitAll()
				.requestMatchers(HttpMethod.POST, API_AUTH_LOGOUT).authenticated()
				.requestMatchers(HttpMethod.POST, API_AUTH_LOGOUT_ALL).authenticated()
				.requestMatchers(HttpMethod.PUT, API_AUTH_CHANGE_PASSWORD).authenticated()
				.requestMatchers(HttpMethod.GET, API_AUTH_ME).authenticated()
				.requestMatchers(API_ADMIN).hasRole(ROLE_ADMIN)
				.requestMatchers(API_SELLER).hasAnyRole(ROLE_ADMIN, ROLE_SELLER)
				.requestMatchers(API_CUSTOMER).hasAnyRole(ROLE_ADMIN, ROLE_CUSTOMER)
				.requestMatchers(API_ALL).authenticated()
				.anyRequest().permitAll())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(splitCsv(authenticationProperties.getCors().getAllowedOrigins()));
		corsConfiguration.setAllowedMethods(splitCsv(authenticationProperties.getCors().getAllowedMethods()));
		corsConfiguration.setAllowedHeaders(splitCsv(authenticationProperties.getCors().getAllowedHeaders()));
		corsConfiguration.setAllowCredentials(authenticationProperties.getCors().isAllowCredentials());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration(ALL_PATHS, corsConfiguration);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}

	private List<String> splitCsv(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
	}
}
