package com.life_admin.backend.exceptions;

public class PendingBookingNotFoundException extends RuntimeException{
	public PendingBookingNotFoundException() {
		super("That booking request has expired. Call prepare_booking again to get a new summary.");
	}
}
