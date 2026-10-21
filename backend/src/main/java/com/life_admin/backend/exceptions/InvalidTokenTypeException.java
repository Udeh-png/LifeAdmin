package com.life_admin.backend.exceptions;

public class InvalidTokenTypeException extends RuntimeException {
	public InvalidTokenTypeException(String message) {
		super(message);
	}
}
