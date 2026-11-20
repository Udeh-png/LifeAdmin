package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.response.BusinessResponse;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BusinessService;

import com.life_admin.backend.utils.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {
	
	private record AvailableSlotsRequests (String serviceId, LocalDate date) {}
	
	@Autowired
	BusinessService businessService;
	
	@GetMapping("/search")
	ResponseEntity<?> search(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) List<String> services,
			@RequestParam(defaultValue = "AND") String operator
	) {
		List<BusinessResponse> businesses = businessService.search(name, category, location, services, operator);
		
		return ResponseEntity.ok(Map.of("businesses", businesses));
	}
	
	@GetMapping("/{id}")
	ResponseEntity<?> findWithId(@PathVariable String id) {
		BusinessResponse business = businessService.findWithId(id);
		return ResponseEntity.ok(business);
	}
	
	@GetMapping("/{businessId}/available-slots")
	ResponseEntity<?> getAvailableSlots (
			@PathVariable String businessId,
			@RequestBody AvailableSlotsRequests req,
			@AuthenticationPrincipal MyUserDetails user
	) {
		List<Interval> list = businessService.getAvailableSlots(businessId, req.serviceId(), req.date(), user);
		
		return ResponseEntity.ok(list);
	}
}
