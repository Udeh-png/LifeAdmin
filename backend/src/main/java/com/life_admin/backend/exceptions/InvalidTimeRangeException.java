package com.life_admin.backend.exceptions;

import lombok.Getter;

public class InvalidTimeRangeException extends RuntimeException {
	@Getter
	String message;
	public InvalidTimeRangeException(String message) {
		super(message);
	}
}
