package com.life_admin.backend.exceptions;

public class SlotNotAvailableException extends RuntimeException {
	public SlotNotAvailableException() {
		super("No available timeslot for this service");
	}
}
