package com.mahendra.bizcart_backend.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("bizcart.admin.bootstrap")
public class AdminBootstrapProperties {
	private boolean enabled;
	private String firstName;
	private String lastName;
	private String username;
	private String email;
	private String password;
	public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getFirstName() { return firstName; } public void setFirstName(String firstName) { this.firstName = firstName; }
	public String getLastName() { return lastName; } public void setLastName(String lastName) { this.lastName = lastName; }
	public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
	public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
	public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
}
