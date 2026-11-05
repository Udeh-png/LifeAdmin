package com.life_admin.backend.data;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.BusinessAccount.WorkTime;
import com.life_admin.backend.models.BusinessAccount.DayHours;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		ZoneId lagos = ZoneId.of("Africa/Lagos");
		
		// Clinic in Lekki: Mon-Fri 9:00-19:00
		BusinessAccount clinic1 = business("lekkiclinic@lifeadmin.test", pw,
				"Lagos Medical Clinic", "clinic", "Lekki", lagos, weekdays("09:00", "19:00"));
		clinic1.setServices(List.of(
				service("svc-clinic1-consult", "Consultation with Dr. Okafor", 20, 30, null),
				service("svc-clinic1-vacc", "Vaccination", 10, 15, null)
		));
		businessRepo.save(clinic1);
		
		// Clinic in Mainland: Mon-Fri 9:00-17:00, Saturday 9:00-13:00, closed Sunday
		WorkTime mainlandHours = weekdays("09:00", "17:00");
		mainlandHours.getDaysHoursMap().put(DayOfWeek.SATURDAY,
				new BusinessAccount.DayHours ("09:00", "13:00"));
		BusinessAccount clinic2 = business("mainlandclinic@lifeadmin.test", pw,
				"Mainland Health Center", "clinic", "Mainland", lagos, mainlandHours);
		clinic2.setServices(List.of(
				service("svc-clinic2-lab", "Lab Test", 30, 60, null),
				service("svc-clinic2-vacc", "Vaccination", 10, 15, null)
		));
		businessRepo.save(clinic2);
		
		// Gym in Lekki: Mon-Fri 6:00-21:00. Yoga has its own shorter hours.
		BusinessAccount gym1 = business("fithub@lifeadmin.test", pw,
				"FitHub Lagos", "gym", "Lekki", lagos, weekdays("06:00", "21:00"));
		gym1.setServices(List.of(
				service("svc-gym1-pt", "Personal Training Session", 45, 60, null),
				service("svc-gym1-yoga", "Yoga Lessons", 30, 45, weekdays("07:00", "10:00"))
		));
		businessRepo.save(gym1);
		
		// Gym in Mainland: Mon-Fri 8:00-20:00, Saturday 9:00-14:00, closed Sunday
		WorkTime sosoHours = weekdays("08:00", "20:00");
		sosoHours.getDaysHoursMap().put(DayOfWeek.SATURDAY,
				new DayHours("09:00","14:00"));
		BusinessAccount gym2 = business("sosogym@lifeadmin.test", pw,
				"Soso Gym", "gym", "Mainland", lagos, sosoHours);
		gym2.setServices(List.of(
				service("svc-gym2-swim", "Swimming Lessons", 30, 45, null),
				service("svc-gym2-spin", "Spin Class", 45, 45, null)
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
	
	private static BusinessAccount business(String email, String pw, String name, String category,
	                                        String location, ZoneId zone, WorkTime hours) {
		BusinessAccount b = new BusinessAccount();
		b.setEmail(email);
		b.setPassword(pw);
		b.setName(name);
		b.setBookingSequence(0L);
		b.setCategory(category);
		b.setLocation(location);
		b.setZoneId(zone);
		b.setWorkingTime(hours);
		return b;
	}
	
	private static BusinessAccount.Service service(String id, String name, int minMinutes,
	                                               int maxMinutes, WorkTime override) {
		BusinessAccount.Service s = new BusinessAccount.Service();
		s.setId(id);
		s.setName(name);
		s.setSessionMin(Duration.ofMinutes(minMinutes));
		s.setSessionMax(Duration.ofMinutes(maxMinutes));
		s.setWorkTime(override);   // null means "use the business's working time"
		return s;
	}
	
	// Monday to Friday with the same hours
	private static WorkTime weekdays(String openHour, String closeHour) {
		Map<DayOfWeek, DayHours> map = new HashMap<>();
		for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
				DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
			map.put(day, new DayHours(openHour, closeHour));
		}
		return new WorkTime(map);
	}
}