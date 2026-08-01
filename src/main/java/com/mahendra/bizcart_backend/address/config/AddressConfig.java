package com.mahendra.bizcart_backend.address.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AddressProperties.class)
public class AddressConfig {
}
