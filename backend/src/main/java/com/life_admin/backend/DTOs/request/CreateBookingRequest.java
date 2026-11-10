package com.life_admin.backend.DTOs.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingRequest {
	private String businessId;
	private String serviceId;
	private LocalDateTime appointmentStartTime;
	
	@Positive
	private Long appointmentDuration;
	private ZoneId zoneId;
}
