package com.life_admin.backend.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingResponse {
	private String bookingId;
	private String businessName;
	private String service;
	private Instant appointmentStartTime;
	private Instant appointmentEndTime;
}
