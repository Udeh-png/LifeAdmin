package com.life_admin.backend.DTOs.request;

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
	private Duration appointmentDuration;
	private ZoneId zoneId;
}
