package com.example.test_presentation.bookkeeping.dto;

import com.example.test_presentation.bookkeeping.model.CustomerStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		@NotNull CustomerStatus status) {
}
