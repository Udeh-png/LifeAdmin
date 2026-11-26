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
public class BookingResponse {
	private String bookingId;
	private Booking.UserInfo userInfo;
	private String businessName;
	private String serviceName;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Instant createdAt;
	
	public BookingResponse (Booking b, ZoneId businessZoneId) {
		bookingId = b.getId();
		userInfo = b.getUserInfo();
		businessName = b.getBusinessName();
		serviceName = b.getServiceName();
		startTime = b.getAppointmentStartTime().atZone(businessZoneId).toLocalDateTime();
		endTime = b.getAppointmentEndTime().atZone(businessZoneId).toLocalDateTime();
		createdAt = b.getCreatedAt();
	}
}
