package com.life_admin.backend.security;

import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.exceptions.InvalidTokenTypeException;
import com.life_admin.backend.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
	@Autowired
	JwtService jwtService;
	@Autowired
	MyUserDetailsService userDetailsService;
	@Autowired
	@Qualifier("handlerExceptionResolver")
	HandlerExceptionResolver handlerResolver;
	
	@NullMarked
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		try {
			String authorizationHeader = request.getHeader("Authorization");
			String token = null;
			String userEmail = null;
			
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				token = authorizationHeader.substring(7);
				userEmail = jwtService.extractClaim(token, Claims::getSubject);
			}
			
			if (token != null && jwtService.getTokenType(token).equals(TokenRole.REFRESH_TOKEN.getName()))
				throw new InvalidTokenTypeException("You sent a refresh token instead of an access token");
			
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
				
				UsernamePasswordAuthenticationToken userToken =
						new UsernamePasswordAuthenticationToken(userDetails, null, null);
				
				userToken.setDetails(userDetails);
				
				SecurityContextHolder.getContext()
						.setAuthentication(userToken);
				
			}
			
			filterChain.doFilter(request, response);
		} catch (Exception ex) {
			ModelAndView mav = handlerResolver.resolveException(request, response, null, ex);
			if (mav == null) {
				log.error("String does not know how to handle this exception: {}", ex.getMessage(), ex);
				throw ex;
			}
		}
	}
}
