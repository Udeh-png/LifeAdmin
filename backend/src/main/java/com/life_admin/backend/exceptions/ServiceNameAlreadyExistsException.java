package com.life_admin.backend.exceptions;

public class ServiceNameAlreadyExistsException extends RuntimeException {
	public ServiceNameAlreadyExistsException(String m) {
		super(m);
	}
}
