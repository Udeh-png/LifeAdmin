package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.AddServicesRequest;
import com.life_admin.backend.DTOs.request.ProfileSettingRequest;
import com.life_admin.backend.DTOs.request.UpdateHoursRequest;
import com.life_admin.backend.DTOs.request.UpdateServiceRequest;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BusinessAccountService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/v1/business-account")
public class BusinessAccountController {
	
	@Autowired
	private BusinessAccountService businessAccountService;
	
	@PostMapping("/add-service")
	@PreAuthorize("hasAuthority('BUSINESS')")
	ResponseEntity<?> addService(@RequestBody @Valid AddServicesRequest service, @AuthenticationPrincipal MyUserDetails userDetails) {
		businessAccountService.addService(service, userDetails);
		
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/profile")
	ResponseEntity<?> profileSetting(@RequestBody @Valid ProfileSettingRequest req, @AuthenticationPrincipal MyUserDetails userDetails) {
		
		businessAccountService.profileSetting(req, userDetails);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/update-service")
	ResponseEntity<?> updateService(@RequestBody @Valid UpdateServiceRequest req, @AuthenticationPrincipal MyUserDetails userDetails) {
		businessAccountService.updateService(req, userDetails);
		
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/my-business/hours")
	ResponseEntity<?> updateHours(@RequestBody @Valid UpdateHoursRequest req,
	                              @AuthenticationPrincipal MyUserDetails userDetails) {
		businessAccountService.updateHours(req.getWorkTime(), req.getZoneId(), userDetails);
		return ResponseEntity.noContent().build();
	}
}
