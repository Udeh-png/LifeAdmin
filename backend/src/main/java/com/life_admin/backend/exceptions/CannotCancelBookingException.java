package com.life_admin.backend.exceptions;

public class CannotCancelBookingException extends RuntimeException {
	public CannotCancelBookingException(String s) {
		super(s);
	}
}
