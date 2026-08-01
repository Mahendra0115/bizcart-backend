package com.mahendra.bizcart_backend.address.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bizcart.address")
public class AddressProperties {

	@Min(1)
	private int maxActiveAddresses = 20;

	public int getMaxActiveAddresses() {
		return maxActiveAddresses;
	}

	public void setMaxActiveAddresses(int maxActiveAddresses) {
		this.maxActiveAddresses = maxActiveAddresses;
	}
}
