package com.life_admin.backend.data;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DataSeeder {
	
	@Autowired
	private BusinessAccountRepo businessRepo;
	
	@Autowired
	private UserRepository userRepo;
	@Autowired
	PasswordEncoder encoder;
	
	@PostConstruct
	public void seed() {
		if (businessRepo.count() > 0) return; // Don't reseed
		
		// Clinics
		BusinessAccount clinic1 = new BusinessAccount();
		clinic1.setName("Lagos Medical Clinic");
		clinic1.setCategory("clinic");
		clinic1.setLocation("Lekki");
		clinic1.setEmail("clinic1@lifeadmin.com");
		clinic1.setPassword(encoder.encode("password123"));
		clinic1.setServices(List.of(
				new BusinessAccount.Service("Consultation with Doctors"),
				new BusinessAccount.Service("Lab Test"),
				new BusinessAccount.Service("Vaccination")
		));
		clinic1.setAvailableSlots(List.of(
				Instant.parse("2026-10-17T14:00:00Z"),
				Instant.parse("2026-10-17T15:00:00Z"),
				Instant.parse("2026-10-18T10:00:00Z"),
				Instant.parse("2026-10-18T11:00:00Z")
		));
		businessRepo.save(clinic1);
		
		BusinessAccount clinic2 = new BusinessAccount();
		clinic2.setName("Mainland Health Center");
		clinic2.setCategory("clinic");
		clinic2.setLocation("Mainland");
		clinic2.setEmail("clinic2@lifeadmin.com");
		clinic2.setPassword(encoder.encode("password123"));
		clinic2.setServices(List.of(
				new BusinessAccount.Service("Consultation with Doctors"),
				new BusinessAccount.Service("Lab Test"),
				new BusinessAccount.Service("Vaccination"),
				new BusinessAccount.Service("Free TB Test")
		));
		clinic2.setAvailableSlots(List.of(
				Instant.parse("2026-10-17T09:00:00Z"),
				Instant.parse("2026-10-17T16:00:00Z"),
				Instant.parse("2026-10-19T14:00:00Z")
		));
		businessRepo.save(clinic2);
		
		// Gyms
		BusinessAccount gym1 = new BusinessAccount();
		gym1.setName("FitHub Lagos");
		gym1.setCategory("gym");
		gym1.setLocation("Lekki");
		gym1.setEmail("gym1@lifeadmin.com");
		gym1.setPassword(encoder.encode("password123"));
		gym1.setServices(List.of(
				new BusinessAccount.Service("Personal Training Session"),
				new BusinessAccount.Service("Group Yoga Class"),
				new BusinessAccount.Service("Spin Class")
		));
		gym1.setAvailableSlots(List.of(
				Instant.parse("2026-10-17T06:00:00Z"),
				Instant.parse("2026-10-17T18:00:00Z"),
				Instant.parse("2026-10-18T06:00:00Z"),
				Instant.parse("2026-10-18T18:00:00Z")
		));
		businessRepo.save(gym1);
		
		// Test user
		UserAccount testUser = new UserAccount();
		testUser.setEmail("user@example.com");
		testUser.setPassword(encoder.encode("password123"));
		testUser.setUsername("Test User");
		userRepo.save(testUser);
	}
}