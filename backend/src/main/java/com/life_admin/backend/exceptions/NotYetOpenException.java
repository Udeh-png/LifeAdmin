package com.life_admin.backend.exceptions;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class NotYetOpenException extends RuntimeException {
	public NotYetOpenException(LocalDateTime openTime, ZoneId zoneId) {
		super("Business opens at " + openTime.toLocalTime() + " ZoneId:" + zoneId);
	}
}
