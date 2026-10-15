package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.DTOs.response.CreateBookingResponse;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {
	
	@Autowired
	private BookingService bookingService;
	
	@PostMapping("/create")
	ResponseEntity<?> createBooking(@RequestBody CreateBookingRequest request, @AuthenticationPrincipal MyUserDetails userDetails) {
		CreateBookingResponse createBookingResponse = bookingService.createBooking(request, userDetails);
		
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(userDetails.getId())
				.toUri();
		
		return ResponseEntity.created(location).body(createBookingResponse);
	}
}
