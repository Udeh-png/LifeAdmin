package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.AddServicesRequest;
import com.life_admin.backend.DTOs.request.ProfileSettingRequest;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BusinessAccountService;
import jakarta.validation.Valid;
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
	@PreAuthorize("hasRole('BUSINESS')")
	ResponseEntity<?> addService (@RequestBody @Valid AddServicesRequest service, @AuthenticationPrincipal MyUserDetails userDetails) throws URISyntaxException {
		businessAccountService.addService(service, userDetails);
		
		return ResponseEntity.created(new URI("")).build();
	}
	
	@PutMapping("/profile")
	ResponseEntity<?> profileSetting (@RequestBody ProfileSettingRequest req, @AuthenticationPrincipal MyUserDetails userDetails) throws URISyntaxException {
		businessAccountService.profileSetting(req.getName(), req.getCategory(), req.getLocation(), req.getEmail(), userDetails);
		return ResponseEntity.created(new URI("")).build();
	}
}
