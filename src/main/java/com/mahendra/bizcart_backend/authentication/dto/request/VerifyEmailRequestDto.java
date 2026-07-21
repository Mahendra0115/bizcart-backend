package com.mahendra.bizcart_backend.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequestDto(@NotBlank String token) {
}
