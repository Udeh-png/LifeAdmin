package com.life_admin.backend.exceptions;

public class CannotBookException extends RuntimeException {
	public CannotBookException() {
		super("Business cannot create bookings");
	}
}
