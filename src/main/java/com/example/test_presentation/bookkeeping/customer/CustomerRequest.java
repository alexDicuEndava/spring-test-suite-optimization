package com.example.test_presentation.bookkeeping.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		@NotNull CustomerStatus status) {
}
