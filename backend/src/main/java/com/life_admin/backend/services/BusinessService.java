package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.response.BusinessResponse;

import com.life_admin.backend.enums.BookingStatus;
import com.life_admin.backend.exceptions.BusinessNotFoundException;
import com.life_admin.backend.exceptions.ServiceNotFoundException;
import com.life_admin.backend.models.Booking;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BookingRepo;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.utils.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BusinessService {
	@Autowired
	BusinessAccountRepo businessRepo;
	@Autowired
	BookingRepo bookingRepo;
	@Autowired
	MongoTemplate mongoTemplate;
	
	public List<BusinessResponse> search(String name, String category, String location, List<String> services, String operator) {
		List<Criteria> criteriaList = new ArrayList<>();
		if (name != null)
			criteriaList.add(Criteria.where("name").regex(name, "i"));
		
		if (category != null)
			criteriaList.add(Criteria.where("category").is(category));
		
		if (location != null)
			criteriaList.add(Criteria.where("location").regex(location, "i"));
		
		if (services != null && !services.isEmpty()) {
			if ("AND".equalsIgnoreCase(operator))
				services.forEach((s) -> criteriaList.add(Criteria.where("services.name").regex(s, "i")));
			else if ("OR".equalsIgnoreCase(operator)) {
				List<Criteria> serviceList = new ArrayList<>();
				services.forEach((s) -> serviceList.add(Criteria.where("service.name").regex(s, "i")));
				
				criteriaList.add(new Criteria().orOperator(serviceList));
			}
		}
		
		if (criteriaList.isEmpty())
			return businessRepo.findAll().stream().map(BusinessResponse::new).toList();
		
		Criteria criteria = new Criteria().andOperator(criteriaList);
		return mongoTemplate.find(
						Query.query(criteria),
						BusinessAccount.class,
						"businesses"
				)
				.stream()
				.map(BusinessResponse::new)
				.toList();
		
	}
	
	public BusinessResponse findWithId(String id) {
		BusinessAccount account = businessRepo.findById(id).orElseThrow(BusinessNotFoundException::new);
		return new BusinessResponse(account);
	}
	
	public List<Interval> getAvailableSlots(String businessId, String serviceId, LocalDate date, MyUserDetails user) {
		BusinessAccount businessAccount = businessRepo.findById(businessId).orElseThrow(BusinessNotFoundException::new);
		
		BusinessAccount.Service service = businessAccount.getServices()
				.stream()
				.filter(
						s -> s.getId().equals(serviceId)
				).findFirst()
				.orElseThrow(ServiceNotFoundException::new);
		
		ZoneId zone = businessAccount.getZoneId();
		
		Instant dayStart = date.atStartOfDay(zone).toInstant();
		Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
		
		Query query = Query.query(new Criteria().andOperator(
				Criteria.where("businessId").is(businessAccount.getId()),
				Criteria.where("status").ne(BookingStatus.CANCELLED),
				Criteria.where("appointmentStartTime").lt(dayEnd),
				Criteria.where("appointmentEndTime").gt(dayStart)));
		
		BusinessAccount.WorkTime workTime = service.getWorkTime() != null ? service.getWorkTime() : businessAccount.getWorkingTime();
		LocalTime openTime = workTime.getDaysHoursMap().get(date.getDayOfWeek()).openTime();
		LocalTime closeTime = workTime.getDaysHoursMap().get(date.getDayOfWeek()).closeTime();
		
		List<Booking> bookings = mongoTemplate.find(query, Booking.class);
		
		List<Interval> freeTimeIntervals = new ArrayList<>();
		
		List<Interval> bookingIntervals = bookings.stream()
				.map(
						booking -> new Interval(
										LocalTime.ofInstant(booking.getAppointmentStartTime(), zone),
										LocalTime.ofInstant(booking.getAppointmentEndTime(), zone)
								)
				).toList();
		
		LocalTime currentStart = openTime;
		
		for (Interval i : bookingIntervals.stream().sorted(Comparator.comparing(Interval::start)).toList()) {
			if (i.start().isAfter(currentStart)) {
				freeTimeIntervals.add(new Interval(currentStart, i.start()));
				currentStart = i.end();
			}
		}
		
		if (currentStart.isBefore(closeTime))
			freeTimeIntervals.add(new Interval(currentStart, closeTime));
		
		return freeTimeIntervals;
	}
}
