package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.LoginRequest;
import com.life_admin.backend.DTOs.response.LogInResponse;
import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.services.AuthService;
import com.life_admin.backend.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api/v1/auth")
public class AuthController {
	
	@Autowired
	AuthService authService;
	@Autowired
	JwtService jwtService;
	
	@PostMapping("/login")
	ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
		AuthService.Tokens tokens = authService.logIn(loginRequest);
		ResponseCookie refreshTokenCookie = ResponseCookie.from(TokenRole.REFRESH_TOKEN.getName(), tokens.refreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.sameSite("Strict")
				.maxAge(TokenRole.REFRESH_TOKEN.getMaxAgeSeconds())
				.build();
		
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
				.body(new LogInResponse(
						tokens.accessToken(),
						"Bearer",
						TokenRole.ACCESS_TOKEN.getMaxAgeSeconds()
				));
	}
}
