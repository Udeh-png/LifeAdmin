package com.life_admin.backend.exceptions;

public class ServiceNameAlreadyExists extends RuntimeException {
	public ServiceNameAlreadyExists(String m) {
		super(m);
	}
}
