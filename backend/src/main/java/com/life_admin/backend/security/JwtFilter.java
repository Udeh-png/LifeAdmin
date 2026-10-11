package com.life_admin.backend.security;

import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.exceptions.InvalidTokenTypeException;
import com.life_admin.backend.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
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
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
		try {
			String authorizationHeader = request.getHeader("Authorization");
			String token = null;
			String userEmail = null;
			
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				token = authorizationHeader.substring(7);
				userEmail = jwtService.extractClaim(token, Claims::getSubject);
			}
			
			if (jwtService.getTokenType(token).equals(TokenRole.REFRESH_TOKEN.getName()))
				throw new InvalidTokenTypeException();
			
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
				
				UsernamePasswordAuthenticationToken userToken =
						new UsernamePasswordAuthenticationToken(userDetails, null, null);
				
				userToken.setDetails(userDetails);
				
				SecurityContextHolder.getContext()
						.setAuthentication(userToken);
				
			}
			
			filterChain.doFilter(request, response);
		} catch (Exception e) {
			handlerResolver.resolveException(request, response, null, e);
		}
	}
}
