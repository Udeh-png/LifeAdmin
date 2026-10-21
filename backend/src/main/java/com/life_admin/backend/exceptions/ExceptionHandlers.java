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
	public ResponseEntity<?> authenticationExceptionHandler(AuthenticationException ax) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bad credentials"));
	}
	
	@ExceptionHandler(TokenNotFoundException.class)
	public ResponseEntity<?> tokenNotFoundExceptionHandler(TokenNotFoundException tnf) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", tnf.getMessage()));
	}
	
	@ExceptionHandler(BusinessNotFoundException.class)
	public ResponseEntity<?> businessFoundExceptionHandler(BusinessNotFoundException bnf) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No business found"));
	}
	
	@ExceptionHandler(BookingNotFoundException.class)
	public ResponseEntity<?> bookingNotFoundExceptionHandler() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No booking was found"));
	}
	
	@ExceptionHandler(CannotBookException.class)
	public ResponseEntity<?> cannotBookException(CannotBookException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(InvalidTimeRangeException.class)
	public ResponseEntity<?> invalidTimeRangeExceptionHandler(InvalidTimeRangeException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(InvalidTokenTypeException.class)
	public ResponseEntity<?> invalidTokenTypeExceptionHandler(InvalidTokenTypeException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(NoOperatorException.class)
	public ResponseEntity<?> noOperatorExceptionHandler (NoOperatorException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(ServiceNotFoundException.class)
	public ResponseEntity<?> serviceNotFoundException(ServiceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(SlotNotAvailableException.class)
	public ResponseEntity<?> slotNotAvailableExceptionHandler (SlotNotAvailableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> catchAllHandler(Exception ex) {
		log.error("An unhandled Exception occurred: {}", ex.getMessage(), ex);
		return ResponseEntity.internalServerError().body(Map.of("message", "Something went wrong. Please try again later"));
	}
}
