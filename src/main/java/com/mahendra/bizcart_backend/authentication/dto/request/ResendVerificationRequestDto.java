package com.mahendra.bizcart_backend.authentication.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequestDto(@NotBlank @Email String email) {
}
