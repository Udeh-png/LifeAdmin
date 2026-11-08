package com.life_admin.backend.models;

import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private String location; // TODO: change location to an object with state, area, city, country and address fields
	@NonNull
	private List<Service> services;
	@NonNull
	private WorkTime workingTime;
	@NonNull
	private ZoneId zoneId;
	
	private boolean emailIsVerified;
	private boolean isPublished;
	
	private Long bookingSequence;
	
	private Duration sessionMax;
	private Duration sessionMin;
	
	private String bookingApiUrl;
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Service {
		private String id;
		private String name;
		
		private Duration sessionDuration;
		private Duration sessionMax;
		private Duration sessionMin;
		
		private WorkTime workTime;
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class WorkTime {
		private Map<DayOfWeek, DayHours> daysHoursMap;
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DayHours {
		private String open;
		private String close;
		
		public LocalTime openTime () {
			return LocalTime.parse(open);
		}
		
		public LocalTime closeTime () {
			return LocalTime.parse(close);
		}
	}
}
