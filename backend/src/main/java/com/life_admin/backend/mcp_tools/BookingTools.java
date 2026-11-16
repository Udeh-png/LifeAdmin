package com.life_admin.backend.mcp_tools;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.BookingResponse;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BookingService;
import jakarta.validation.constraints.Positive;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Component
public class BookingTools {
	
	@Autowired
	BookingService bookingService;
	
	@McpTool(name = "create_booking",
			description = "Book a service at a business. Only call this after the user has explicitly " +
					"confirmed the business, service and time. Use ids from search_businesses.")
	public CreateBookingResponse createBooking(
			@McpToolParam(description = "Business id from search_businesses") String businessId,
			@McpToolParam(description = "Service id from search_businesses") String serviceId,
			@McpToolParam(description = "Start time, ISO-8601 without an offset, e.g. 2026-11-20T09:00")
			LocalDateTime startTime,
			@McpToolParam(description = "Duration in minutes, required for range services", required = false) Long minutes) {
		
		MyUserDetails user = (MyUserDetails) Objects.requireNonNull(SecurityContextHolder.getContext()
				.getAuthentication()).getPrincipal();
		
		CreateBookingRequest req = new CreateBookingRequest();
		req.setBusinessId(businessId);
		req.setServiceId(serviceId);
		req.setAppointmentStartTime(startTime);
		req.setAppointmentDuration(minutes);
		
		return bookingService.createBooking(req, user);
	}
	
	@McpTool(name = "my_bookings",
			description = "List the logged-in user's bookings. Every filter is optional, so call it with no " +
					"arguments to get all of them. Times are Nigerian time.")
	public List<BookingResponse> myBookings(
			@McpToolParam(description = "Part of the service name, e.g. 'vaccination'. Case-insensitive. Leave empty to ignore.",
					required = false) String serviceName,
			
			@McpToolParam(description = "Part of the user name, e.g. 'vaccination'. Case-insensitive. Leave empty to ignore.",
					required = false) String userName,
			
			@McpToolParam(description = "Part of the business name, e.g. 'Lagos Medical'. Case-insensitive. Leave empty to ignore.",
					required = false) String businessName,
			
			@McpToolParam(description = "Booking status: CONFIRMED, CANCELLED or PENDING. Leave empty for all statuses.",
					required = false) String status,
			
			@McpToolParam(description = "Only bookings created at or after this time. Nigerian time, ISO-8601 without an offset, " +
					"e.g. 2026-11-01T00:00. Leave empty for no lower limit.", required = false) LocalDateTime createdFrom,
			
			@McpToolParam(description = "Only bookings created before this time. Nigerian time, ISO-8601 without an offset, " +
					"e.g. 2026-12-01T00:00. Leave empty for no upper limit.", required = false) LocalDateTime createdTo,
			
			@McpToolParam(description = "Only bookings starting at or after this time. Nigerian time, ISO-8601 without an offset, " +
					"e.g. 2026-11-20T09:00. Leave empty for no lower limit.", required = false) LocalDateTime startFrom,
			
			@McpToolParam(description = "Only bookings starting before this time. Nigerian time, ISO-8601 without an offset, " +
					"e.g. 2026-11-21T00:00. Leave empty for no upper limit.", required = false) LocalDateTime startTo) {
		
		MyUserDetails user = (MyUserDetails) Objects.requireNonNull(SecurityContextHolder.getContext()
				.getAuthentication()).getPrincipal();
		
		assert user != null;
		return bookingService.getBookings(serviceName, userName, businessName,
				createdFrom, createdTo, status,
				startFrom, startTo, user);
	}
	
	@McpTool(name = "cancel_booking",
			description = "Cancel one of the logged-in user's bookings. The booking stays in the records with status " +
					"CANCELLED, and its time becomes available again. Call only after the user has confirmed they " +
					"want to cancel this specific booking. Use the id from my_bookings.")
	public void cancelBooking(
			@McpToolParam(description = "Id of the booking to cancel, from my_bookings. Do not guess or make one up.")
			String bookingId) {
		MyUserDetails user = (MyUserDetails) Objects.requireNonNull(SecurityContextHolder.getContext()
				.getAuthentication()).getPrincipal();
		
		assert user != null;
		bookingService.cancelBooking(bookingId, user);
	}
}
