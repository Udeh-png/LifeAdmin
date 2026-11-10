package com.life_admin.backend.utils;

import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OptionalStringValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalString {
	String message () default "Field cannot be empty or blank";
	
	Class<?>[] groups() default {};
	Class<?>[] payload() default {};
}
