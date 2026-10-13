package com.life_admin.backend.controllers;

import com.life_admin.backend.DTOs.request.LoginRequest;
import com.life_admin.backend.DTOs.request.RegistrationRequest;
import com.life_admin.backend.DTOs.response.LogInResponse;
import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.exceptions.TokenNotFoundException;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.UserRepository;
import com.life_admin.backend.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	
	@Autowired
	AuthService authService;
	
	@PostMapping("/register")
	ResponseEntity<?> register(@RequestBody RegistrationRequest regRequest) {
		authService.register(regRequest);
		return ResponseEntity.ok("created");
	}
	
	@PostMapping("/token/refresh")
	ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
		Cookie refreshTokenCookie = WebUtils.getCookie(request, TokenRole.REFRESH_TOKEN.getName());
		if (refreshTokenCookie == null)
			throw new TokenNotFoundException();
		
		String refreshToken = refreshTokenCookie.getValue();
		
		String accessToken = authService.refreshAccessToken(refreshToken);
		
		return ResponseEntity.ok(new LogInResponse(
				accessToken,
				"Bearer",
				TokenRole.ACCESS_TOKEN.getMaxAgeSeconds()
		));
	}
	
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
