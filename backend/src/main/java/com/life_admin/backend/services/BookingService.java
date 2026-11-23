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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
	
	public ResolvedBookingRequest resolveBookingRequest(String businessId, String serviceId,
	                                                    Long duration, LocalDateTime requestedStartTime,
	                                                    String userId, ZoneId userZoneId) {
		boolean hasBookingInFLight = !redisTemplate.opsForValue().setIfAbsent("booking:in:flight:" + userId, 1, Duration.ofSeconds(30));
		
		if (hasBookingInFLight)
			throw new RuntimeException();
		// to prevent one user sending another booking request while one is in progress
		
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
						.lt(toInstant(requestedEndTime))
						.and("appointmentEndTIme")
						.gt(toInstant(requestedStartTime))
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("You already have an appointment scheduled during this time");
		
		checkWithinOpeningHours(businessAccount, service, requestedStartTime, requestedEndTime, userZoneId);
		
		return new ResolvedBookingRequest(businessAccount, service, requestedEndTime);
	}
	
	public CreateBookingResponse createBooking(CreateBookingRequest request, MyUserDetails userDetails) {
		ResolvedBookingRequest resolvedRequest = resolveBookingRequest(
				request.getBusinessId(),
				request.getServiceId(),
				request.getAppointmentDuration(),
				request.getAppointmentStartTime(),
				userDetails.getId(),
				userDetails.getZoneId()
		);
		
		int attempts = 0;
		int MAX_RETRY_ATTEMPTS = 3;
		
		for (; ; ) {
			attempts++;
			try {
				return template.execute(status ->
						createBookingTransactional(
								resolvedRequest.getService(),
								userDetails,
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
				redisTemplate.delete("booking:in:flight:" + userDetails.getId());
			}
		}
		
		
	}
	
	public CreateBookingResponse confirmBooking(String pendingId, MyUserDetails user) {
		CreateBookingRequest request = (CreateBookingRequest) redisTemplate.opsForValue().get("pending:booking:request:" + pendingId + ":" + user.getId());
		
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
			}finally {
				redisTemplate.executePipelined(new SessionCallback<>() {
					
					@SuppressWarnings({"unchecked", "NullableProblems"})
					@Override
					public Object execute(RedisOperations operations) throws DataAccessException {
						
						operations.delete("booking:in:flight:" + user.getId());
						operations.delete("pending:booking:request:" + pendingId + ":" + user.getId());
						
						return null;
					}
				});
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
						Criteria.where("appointmentEndTime").gt(requestedStartTime),
						Criteria.where("appointmentStartTime").lt(resolvedRequest.getRequestedEndTime())
				)
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("Time slot not available, please select another time");
		
		BusinessAccount business = resolvedRequest.getBusiness();
		ZoneId userZone = user.getZoneId();
		ZoneId businessZone = business.getZoneId();
		LocalDateTime requestedEndTime = resolvedRequest.getRequestedEndTime();
		
		String pendingId = UUID.randomUUID().toString();
		
		redisTemplate.opsForValue()
				.set("pending:booking:request:" + pendingId + ":" + user.getId(), new CreateBookingRequest(
						businessId,
						serviceId,
						requestedStartTime,
						user.getZoneId(),
						duration
				));
		
		redisTemplate.delete("booking:in:flight:" + user.getId());
		
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
	
	private CreateBookingResponse createBookingTransactional(BusinessAccount.Service service, MyUserDetails userDetails,
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
						Criteria.where("appointmentEndTime").gt(requestedStartTime),
						Criteria.where("appointmentStartTime").lt(requestedEndTime)
				)
		);
		
		if (mongoTemplate.exists(query, Booking.class))
			throw new ScheduleConflict("Time slot not available, please select another time");
		
		if (businessAccount.getBookingApiUrl() != null) {
			// call booking api url
		}
		
		Booking booking = (new Booking(
				userDetails.getId(),
				businessAccount.getId(),
				businessAccount.getName(),
				service.getName(),
				service.getId(),
				requestedStartTime.atZone(userDetails.getZoneId()).toInstant(),
				requestedEndTime.atZone(userDetails.getZoneId()).toInstant(),
				BookingStatus.CONFIRMED,
				Instant.now(),
				Instant.now()
		));
		
		booking.setUserInfo(new Booking.UserInfo(
				userDetails.getUsername(),
				userDetails.getPhoneNumber(),
				userDetails.getEmail()
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
	
	public void cancelBooking(String id, MyUserDetails userDetails) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		boolean isOwner = userDetails.getId().equals(booking.getUserId());
		boolean isBusiness = userDetails.getId().equals(booking.getBusinessId());
		
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
	
	public BookingResponse getBooking(String id, MyUserDetails userDetails) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		
		if (!userDetails.getId().equals(booking.getBusinessId()) && !userDetails.getId().equals(booking.getUserId()))
			throw new BookingNotFoundException();
		
		return new BookingResponse(booking);
	}
	
	public List<BookingResponse> getBookings(String serviceName, String username, String businessName,
	                                         LocalDateTime createdAtFrom, LocalDateTime createdAtTo, String status,
	                                         LocalDateTime startTimeFrom, LocalDateTime startTimeTo, MyUserDetails userDetails) {
		
		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(new Criteria().orOperator(
				Criteria.where("userId").is(userDetails.getId()),
				Criteria.where("businessId").is(userDetails.getId())
		));
		
		if (serviceName != null)
			criteriaList.add(Criteria.where("serviceName").regex(serviceName, "i"));
		
		if (username != null)
			criteriaList.add(Criteria.where("userInfo.userName").regex(username, "i"));
		
		if (businessName != null)
			criteriaList.add(Criteria.where("businessName").regex(businessName, "i"));
		
		if (status != null)
			criteriaList.add(Criteria.where("status").is(status));
		
		timeRangeHelper(criteriaList, createdAtFrom, createdAtTo, "createdAt");
		
		timeRangeHelper(criteriaList, startTimeFrom, startTimeTo, "appointmentStartTime");
		
		if (criteriaList.isEmpty())
			return bookingRepo.findAll().stream().map(BookingResponse::new).toList();
		
		Criteria criteria = new Criteria().andOperator(criteriaList);
		Query query = Query.query(criteria);
		
		return mongoTemplate.find(query, Booking.class).stream().map(BookingResponse::new).toList();
	}
	
	private void timeRangeHelper(List<Criteria> criteria, LocalDateTime from, LocalDateTime to, String field) {
		Instant start = toInstant(from);
		Instant end = toInstant(to);
		
		if (start == null && end == null)
			return;
		
		if (start != null && end != null && !end.isAfter(start))
			throw new InvalidTimeRangeException("Time range is invalid");
		
		Criteria c = Criteria.where(field);
		
		if (start != null) c.gte(start);
		if (end != null) c.lt(end);
		
		criteria.add(c);
	}
	
	private static Instant toInstant(LocalDateTime dateTime) {
		if (dateTime == null)
			return null;
		return dateTime.atZone(ZoneId.of("Africa/Lagos")).toInstant();
	}
}
