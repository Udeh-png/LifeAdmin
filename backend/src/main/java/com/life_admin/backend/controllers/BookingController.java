package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.BookingResponse;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {
	
	@Autowired
	private BookingService bookingService;
	
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('USER')")
	ResponseEntity<?> createBooking(@RequestBody CreateBookingRequest request, @AuthenticationPrincipal MyUserDetails userDetails) {
		CreateBookingResponse createBookingResponse = bookingService.createBooking(request, userDetails);
		
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(userDetails.getId())
				.toUri();
		
		return ResponseEntity.created(location).body(createBookingResponse);
	}
	
	@DeleteMapping("/{id}")
	ResponseEntity<?> cancelBooking(@PathVariable String id, @AuthenticationPrincipal MyUserDetails userDetails) {
		bookingService.cancelBooking(id, userDetails);
		
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/{id}")
	ResponseEntity<?> getBooking(@PathVariable String id, @AuthenticationPrincipal MyUserDetails userDetails) {
		BookingResponse booking = bookingService.getBooking(id, userDetails);
		
		return ResponseEntity.ok(booking);
	}
	
	@GetMapping("/search")
	ResponseEntity<?> getBookings(
			@RequestParam(required = false) String serviceName,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) String businessName,
			@RequestParam(required = false) LocalDateTime createdAtStartTime,
			@RequestParam(required = false) LocalDateTime createdAtEndTime,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) LocalDateTime startTimeFrom,
			@RequestParam(required = false) LocalDateTime startTimeTo,
			@AuthenticationPrincipal MyUserDetails userDetails
	) {
		List<BookingResponse> bookings = bookingService.getBookings(
				serviceName,
				username,
				businessName,
				createdAtStartTime,
				createdAtEndTime,
				status,
				startTimeFrom,
				startTimeTo,
				userDetails
		);
		
		return ResponseEntity.ok(bookings);
	}
}
