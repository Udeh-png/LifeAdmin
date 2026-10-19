package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.enums.BookingStatus;
import com.life_admin.backend.exceptions.*;
import com.life_admin.backend.models.Booking;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BookingRepo;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class BookingService {
	@Autowired
	private BookingRepo bookingRepo;
	
	@Autowired
	private BusinessAccountRepo businessRepo;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Transactional
	public CreateBookingResponse createBooking(CreateBookingRequest createBookingRequest, MyUserDetails userDetails) {
		if (!createBookingRequest.getAppointmentEndTime().isAfter(createBookingRequest.getAppointmentStartTime()))
			throw new InvalidTimeRangeException("The start time you set is less than the end time");
		
		Duration requestedDuration = Duration.between(
				createBookingRequest.getAppointmentStartTime(),
				createBookingRequest.getAppointmentEndTime()
		);
		
		BusinessAccount businessAccount = businessRepo.findById(
				createBookingRequest.getBusinessId())
				.orElseThrow(BusinessNotFoundException::new
		);
		
		BusinessAccount.Service service = businessAccount.getServices()
				.stream()
				.filter(
						service1 -> service1.getId().equals(createBookingRequest.getServiceId())
				).findFirst()
				.orElseThrow();
		
		if (requestedDuration.compareTo(service.getLimit()) > 0)
			throw new InvalidTimeRangeException("The duration you set is more than the limit of this service");
		
		if (businessAccount.getBookingApiUrl() != null) {
			// call booking api url
		} else {
			Criteria criteria = Criteria.where("_id").is(businessAccount.getId()).and("services").elemMatch(
					new Criteria().andOperator(
							Criteria.where("id").is(createBookingRequest.getServiceId()),
							Criteria.where("availableSlots").is(createBookingRequest.getAppointmentStartTime())
					)
			);
			Update update = new Update().pull("services.$.availableSlots", createBookingRequest.getAppointmentStartTime());
			FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false).upsert(false);
			BusinessAccount claimed = mongoTemplate.findAndModify(Query.query(criteria), update, options, BusinessAccount.class);
			
			if (claimed == null) {
				throw new SlotNotAvailableException();
			}
		}
		
		Booking booking = (new Booking(
				userDetails.getId(),
				businessAccount.getId(),
				businessAccount.getName(),
				service.getName(),
				service.getId(),
				createBookingRequest.getAppointmentStartTime(),
				createBookingRequest.getAppointmentEndTime(),
				BookingStatus.CONFIRMED,
				Instant.now()
		));
		
		booking.setUserInfo(new Booking.UserInfo(
				userDetails.getUsername(),
				userDetails.getPhoneNumber(),
				userDetails.getEmail()
		));
		bookingRepo.save(booking);
		
		return new CreateBookingResponse(
				booking.getId(),
				businessAccount.getName(),
				booking.getServiceName(),
				booking.getAppointmentStartTime(),
				booking.getAppointmentEndTime()
		);
	}
	
	@Transactional
	public void cancelBooking (String id) {
		Booking booking = bookingRepo.findById(id).orElseThrow(BookingNotFoundException::new);
		
		Update update = new Update().addToSet("services.$.availableSlots", booking.getAppointmentStartTime());
		
		Query query = Query.query(Criteria.where("_id")
				.is(booking.getBusinessId())
				.and("services")
				.elemMatch(
						Criteria.where("id").is(booking.getServiceId())
				));
		
		mongoTemplate.updateFirst(query, update, BusinessAccount.class); // add the slot back
		
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(booking.getId())
						.and("status").is(BookingStatus.CONFIRMED)),
				new Update().set("status", BookingStatus.CANCELLED),
				Booking.class); // change status to cancelled
	}
}

// so the create_booking process works in 3 steps
// looks up the business using the name and other
// criteria confirms from the user
// and passes the id to this method