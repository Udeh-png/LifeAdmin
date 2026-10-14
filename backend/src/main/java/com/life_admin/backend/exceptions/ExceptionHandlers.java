package com.life_admin.backend.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlers {
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<?> duplicateKeyExceptionHandler(DuplicateKeyException dke) {
		String message = "";
		if (dke.getMessage().contains("email"))
			message = "This email already already registered";
		else if (dke.getMessage().contains("username"))
			message = "This username is taken";
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
	}
	
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<?> authenticationExceptionHandler (AuthenticationException ax) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bad credentials"));
	}
	
	@ExceptionHandler(TokenNotFoundException.class)
	public ResponseEntity<?> tokenNotFoundExceptionHandler (TokenNotFoundException tnf) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", tnf.getMessage()));
	}
	
	@ExceptionHandler(BusinessNotFoundException.class)
	public ResponseEntity<?> businessFoundExceptionHandler (BusinessNotFoundException bnf) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No business found"));
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> catchAllHandler (Exception ex) {
		log.error("An unhandled Exception occurred: {}", ex.getMessage(), ex);
		return ResponseEntity.internalServerError().body(Map.of("message", "Something went wrong. Please try again later"));
	}
}
