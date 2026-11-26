package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.BookingResponse;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.DTOs.response.PrepareBookingResponse;
import com.life_admin.backend.enums.BookingStatus;
import com.life_admin.backend.exceptions.*;
import com.life_admin.backend.models.Booking;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BookingRepo;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.redis_services.BookingInFlightRedisService;
import com.life_admin.backend.services.redis_services.PendingBookingRedisService;
import com.life_admin.backend.utils.ResolvedBookingRequest;
import com.mongodb.MongoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class BookingService {
	@Autowired
	private BookingRepo bookingRepo;
	@Autowired
	private BusinessAccountRepo businessRepo;
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private TransactionTemplate template;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private PendingBookingRedisService pendingBookingRedisService;
	@Autowired
	private BookingInFlightRedisService bookingInFlightRedisService;
	
	public ResolvedBookingRequest resolveBookingRequest(String businessId, String serviceId,
	                                                    Long duration, LocalDateTime requestedStartTime,
	                                                    String userId, ZoneId userZoneId) {
		BusinessAccount businessAccount = businessRepo.findById(
				businessId
		).orElseThrow(BusinessNotFoundException::new);
		
		BusinessAccount.Service service = businessAccount.getServices()
				.stream()
				.filter(
						s -> s.getId().equals(serviceId)
				).findFirst()
				.orElseThrow(ServiceNotFoundException::new);
		
		if (service.getSessionDuration() != null) {
			if (duration != service.getSessionDuration().toMinutes())
				throw new CannotBookException("This service is fixed at "
						+ service.getSessionDuration().toMinutes() + " mins");
		} else if (duration > service.getSessionMax().toMinutes() ||
				duration < service.getSessionMin().toMinutes()) {
			throw new CannotBookException("This service has a maximum duration of "
					+ service.getSessionMax().toMinutes()
					+ " mins and a minimum of "
					+ service.getSessionMin().toMinutes());
		}
		
		Duration requestedDuration = Duration.ofMinutes(duration);
		
		LocalDateTime requestedEndTime = requestedStartTime
				.plusSeconds(
						requestedDuration.toSeconds()
				);
		
		Query query = new Query().addCriteria(
				Criteria.where("userId")
						.is(userId)
						.and("appointmentStartTime")
						.lt(toInstant(requestedEndTime, userZoneId))
						.and("appointmentEndTIme")
						.gt(toInstant(requestedStartTime, userZoneId))
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("You already have an appointment scheduled during this time");
		
		checkWithinOpeningHours(businessAccount, service, requestedStartTime, requestedEndTime, userZoneId);
		
		return new ResolvedBookingRequest(businessAccount, service, requestedEndTime);
	}
	
	public CreateBookingResponse createBooking(CreateBookingRequest request, MyUserDetails user) {
		bookingInFlightRedisService.acquire(user.getId());
		// to prevent one user sending another booking request while one is in progress
		
		ResolvedBookingRequest resolvedRequest = resolveBookingRequest(
				request.getBusinessId(),
				request.getServiceId(),
				request.getAppointmentDuration(),
				request.getAppointmentStartTime(),
				user.getId(),
				user.getZoneId()
		);
		
		int attempts = 0;
		int MAX_RETRY_ATTEMPTS = 3;
		
		for (; ; ) {
			attempts++;
			try {
				return template.execute(status ->
						createBookingTransactional(
								resolvedRequest.getService(),
								user,
								resolvedRequest.getBusiness(),
								request.getAppointmentStartTime(),
								resolvedRequest.getRequestedEndTime()
						)
				);
			} catch (DataAccessException e) {
				if (!isTransient(e))
					throw e;
				
				if (attempts >= MAX_RETRY_ATTEMPTS)
					throw new CannotBookException("Cannot book this appointment at the moment, try again later");
				
				pauseThreadAndRetry();
			} finally {
				bookingInFlightRedisService.release(user.getId());
			}
		}
	}
	
	public CreateBookingResponse confirmBooking(String pendingId, MyUserDetails user) {
		bookingInFlightRedisService.acquire(user.getId());
		// to prevent one user sending another booking request while one is in progress
		
		CreateBookingRequest request = pendingBookingRedisService.claim(user.getId(), pendingId);
		
		assert request != null;
		
		ResolvedBookingRequest resolvedRequest = resolveBookingRequest(
				request.getBusinessId(),
				request.getServiceId(),
				request.getAppointmentDuration(),
				request.getAppointmentStartTime(),
				user.getId(),
				request.getUserZoneId()
		);
		
		int attempts = 0;
		int MAX_RETRY_ATTEMPTS = 3;
		
		for (; ; ) {
			attempts++;
			try {
				return template.execute(status ->
						createBookingTransactional(
								resolvedRequest.getService(),
								user,
								resolvedRequest.getBusiness(),
								request.getAppointmentStartTime(),
								resolvedRequest.getRequestedEndTime()
						)
				);
			} catch (DataAccessException e) {
				if (!isTransient(e))
					throw e;
				
				if (attempts >= MAX_RETRY_ATTEMPTS)
					throw new CannotBookException("");
				
				pauseThreadAndRetry();
			} finally {
				bookingInFlightRedisService.release(user.getId());
			}
		}
	}
	
	public PrepareBookingResponse prepareBooking(String businessId, String serviceId,
	                                             Long duration, LocalDateTime requestedStartTime,
	                                             MyUserDetails user) {
		ResolvedBookingRequest resolvedRequest = resolveBookingRequest(
				businessId,
				serviceId,
				duration,
				requestedStartTime,
				user.getId(),
				user.getZoneId()
		);
		
		Query query = Query.query(
				new Criteria().andOperator(
						Criteria.where("businessId").is(businessId),
						Criteria.where("status").ne(BookingStatus.CANCELLED),
						Criteria.where("appointmentEndTime").gt(toInstant(requestedStartTime, user.getZoneId())),
						Criteria.where("appointmentStartTime").lt(toInstant(resolvedRequest.getRequestedEndTime(), user.getZoneId()))
				)
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("Time slot not available, please select another time");
		
		BusinessAccount business = resolvedRequest.getBusiness();
		ZoneId userZone = user.getZoneId();
		ZoneId businessZone = business.getZoneId();
		LocalDateTime requestedEndTime = resolvedRequest.getRequestedEndTime();
		
		String pendingId = pendingBookingRedisService.save(new CreateBookingRequest(
				businessId,
				serviceId,
				requestedStartTime,
				user.getZoneId(),
				duration
		), user.getId());
		
		return new PrepareBookingResponse(
				pendingId,
				business.getName(),
				resolvedRequest.getService().getName(),
				requestedStartTime.atZone(userZone).toOffsetDateTime().toString(),
				requestedEndTime.atZone(userZone).toOffsetDateTime().toString(),
				userZone.toString(),
				requestedStartTime.atZone(userZone).toInstant().atZone(businessZone).toOffsetDateTime().toString(),
				requestedEndTime.atZone(userZone).toInstant().atZone(businessZone).toOffsetDateTime().toString(),
				business.getZoneId().toString()
		);
	}
	
	private CreateBookingResponse createBookingTransactional(BusinessAccount.Service service, MyUserDetails user,
	                                                         BusinessAccount businessAccount, LocalDateTime requestedStartTime,
	                                                         LocalDateTime requestedEndTime) {
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(businessAccount.getId())),
				new Update().inc("bookingSequence", 1),
				BusinessAccount.class
		); // this prevents race condition because if two different transactional sessions try to update a doc at once mongodb does not allow it
		
		Query query = Query.query(
				new Criteria().andOperator(
						Criteria.where("businessId").is(businessAccount.getId()),
						Criteria.where("status").ne(BookingStatus.CANCELLED),
						Criteria.where("appointmentEndTime").gt(toInstant(requestedStartTime, user.getZoneId())),
						Criteria.where("appointmentStartTime").lt(toInstant(requestedEndTime, user.getZoneId()))
				)
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("Time slot not available, please select another time");
		
		if (businessAccount.getBookingApiUrl() != null) {
			// call booking api url
		}
		
		Booking booking = (new Booking(
				user.getId(),
				businessAccount.getId(),
				businessAccount.getName(),
				service.getName(),
				service.getId(),
				requestedStartTime.atZone(user.getZoneId()).toInstant(),
				requestedEndTime.atZone(user.getZoneId()).toInstant(),
				BookingStatus.CONFIRMED,
				Instant.now(),
				Instant.now()
		));
		
		booking.setUserInfo(new Booking.UserInfo(
				user.getUsername(),
				user.getPhoneNumber(),
				user.getEmail()
		));
		bookingRepo.save(booking);
		
		return new CreateBookingResponse(booking, businessAccount.getZoneId());
	}
	
	public void checkWithinOpeningHours(BusinessAccount business, BusinessAccount.Service service,
	                                    LocalDateTime start, LocalDateTime end, ZoneId userZoneId) {
		if (!start.isBefore(end))
			throw new InvalidTimeRangeException("Invalid time range bruh!");
		
		BusinessAccount.WorkTime workTime = service.getWorkTime() != null
				? service.getWorkTime()
				: business.getWorkingTime();
		
		Instant startInstant = start.atZone(userZoneId).toInstant();
		Instant endInstant = end.atZone(userZoneId).toInstant();
		
		if (startInstant.isBefore(Instant.now()))
			throw new InvalidTimeRangeException("Are you dumb?");
		
		LocalDate bizDate = startInstant.atZone(business.getZoneId()).toLocalDate();
		BusinessAccount.DayHours dayHours = workTime.getDaysHoursMap().get(bizDate.getDayOfWeek());
		
		if (dayHours == null)
			throw new BusinessClosedException("Business is closed on " + bizDate.getDayOfWeek());
		
		Instant bizOpenInstant = bizDate.atTime(dayHours.openTime()).atZone(business.getZoneId()).toInstant();
		Instant bizCloseInstant = bizDate.atTime(dayHours.closeTime()).atZone(business.getZoneId()).toInstant();
		
		if (startInstant.isBefore(bizOpenInstant) || endInstant.isAfter(bizCloseInstant))
			throw new BusinessClosedException(bizOpenInstant.atZone(userZoneId).toLocalTime(), bizCloseInstant.atZone(userZoneId).toLocalTime());
	}
	
	private void pauseThreadAndRetry() {
		try {
			long base = 20L;
			long jitter = ThreadLocalRandom.current().nextLong(base);
			
			Thread.sleep(base + jitter);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ie);
		}
	}
	
	private boolean isTransient(Throwable t) {
		for (; t != null; t = t.getCause()) {
			if (t instanceof MongoException me && me.hasErrorLabel("TransientTransactionError"))
				return true;
		}
		
		return false;
	}
	
	public void cancelBooking(String id, MyUserDetails user) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		boolean isOwner = user.getId().equals(booking.getUserId());
		boolean isBusiness = user.getId().equals(booking.getBusinessId());
		
		if (!isBusiness && !isOwner)
			throw new BookingNotFoundException();
		
		if (booking.getAppointmentStartTime().isBefore(Instant.now())) {
			throw new CannotCancelBookingException("Cannot cancel an appointment after it has started");
		}
		
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(booking.getId())
						.and("status").is(BookingStatus.CONFIRMED)),
				new Update().set("status", BookingStatus.CANCELLED).set("updatedAt", Instant.now()),
				Booking.class); // change status to cancelled
	}
	
	public BookingResponse getBooking(String id, MyUserDetails user) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		
		if (!user.getId().equals(booking.getBusinessId()) && !user.getId().equals(booking.getUserId()))
			throw new BookingNotFoundException();
		
		BusinessAccount business = businessRepo.findById(booking.getBusinessId()).orElseThrow(BusinessNotFoundException::new);
		
		return new BookingResponse(booking, business.getZoneId());
	}
	
	public List<BookingResponse> getBookings(String serviceName, String username, String businessName,
	                                         LocalDateTime createdAtFrom, LocalDateTime createdAtTo, String status,
	                                         LocalDateTime startTimeFrom, LocalDateTime startTimeTo, MyUserDetails user) {
		
		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(new Criteria().orOperator(
				Criteria.where("userId").is(user.getId()),
				Criteria.where("businessId").is(user.getId())
		));
		
		if (serviceName != null)
			criteriaList.add(Criteria.where("serviceName").regex(serviceName, "i"));
		
		if (username != null)
			criteriaList.add(Criteria.where("userInfo.userName").regex(username, "i"));
		
		if (businessName != null)
			criteriaList.add(Criteria.where("businessName").regex(businessName, "i"));
		
		if (status != null)
			criteriaList.add(Criteria.where("status").is(status));
		
		timeRangeHelper(criteriaList, toInstant(createdAtFrom, user.getZoneId()), toInstant(createdAtTo, user.getZoneId()), "createdAt");
		
		timeRangeHelper(criteriaList, toInstant(startTimeFrom, user.getZoneId()), toInstant(startTimeTo, user.getZoneId()), "appointmentStartTime");
		
		if (criteriaList.isEmpty()) {
			List<Booking> bookings = bookingRepo.findAll();
			
			return bookingsToBookingResponses(bookings);
		}
		
		Criteria criteria = new Criteria().andOperator(criteriaList);
		Query query = Query.query(criteria);
		
		List<Booking> bookings = mongoTemplate.find(query, Booking.class);
		
		return bookingsToBookingResponses(bookings);
	}
	
	private List<BookingResponse> bookingsToBookingResponses (List<Booking> bookings) {
		Set<String> ids = bookings.stream().map(Booking::getBusinessId).collect(Collectors.toSet());
		
		Map<String, ZoneId> zoneIdMap = businessRepo.findAllById(ids).stream().collect(Collectors.toMap(BusinessAccount::getId, BusinessAccount::getZoneId));
		
		List<BookingResponse> bookingResponses = new ArrayList<>();
		
		for (Booking b : bookings) {
			bookingResponses.add(new BookingResponse(b, zoneIdMap.get(b.getBusinessId())));
		}
		
		return bookingResponses;
	}
	
	private void timeRangeHelper(List<Criteria> criteria, Instant from, Instant to, String field) {
		if (from == null && to == null)
			return;
		
		if (from != null && to != null && !to.isAfter(from))
			throw new InvalidTimeRangeException("Time range is invalid");
		
		Criteria c = Criteria.where(field);
		
		if (from != null) c.gte(from);
		if (to != null) c.lt(to);
		
		criteria.add(c);
	}
	
	private static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
		if (dateTime == null)
			return null;
		return dateTime.atZone(zoneId).toInstant();
	}
}
/*
{
  "businessId": "6aeb6095550204a005855d02",
  "serviceId": "svc-gym1-pt",
  "startTime": "2026-11-26T09:00",
  "minutes": 45
}
*/