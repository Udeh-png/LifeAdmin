package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.enums.BookingStatus;
import com.life_admin.backend.exceptions.BusinessNotFoundException;
import com.life_admin.backend.exceptions.InvalidTimeRangeException;
import com.life_admin.backend.exceptions.ServiceNotFoundException;
import com.life_admin.backend.exceptions.SlotNotAvailableException;
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

@Service
public class BookingService {
	@Autowired
	private BookingRepo bookingRepo;
	
	@Autowired
	private BusinessAccountRepo businessRepo;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	public CreateBookingResponse createBooking(CreateBookingRequest createBookingRequest, MyUserDetails userDetails) {
		if (!createBookingRequest.getAppointmentEndTime().isAfter(createBookingRequest.getAppointmentStartTime()))
			throw new InvalidTimeRangeException();
		
		BusinessAccount businessAccount = businessRepo.findById(
				createBookingRequest.getBusinessId())
				.orElseThrow(BusinessNotFoundException::new
		);
		
		if (businessAccount.getServices().stream().noneMatch((service) -> service.getName().equalsIgnoreCase(createBookingRequest.getService())))
			throw new ServiceNotFoundException();
		
		if (businessAccount.getBookingApiUrl() != null) {
			// call booking api url
		} else {
			Criteria criteria = Criteria.where("_id").is(businessAccount.getId()).and("services").elemMatch(
					new Criteria().andOperator(
							Criteria.where("name").is(createBookingRequest.getService()),
							Criteria.where("availableSlots").is(createBookingRequest.getAppointmentStartTime())
					)
			);
			Update update = new Update().pull("service.$.availableSlots", createBookingRequest.getAppointmentStartTime());
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
				createBookingRequest.getService(),
				createBookingRequest.getAppointmentStartTime(),
				createBookingRequest.getAppointmentEndTime(),
				BookingStatus.CONFIRMED
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
				booking.getService(),
				booking.getAppointmentStartTime(),
				booking.getAppointmentEndTime()
		);
	}
}

// so the create_booking process works in 3 steps
// looks up the business using the name and other
// criteria confirms from the user
// and passes the id to this method