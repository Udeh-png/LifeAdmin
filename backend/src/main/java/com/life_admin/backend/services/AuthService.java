package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.LoginRequest;
import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service()
public class AuthService {
	
	@Autowired
	JwtService jwtService;
	
	public record Tokens (String accessToken, String refreshToken){
	}
	
	@Autowired
	AuthenticationManager authManager;
	
	public Tokens logIn(LoginRequest loginRequest) {
		Authentication authentication = authManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						loginRequest.getIdentifier(),
						loginRequest.getPassword()
				)
		);
		
		if (!authentication.isAuthenticated())
			throw new UsernameNotFoundException("Bad credentials");
		
		MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
		
		assert userDetails != null;
		
		String accessToken = jwtService.generateToken(userDetails, TokenRole.ACCESS_TOKEN);
		String refreshToken = jwtService.generateToken(userDetails, TokenRole.REFRESH_TOKEN);
		
		return new Tokens(accessToken, refreshToken);
	}
}
