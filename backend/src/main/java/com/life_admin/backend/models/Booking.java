package com.life_admin.backend.models;

import com.life_admin.backend.enums.BookingStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document("bookings")
public class Booking {
	@Id
	private String id;
	
	@Indexed
	@NonNull
	private String userId;
	
	@Indexed
	@NonNull
	private String businessId;
	@NonNull
	private String businessName;
	
	@NonNull
	private String serviceName;
	@NonNull
	private String serviceId;
	@NonNull
	private Instant appointmentStartTime;
	@NonNull
	private Instant appointmentEndTime;
	@NonNull
	private BookingStatus status;
	@NonNull
	private Instant createdAt;
	
	private UserInfo userInfo;
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class UserInfo {
		@NonNull
		private String userName;
		@NonNull
		private String phoneNumber;
		@NonNull
		private String email;
	}
}
// TODO: Create compound index on userUd and status