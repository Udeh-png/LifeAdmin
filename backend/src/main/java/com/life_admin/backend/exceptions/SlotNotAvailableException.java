package com.life_admin.backend.exceptions;

public class SlotNotAvailableException extends RuntimeException {
	public SlotNotAvailableException() {
		super("The timeslot you requested is not available");
	}
}
