package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.BookingResponse;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.enums.BookingStatus;
import com.life_admin.backend.exceptions.*;
import com.life_admin.backend.models.Booking;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BookingRepo;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.DataFormatException;

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
	
	public CreateBookingResponse createBooking(CreateBookingRequest createBookingRequest, MyUserDetails userDetails) {
		if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_BUSINESS")))
			throw new CannotBookException("Businesses cannot create bookings, you knob");
		
		BusinessAccount businessAccount = businessRepo.findById(
						createBookingRequest.getBusinessId())
				.orElseThrow(BusinessNotFoundException::new
				);
		
		BusinessAccount.Service service = businessAccount.getServices()
				.stream()
				.filter(
						s -> s.getId().equals(createBookingRequest.getServiceId())
				).findFirst()
				.orElseThrow(ServiceNotFoundException::new);
		
		if (service.getSessionDuration() != null) {
			if (createBookingRequest.getAppointmentDuration().compareTo(service.getSessionDuration()) != 0)
				throw new CannotBookException("This service is fixed at "
						+ service.getSessionDuration().toMinutes() + " mins");
		} else if (createBookingRequest.getAppointmentDuration().compareTo(service.getSessionMax()) > 0 ||
				createBookingRequest.getAppointmentDuration().compareTo(service.getSessionMin()) < 0) {
			throw new CannotBookException("This service has a maximum duration of "
					+ service.getSessionMax().toMinutes()
					+ " mins and a minimum of "
					+ service.getSessionMin());
		}
		
		Instant requestedStartTime = createBookingRequest.getAppointmentStartTime();
		Instant requestedEndTime = requestedStartTime
				.plusSeconds(
						createBookingRequest.getAppointmentDuration().toSeconds()
				);
		
		checkWithinOpeningHours(businessAccount, service, requestedStartTime, requestedEndTime);
		
		int attempts = 0;
		int MAX_RETRY_ATTEMPTS = 3;
		
		for (; ; ) {
			attempts++;
			try {
				return template.execute(status ->
						createBookingTransactional(service, userDetails, businessAccount, createBookingRequest, requestedEndTime)
				);
			} catch (DataAccessException e) {
				if (!isTransient(e))
					throw e;
				
				if (attempts >= MAX_RETRY_ATTEMPTS)
					throw new CannotBookException("");
				
				pauseThreadAndRetry();
			}
		}
	}
	
	private CreateBookingResponse createBookingTransactional(BusinessAccount.Service service, MyUserDetails userDetails,
	                                                         BusinessAccount businessAccount, CreateBookingRequest createBookingRequest,
	                                                         Instant requestedEndTime) {
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(businessAccount.getId())),
				new Update().inc("bookingSequence", 1),
				BusinessAccount.class
		); // this prevents race condition because if two different transactional sessions try to update a doc at once mongodb does not allow it
		
		Query query = Query.query(
				new Criteria().andOperator(
						Criteria.where("businessId").is(createBookingRequest.getServiceId()),
						Criteria.where("status").not().is(BookingStatus.CANCELLED),
						Criteria.where("appointmentEndTime").gt(createBookingRequest.getAppointmentStartTime()),
						Criteria.where("appointmentStartTime").lt(requestedEndTime)
				)
		);
		
		Optional<Booking> bookingOpt = mongoTemplate.find(query, Booking.class)
				.stream()
				.findFirst();
		
		if (bookingOpt.isPresent())
			throw new SlotNotAvailableException();
		
		if (businessAccount.getBookingApiUrl() != null) {
			// call booking api url
		}
		
		Booking booking = (new Booking(
				userDetails.getId(),
				businessAccount.getId(),
				businessAccount.getName(),
				service.getName(),
				service.getId(),
				createBookingRequest.getAppointmentStartTime(),
				requestedEndTime,
				BookingStatus.CONFIRMED,
				Instant.now()
		));
		
		booking.setUserInfo(new Booking.UserInfo(
				userDetails.getUsername(),
				userDetails.getPhoneNumber(),
				userDetails.getEmail()
		));
		bookingRepo.save(booking);
		
		return new CreateBookingResponse(booking);
	}
	
	private void checkWithinOpeningHours(BusinessAccount business, BusinessAccount.Service service,
	                                     Instant start, Instant end) {
		if (start.isBefore(Instant.now()))
			throw new InvalidTimeRangeException("Are you dumb?");
		
		BusinessAccount.WorkTime rule = service.getWorkTime() != null
				? service.getWorkTime()
				: business.getWorkingTime();
		
		LocalDateTime s = start.atZone(business.getZoneId()).toLocalDateTime();
		LocalDateTime e = end.atZone(business.getZoneId()).toLocalDateTime();
		
		BusinessAccount.DayHours day = rule.getDaysHoursMap().get(s.getDayOfWeek());
		if (day == null)
			throw new BusinessAlreadyClosedException();    // closed that day
		
		if (s.toLocalTime().isBefore(day.openTime()))
			throw new NotYetOpenException();
		
		if (e.toLocalTime().isAfter(day.closeTime()))
			throw new BusinessAlreadyClosedException();
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
		
		if (booking.getAppointmentStartTime().isBefore(Instant.now()))
			throw new CannotCancelBookingException("Cannot cancel an appointment after it has started");
		
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(booking.getId())
						.and("status").is(BookingStatus.CONFIRMED)),
				new Update().set("status", BookingStatus.CANCELLED),
				Booking.class); // change status to cancelled
	}
	
	public BookingResponse getBooking(String id, MyUserDetails userDetails) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		
		if (!userDetails.getId().equals(booking.getBusinessId()) && !userDetails.getId().equals(booking.getUserId()))
			throw new BookingNotFoundException();
		
		return new BookingResponse(booking);
	}
	
	public List<BookingResponse> getBookings(String serviceName, String username, String businessName,
	                                         Instant createdAtStartTime, Instant createdAtEndTime, String createdAtOperator,
	                                         String status, Instant startTime, String startTimeOperation, MyUserDetails userDetails) {
		
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
		
		if (createdAtStartTime != null && createdAtEndTime == null) {
			criteriaList.add(applyTimeOperator(createdAtOperator, "createdAt", createdAtStartTime));
		}
		
		if (createdAtEndTime != null && createdAtStartTime != null) {
			if (!createdAtEndTime.isAfter(createdAtStartTime)) {
				throw new InvalidTimeRangeException("Time range is invalid");
			}
			
			criteriaList.add(Criteria.where("createdAt").gte(createdAtStartTime).lte(createdAtEndTime));
			// is on or after start time but on or before endtime
		}
		if (startTime != null) {
			criteriaList.add(applyTimeOperator(startTimeOperation, "appointmentStartTime", startTime));
		}
		
		Criteria criteria = new Criteria().andOperator(criteriaList);
		Query query = Query.query(criteria);
		
		return mongoTemplate.find(query, Booking.class).stream().map(BookingResponse::new).toList();
	}
	
	private Criteria applyTimeOperator(String op, String field, Instant value) {
		if (op == null)
			throw new NoOperatorException();
		
		return switch (op.toUpperCase()) {
			case "EQ" -> Criteria.where(field).is(value);
			case "GT" -> Criteria.where(field).gt(value); // after given date/time
			case "LT" -> Criteria.where(field).lt(value); // before given date/time
			case "GTE" -> Criteria.where(field).gte(value); // on or after given date/time
			case "LTE" -> Criteria.where(field).lte(value); // on or before given date/time
			
			default -> throw new IllegalArgumentException("Unknown operator: " + op);
		};
	}
}

// so the create_booking process works in 3 steps
// looks up the business using the name and other
// criteria confirms from the user
// and passes the id to this method