package com.life_admin.backend.exceptions;

public class InvalidServiceException extends RuntimeException {
	public InvalidServiceException(String m) {
		super(m);
	}
}
