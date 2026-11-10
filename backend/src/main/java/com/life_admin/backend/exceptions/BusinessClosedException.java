package com.life_admin.backend.exceptions;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class BusinessClosedException extends RuntimeException {
	public BusinessClosedException(LocalTime openTime, LocalTime closeTime) {
		super("This business closed at this time. Please schedule an appointment during working hours (" +
				openTime+ " - " + closeTime + ").");
// Output: The business is currently closed. Please schedule an appointment during our working hours (09:00 - 17:00).
	
	}
	
	public BusinessClosedException(String message) {
		super(message);
	}
}
