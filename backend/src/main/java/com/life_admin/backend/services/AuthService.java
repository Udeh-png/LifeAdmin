package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.LoginRequest;
import com.life_admin.backend.DTOs.request.RegistrationRequest;
import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.exceptions.InvalidTokenTypeException;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.repositories.UserRepository;
import com.life_admin.backend.security.MyUserDetails;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service()
public class AuthService {
	
	@Autowired
	JwtService jwtService;
	
	public record Tokens(String accessToken, String refreshToken) {
	}
	
	@Autowired
	AuthenticationManager authManager;
	@Autowired
	UserRepository userRepo;
	@Autowired
	BusinessAccountRepo businessRepo;
	
	public void register (RegistrationRequest regReq) {
		String email = regReq.getEmail();
		String username = regReq.getUsername();
		String password = regReq.getPassword();
		
		userRepo.save((new UserAccount(email, username, password)));
	}
	
	public String refreshAccessToken(String refreshToken) {
		String userEmail = jwtService.extractClaim(refreshToken, Claims::getSubject);
		String tokenType = jwtService.getTokenType(refreshToken);
		
		if (tokenType.equals(TokenRole.ACCESS_TOKEN.getName()))
			throw new InvalidTokenTypeException("You sent an access token instead of a refresh token");
		
		UserAccount user = userRepo.findByEmail(userEmail).orElse(null);
		if (user != null)
			return jwtService.generateToken(new MyUserDetails(user), TokenRole.ACCESS_TOKEN);
		
		BusinessAccount businessAccount = businessRepo.findByEmail(userEmail).orElse(null);
		if (businessAccount != null)
			return jwtService.generateToken(new MyUserDetails(businessAccount), TokenRole.ACCESS_TOKEN);
		
		throw new UsernameNotFoundException("Account was deleted or sum idk");
	}
	
	public Tokens logIn(LoginRequest loginRequest) {
		Authentication authentication = authManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						loginRequest.getIdentifier(),
						loginRequest.getPassword()
				)
		);
		MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
		
		assert userDetails != null;
		
		String accessToken = jwtService.generateToken(userDetails, TokenRole.ACCESS_TOKEN);
		String refreshToken = jwtService.generateToken(userDetails, TokenRole.REFRESH_TOKEN);
		
		return new Tokens(accessToken, refreshToken);
	}
}
