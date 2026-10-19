package com.life_admin.backend.exceptions;

public class ServiceNotFoundException extends RuntimeException {
	public ServiceNotFoundException() {
		super("The business does not offer this service");
	}
}
