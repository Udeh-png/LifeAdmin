package com.life_admin.backend.DTOs.response;

import com.life_admin.backend.models.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponse {
	private String bookingId;
	private String businessName;
	private String serviceName;
	private String appointmentStartTime; // startTime in business time zone
	private String appointmentEndTime; // endTime in business time zone
	
	public CreateBookingResponse(Booking b, ZoneId businessZoneId) {
		this.bookingId = b.getId();
		this.businessName = b.getBusinessName();
		this.serviceName = b.getServiceName();
		this.appointmentStartTime = b.getAppointmentStartTime().atZone(businessZoneId).toOffsetDateTime().toString();
		this.appointmentEndTime = b.getAppointmentEndTime().atZone(businessZoneId).toOffsetDateTime().toString();
	}
}
