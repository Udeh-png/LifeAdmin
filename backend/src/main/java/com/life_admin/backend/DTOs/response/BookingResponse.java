package com.life_admin.backend.DTOs.response;

import com.life_admin.backend.models.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
	private String bookingId;
	private Booking.UserInfo userInfo;
	private String businessName;
	private String serviceName;
	private Instant startTime;
	private Instant endTime;
	private Instant createdAt;
	
	public BookingResponse (Booking b) {
		bookingId = b.getId();
		userInfo = b.getUserInfo();
		businessName = b.getBusinessName();
		serviceName = b.getServiceName();
		startTime = b.getAppointmentStartTime();
		endTime = b.getAppointmentEndTime();
		createdAt = b.getCreatedAt();
	}
}
