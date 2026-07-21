package com.mahendra.bizcart_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BizcartBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BizcartBackendApplication.class, args);
	}

}
