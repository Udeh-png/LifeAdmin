package com.life_admin.backend.exceptions;

public class TokenNotFoundException extends RuntimeException {
	public TokenNotFoundException() {
		super("Refresh token not found");
	}
}
