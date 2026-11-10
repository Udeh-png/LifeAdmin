package com.life_admin.backend.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OptionalStringValidator implements ConstraintValidator<OptionalString, String> {
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null)
			return true;
		
		return !value.trim().isEmpty();
	}
}
