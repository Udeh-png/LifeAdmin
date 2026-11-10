package com.life_admin.backend.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
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
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(NoOperatorException.class)
	public ResponseEntity<?> noOperatorExceptionHandler (NoOperatorException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(ServiceNotFoundException.class)
	public ResponseEntity<?> serviceNotFoundException(ServiceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(SlotNotAvailableException.class)
	public ResponseEntity<?> slotNotAvailableExceptionHandler (SlotNotAvailableException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<?> httpRequestMethodNotSupportedHandler (HttpRequestMethodNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(CannotCancelBookingException.class)
	public ResponseEntity<?> cannotCancelBookingExceptionHandler(CannotCancelBookingException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<?> emailAlreadyExistsExceptionHandler(EmailAlreadyExistsException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(InvalidServiceException.class)
	public ResponseEntity<?> invalidServiceExceptionHandler(InvalidServiceException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(ServiceNameAlreadyExistsException.class)
	public ResponseEntity<?> serviceNameAlreadyExistsExceptionHandler(ServiceNameAlreadyExistsException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> malformedBodyHandler(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(BusinessClosedException.class)
	public ResponseEntity<?> BusinessAlreadyClosedException(BusinessClosedException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<?> accessDeniedHandler(AccessDeniedException e) {
	 	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You can't do that"));
	}
	
	@ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
	public ResponseEntity<?> validationHandler(Exception e) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		
		if (e instanceof MethodArgumentNotValidException manv) {
			for (FieldError fe : manv.getBindingResult().getFieldErrors())
				fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
		} else if (e instanceof ConstraintViolationException cve) {
			for (ConstraintViolation<?> v : cve.getConstraintViolations())
				fieldErrors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
		}
		
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Some fields are invalid");
		body.put("fieldErrors", fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> catchAllHandler(Exception ex) {
		log.error("An unhandled Exception occurred: {}", ex.getMessage(), ex);
		return ResponseEntity.internalServerError().body(Map.of("message", "Something went wrong. Please try again later"));
	}
}
