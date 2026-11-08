package com.life_admin.backend.exceptions;

public class EmailAlreadyExistsException extends RuntimeException{
	public EmailAlreadyExistsException() {
		super("The email you entered is already used");
	}
}
