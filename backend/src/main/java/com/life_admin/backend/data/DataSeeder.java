package com.life_admin.backend.data;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
class DataSeeder {
	
	@Autowired
	private BusinessAccountRepo businessRepo;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PostConstruct
	public void seed() {
		if (businessRepo.count() > 0 || userRepo.count() > 0) return;
		
		String pw = passwordEncoder.encode("password123");
		
		// Business 1: clinic in Lekki
		BusinessAccount clinic1 = new BusinessAccount();
		clinic1.setEmail("lekkiclinic@lifeadmin.test");
		clinic1.setPassword(pw);
		clinic1.setName("Lagos Medical Clinic");
		clinic1.setCategory("clinic");
		clinic1.setLocation("Lekki");
		clinic1.setServices(List.of(
				new BusinessAccount.Service("svc-clinic1-consult", "Consultation",
						Duration.ofMinutes(30), List.of(
						Instant.parse("2026-10-20T14:00:00Z"),
						Instant.parse("2026-10-20T14:30:00Z"))),
				new BusinessAccount.Service("svc-clinic1-vacc", "Vaccination",
						Duration.ofMinutes(15), List.of(
						Instant.parse("2026-10-20T10:00:00Z"),
						Instant.parse("2026-10-21T10:00:00Z")))
		));
		businessRepo.save(clinic1);
		
		// Business 2: clinic in Mainland
		BusinessAccount clinic2 = new BusinessAccount();
		clinic2.setEmail("mainlandclinic@lifeadmin.test");
		clinic2.setPassword(pw);
		clinic2.setName("Mainland Health Center");
		clinic2.setCategory("clinic");
		clinic2.setLocation("Mainland");
		clinic2.setServices(List.of(
				new BusinessAccount.Service("svc-clinic2-lab", "Lab Test",
						Duration.ofMinutes(60), List.of(
						Instant.parse("2026-10-20T09:00:00Z"),
						Instant.parse("2026-10-22T09:00:00Z"))),
				new BusinessAccount.Service("svc-clinic2-vacc", "Vaccination",
						Duration.ofMinutes(15), List.of(
						Instant.parse("2026-10-20T16:00:00Z")))
		));
		businessRepo.save(clinic2);
		
		// Business 3: gym in Lekki
		BusinessAccount gym1 = new BusinessAccount();
		gym1.setEmail("fithub@lifeadmin.test");
		gym1.setPassword(pw);
		gym1.setName("FitHub Lagos");
		gym1.setCategory("gym");
		gym1.setLocation("Lekki");
		gym1.setServices(List.of(
				new BusinessAccount.Service("svc-gym1-pt", "Personal Training Session",
						Duration.ofMinutes(60), List.of(
						Instant.parse("2026-10-20T06:00:00Z"),
						Instant.parse("2026-10-20T18:00:00Z"))),
				new BusinessAccount.Service("svc-gym1-yoga", "Yoga Lessons",
						Duration.ofMinutes(45), List.of(
						Instant.parse("2026-10-21T07:00:00Z")))
		));
		businessRepo.save(gym1);
		
		// Business 4: gym in Mainland
		BusinessAccount gym2 = new BusinessAccount();
		gym2.setEmail("sosogym@lifeadmin.test");
		gym2.setPassword(pw);
		gym2.setName("Soso Gym");
		gym2.setCategory("gym");
		gym2.setLocation("Mainland");
		gym2.setServices(List.of(
				new BusinessAccount.Service("svc-gym2-swim", "Swimming Lessons",
						Duration.ofMinutes(45), List.of(
						Instant.parse("2026-10-20T08:00:00Z"),
						Instant.parse("2026-10-22T08:00:00Z"))),
				new BusinessAccount.Service("svc-gym2-spin", "Spin Class",
						Duration.ofMinutes(45), List.of(
						Instant.parse("2026-10-21T17:00:00Z")))
		));
		businessRepo.save(gym2);
		
		// Users
		UserAccount alice = new UserAccount();
		alice.setEmail("alice@lifeadmin.test");
		alice.setUsername("alice");
		alice.setPassword(pw);
		alice.setPhoneNumber("+2348012345678");
		userRepo.save(alice);
		
		UserAccount bob = new UserAccount();
		bob.setEmail("bob@lifeadmin.test");
		bob.setUsername("bob");
		bob.setPassword(pw);
		bob.setPhoneNumber("+2348098765432");
		userRepo.save(bob);
	}
}