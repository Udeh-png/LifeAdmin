package com.life_admin.backend.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingRequest {
	private String businessId;
	private String businessName;
	private String service;
	private Instant appointmentStartTime;
	private Instant appointmentEndTime;
}
