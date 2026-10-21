package com.life_admin.backend.exceptions;

public class NoOperatorException extends RuntimeException {
	public NoOperatorException() {
		super("You did not provide an operator for this query");
	}
}
