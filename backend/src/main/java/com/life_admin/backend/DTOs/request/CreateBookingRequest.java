package com.life_admin.backend.DTOs.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingRequest {
	private String businessId;
	private String serviceId;
	private Instant appointmentStartTime;
	
	@Positive
	private Long appointmentDurationMins;
	private ZoneId zoneId;
}
