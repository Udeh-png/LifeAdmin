package com.life_admin.backend.exceptions;

public class ScheduleConflict extends RuntimeException {
	public ScheduleConflict(String message) {
		super(message);
	}
}
