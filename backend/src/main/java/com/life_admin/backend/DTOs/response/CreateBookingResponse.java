package com.life_admin.backend.DTOs.response;

import com.life_admin.backend.models.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class CreateBookingResponse {
	private String bookingId;
	private String businessName;
	private String serviceName;
	private Instant appointmentStartTime;
	private Instant appointmentEndTime;
	
	public CreateBookingResponse(Booking b) {
		this.bookingId = b.getId();
		this.businessName = b.getBusinessName();
		this.serviceName = b.getServiceName();
		this.appointmentStartTime = b.getAppointmentStartTime();
		this.appointmentEndTime = b.getAppointmentEndTime();
	}
}
