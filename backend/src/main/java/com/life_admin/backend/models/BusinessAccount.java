package com.life_admin.backend.models;

import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document("businesses")
public class BusinessAccount {
	@Id
	private String id;
	@NonNull
	@Email
	private String email;
	@NonNull
	private String password;
	@NonNull
	private String name;
	@NonNull
	private String category;
	@NonNull
	private String location;
	@NonNull
	private List<Service> services;
	@NonNull
	private List<Instant> availableSlots;
	
	@Data
	@RequiredArgsConstructor
	@NoArgsConstructor
	public static class Service {
		@NonNull
		private String name;
		private String bookingApiUrl;
	}
}
